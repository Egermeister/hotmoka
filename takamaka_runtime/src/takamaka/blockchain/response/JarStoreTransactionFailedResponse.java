package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Stream;

import takamaka.blockchain.TransactionException;
import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

/**
 * A response for a failed transaction that should have installed a jar in the blockchain.
 */
@Immutable
public class JarStoreTransactionFailedResponse extends JarStoreTransactionResponse {

	private static final long serialVersionUID = -8888957484092351352L;

	/**
	 * The exception that justifies why the transaction failed. This is not reported
	 * in the serialization of this response.
	 */
	public final transient TransactionException cause;

	/**
	 * Builds the transaction response.
	 * 
	 * @param cause the exception that justifies why the transaction failed
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreTransactionFailedResponse(TransactionException cause, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForStorage);

		this.cause = cause;
	}
}