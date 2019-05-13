/**
 * 
 */
package takamaka.tests;

import static takamaka.blockchain.types.BasicTypes.INT;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.MethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.lang.RequirementViolationException;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for the remote purchase contract.
 */
class Purchase {

	private static final BigInteger _1_000 = BigInteger.valueOf(1000);

	private static final ClassType PURCHASE = new ClassType("takamaka.tests.remotepurchase.Purchase");

	private static final ConstructorSignature CONSTRUCTOR_PURCHASE = new ConstructorSignature("takamaka.tests.remotepurchase.Purchase", INT);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private Blockchain blockchain;

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference gamete;

	/**
	 * The seller contract.
	 */
	private StorageReference seller;

	/**
	 * The buyer contract.
	 */
	private StorageReference buyer;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference purchase = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/purchase.jar")), takamakaBase));

		classpath = new Classpath(purchase, true);

		seller = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount", INT), new IntValue(1000)));

		buyer = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount", INT), new IntValue(1000)));
	}

	@Test @DisplayName("new Purchase(21)")
	void oddDeposit() throws TransactionException, CodeExecutionException {
		try {
			blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(seller, _1_000, classpath, CONSTRUCTOR_PURCHASE,
				new IntValue(21)));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException)
				return;
		}
	}

	@Test @DisplayName("new Purchase(20)")
	void evenDeposit() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _1_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(18)")
	void buyerCheats() throws TransactionException, CodeExecutionException {
		StorageReference purchase = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _1_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));

		try {
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(buyer, _1_000, classpath, new MethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(18)));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException)
				return;
		}
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20)")
	void buyerHonest() throws TransactionException, CodeExecutionException {
		StorageReference purchase = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _1_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(buyer, _1_000, classpath, new MethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(20)));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmReceived()")
	void confirmReceptionBeforePaying() throws TransactionException, CodeExecutionException {
		StorageReference purchase = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _1_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));

		try {
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(buyer, _1_000, classpath, new MethodSignature(PURCHASE, "confirmReceived"), purchase));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException)
				return;
		}
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20) and then purchase.confirmReception()")
	void buyerPaysAndConfirmReception() throws TransactionException, CodeExecutionException {
		StorageReference purchase = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _1_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(buyer, _1_000, classpath, new MethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(20)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(buyer, _1_000, classpath, new MethodSignature(PURCHASE, "confirmReceived"), purchase));
	}
}