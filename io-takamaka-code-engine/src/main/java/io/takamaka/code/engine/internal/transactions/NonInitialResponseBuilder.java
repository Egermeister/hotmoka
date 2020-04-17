package io.takamaka.code.engine.internal.transactions;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.OutOfGasError;
import io.takamaka.code.engine.AbstractNode;

/**
 * The creator of the response for a non-initial transaction. Non-initial transactions consume gas.
 */
public abstract class NonInitialResponseBuilder<Request extends NonInitialTransactionRequest<Response>, Response extends NonInitialTransactionResponse> extends AbstractResponseBuilder<Request, Response> {

	/**
	 * The cost model of the node for which the transaction is being built.
	 */
	protected final GasCostModel gasCostModel;

	/**
	 * Creates a the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be built
	 */
	protected NonInitialResponseBuilder(Request request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);

		try {
			callerMustBeExternallyOwnedAccount();
			this.gasCostModel = node.getGasCostModel();

			if (request.gasLimit.compareTo(minimalGasRequiredForTransaction()) < 0)
				throw new TransactionRejectedException("not enough gas to start the transaction");
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	/**
	 * Computes a minimal threshold of gas that is required for the transaction.
	 * Below this threshold, the response builder cannot be created.
	 * 
	 * @return the minimal threshold
	 */
	protected BigInteger minimalGasRequiredForTransaction() {
		BigInteger result = gasCostModel.cpuBaseTransactionCost();
		result = result.add(sizeCalculator.sizeOfRequest(request));
		result = result.add(gasForStoringFailedResponse());
		result = result.add(classLoader.getLengthsOfJars().mapToObj(gasCostModel::cpuCostForLoadingJar).reduce(ZERO, BigInteger::add));
		result = result.add(classLoader.getLengthsOfJars().mapToObj(gasCostModel::ramCostForLoadingJar).reduce(ZERO, BigInteger::add));
		result = result.add(classLoader.getTransactionsOfJars().map(gasCostModel::cpuCostForGettingResponseAt).reduce(ZERO, BigInteger::add));

		return result;
	}

	/**
	 * Checks if the caller is an externally owned account or subclass.
	 * @throws Exception 
	 * 
	 * @throws IllegalArgumentException if the caller is not an externally owned account
	 */
	private void callerMustBeExternallyOwnedAccount() throws Exception {
		ClassTag classTag = node.getClassTagOf(request.caller, i -> {});
		Class<?> clazz = classLoader.loadClass(classTag.className);
		if (!classLoader.getExternallyOwnedAccount().isAssignableFrom(clazz)
				&& !classLoader.getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
			throw new TransactionRejectedException("only an externally owned account can start a transaction");
	}

	/**
	 * Yields the cost for storage a failed response for the transaction that is being built.
	 * 
	 * @return the cost
	 */
	protected abstract BigInteger gasForStoringFailedResponse();

	protected abstract class ResponseCreator extends AbstractResponseBuilder<Request, Response>.ResponseCreator {

		/**
		 * The deserialized caller.
		 */
		private final Object deserializedCaller;

		/**
		 * A stack of available gas. When a sub-computation is started
		 * with a subset of the available gas, the latter is taken away from
		 * the current available gas and pushed on top of this stack.
		 */
		private final LinkedList<BigInteger> oldGas = new LinkedList<>();

		/**
		 * The remaining amount of gas for the current transaction, not yet consumed.
		 */
		private BigInteger gas;

		/**
		 * The amount of gas consumed for CPU execution.
		 */
		private BigInteger gasConsumedForCPU = BigInteger.ZERO;

		/**
		 * The amount of gas consumed for RAM allocation.
		 */
		private BigInteger gasConsumedForRAM = BigInteger.ZERO;

		/**
		 * The amount of gas consumed for storage consumption.
		 */
		private BigInteger gasConsumedForStorage = BigInteger.ZERO;

		protected ResponseCreator(TransactionReference current) throws TransactionRejectedException {
			super(current);

			try {
				this.gas = request.gasLimit;

				chargeGasForCPU(gasCostModel.cpuBaseTransactionCost());
				chargeGasForStorage(sizeCalculator.sizeOfRequest(request));
				chargeGasForClassLoader();

				this.deserializedCaller = deserializer.deserialize(request.caller);

				if (!(NonInitialResponseBuilder.this instanceof ViewResponseBuilder))
					callerAndRequestMustAgreeOnNonce();

				sellAllGasToCaller();

				if (!(NonInitialResponseBuilder.this instanceof ViewResponseBuilder))
					increaseNonceOfCaller();
			}
			catch (Throwable t) {
				throw new TransactionRejectedException(t);
			}
		}

		/**
		 * Yields the deserialized caller of the transaction.
		 * 
		 * @return the deserialized caller
		 */
		protected final Object getDeserializedCaller() {
			return deserializedCaller;
		}

		/**
		 * Yields the amount of gas consumed for CPU execution.
		 * 
		 * @return the amount of gas
		 */
		protected final BigInteger gasConsumedForCPU() {
			return gasConsumedForCPU;
		}

		/**
		 * Yields the amount of gas consumed for RAM allocation.
		 * 
		 * @return the amount of gas
		 */
		protected final BigInteger gasConsumedForRAM() {
			return gasConsumedForRAM;
		}

		/**
		 * Yields the amount of gas consumed for storage consumption.
		 * 
		 * @return the amount of gas
		 */
		protected final BigInteger gasConsumedForStorage() {
			return gasConsumedForStorage;
		}

		/**
		 * Yields the gas that would be paid if the transaction fails.
		 * 
		 * @return the gas for penalty, computed as the total initial gas minus
		 *         the gas already consumed for PCU, for RAM and for storage
		 */
		protected final BigInteger gasConsumedForPenalty() {
			return request.gasLimit.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
		}

		/**
		 * Reduces the remaining amount of gas. It performs a task at the end.
		 * 
		 * @param amount the amount of gas to consume
		 * @param forWhat the task performed at the end, for the amount of gas to consume
		 */
		private void charge(BigInteger amount, Consumer<BigInteger> forWhat) {
			if (amount.signum() < 0)
				throw new IllegalArgumentException("gas cannot increase");

			// gas can be negative only if it was initialized so; this special case is
			// used for the creation of the gamete, when gas should not be counted
			if (gas.signum() < 0)
				return;

			if (gas.compareTo(amount) < 0)
				// we report how much gas is missing
				throw new OutOfGasError();
		
			gas = gas.subtract(amount);
			forWhat.accept(amount);
		}

		/**
		 * Decreases the available gas by the given amount, for storage allocation.
		 * 
		 * @param amount the amount of gas to consume
		 */
		private void chargeGasForStorage(BigInteger amount) {
			charge(amount, x -> gasConsumedForStorage = gasConsumedForStorage.add(x));
		}

		/**
		 * Decreases the available gas for the given response, for storage allocation.
		 * 
		 * @param response the response
		 */
		protected final void chargeGasForStorageOf(Response response) {
			chargeGasForStorage(sizeCalculator.sizeOfResponse(response));
		}

		@Override
		public final void chargeGasForCPU(BigInteger amount) {
			charge(amount, x -> gasConsumedForCPU = gasConsumedForCPU.add(x));
		}

		@Override
		public final void chargeGasForRAM(BigInteger amount) {
			charge(amount, x -> gasConsumedForRAM = gasConsumedForRAM.add(x));
		}

		/**
		 * Charges gas proportional to the complexity of the class loader that has been created.
		 */
		protected final void chargeGasForClassLoader() {
			classLoader.getLengthsOfJars().mapToObj(gasCostModel::cpuCostForLoadingJar).forEach(this::chargeGasForCPU);
			classLoader.getLengthsOfJars().mapToObj(gasCostModel::ramCostForLoadingJar).forEach(this::chargeGasForRAM);
			classLoader.getTransactionsOfJars().map(gasCostModel::cpuCostForGettingResponseAt).forEach(this::chargeGasForCPU);
		}

		/**
		 * Collects all updates to the balance or nonce of the caller of the transaction.
		 * 
		 * @return the updates
		 */
		protected final Stream<Update> updatesToBalanceOrNonceOfCaller() {
			return updatesExtractor.extractUpdatesFrom(Stream.of(deserializedCaller))
				.filter(update -> update.object.equals(request.caller))
				.filter(update -> update instanceof UpdateOfField)
				.filter(update -> ((UpdateOfField) update).getField().equals(FieldSignature.BALANCE_FIELD)
						|| ((UpdateOfField) update).getField().equals(FieldSignature.EOA_NONCE_FIELD)
						|| ((UpdateOfField) update).getField().equals(FieldSignature.RGEOA_NONCE_FIELD));
		}

		/**
		 * Computes the cost of the given units of gas.
		 * 
		 * @param gas the units of gas
		 * @return the cost, as {@code gas} times {@code gasPrice}
		 */
		private BigInteger costOf(BigInteger gas) {
			return gas.multiply(request.gasPrice);
		}

		/**
		 * Buys back the remaining gas to the caller of the transaction.
		 */
		protected final void payBackAllRemainingGasToCaller() {
			classLoader.setBalanceOf(deserializedCaller, classLoader.getBalanceOf(deserializedCaller).add(costOf(gas)));
		}

		@Override
		public final <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
			chargeGasForCPU(amount);
			oldGas.addFirst(gas);
			amount.hashCode();
			gas = amount;
		
			try {
				return what.call();
			}
			finally {
				gas = gas.add(oldGas.removeFirst());
			}
		}

		/**
		 * Checks if the caller has the same nonce as the request.
		 * 
		 * @throws IllegalArgumentException if the nonce of the caller is not equal to that in {@code request}
		 */
		private void callerAndRequestMustAgreeOnNonce() {
			BigInteger expected = classLoader.getNonceOf(deserializedCaller);
			if (!expected.equals(request.nonce))
				throw new IllegalArgumentException("incorrect nonce: the request reports " + request.nonce + " but the account contains " + expected);
		}

		/**
		 * Sets the nonce to the value successive to that in the request.
		 */
		private void increaseNonceOfCaller() {
			classLoader.setNonceOf(deserializedCaller, request.nonce.add(BigInteger.ONE));
		}

		/**
		 * Sells to the caller of the transaction all gas promised for the transaction.
		 * 
		 * @throws IllegalStateException if the caller has not enough money to buy the promised gas
		 */
		private void sellAllGasToCaller() {
			BigInteger balance = classLoader.getBalanceOf(deserializedCaller);
			BigInteger cost = costOf(request.gasLimit);

			if (balance.subtract(cost).signum() < 0)
				throw new IllegalStateException("caller has not enough funds to buy " + request.gasLimit + " units of gas");

			classLoader.setBalanceOf(deserializedCaller, balance.subtract(cost));
		}
	}
}