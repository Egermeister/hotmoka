package io.takamaka.code.engine.internal.executors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;
import io.takamaka.code.engine.internal.transactions.CodeCallTransactionRun;
import io.takamaka.code.engine.internal.transactions.InstanceMethodCallTransactionRun;
import io.takamaka.code.engine.internal.transactions.MethodCallTransactionRun;
import io.takamaka.code.engine.internal.transactions.NonInitialTransactionRun;

/**
 * The thread that executes an instance method of a storage object. It creates the class loader
 * from the class path and deserializes receiver and actuals. Then calls the code and serializes
 * the resulting value back.
 */
public class InstanceMethodExecutor extends CodeExecutor<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> {

	/**
	 * Builds the executor of an instance method.
	 * 
	 * @param run the engine for which the method is being executed
	 * @throws TransactionException 
	 * @throws IllegalTransactionRequestException 
	 */
	public InstanceMethodExecutor(NonInitialTransactionRun<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> run) throws TransactionException {
		super(run);
	}

	@Override
	public void run() {
		try {
			Method methodJVM;
			Object[] deserializedActuals;

			try {
				// we first try to call the method with exactly the parameter types explicitly provided
				methodJVM = run.getMethod(this);
				deserializedActuals = ((CodeCallTransactionRun<?,?>) run).deserializedActuals;
			}
			catch (NoSuchMethodException e) {
				// if not found, we try to add the trailing types that characterize the @Entry methods
				try {
					methodJVM = run.getEntryMethod(this);
					deserializedActuals = run.addExtraActualsForEntry(this);
				}
				catch (NoSuchMethodException ee) {
					throw e; // the message must be relative to the method as the user sees it
				}
			}

			if (Modifier.isStatic(methodJVM.getModifiers()))
				throw new NoSuchMethodException("Cannot call a static method: use addStaticMethodCallTransaction instead");

			run.ensureWhiteListingOf(this, methodJVM, deserializedActuals);

			((MethodCallTransactionRun<?>) run).isVoidMethod = methodJVM.getReturnType() == void.class;
			((MethodCallTransactionRun<?>) run).isViewMethod = AbstractTransactionRun.hasAnnotation(methodJVM, io.takamaka.code.constants.Constants.VIEW_NAME);
			if (AbstractTransactionRun.hasAnnotation(methodJVM, io.takamaka.code.constants.Constants.RED_PAYABLE_NAME))
				run.checkIsRedGreenExternallyOwned(((CodeCallTransactionRun<?,?>) run).deserializedCaller);

			try {
				((CodeCallTransactionRun<?,?>) run).result = methodJVM.invoke(((InstanceMethodCallTransactionRun) run).deserializedReceiver, deserializedActuals);
			}
			catch (InvocationTargetException e) {
				((CodeCallTransactionRun<?,?>) run).exception = AbstractTransactionRun.unwrapInvocationException(e, methodJVM);
			}
		}
		catch (Throwable t) {
			((CodeCallTransactionRun<?,?>) run).exception = t;
		}
	}
}