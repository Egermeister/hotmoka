package io.hotmoka.beans.responses;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A response for a successful transaction that calls a method
 * in blockchain. The method has been called without problems and
 * without generating exceptions. The method does not return {@code void}.
 */
@Immutable
public class MethodCallTransactionSuccessfulResponse extends MethodCallTransactionResponse implements TransactionResponseWithEvents {

	private static final long serialVersionUID = 2888406427592732867L;

	/**
	 * The return value of the method.
	 */
	public final StorageValue result;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * Builds the transaction response.
	 * 
	 * @param result the value returned by the method
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public MethodCallTransactionSuccessfulResponse(StorageValue result, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.events = events.toArray(StorageReference[]::new);
		this.result = result;
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
        	+ "  returned value: " + result + "\n"
        	+ "  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public StorageValue getOutcome() {
		return result;
	}
}