package io.hotmoka.beans.responses;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.values.StorageValue;

/**
 * A response for a failed transaction that should have called a method in blockchain.
 */
@Immutable
public class MethodCallTransactionFailedResponse extends MethodCallTransactionResponse implements TransactionResponseFailed {

	private static final long serialVersionUID = -4635934226304384321L;

	/**
	 * The update of balance of the caller of the transaction, for paying for the transaction.
	 */
	private final UpdateOfBalance callerBalanceUpdate;

	/**
	 * The amount of gas consumed by the transaction as penalty for the failure.
	 */
	private final BigInteger gasConsumedForPenalty;

	/**
	 * The fully-qualified class name of the cause exception.
	 */
	public final String classNameOfCause;

	/**
	 * The message of the cause exception. This might be {@code null}.
	 */
	public final String messageOfCause;

	/**
	 * Builds the transaction response.
	 * 
	 * @param cause the exception that justifies why the transaction failed
	 * @param callerBalanceUpdate the update of balance of the caller of the transaction, for paying for the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 */
	public MethodCallTransactionFailedResponse(Throwable cause, UpdateOfBalance callerBalanceUpdate, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		super(gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.callerBalanceUpdate = callerBalanceUpdate;
		this.gasConsumedForPenalty = gasConsumedForPenalty;
		this.classNameOfCause = cause == null ? "<unknown exception>" : cause.getClass().getName();
		this.messageOfCause = cause == null ? "<unknown message>" : cause.getMessage();
	}

	@Override
	protected String gasToString() {
		return super.gasToString() + "  gas consumed for penalty: " + gasConsumedForPenalty + "\n";
	}

	@Override
	public BigInteger gasConsumedForPenalty() {
		return gasConsumedForPenalty;
	}

	@Override
	public String getClassNameOfCause() {
		return classNameOfCause;
	}

	@Override
	public String getMessageOfCause() {
		return messageOfCause;
	}

	@Override
	public Stream<Update> getUpdates() {
		return Stream.of(callerBalanceUpdate);
	}

	@Override
	public String toString() {
        return super.toString()
        	+ "\n  cause: " + classNameOfCause + ":" + messageOfCause;
	}

	@Override
	public StorageValue getOutcome() throws TransactionException {
		throw new TransactionException(classNameOfCause + ": " + messageOfCause);
	}
}