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

package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.marshalling.MarshallingContext;

/**
 * An update of a field states that the {@link java.lang.String}
 * field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfString extends UpdateOfField {
	final static byte SELECTOR = 17;
	final static byte SELECTOR_PUBLIC_KEY = 32;

	/**
	 * The new value of the field.
	 */
	public final String value;

	/**
	 * Builds an update of a {@link java.lang.String} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfString(StorageReference object, FieldSignature field, String value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new StringValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfString && super.equals(other) && ((UpdateOfString) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public String toString() {
		return "<" + object + "|" + getField() + "|\"" + getValue() + "\">";
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfString) other).value);
	}

	@Override
	public boolean isEager() {
		// a lazy String could be stored into a lazy Object or Serializable or Comparable or CharSequence field
		return field.type.equals(ClassType.STRING);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(value));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (FieldSignature.EOA_PUBLIC_KEY_FIELD.equals(field)) {
			context.writeByte(SELECTOR_PUBLIC_KEY);
			super.intoWithoutField(context);
		}
		else {
			context.writeByte(SELECTOR);
			super.into(context);
		}

		context.writeUTF(value);
	}
}