package io.hotmoka.takamaka.beans.responses;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.updates.Update;

/**
 * A response for a request that adds or reduces the coins of an account.
 */
@Immutable
public abstract class MintTransactionResponse extends NonInitialTransactionResponse {

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public MintTransactionResponse(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MintTransactionResponse && super.equals(other);
	}

	/**
	 * Yields the outcome of this response. There is no resulting value,
	 * but this method might throw an exception if the response was a failed one.
	 * 
	 * @throws TransactionException if the outcome of the transaction is that exception
	 */
	public abstract void getOutcome() throws TransactionException;
}