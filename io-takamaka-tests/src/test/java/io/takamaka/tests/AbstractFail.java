/**
 * 
 */
package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.nodes.CodeExecutionException;

/**
 * A test for the remote purchase contract.
 */
class AbstractFail extends TakamakaTest {

	private static final ClassType ABSTRACT_FAIL = new ClassType("io.takamaka.tests.abstractfail.AbstractFail");
	private static final ClassType ABSTRACT_FAIL_IMPL = new ClassType("io.takamaka.tests.abstractfail.AbstractFailImpl");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private MemoryBlockchain blockchain;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = mkMemoryBlockchain(_1_000_000_000, BigInteger.valueOf(100_000L), BigInteger.valueOf(1_000_000L));

		TransactionReference abstractfail = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("abstractfail.jar"), blockchain.takamakaCode()));

		classpath = new Classpath(abstractfail, true);
	}

	@Test @DisplayName("new AbstractFail() throws InstantiationException")
	void createAbstractFail() throws TransactionException, CodeExecutionException {
		throwsTransactionExceptionWithCause(InstantiationException.class, () ->
			// cannot instantiate an abstract class
			blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature(ABSTRACT_FAIL)))
		);
	}

	@Test @DisplayName("new AbstractFailImpl()")
	void createAbstractFailImpl() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath,
			new ConstructorSignature(ABSTRACT_FAIL_IMPL, BasicTypes.INT),
			new IntValue(42)));
	}

	@Test @DisplayName("new AbstractFailImpl().method() yields an AbstractFailImpl")
	void createAbstractFailImplThenCallAbstractMethod() throws TransactionException, CodeExecutionException {
		StorageReference abstractfail = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath,
			new ConstructorSignature(ABSTRACT_FAIL_IMPL, BasicTypes.INT),
			new IntValue(42)));

		StorageReference result = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(ABSTRACT_FAIL, "method", ABSTRACT_FAIL),
			abstractfail));

		assertEquals("io.takamaka.tests.abstractfail.AbstractFailImpl", blockchain.getClassNameOf(result));
	}
}