package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.EngineClassLoader;
import io.takamaka.code.engine.NonInitialResponseBuilder;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

/**
 * The creator of a response for a transaction that installs a jar in the node.
 */
public class JarStoreResponseBuilder extends NonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse> {

	/**
	 * The response computed with this builder.
	 */
	private final JarStoreTransactionResponse response;

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreResponseBuilder(TransactionReference reference, JarStoreTransactionRequest request, AbstractNode<?,?> node) throws TransactionRejectedException {
		super(reference, request, node);

		this.response = new ResponseCreator().create();
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws Exception {
		// we redefine this method, since the class loader must be able to access the
		// jar that is being installed and its dependencies, in order to instrument them
		return new EngineClassLoader(request.getJar(), request.getDependencies(), node);
	}

	@Override
	protected BigInteger minimalGasRequiredForTransaction() {
		int jarLength = request.getJarLength();
		BigInteger result = super.minimalGasRequiredForTransaction();
		result = result.add(gasCostModel.cpuCostForInstallingJar(jarLength));
		result = result.add(gasCostModel.ramCostForInstallingJar(jarLength));

		return result;
	}

	@Override
	protected final BigInteger gasForStoringFailedResponse() {
		BigInteger gas = request.gasLimit;
		return new JarStoreTransactionFailedResponse("placeholder for the name of the exception", "placeholder for the message of the exception", Stream.empty(), gas, gas, gas, gas).size(gasCostModel);
	}

	@Override
	public JarStoreTransactionResponse getResponse() {
		return response;
	}

	private class ResponseCreator extends NonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse>.ResponseCreator {
		
		private ResponseCreator() throws TransactionRejectedException {
		}

		@Override
		protected JarStoreTransactionResponse body() throws Exception {
			int jarLength = request.getJarLength();
			chargeGasForCPU(gasCostModel.cpuCostForInstallingJar(jarLength));
			chargeGasForRAM(gasCostModel.ramCostForInstallingJar(jarLength));
		
			try {
				VerifiedJar verifiedJar = VerifiedJar.of(request.getJar(), classLoader, false);
				InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasCostModel);
				byte[] instrumentedBytes = instrumentedJar.toBytes();
				chargeGasForStorageOf(new JarStoreTransactionSuccessfulResponse(instrumentedBytes, request.getDependencies(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundPayerForAllRemainingGas();
				return new JarStoreTransactionSuccessfulResponse(instrumentedBytes, request.getDependencies(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				// we do not pay back the gas
				return new JarStoreTransactionFailedResponse(t.getClass().getName(), t.getMessage(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
		}

		@Override
		public void event(Object event) {
		}
	}
}