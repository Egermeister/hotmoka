package io.takamaka.code.tests;

/**
 * MODIFY AT LINE 130 TO SELECT THE NODE IMPLEMENTATION TO TEST.
 */
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.requests.TransferTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.Node.CodeSupplier;
import io.hotmoka.nodes.Node.JarSupplier;
import io.hotmoka.nodes.NodeWithRequestsAndResponses;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.nodes.views.NodeWithAccounts;
import io.hotmoka.nodes.views.NodeWithJars;
import io.hotmoka.takamaka.DeltaGroupExecutionResult;
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.AbstractNodeWithRequestsAndResponses;
import io.takamaka.code.verification.VerificationException;

public abstract class TakamakaTest {

	/**
	 * The node that gets created before starting running the tests.
	 * This node will hence be created only once and
	 * each test will decorate it into {@linkplain #nodeWithAccountsView},
	 * with the addition of the jar and accounts that the test needs.
	 */
	private final static Node originalView;

	/**
	 * An initialized view of {@linkplain #originalView}.
	 */
	private final static InitializedNode initializedView;

	/**
	 * The signature algorithm used for signing the requests.
	 */
	private static SignatureAlgorithm<NonInitialTransactionRequest<?>> signature;

	/**
	 * The node under test. This is a view of {@linkplain #initializedView},
	 * with the addition of some jars for testing, recreated before each test.
	 */
	protected NodeWithJars nodeWithJarsView;

	/**
	 * The node under test. This is a view of {@linkplain #initializedView},
	 * with the addition of some initial accounts, recreated before each test.
	 */
	protected NodeWithAccounts nodeWithAccountsView;

	/**
	 * The nonce of each externally owned account used in the test.
	 */
	private final Map<StorageReference, BigInteger> nonces = new HashMap<>();

	/**
	 * The chain identifier of the node used for the tests.
	 */
	protected final static String chainId;

	/**
	 * The version of the project, as stated in the pom file.
	 */
	private final static String version;

	private final static Logger logger = LoggerFactory.getLogger(AbstractNodeWithRequestsAndResponses.class);

	@BeforeEach
	void logTestName(TestInfo testInfo) {
		logger.info("**** Starting test " + testInfo.getTestClass().get().getSimpleName() + '.' + testInfo.getTestMethod().get().getName() + ": " + testInfo.getDisplayName());
	}

	public interface TestBody {
		public void run() throws Exception;
	}

	static {
		try {
			// we access the project.version property from the pom.xml file of the parent project
			MavenXpp3Reader reader = new MavenXpp3Reader();
	        Model model = reader.read(new FileReader("../pom.xml"));
	        version = (String) model.getProperties().get("project.version");
	        chainId = TakamakaTest.class.getName();

	        // Change this to test with different node implementations
	        originalView = testWithMemoryBlockchain();
	        //originalView = testWithTendermintBlockchain();
	        //originalView = testWithTakamakaBlockchainExecuteOneByOne();
	        //originalView = testWithTakamakaBlockchainExecuteAtEachTimeslot();

			// the gamete has both red and green coins, enough for all tests
			initializedView = InitializedNode.of
				(originalView, Paths.get("../modules/explicit/io-takamaka-code-" + version + ".jar"),
				Constants.MANIFEST_NAME, chainId, BigInteger.valueOf(999_999_999).pow(5), BigInteger.valueOf(999_999_999).pow(5));
			signature = originalView.getSignatureAlgorithmForRequests();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ExceptionInInitializerError(e);
		}
	}

	private static Node testWithTendermintBlockchain() {
		io.hotmoka.tendermint.Config config = new io.hotmoka.tendermint.Config.Builder().build();
		return io.hotmoka.tendermint.TendermintBlockchain.of(config);
	}

	private static Node testWithMemoryBlockchain() {
		io.hotmoka.memory.Config config = new io.hotmoka.memory.Config.Builder().build();
		return io.hotmoka.memory.MemoryBlockchain.of(config);
	}

	private static Node testWithTakamakaBlockchainExecuteOneByOne() {
		io.hotmoka.takamaka.Config config = new io.hotmoka.takamaka.Config.Builder().build();
		return io.hotmoka.takamaka.TakamakaBlockchain.simulation(config, TakamakaTest::postTransactionTakamakaBlockchainRequestsOneByOne);
	}

	private static Node testWithTakamakaBlockchainExecuteAtEachTimeslot() {
		io.hotmoka.takamaka.Config config = new io.hotmoka.takamaka.Config.Builder().build();
		List<TransactionRequest<?>> mempool = new ArrayList<>();

		// we provide an implementation of postTransaction() that just adds the request in the mempool
		TakamakaBlockchain node = io.hotmoka.takamaka.TakamakaBlockchain.simulation(config,
			(_node, request) -> {
				synchronized (mempool) {
					mempool.add(request);
				}
			}
		);

		// we start a scheduler that checks the mempool every timeslot to see if there are requests to execute
		Thread scheduler = new Thread() {

			@Override
			public void run() {
				byte[] hash = null;

				while (true) {
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {}

					// we check if a previous execute() is still running,
					// since we cannot run two execute() at the same time
					if (node.getCurrentExecutionId().isEmpty()) {
						Stream<TransactionRequest<?>> requests;

						synchronized (mempool) {
							int mempoolSize = mempool.size();
							if (mempoolSize == 0)
								// it is possible, but useless, to start an empty execute()
								continue;

							// the clone of the mempool is needed or otherwise a concurrent modification exception might occur later
							requests = new ArrayList<>(mempool).stream();
							mempool.clear();
						}

						DeltaGroupExecutionResult result = node.execute(hash, System.currentTimeMillis(), requests, "id");
						hash = result.getHash();
						node.checkOut(hash);
					}
				}
			}
		};

		scheduler.start();

		logger.info("scheduled mempool check every 100 milliseconds");
		return node;
	}

	private static byte[] hash; // used for the simulation of the Takamaka blockchain only

	/**
	 * This simulates the implementation of postTransaction() in such a way to put
	 * each request in a distinct delta group.
	 * 
	 * @param node the Takamaka blockchain
	 * @param request the request
	 */
	private static void postTransactionTakamakaBlockchainRequestsOneByOne(TakamakaBlockchain node, TransactionRequest<?> request) {
		DeltaGroupExecutionResult result = node.execute(hash, System.currentTimeMillis(), Stream.of(request), "id");
		hash = result.getHash();
		node.checkOut(hash);
	}

	protected final void setNode(BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithJarsView = null;
		nodeWithAccountsView = NodeWithAccounts.of(initializedView, initializedView.keysOfGamete().getPrivate(), coins);
	}

	protected final void setNodeRedGreen(BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithJarsView = null;
		nodeWithAccountsView = NodeWithAccounts.ofRedGreen(initializedView, initializedView.keysOfGamete().getPrivate(), coins);
	}

	protected final void setNode(String jar, BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithJarsView = NodeWithJars.of(initializedView, initializedView.keysOfGamete().getPrivate(), pathOfExample(jar));
		nodeWithAccountsView = NodeWithAccounts.of(initializedView, initializedView.keysOfGamete().getPrivate(), coins);
	}

	protected final void setNodeRedGreen(String jar, BigInteger... coins) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		nodeWithJarsView = NodeWithJars.of(initializedView, initializedView.keysOfGamete().getPrivate(), pathOfExample(jar));
		nodeWithAccountsView = NodeWithAccounts.ofRedGreen(initializedView, initializedView.keysOfGamete().getPrivate(), coins);
	}

	protected final TransactionReference takamakaCode() {
		return nodeWithAccountsView.getTakamakaCode();
	}

	protected final TransactionReference jar() {
		return nodeWithJarsView.jar(0);
	}

	protected final StorageReference account(int i) {
		return nodeWithAccountsView.account(i);
	}

	protected final PrivateKey privateKey(int i) {
		return nodeWithAccountsView.privateKey(i);
	}

	protected final SignatureAlgorithm<NonInitialTransactionRequest<?>> signature() throws NoSuchAlgorithmException {
		return nodeWithAccountsView.getSignatureAlgorithmForRequests();
	}

	protected final TransactionRequest<?> getRequestAt(TransactionReference reference) {
		return ((NodeWithRequestsAndResponses) originalView).getRequestAt(reference);
	}

	protected final TransactionReference addJarStoreInitialTransaction(byte[] jar, TransactionReference... dependencies) throws TransactionException, TransactionRejectedException {
		return nodeWithAccountsView.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final TransactionReference addJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.addJarStoreTransaction(new JarStoreTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageReference addConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.addConstructorCallTransaction(new ConstructorCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue addStaticMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final void addTransferTransaction(PrivateKey key, StorageReference caller, BigInteger gasPrice, TransactionReference classpath, StorageReference receiver, int howMuch) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException {
		nodeWithAccountsView.addInstanceMethodCallTransaction(new TransferTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasPrice, classpath, receiver, howMuch));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runViewInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, BigInteger.ZERO, chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final StorageValue runViewStaticMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.runStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(Signer.with(signature, key), caller, BigInteger.ZERO, chainId, gasLimit, gasPrice, classpath, method, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final JarSupplier postJarStoreTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, byte[] jar, TransactionReference... dependencies) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.postJarStoreTransaction(new JarStoreTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, jar, dependencies));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, method, receiver, actuals));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageValue> postTransferTransaction(PrivateKey key, StorageReference caller, BigInteger gasPrice, TransactionReference classpath, StorageReference receiver, int howMuch) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.postInstanceMethodCallTransaction(new TransferTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasPrice, classpath, receiver, howMuch));
	}

	/**
	 * Takes care of computing the next nonce.
	 */
	protected final CodeSupplier<StorageReference> postConstructorCallTransaction(PrivateKey key, StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, TransactionReference classpath, ConstructorSignature constructor, StorageValue... actuals) throws TransactionRejectedException, InvalidKeyException, SignatureException {
		return nodeWithAccountsView.postConstructorCallTransaction(new ConstructorCallTransactionRequest(Signer.with(signature, key), caller, getNonceOf(caller, key), chainId, gasLimit, gasPrice, classpath, constructor, actuals));
	}

	protected static byte[] bytesOf(String fileName) throws IOException {
		return Files.readAllBytes(pathOfExample(fileName));
	}

	protected static Path pathOfExample(String fileName) {
		return Paths.get("../io-takamaka-examples/target/io-takamaka-examples-" + version + '-' + fileName);
	}

	protected static void throwsTransactionExceptionWithCause(Class<? extends Throwable> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected.getName()))
				return;

			fail("wrong cause: expected " + expected.getName() + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionRejectedWithCause(Class<? extends Throwable> expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionRejectedException e) {
			if (e.getMessage().startsWith(expected.getName()))
				return;

			fail("wrong cause: expected " + expected.getName() + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionRejectedException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionRejectedException.class.getName());
	}

	protected static void throwsTransactionExceptionWithCause(String expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			if (e.getMessage().startsWith(expected))
				return;

			fail("wrong cause: expected " + expected + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionRejectedWithCause(String expected, TestBody what) {
		try {
			what.run();
		}
		catch (TransactionRejectedException e) {
			if (e.getMessage().startsWith(expected))
				return;

			fail("wrong cause: expected " + expected + " but got " + e.getMessage());
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionRejectedException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionRejectedException.class.getName());
	}

	protected static void throwsTransactionException(TestBody what) {
		try {
			what.run();
		}
		catch (TransactionException e) {
			return;
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionException.class.getName());
	}

	protected static void throwsTransactionRejectedException(TestBody what) {
		try {
			what.run();
		}
		catch (TransactionRejectedException e) {
			return;
		}
		catch (Exception e) {
			fail("wrong exception: expected " + TransactionRejectedException.class.getName() + " but got " + e.getClass().getName());
		}

		fail("no exception: expected " + TransactionRejectedException.class.getName());
	}

	protected static void throwsVerificationException(TestBody what) {
		throwsTransactionExceptionWithCause(VerificationException.class, what);
	}

	/**
	 * Gets the nonce of the given account. It calls the {@code Account.nonce()} method.
	 * 
	 * @param account the account
	 * @param key the private key of the account
	 * @return the nonce
	 * @throws TransactionException if the nonce cannot be found
	 */
	private BigInteger getNonceOf(StorageReference account, PrivateKey key) throws TransactionRejectedException {
		try {
			BigInteger nonce = nonces.get(account);
			if (nonce != null)
				nonce = nonce.add(BigInteger.ONE);
			else
				// we ask the account: 10,000 units of gas should be enough to run the method
				nonce = ((BigIntegerValue) nodeWithAccountsView.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(Signer.with(signature, key), account, BigInteger.ZERO, "", BigInteger.valueOf(10_000), BigInteger.ZERO, nodeWithAccountsView.getClassTag(account).jar, new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), account))).value;

			nonces.put(account, nonce);
			return nonce;
		}
		catch (Exception e) {
			logger.error("failed computing nonce", e);
			throw new TransactionRejectedException("cannot compute the nonce of " + account);
		}
	}
}