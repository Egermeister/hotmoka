package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.internal.MarshallingUtils;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.updates.Update;

/**
 * A response for a transaction that should install a jar in the blockchain.
 * 
 * @param <Outcome> the type of the outcome of the execution having this response
 */
@Immutable
public abstract class JarStoreTransactionResponse implements NonInitialTransactionResponse, TransactionResponseWithGas, TransactionResponseWithUpdates {

	private static final long serialVersionUID = -8888957484092351352L;

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The amount of gas consumed by the transaction for CPU execution.
	 */
	private final BigInteger gasConsumedForCPU;

	/**
	 * The amount of gas consumed by the transaction for RAM allocation.
	 */
	private final BigInteger gasConsumedForRAM;

	/**
	 * The amount of gas consumed by the transaction for storage consumption.
	 */
	private final BigInteger gasConsumedForStorage;

	/**
	 * Builds the transaction response.
	 *
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreTransactionResponse(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		this.updates = updates.toArray(Update[]::new);
		this.gasConsumedForCPU = gasConsumedForCPU;
		this.gasConsumedForRAM = gasConsumedForRAM;
		this.gasConsumedForStorage = gasConsumedForStorage;
	}

	@Override
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof JarStoreTransactionResponse) {
			JarStoreTransactionResponse otherCast = (JarStoreTransactionResponse) other;
			return Arrays.equals(updates, otherCast.updates) && gasConsumedForCPU.equals(otherCast.gasConsumedForCPU)
				&& gasConsumedForRAM.equals(otherCast.gasConsumedForRAM) && gasConsumedForStorage.equals(otherCast.gasConsumedForStorage);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(updates) ^ gasConsumedForCPU.hashCode() ^ gasConsumedForRAM.hashCode() ^ gasConsumedForStorage.hashCode();
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n" + gasToString()
        	+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	/**
	 * Yields a description of the gas consumption.
	 * 
	 * @return the description
	 */
	protected String gasToString() {
		return "  gas consumed for CPU execution: " + gasConsumedForCPU + "\n"
			+ "  gas consumed for RAM allocation: " + gasConsumedForRAM + "\n"
	        + "  gas consumed for storage consumption: " + gasConsumedForStorage + "\n";
	}

	@Override
	public BigInteger gasConsumedForCPU() {
		return gasConsumedForCPU;
	}

	@Override
	public BigInteger gasConsumedForRAM() {
		return gasConsumedForRAM;
	}

	@Override
	public BigInteger gasConsumedForStorage() {
		return gasConsumedForStorage;
	}

	/**
	 * Yields the outcome of the execution having this response, performed
	 * at the given transaction reference.
	 * 
	 * @param transactionReference the transaction reference
	 * @return the outcome
	 * @throws TransactionException if the outcome of the transaction is this exception
	 */
	public abstract TransactionReference getOutcomeAt(TransactionReference transactionReference) throws TransactionException;

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeInt(updates.length);
		for (Update update: updates)
			update.into(oos);

		MarshallingUtils.marshal(gasConsumedForCPU, oos);
		MarshallingUtils.marshal(gasConsumedForRAM, oos);
		MarshallingUtils.marshal(gasConsumedForStorage, oos);
	}
}