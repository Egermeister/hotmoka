package takamaka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.NonVoidMethodSignature;
import takamaka.blockchain.NonWhiteListedCallException;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.request.StaticMethodCallTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.memory.InitializedMemoryBlockchain;
import takamaka.tests.TakamakaTest;

class IllegalCallToNonWhiteListedMethod7 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"), _1_000_000_000);
	}

	@Test @DisplayName("C.foo()")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		TransactionReference jar = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/illegalcalltononwhitelistedmethod7.jar")), blockchain.takamakaBase));		

		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
				(blockchain.account(0), _20_000, new Classpath(jar, true),
				new NonVoidMethodSignature(new ClassType("takamaka.tests.errors.illegalcalltononwhitelistedmethod7.C"), "foo", ClassType.STRING)))
		);
	}
}