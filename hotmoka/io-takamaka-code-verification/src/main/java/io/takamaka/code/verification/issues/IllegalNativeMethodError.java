package io.takamaka.code.verification.issues;

public class IllegalNativeMethodError extends Error {

	public IllegalNativeMethodError(String where, String methodName) {
		super(where, methodName, -1, "native code is not allowed");
	}
}