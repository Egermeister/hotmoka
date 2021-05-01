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

package io.hotmoka.tests;

import static io.hotmoka.beans.Coin.panarea;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.constants.Constants;

/**
 * A test for the remote purchase contract.
 */
class AbstractFail extends TakamakaTest {
	private static final ClassType ABSTRACT_FAIL = new ClassType("io.hotmoka.examples.abstractfail.AbstractFail");
	private static final ConstructorSignature ABSTRACT_FAIL_IMPL_CONSTRUCTOR = new ConstructorSignature(new ClassType("io.hotmoka.examples.abstractfail.AbstractFailImpl"), BasicTypes.INT);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("abstractfail.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _100_000, _1_000_000);
	}

	@Test @DisplayName("new AbstractFail() throws InstantiationException")
	void createAbstractFail() {
		throwsTransactionExceptionWithCause(InstantiationException.class, () ->
			// cannot instantiate an abstract class
			addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), new ConstructorSignature(ABSTRACT_FAIL))
		);
	}

	@Test @DisplayName("new AbstractFailImpl()")
	void createAbstractFailImpl() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, new IntValue(42));
	}

	@Test @DisplayName("new AbstractFailImpl().method() yields an AbstractFailImpl")
	void createAbstractFailImplThenCallAbstractMethod() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference abstractfail = addConstructorCallTransaction(privateKey(0), account(0), _100_000, panarea(1), jar(), ABSTRACT_FAIL_IMPL_CONSTRUCTOR, new IntValue(42));

		StorageReference result = (StorageReference) addInstanceMethodCallTransaction
			(privateKey(0), account(0), _100_000, panarea(1), jar(), new NonVoidMethodSignature(ABSTRACT_FAIL, "method", ABSTRACT_FAIL), abstractfail);

		String className = ((StringValue) runInstanceMethodCallTransaction(account(0), _100_000, jar(), new NonVoidMethodSignature(Constants.STORAGE_NAME, "getClassName", ClassType.STRING), result)).value;

		assertEquals("io.hotmoka.examples.abstractfail.AbstractFailImpl", className);
	}
}