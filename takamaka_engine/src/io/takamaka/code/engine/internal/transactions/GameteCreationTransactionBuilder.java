package io.takamaka.code.engine.internal.transactions;

import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;

public class GameteCreationTransactionBuilder extends AbstractTransactionBuilder<GameteCreationTransactionRequest, GameteCreationTransactionResponse> {
	private final EngineClassLoader classLoader;

	/**
	 * The response computed at the end of the transaction.
	 */
	private final GameteCreationTransactionResponse response;

	public GameteCreationTransactionBuilder(GameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
			this.classLoader = classLoader;

			if (request.initialAmount.signum() < 0)
				throw new IllegalArgumentException("the gamete must be initialized with a non-negative amount of coins");

			// we create an initial gamete ExternallyOwnedContract and we fund it with the initial amount
			GameteThread thread = new GameteThread();
			thread.start();
			thread.join();
			if (thread.exception != null)
				throw thread.exception;

			classLoader.setBalanceOf(thread.gamete, request.initialAmount);
			this.response = new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(thread.gamete)), classLoader.getStorageReferenceOf(thread.gamete));
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t);
		}
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public final GameteCreationTransactionResponse getResponse() {
		return response;
	}

	private class GameteThread extends TakamakaThread {
		private Object gamete;

		private GameteThread() {}

		@Override
		protected void body() throws Exception {
			gamete = classLoader.getExternallyOwnedAccount().getDeclaredConstructor().newInstance();
		}
	}
}