package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import takamaka.blockchain.TransactionResponse;
import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

/**
 * A response for a transaction that should install a jar in the blockchain.
 */
@Immutable
public abstract class JarStoreTransactionResponse implements TransactionResponse, AbstractTransactionResponseWithUpdates {

	private static final long serialVersionUID = -8888957484092351352L;

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The amount of gas consumed by the transaction for CPU execution.
	 */
	public final BigInteger gasConsumedForCPU;

	/**
	 * The amount of gas consumed by the transaction for storage consumption.
	 */
	public final BigInteger gasConsumedForStorage;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreTransactionResponse(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForStorage) {
		this.updates = updates.toArray(Update[]::new);
		this.gasConsumedForCPU = gasConsumedForCPU;
		this.gasConsumedForStorage = gasConsumedForStorage;
	}

	/**
	 * Yields the updates induced by the execution of this trsnaction.
	 * 
	 * @return the updates
	 */
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  gas consumed for CPU execution: " + gasConsumedForCPU + "\n"
        	+ "  gas consumed for storage consumption: " + gasConsumedForStorage + "\n"
        	+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}