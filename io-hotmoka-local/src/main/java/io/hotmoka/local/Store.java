package io.hotmoka.local;

import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;

/**
 * The shared store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. This store is external to the node and, typically, only
 * its hash is held in the node, if consensus is needed. Stores must be thread-safe, since they can
 * be used concurrently for executing more requests.
 */
@ThreadSafe
public interface Store extends AutoCloseable {

	/**
	 * Yields the UTC time that must be used for a transaction, if it is executed
	 * with this state in this moment.
	 * 
	 * @return the UTC time, in the same format as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	long getNow();

	/**
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponse(TransactionReference reference);

	/**
	 * Yields the response of the transaction having the given reference.
	 * The response if returned also when it is not yet committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference);

	/**
	 * Yields the error generated by the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the error, if any
	 */
	Optional<String> getError(TransactionReference reference);

	/**
	 * Yields the history of the given object, that is, the references of the transactions
	 * that provide information about the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history. Yields an empty stream if there is no history for {@code object}
	 */
	Stream<TransactionReference> getHistory(StorageReference object);

	/**
	 * Yields the history of the given object, that is, the references of the transactions
	 * that provide information about the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history. Yields an empty stream if there is no history for {@code object}
	 */
	Stream<TransactionReference> getHistoryUncommitted(StorageReference object);

	/**
	 * Yields the manifest installed when the node is initialized.
	 * 
	 * @return the manifest
	 */
	Optional<StorageReference> getManifest();

	/**
	 * Yields the manifest installed when the node is initialized, also when the
	 * transaction that installed it is not yet committed.
	 * 
	 * @return the manifest
	 */
	Optional<StorageReference> getManifestUncommitted();

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method is called only when
	 * the transaction has been already committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request, if any
	 */
	Optional<TransactionRequest<?>> getRequest(TransactionReference reference);

	/**
	 * Pushes into the store the result of executing a successful Hotmoka request.
	 * This method assumes that the given request was not already present in the store.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	void push(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response);

	/**
	 * Pushes into the store the result of executing a successful Hotmoka request.
	 * This method assumes that the given request was already present in the store.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	void replace(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response);

	/**
	 * Pushes into state the error message resulting from the unsuccessful execution of a Hotmoka request.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param errorMessage the error message
	 */
	void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage);
}