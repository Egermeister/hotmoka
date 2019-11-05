package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalEntryArgumentError;
import io.takamaka.code.verification.issues.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryCodeIsInstanceAndInContractsCheck extends VerifiedClassImpl.ClassVerification.MethodVerification.Check {

	public EntryCodeIsInstanceAndInContractsCheck(VerifiedClassImpl.ClassVerification.MethodVerification verification) {
		verification.super();

		annotations.isEntry(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.getContract().isAssignableFrom(tag))
				issue(new IllegalEntryArgumentError(inferSourceFile(), methodName));
			if (method.isStatic() || !classLoader.isContract(className))
				issue(new IllegalEntryMethodError(inferSourceFile(), methodName));
		});
	}
}