package io.hotmoka.memory.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.Transaction;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 */
public abstract class AbstractMemoryBlockchain extends AbstractNode {

	/**
	 * The name used for the file containing the serialized header of a block.
	 */
	private static final Path HEADER_NAME = Paths.get("header");

	/**
	 * The name used for the file containing the textual header of a block.
	 */
	private final static Path HEADER_TXT_NAME = Paths.get("header.txt");

	/**
	 * The name used for the file containing the serialized request of a transaction.
	 */
	private final static Path REQUEST_NAME = Paths.get("request");

	/**
	 * The name used for the file containing the serialized response of a transaction.
	 */
	private final static Path RESPONSE_NAME = Paths.get("response");

	/**
	 * The name used for the file containing the textual request of a transaction.
	 */
	private final static Path REQUEST_TXT_NAME = Paths.get("request.txt");

	/**
	 * The name used for the file containing the textual response of a transaction.
	 */
	private final static Path RESPONSE_TXT_NAME = Paths.get("response.txt");

	/**
	 * The number of transactions that fit inside a block.
	 */
	public final static int TRANSACTIONS_PER_BLOCK = 5;

	/**
	 * The root path where the blocks are stored.
	 */
	private final Path root;

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	private final Classpath takamakaCode;

	/**
	 * The reference to the topmost transaction reference.
	 * This is {@code null} if the blockchain is empty.
	 */
	private MemoryTransactionReference topmost;

	/**
	 * True if and only if this node doesn't allow initial transactions anymore.
	 */
	private boolean initialized;

	/**
	 * Builds a blockchain that stores transaction in disk memory.
	 * 
	 * @param root the directory where blocks and transactions must be stored.
	 * @throws IOException if the root directory cannot be created
	 * @throws TransactionException if the initialization of the blockchain fails
	 */
	protected AbstractMemoryBlockchain(Path takamakaCodePath) throws IOException, TransactionException {
		this.root = Paths.get("chain");
		ensureDeleted(root);  // cleans the directory where the blockchain lives
		Files.createDirectories(root);
		createHeaderOfBlock(BigInteger.ZERO);
		TransactionReference support = addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath)));
		this.takamakaCode = new Classpath(support, false);
	}

	public final Classpath takamakaCode() {
		return takamakaCode;
	}

	@Override
	public long getNow() throws Exception {
		// we access the block header where the transaction would be added
		if (topmost != null) {
			MemoryTransactionReference next = topmost.getNext();
			Path headerPath = getPathInBlockFor(next.blockNumber, HEADER_NAME);
			try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(headerPath)))) {
				MemoryBlockHeader header = (MemoryBlockHeader) in.readObject();
				return header.time;
			}
		}
		else
			// the first transaction does not use the time anyway, since it can only be the installation of a jar
			return 0L;
	}

	/**
	 * Yields the reference that must be used to refer to a new transaction
	 * that follows the topmost one.
	 * 
	 * @return the reference to the next transaction
	 */
	private TransactionReference getNextTransaction() {
		return topmost == null ? new MemoryTransactionReference(BigInteger.ZERO, (short) 0) : topmost.getNext();
	}

	@Override
	public TransactionReference getTransactionReferenceFor(String toString) {
		return new MemoryTransactionReference(toString);
	}

	@Override
	protected Stream<TransactionReference> getHistoryOf(StorageReference object) {
		List<TransactionReference> history = new ArrayList<>();
		TransactionReference whenCreated = object.transaction;
		if (topmost != null)
			for (MemoryTransactionReference cursor = topmost; !cursor.isOlderThan(whenCreated); cursor = cursor.getPrevious())
				history.add(cursor);

		return history.stream();
	}


	@Override
	protected boolean isInitialized() {
		return initialized;
	}

	@Override
	protected void markAsInitialized() {
		initialized = true;
	}

	@Override
	protected TransactionReference addJarStoreInitialTransactionInternal(JarStoreInitialTransactionRequest request) throws Exception {
		TransactionReference transactionReference = getNextTransaction();
		Transaction<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> transaction = Transaction.mkFor(request, transactionReference, this);
		expandStoreWith(transaction);
		return transaction.getResponse().getOutcomeAt(transactionReference);
	}

	@Override
	protected StorageReference addGameteCreationTransactionInternal(GameteCreationTransactionRequest request) throws Exception {
		Transaction<GameteCreationTransactionRequest, GameteCreationTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
		expandStoreWith(transaction);
		return transaction.getResponse().getOutcome();
	}

	@Override
	protected StorageReference addRedGreenGameteCreationTransactionInternal(RedGreenGameteCreationTransactionRequest request) throws Exception {
		Transaction<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
		expandStoreWith(transaction);
		return transaction.getResponse().getOutcome();
	}

	@Override
	protected TransactionReference addJarStoreTransactionInternal(JarStoreTransactionRequest request) throws Exception {
		TransactionReference transactionReference = getNextTransaction();
		Transaction<JarStoreTransactionRequest, JarStoreTransactionResponse> transaction = Transaction.mkFor(request, transactionReference, this);
		expandStoreWith(transaction);
		return transaction.getResponse().getOutcomeAt(transactionReference);
	}

	@Override
	protected StorageReference addConstructorCallTransactionInternal(ConstructorCallTransactionRequest request) throws Exception {
		Transaction<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
		expandStoreWith(transaction);
		return transaction.getResponse().getOutcome();
	}

	@Override
	protected StorageValue addInstanceMethodCallTransactionInternal(InstanceMethodCallTransactionRequest request) throws Exception {
		Transaction<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
		expandStoreWith(transaction);
		return transaction.getResponse().getOutcome();
	}

	@Override
	protected StorageValue addStaticMethodCallTransactionInternal(StaticMethodCallTransactionRequest request) throws Exception {
		Transaction<StaticMethodCallTransactionRequest, MethodCallTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
		expandStoreWith(transaction);
		return transaction.getResponse().getOutcome();
	}

	@Override
	protected StorageValue runViewInstanceMethodCallTransactionInternal(InstanceMethodCallTransactionRequest request) throws Exception {
		return Transaction.mkForView(request, getNextTransaction(), this).getResponse().getOutcome();
	}

	@Override
	protected StorageValue runViewStaticMethodCallTransactionInternal(StaticMethodCallTransactionRequest request) throws Exception {
		return Transaction.mkForView(request, getNextTransaction(), this).getResponse().getOutcome();
	}

	/**
	 * Expands the store of this node with a transaction, that is added after the topmost one and
	 * becomes the new topmost transaction.
	 * 
	 * @param <Request> the type of the request of the transaction
	 * @param <Response> the type of the response of the transaction
	 * @param transaction the transaction
	 * @throws Exception if the expansion cannot be completed
	 */
	private <Request extends TransactionRequest<Response>, Response extends TransactionResponse> void expandStoreWith(Transaction<Request, Response> transaction) throws Exception {
		MemoryTransactionReference next = (MemoryTransactionReference) getNextTransaction();
		Path requestPath = getPathFor(next, REQUEST_NAME);
		Path parent = requestPath.getParent();
		ensureDeleted(parent);
		Files.createDirectories(parent);

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(requestPath)))) {
			os.writeObject(transaction.getRequest());
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(next, REQUEST_TXT_NAME)))) {
			output.print(transaction.getRequest());
		}

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(getPathFor(next, RESPONSE_NAME))))) {
			os.writeObject(transaction.getResponse());
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(next, RESPONSE_TXT_NAME)))) {
			output.print(transaction.getResponse());
		}

		topmost = next;
		if (next.isLastInBlock())
			createHeaderOfBlock(next.blockNumber.add(BigInteger.ONE));
	}

	/**
	 * Creates the header of the given block.
	 * 
	 * @param blockNumber the number of the block
	 * @throws IOException if the header cannot be created
	 */
	private void createHeaderOfBlock(BigInteger blockNumber) throws IOException {
		Path headerPath = getPathInBlockFor(blockNumber, HEADER_NAME);
		ensureDeleted(headerPath.getParent());
		Files.createDirectories(headerPath.getParent());

		MemoryBlockHeader header = new MemoryBlockHeader();

		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(headerPath)))) {
			os.writeObject(header);
		}

		try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathInBlockFor(blockNumber, HEADER_TXT_NAME)))) {
			output.print(header);
		}
	}

	@Override
	public Stream<Classpath> getDependenciesOfJarStoreTransactionAt(TransactionReference reference) throws IOException, ClassNotFoundException {
		Path path = getPathFor((MemoryTransactionReference) reference, REQUEST_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
			TransactionRequest<?> request = (TransactionRequest<?>) in.readObject();
			if (!(request instanceof AbstractJarStoreTransactionRequest))
				throw new IllegalArgumentException("the transaction does not contain a jar store request");

			return ((AbstractJarStoreTransactionRequest) request).getDependencies();
		}
	}

	@Override
	public TransactionResponse getResponseAt(TransactionReference reference) throws IOException, ClassNotFoundException {
		Path response = getPathFor((MemoryTransactionReference) reference, RESPONSE_NAME);
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(response)))) {
			return (TransactionResponse) in.readObject();
		}
	}

	/**
	 * Yields the path for the given file name inside the directory for the given transaction.
	 * 
	 * @param fileName the name of the file
	 * @return the path
	 */
	private Path getPathFor(MemoryTransactionReference reference, Path fileName) {
		return root.resolve("b" + reference.blockNumber).resolve("t" + reference.transactionNumber).resolve(fileName);
	}

	/**
	 * Yields the path for a file inside the given block.
	 * 
	 * @param blockNumber the number of the block
	 * @param fileName the file name
	 * @return the path
	 */
	private Path getPathInBlockFor(BigInteger blockNumber, Path fileName) {
		return root.resolve("b" + blockNumber).resolve(fileName);
	}

	/**
	 * Deletes the given directory, if it exists.
	 * 
	 * @param dir the directory
	 * @throws IOException if a disk error occurs
	 */
	private static void ensureDeleted(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

	/**
	 * The header of a block. It contains the time that must be used
	 * as {@code now} by the transactions that will be added to the block.
	 */
	private static class MemoryBlockHeader implements Serializable {
		private static final long serialVersionUID = 6163345302977772036L;
		private final static DateFormat formatter;

		static {
			formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
			formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		/**
		 * The time of creation of the block, as returned by {@link java.lang.System#currentTimeMillis()}.
		 */
		private final long time;

		/**
		 * Builds block header.
		 */
		private MemoryBlockHeader() {
			this.time = System.currentTimeMillis();
		}

		@Override
		public String toString() {
			return "block creation time: " + time + " [" + formatter.format(new Date(time)) + " UTC]";
		}
	}
}