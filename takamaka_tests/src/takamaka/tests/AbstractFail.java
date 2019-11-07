/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.NonVoidMethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.memory.InitializedMemoryBlockchain;

/**
 * A test for the remote purchase contract.
 */
class AbstractFail extends TakamakaTest {

	private static final ClassType ABSTRACT_FAIL = new ClassType("takamaka.tests.abstractfail.AbstractFail");
	private static final ClassType ABSTRACT_FAIL_IMPL = new ClassType("takamaka.tests.abstractfail.AbstractFailImpl");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"),
			_1_000_000_000, BigInteger.valueOf(100_000L), BigInteger.valueOf(1_000_000L));

		TransactionReference abstractfail = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/abstractfail.jar")), blockchain.takamakaBase));

		classpath = new Classpath(abstractfail, true);
	}

	@Test @DisplayName("new AbstractFail() throws InstantiationException")
	void createAbstractFail() throws TransactionException, CodeExecutionException {
		throwsTransactionExceptionWithCause(InstantiationException.class, () ->
			// cannot instantiate an abstract class
			blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, classpath, new ConstructorSignature(ABSTRACT_FAIL)))
		);
	}

	@Test @DisplayName("new AbstractFailImpl()")
	void createAbstractFailImpl() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, classpath,
			new ConstructorSignature(ABSTRACT_FAIL_IMPL, BasicTypes.INT),
			new IntValue(42)));
	}

	@Test @DisplayName("new AbstractFailImpl().method() yields an AbstractFailImpl")
	void createAbstractFailImplThenCallAbstractMethod() throws TransactionException, CodeExecutionException {
		StorageReference abstractfail = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, classpath,
			new ConstructorSignature(ABSTRACT_FAIL_IMPL, BasicTypes.INT),
			new IntValue(42)));

		StorageReference result = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(blockchain.account(0), _20_000, classpath,
			new NonVoidMethodSignature(ABSTRACT_FAIL, "method", ABSTRACT_FAIL),
			abstractfail));

		assertEquals("takamaka.tests.abstractfail.AbstractFailImpl", result.getClassName(blockchain));
	}
}