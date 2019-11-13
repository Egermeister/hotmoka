package io.takamaka.code.memory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.ConstructorSignature;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.request.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.request.GameteCreationTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreInitialTransactionRequest;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.BigIntegerValue;
import io.takamaka.code.blockchain.values.StorageReference;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * It provides support for the creation of a given number of initial accounts.
 */
public class InitializedMemoryBlockchain extends MemoryBlockchain {

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	public final Classpath takamakaBase;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * Builds a blockchain in disk memory and initializes user accounts
	 * with the given initial funds.
	 * 
	 * @param takamakaBasePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.takamaka.code.memory.InitializedMemoryBlockchain#takamakaBase}
	 * @param funds the initial funds of the accounts that are created
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 */
	public InitializedMemoryBlockchain(Path takamakaBasePath, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException {
		super(Paths.get("chain"));

		TransactionReference takamaka_base = addJarStoreInitialTransaction
			(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaBasePath)));
		this.takamakaBase = new Classpath(takamaka_base, false);

		// we compute the total amount of funds needed to create the accounts:
		// we do not start from 0 since we need some gas to create the accounts, below
		BigInteger sum = BigInteger.valueOf(1000000000L * funds.length);
		for (BigInteger fund: funds)
			sum = sum.add(fund);

		StorageReference gamete = addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, sum));

		// let us create the accounts
		this.accounts = new StorageReference[funds.length];
		BigInteger gas = BigInteger.valueOf(10000); // enough for creating an account
		for (int i = 0; i < accounts.length; i++)
			this.accounts[i] = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, gas, takamakaBase, new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER), new BigIntegerValue(funds[i])));
	}

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in blockchain
	 */
	public StorageReference account(int i) {
		return accounts[i];
	}
}