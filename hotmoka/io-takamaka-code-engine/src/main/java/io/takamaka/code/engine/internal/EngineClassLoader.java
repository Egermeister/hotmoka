package io.takamaka.code.engine.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionBuilder;
import io.takamaka.code.instrumentation.InstrumentationConstants;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;

/**
 * A class loader used to access the definition of the classes
 * of Takamaka methods or constructors executed during a transaction.
 */
public class EngineClassLoader implements TakamakaClassLoader {

	/**
	 * The builder of the transaction for which deserialization is performed.
	 */
	private final AbstractTransactionBuilder<?,?> builder;

	/**
	 * The parent of this class loader;
	 */
	private final TakamakaClassLoader parent;

	/**
	 * The temporary files that hold the classpath for the transaction.
	 */
	private final List<Path> classpathElements = new ArrayList<>();

	/**
	 * The temporary file that holds a jar installed by a transaction, if any.
	 */
	private final TempJarFile tempJarFile;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#entry(io.takamaka.code.lang.Contract)}.
	 */
	private final Method entry;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, int)}.
	 */
	private final Method payableEntryInt;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, long)}.
	 */
	private final Method payableEntryLong;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, BigInteger)}.
	 */
	private final Method payableEntryBigInteger;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, int)}.
	 */
	private final Method redPayableInt;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, long)}.
	 */
	private final Method redPayableLong;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, BigInteger)}.
	 */
	private final Method redPayableBigInteger;

	/**
	 * Field {@link io.takamaka.code.lang.Storage#storageReference}.
	 */
	private final Field storageReference;

	/**
	 * Field {@link io.takamaka.code.lang.Storage#inStorage}.
	 */
	private final Field inStorage;

	/**
	 * Field {@link io.takamaka.code.lang.Contract#balance}.
	 */
	private final Field balanceField;

	/**
	 * Field {@link io.takamaka.code.lang.RedGreenContract#redBalance}.
	 */
	private final Field redBalanceField;

	/**
	 * Builds the class loader for the given class path and its dependencies.
	 * 
	 * @param classpath the class path
	 * @param builder the builder of the transaction for which the class loader is created
	 * @throws Exception if an error occurs
	 */
	public EngineClassLoader(Classpath classpath, AbstractTransactionBuilder<?,?> builder) throws Exception {
		this.builder = builder;
		this.tempJarFile = null;
		this.parent = TakamakaClassLoader.of(collectURLs(Stream.of(classpath), null));
		Class<?> contract = getContract(), redGreenContract = getRedGreenContract(), storage = getStorage();
		this.entry = contract.getDeclaredMethod("entry", contract);
		this.entry.setAccessible(true); // it was private
		this.payableEntryInt = contract.getDeclaredMethod("payableEntry", contract, int.class);
		this.payableEntryInt.setAccessible(true); // it was private
		this.payableEntryLong = contract.getDeclaredMethod("payableEntry", contract, long.class);
		this.payableEntryLong.setAccessible(true); // it was private
		this.payableEntryBigInteger = contract.getDeclaredMethod("payableEntry", contract, BigInteger.class);
		this.payableEntryBigInteger.setAccessible(true); // it was private
		this.redPayableInt = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, int.class);
		this.redPayableInt.setAccessible(true); // it was private
		this.redPayableLong = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, long.class);
		this.redPayableLong.setAccessible(true); // it was private
		this.redPayableBigInteger = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, BigInteger.class);
		this.redPayableBigInteger.setAccessible(true); // it was private
		this.redBalanceField = redGreenContract.getDeclaredField("balanceRed");
		this.redBalanceField.setAccessible(true); // it was private
		this.storageReference = storage.getDeclaredField(InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME);
		this.storageReference.setAccessible(true); // it was private
		this.inStorage = storage.getDeclaredField(InstrumentationConstants.IN_STORAGE);
		this.inStorage.setAccessible(true); // it was private
		this.balanceField = contract.getDeclaredField("balance");
		this.balanceField.setAccessible(true); // it was private
	}

	/**
	 * Builds the class loader for the given jar and its dependencies.
	 * 
	 * @param jar the jar
	 * @param dependencies the dependencies
	 * @param builder the builder of the transaction for which the class loader is created
	 * @throws Exception if an error occurs
	 */
	public EngineClassLoader(byte[] jar, Stream<Classpath> dependencies, AbstractTransactionBuilder<?,?> builder) throws Exception {
		this.tempJarFile = new TempJarFile(jar);

		try {
			this.builder = builder;
			this.parent = TakamakaClassLoader.of(collectURLs(dependencies, tempJarFile.toPath().toUri()));
			Class<?> contract = getContract(), redGreenContract = getRedGreenContract(), storage = getStorage();
			this.entry = contract.getDeclaredMethod("entry", contract);
			this.entry.setAccessible(true); // it was private
			this.payableEntryInt = contract.getDeclaredMethod("payableEntry", contract, int.class);
			this.payableEntryInt.setAccessible(true); // it was private
			this.payableEntryLong = contract.getDeclaredMethod("payableEntry", contract, long.class);
			this.payableEntryLong.setAccessible(true); // it was private
			this.payableEntryBigInteger = contract.getDeclaredMethod("payableEntry", contract, BigInteger.class);
			this.payableEntryBigInteger.setAccessible(true); // it was private
			this.redPayableInt = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, int.class);
			this.redPayableInt.setAccessible(true); // it was private
			this.redPayableLong = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, long.class);
			this.redPayableLong.setAccessible(true); // it was private
			this.redPayableBigInteger = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, BigInteger.class);
			this.redPayableBigInteger.setAccessible(true); // it was private
			this.redBalanceField = redGreenContract.getDeclaredField("balanceRed");
			this.redBalanceField.setAccessible(true); // it was private
			this.storageReference = storage.getDeclaredField(InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME);
			this.storageReference.setAccessible(true); // it was private
			this.inStorage = storage.getDeclaredField(InstrumentationConstants.IN_STORAGE);
			this.inStorage.setAccessible(true); // it was private
			this.balanceField = contract.getDeclaredField("balance");
			this.balanceField.setAccessible(true); // it was private
		}
		catch (Throwable t) {
			tempJarFile.close();
			throw t;
		}
	}

	/**
	 * Yields the path of the jar temporary file that contains the
	 * jar being installed in the node, if any.
	 * 
	 * @return the path
	 */
	public final Path jarPath() {
		return tempJarFile.toPath();
	}

	/**
	 * Yields the array of URL that refer to the components of the given classpaths.
	 * 
	 * @param classpaths the classpaths
	 * @param start an initial URL, if any
	 * @return the array of URLs
	 * @throws Exception if some URL cannot be created
	 */
	private URL[] collectURLs(Stream<Classpath> classpaths, URI start) throws Exception {
		List<URL> urls = new ArrayList<>();
		if (start != null) {
			urls.add(start.toURL());
			classpathElements.add(Paths.get(start));
		}

		for (Classpath classpath: classpaths.toArray(Classpath[]::new))
			addURLs(classpath, urls);

		return urls.toArray(new URL[urls.size()]);
	}

	/**
	 * Yields the request that generated the given transaction and charges gas for that operation.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the request
	 * @throws Exception if the request could not be found
	 */
	private TransactionRequest<?> getRequestAndCharge(TransactionReference transaction) throws Exception {
		builder.chargeForCPU(builder.node.getGasCostModel().cpuCostForGettingRequestAt(transaction));
		return builder.node.getRequestAt(transaction);
	}

	/**
	 * Yields the response that generated the given transaction and charges gas for that operation.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the response
	 * @throws Exception if the response could not be found
	 */
	private TransactionResponse getResponseAndCharge(TransactionReference transaction) throws Exception {
		builder.chargeForCPU(builder.node.getGasCostModel().cpuCostForGettingResponseAt(transaction));
		return builder.node.getResponseAt(transaction);
	}

	/**
	 * Expands the given list of URLs with the components of the given classpath.
	 * 
	 * @param classpath the classpath
	 * @param bag the list of URLs
	 * @throws Exception if some URL cannot be created
	 */
	private void addURLs(Classpath classpath, List<URL> bag) throws Exception {
		// if the class path is recursive, we consider its dependencies as well, recursively
		if (classpath.recursive) {
			TransactionRequest<?> request = getRequestAndCharge(classpath.transaction);
			if (!(request instanceof AbstractJarStoreTransactionRequest))
				throw new IllegalArgumentException("classpath does not refer to a jar store transaction");

			Stream<Classpath> dependencies = ((AbstractJarStoreTransactionRequest) request).getDependencies();
			for (Classpath dependency: dependencies.toArray(Classpath[]::new))
				addURLs(dependency, bag);
		}

		TransactionResponse response = getResponseAndCharge(classpath.transaction);
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new IllegalArgumentException("classpath does not refer to a successful jar store transaction");

		byte[] instrumentedJarBytes = ((TransactionResponseWithInstrumentedJar) response).getInstrumentedJar();
		builder.chargeForCPU(builder.node.getGasCostModel().cpuCostForLoadingJar(instrumentedJarBytes.length));
		builder.chargeForRAM(builder.node.getGasCostModel().ramCostForLoading(instrumentedJarBytes.length));

		try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(instrumentedJarBytes))) {
			Path classpathElement = Files.createTempFile("takamaka_", "@" + classpath.transaction + ".jar");
			Files.copy(is, classpathElement, StandardCopyOption.REPLACE_EXISTING);

			// we add, for class loading, the jar containing the instrumented code
			URI uri = classpathElement.toFile().toURI();
			bag.add(uri.toURL());
			classpathElements.add(Paths.get(uri));
		}
	}

	@Override
	public void close() throws IOException {
		IOException ioe = null;

		// we delete all paths elements that were used to build this class loader
		for (Path classpathElement: classpathElements)
			try {
				Files.deleteIfExists(classpathElement);
			}
			catch (IOException e) {
				if (ioe == null)
					ioe = e;
			}

		if (tempJarFile != null)
			try {
				tempJarFile.close();
			}
			catch (IOException e) {
				if (ioe == null)
					ioe = e;
			}

		try {
			parent.close();
		}
		catch (IOException e) {
			if (ioe == null)
				ioe = e;
		}

		if (ioe != null)
			throw ioe;
	}

	/**
	 * Yields the value of the {@code storageReference} field of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	public final StorageReference getStorageReferenceOf(Object object) {
		try {
			return (StorageReference) storageReference.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the storage reference of a storage object of class " + object.getClass().getName());
		}
	}

	/**
	 * Yields the value of the boolean {@code inStorage} field of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	public final boolean getInStorageOf(Object object) {
		try {
			return (boolean) inStorage.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the inStorage tag of a storage object of class " + object.getClass().getName());
		}
	}

	/**
	 * Yields the value of the {@code balance} field of the given contract in RAM.
	 * 
	 * @param object the contract
	 * @return the value of the field
	 */
	public final BigInteger getBalanceOf(Object object) {
		try {
			return (BigInteger) balanceField.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Sets the value of the {@code balance} field of the given contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the balance of the contract
	 */
	public final void setBalanceOf(Object object, BigInteger value) {
		try {
			balanceField.set(object, value);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot write the balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Sets the value of the {@code balanceRed} field of the given red/green contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the red balance of the contract
	 */
	public final void setRedBalanceOf(Object object, BigInteger value) {
		try {
			redBalanceField.set(object, value);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot write the red balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Called at the beginning of the instrumentation of an entry method or constructor
	 * of a contract. It forwards the call to {@code io.takamaka.code.lang.Contract.entry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.entry()}
	 */
	public final void entry(Object callee, Object caller) throws Throwable {
		// we call the private method of contract
		try {
			entry.invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, BigInteger amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryBigInteger.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, BigInteger amount) throws Throwable {
		// we call the private methods of contract
		try {
			entry.invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}

		try {
			redPayableBigInteger.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, int amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryInt.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, int amount) throws Throwable {
		// we call the private methods of contract
		try {
			entry.invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}

		try {
			redPayableInt.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry(): we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, long amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryLong.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, long amount) throws Throwable {
		// we call the private methods of contract
		try {
			entry.invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}
	
		try {
			redPayableLong.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	@Override
	public final Class<?> loadClass(String className) throws ClassNotFoundException {
		return parent.loadClass(className);
	}

	@Override
	public WhiteListingWizard getWhiteListingWizard() {
		return parent.getWhiteListingWizard();
	}

	@Override
	public Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException {
		return parent.resolveField(className, name, type);
	}

	@Override
	public Optional<Field> resolveField(Class<?> clazz, String name, Class<?> type) {
		return parent.resolveField(clazz, name, type);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException {
		return parent.resolveConstructor(className, args);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(Class<?> clazz, Class<?>[] args) {
		return parent.resolveConstructor(clazz, args);
	}

	@Override
	public Optional<Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveMethod(className, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveMethod(clazz, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveInterfaceMethod(className, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveInterfaceMethod(clazz, methodName, args, returnType);
	}

	@Override
	public boolean isStorage(String className) {
		return parent.isStorage(className);
	}

	@Override
	public boolean isContract(String className) {
		return parent.isContract(className);
	}

	@Override
	public boolean isRedGreenContract(String className) {
		return parent.isRedGreenContract(className);
	}

	@Override
	public boolean isInterface(String className) {
		return parent.isInterface(className);
	}

	@Override
	public boolean isLazilyLoaded(Class<?> type) {
		return parent.isLazilyLoaded(type);
	}

	@Override
	public boolean isEagerlyLoaded(Class<?> type) {
		return parent.isEagerlyLoaded(type);
	}

	@Override
	public Class<?> getContract() {
		return parent.getContract();
	}

	@Override
	public Class<?> getRedGreenContract() {
		return parent.getRedGreenContract();
	}

	@Override
	public Class<?> getStorage() {
		return parent.getStorage();
	}

	@Override
	public Class<?> getExternallyOwnedAccount() {
		return parent.getExternallyOwnedAccount();
	}

	@Override
	public Class<?> getRedGreenExternallyOwnedAccount() {
		return parent.getRedGreenExternallyOwnedAccount();
	}

	@Override
	public ClassLoader getJavaClassLoader() {
		return parent.getJavaClassLoader();
	}
}