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
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.marshalling.MarshallingContext;

/**
 * An update that states that the enumeration
 * field of a given storage object has been
 * modified to a given value. The type of the field is lazy.
 * Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfEnumLazy extends UpdateOfField {
	final static byte SELECTOR = 9;

	/**
	 * The name of the enumeration class whose element is being assigned to the field.
	 */
	public final String enumClassName;

	/**
	 * The name of the enumeration value put as new value of the field.
	 */
	public final String name;

	/**
	 * Builds an update of an enumeration field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param enumClassName the name of the enumeration class whose element is being assigned to the field
	 * @param name the name of the enumeration value put as new value of the field
	 */
	public UpdateOfEnumLazy(StorageReference object, FieldSignature field, String enumClassName, String name) {
		super(object, field);

		this.enumClassName = enumClassName;
		this.name = name;
	}

	@Override
	public StorageValue getValue() {
		return new EnumValue(enumClassName, name);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfEnumLazy && super.equals(other) && ((UpdateOfEnumLazy) other).name.equals(name)
			&& ((UpdateOfEnumLazy) other).enumClassName.equals(enumClassName);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ name.hashCode() ^ enumClassName.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;

		diff = enumClassName.compareTo(((UpdateOfEnumLazy) other).enumClassName);
		if (diff != 0)
			return diff;
		else
			return name.compareTo(((UpdateOfEnumLazy) other).name);
	}

	@Override
	public boolean isEager() {
		return false;
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(enumClassName)).add(gasCostModel.storageCostOf(name));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		context.writeUTF(enumClassName);
		context.writeUTF(name);
	}
}