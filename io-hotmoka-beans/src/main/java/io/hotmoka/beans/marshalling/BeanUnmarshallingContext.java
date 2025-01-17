/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.beans.marshalling;

import java.io.IOException;
import java.io.InputStream;

import io.hotmoka.beans.marshalling.internal.FieldSignatureUnmarshaller;
import io.hotmoka.beans.marshalling.internal.StorageReferenceUnmarshaller;
import io.hotmoka.beans.marshalling.internal.TransactionReferenceUnmarshaller;
import io.hotmoka.marshalling.UnmarshallingContext;

/**
 * A context used during bytes unmarshalling into beans.
 */
public class BeanUnmarshallingContext extends UnmarshallingContext {

	public BeanUnmarshallingContext(InputStream is) throws IOException {
		super(is);

		registerObjectUnmarshaller(new StorageReferenceUnmarshaller());
		registerObjectUnmarshaller(new TransactionReferenceUnmarshaller());
		registerObjectUnmarshaller(new FieldSignatureUnmarshaller());
	}
}