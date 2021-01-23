package io.takamaka.code.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.InitializationTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;

/**
 * Shared implementation of the store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. This store is external to the node and, typically, only
 * its hash is stored in the node, if consensus is needed.
 */
@ThreadSafe
public abstract class AbstractStore<C extends Config> implements Store {
	protected final static Logger logger = LoggerFactory.getLogger(AbstractStore.class);

	/**
	 * The time spent inside the state procedures, for profiling.
	 */
	private final AtomicLong timeSpent = new AtomicLong();

	/**
	 * The lock for modifications of the store.
	 */
	protected final Object lock = new Object();

	/**
	 * The configuration of the node having this store.
	 */
	protected final C config;

	/**
	 * Builds the store for a node.
	 * 
	 * @param config the configuration of the node having this store
	 */
	protected AbstractStore(C config) {
		this.config = config;
	}

	/**
	 * Builds a clone of the given store.
	 * 
	 * @param parent the store to clone
	 */
	protected AbstractStore(AbstractStore<? extends C> parent) {
		this.config = parent.config;
	}

	@Override
	public void close() {
		logger.info("Time spent in state procedures: " + timeSpent + "ms");
	}

	@Override
	public final void push(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		synchronized (lock) {
			setResponse(reference, request, response);

			if (response instanceof TransactionResponseWithUpdates)
				expandHistory(reference, (TransactionResponseWithUpdates) response);

			if (response instanceof InitializationTransactionResponse) {
				StorageReference manifest = ((InitializationTransactionRequest) request).manifest;
				setManifest(manifest);
				logger.info(manifest + ": set as manifest");
				logger.info("the node has been initialized");
			}

			if (response instanceof GameteCreationTransactionResponse)
				logger.info(((GameteCreationTransactionResponse) response).gamete + ": created as gamete");
		}
	}

	@Override
	public final void replace(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		synchronized (lock) {
			setResponse(reference, request, response);
		}
	}

	/**
	 * Writes in store the given request and response for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param request the request
	 * @param response the response
	 */
	protected abstract void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response);

	/**
	 * Sets the history of the given object, that is,
	 * the references to the transactions that provide information about
	 * its current state, in reverse chronological order (from newest to oldest).
	 * 
	 * @param object the object whose history is set
	 * @param history the stream that will become the history of the object,
	 *                replacing its previous history
	 */
	protected abstract void setHistory(StorageReference object, Stream<TransactionReference> history);

	/**
	 * Mark the node as initialized. This happens for initialization requests.
	 * 
	 * @param manifest the manifest to put in the node
	 */
	protected abstract void setManifest(StorageReference manifest);

	/**
	 * Executes the given task, taking note of the time required for it.
	 * 
	 * @param task the task
	 */
	protected final void recordTime(Runnable task) {
		long start = System.currentTimeMillis();
		task.run();
		timeSpent.addAndGet(System.currentTimeMillis() - start);
	}

	/**
	 * Executes the given task, taking note of the time required for it.
	 * 
	 * @param task the task
	 */
	protected final <T> T recordTime(Supplier<T> task) {
		long start = System.currentTimeMillis();
		T result = task.get();
		timeSpent.addAndGet(System.currentTimeMillis() - start);
		return result;
	}

	/**
	 * Executes the given task, taking note of the time required for it,
	 * inside a synchronized block on {@link #lock}.
	 * 
	 * @param task the task
	 */
	protected final <T> T recordTimeSynchronized(Supplier<T> task) {
		long start = System.currentTimeMillis();

		T result;
		synchronized (lock) {
			result = task.get();
		}

		timeSpent.addAndGet(System.currentTimeMillis() - start);
		return result;
	}

	/**
	 * Process the updates contained in the given response, expanding the history of the affected objects.
	 * 
	 * @param reference the transaction that has generated the given response
	 * @param response the response
	 */
	private void expandHistory(TransactionReference reference, TransactionResponseWithUpdates response) {
		// we collect the storage references that have been updated in the response; for each of them,
		// we fetch the list of the transaction references that affected them in the past, we add the new transaction reference
		// in front of such lists and store back the updated lists, replacing the old ones
		response.getUpdates()
			.map(Update::getObject)
			.distinct()
			.forEachOrdered(object -> setHistory(object, simplifiedHistory(object, reference, response.getUpdates())));
	}

	/**
	 * Adds the given transaction reference to the history of the given object and yields the simplified
	 * history. Simplification means that some elements of the previous history might not be useful anymore,
	 * since they get shadowed by the updates in the added transaction reference. This occurs when the values
	 * of some fields are updated in {@code added} and the useless old history element provided only values
	 * for the newly updated fields.
	 * 
	 * @param object the object whose history is being simplified
	 * @param added the transaction reference to add in front of the history of {@code object}
	 * @param addedUpdates the updates generated in {@code added}
	 * @return the simplified history, with {@code added} in front followed by a subset of {@code old}
	 */
	private Stream<TransactionReference> simplifiedHistory(StorageReference object, TransactionReference added, Stream<Update> addedUpdates) {
		Stream<TransactionReference> old = getHistoryUncommitted(object);

		// we trace the set of updates that are already covered by previous transactions, so that
		// subsequent history elements might become unnecessary, since they do not add any yet uncovered update
		Set<Update> covered = addedUpdates.filter(update -> update.getObject() == object).collect(Collectors.toSet());
		List<TransactionReference> simplified = new ArrayList<>();
		simplified.add(added);
	
		TransactionReference[] oldAsArray = old.toArray(TransactionReference[]::new);
		int length = oldAsArray.length;
		for (int pos = 0; pos < length - 1; pos++)
			addIfUncovered(oldAsArray[pos], object, covered, simplified);
	
		// the last is always useful, since it contains at least the class tag of the object
		if (length >= 1)
			simplified.add(oldAsArray[length - 1]);
	
		return simplified.stream();
	}

	/**
	 * Adds the given transaction reference to the history of the given object,
	 * if it provides updates for fields that have not yet been covered by other updates.
	 * 
	 * @param reference the transaction reference
	 * @param object the object
	 * @param covered the set of updates for the already covered fields
	 * @param history the history; this might be modified by the method, by prefixing {@code reference} at its front
	 */
	private void addIfUncovered(TransactionReference reference, StorageReference object, Set<Update> covered, List<TransactionReference> history) {
		Optional<TransactionResponse> response = getResponseUncommitted(reference);

		if (response.isEmpty()) {
			logger.error("history contains a reference to a transaction not in store");
			throw new InternalFailureException("history contains a reference to a transaction not in store");
		}

		if (!(response.get() instanceof TransactionResponseWithUpdates)) {
			logger.error("history contains a reference to a transaction without updates");
			throw new InternalFailureException("history contains a reference to a transaction without updates");
		}

		// we check if there is at least an update for a field of the object
		// that is not yet covered by another update in a previous element of the history
		Set<Update> diff = ((TransactionResponseWithUpdates) response.get()).getUpdates()
			.filter(update -> update.getObject().equals(object))
			// in the past history, but the last response, only UpdateOfField's can occur
			.map(update -> (UpdateOfField) update)
			.filter(update -> !isAlreadyIn(update, covered))
			.collect(Collectors.toSet());

		if (!diff.isEmpty()) {
			// the transaction reference actually adds at least one useful update
			history.add(reference);
			covered.addAll(diff);
		}
	}

	/**
	 * Determines if the given set of updates contains an update for the
	 * same object and field as the given update.
	 * 
	 * @param update the given update
	 * @param updates the set
	 * @return true if and only if that condition holds
	 */
	private static boolean isAlreadyIn(UpdateOfField update, Set<Update> updates) { // TODO: repetition of code in StoreUtilitiesImpl
		FieldSignature field = update.getField();
		return updates.stream()
			.filter(_update -> _update instanceof UpdateOfField)
			.map(_update -> (UpdateOfField) _update)
			.map(UpdateOfField::getField)
			.anyMatch(field::equals);
	}
}