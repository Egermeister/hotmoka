package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.GasCosts;
import takamaka.blockchain.Update;
import takamaka.blockchain.values.StorageReference;

/**
 * A response for a successful transaction that calls a constructor of a storage
 * class in blockchain. The constructor is annotated as {@link io.takamaka.code.lang.ThrowsExceptions}.
 * It has been called without problems but it threw an instance of {@link java.lang.Exception}.
 */
@Immutable
public class ConstructorCallTransactionExceptionResponse extends ConstructorCallTransactionResponse implements TransactionResponseWithEvents {

	private static final long serialVersionUID = -1571448149485752630L;

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * The exception that has been thrown by the constructor.
	 */
	public final transient Exception exception;

	/**
	 * Builds the transaction response.
	 * 
	 * @param exception the exception that has been thrown by the constructor
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public ConstructorCallTransactionExceptionResponse(Exception exception, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.updates = updates.toArray(Update[]::new);
		this.events = events.toArray(StorageReference[]::new);
		this.exception = exception;
	}

	@Override
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public String toString() {
        return super.toString() + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.STORAGE_COST_PER_SLOT).add(GasCosts.STORAGE_COST_PER_SLOT)
			.add(getEvents().map(StorageReference::size).reduce(BigInteger.ZERO, BigInteger::add));
	}
}