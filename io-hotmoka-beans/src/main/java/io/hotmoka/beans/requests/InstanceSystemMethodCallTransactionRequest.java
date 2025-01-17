/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.beans.requests;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.marshalling.BeanUnmarshaller;
import io.hotmoka.beans.marshalling.MarshallableBean;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.marshalling.MarshallingContext;
import io.hotmoka.marshalling.UnmarshallingContext;

/**
 * A request for calling an instance method of a storage object in a node.
 * This request is not signed, hence it is only used for calls started by the same
 * node. Users cannot run a transaction from this request.
 */
@Immutable
public class InstanceSystemMethodCallTransactionRequest extends AbstractInstanceMethodCallTransactionRequest implements SystemTransactionRequest {

	final static byte SELECTOR = 11;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param nonce the nonce used for transaction ordering and to forbid transaction replay; it is relative to the {@code caller}
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the receiver of the call
	 * @param actuals the actual arguments passed to the method
	 */
	public InstanceSystemMethodCallTransactionRequest(StorageReference caller, BigInteger nonce, BigInteger gasLimit, TransactionReference classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) {
		super(caller, nonce, gasLimit, BigInteger.ZERO, classpath, method, receiver, actuals);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":\n"
	       	+ "  caller: " + caller + "\n"
	       	+ "  nonce: " + nonce + "\n"
	       	+ "  gas limit: " + gasLimit + "\n"
	       	+ "  class path: " + classpath + "\n"
	       	+ "  receiver: " + receiver + "\n"
	       	+ toStringMethod();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InstanceSystemMethodCallTransactionRequest && super.equals(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		caller.intoWithoutSelector(context);
		context.writeBigInteger(gasLimit);
		classpath.into(context);
		context.writeBigInteger(nonce);
		intoArray(actuals().toArray(MarshallableBean[]::new), context);
		method.into(context);
		receiver.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static InstanceSystemMethodCallTransactionRequest from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		StorageReference caller = StorageReference.from(context);
		BigInteger gasLimit = context.readBigInteger();
		TransactionReference classpath = TransactionReference.from(context);
		BigInteger nonce = context.readBigInteger();
		StorageValue[] actuals = context.readArray((BeanUnmarshaller<StorageValue>) StorageValue::from, StorageValue[]::new);
		MethodSignature method = (MethodSignature) CodeSignature.from(context);
		StorageReference receiver = StorageReference.from(context);

		return new InstanceSystemMethodCallTransactionRequest(caller, nonce, gasLimit, classpath, method, receiver, actuals);
	}
}