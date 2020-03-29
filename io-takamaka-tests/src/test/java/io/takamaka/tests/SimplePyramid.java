/**
 * 
 */
package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.MemoryBlockchain;

/**
 * A test for the simple pyramid contract.
 */
class SimplePyramid extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final BigIntegerValue MINIMUM_INVESTMENT = new BigIntegerValue(BigInteger.valueOf(10_000L));

	private static final ClassType SIMPLE_PYRAMID = new ClassType("io.takamaka.tests.ponzi.SimplePyramid");

	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = new ConstructorSignature(SIMPLE_PYRAMID, ClassType.BIG_INTEGER);

	private static final MethodSignature INVEST = new VoidMethodSignature(SIMPLE_PYRAMID, "invest", ClassType.BIG_INTEGER);

	private static final MethodSignature GET_BALANCE = new NonVoidMethodSignature(ClassType.TEOA, "getBalance", ClassType.BIG_INTEGER);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private MemoryBlockchain blockchain;

	/**
	 * The four participants to the pyramid.
	 */
	private StorageReference[] players = new StorageReference[4];

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = mkMemoryBlockchain(BigInteger.valueOf(200_000L), BigInteger.valueOf(200_000L), BigInteger.valueOf(200_000L), BigInteger.valueOf(200_000L));
		players[0] = blockchain.account(0);
		players[1] = blockchain.account(1);
		players[2] = blockchain.account(2);
		players[3] = blockchain.account(3);

		TransactionReference ponzi = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(players[0], _20_000, BigInteger.ZERO, blockchain.takamakaCode(),
			bytesOf("ponzi.jar"), blockchain.takamakaCode()));

		classpath = new Classpath(ponzi, true);
	}

	@Test @DisplayName("two investors do not get investment back yet")
	void twoInvestors() throws TransactionException, CodeExecutionException {
		StorageReference pyramid = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(players[0], _10_000, BigInteger.ZERO, classpath, CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[1], _10_000, BigInteger.ZERO, classpath, INVEST, pyramid, MINIMUM_INVESTMENT));
		BigIntegerValue balance0 = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[0], _10_000, BigInteger.ZERO, classpath, GET_BALANCE, players[0]));
		assertTrue(balance0.value.compareTo(BigInteger.valueOf(190_000)) <= 0);
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException {
		StorageReference pyramid = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(players[0], _10_000, BigInteger.ZERO, classpath, CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[1], _10_000, BigInteger.ZERO, classpath, INVEST, pyramid, MINIMUM_INVESTMENT));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[2], _20_000, BigInteger.ZERO, classpath, INVEST, pyramid, MINIMUM_INVESTMENT));
		BigIntegerValue balance0 = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[0], _10_000, BigInteger.ZERO, classpath, GET_BALANCE, players[0]));
		assertTrue(balance0.value.compareTo(BigInteger.valueOf(201_000)) >= 0);
	}
}