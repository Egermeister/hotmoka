package io.hotmoka.beans.updates;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that a character field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfChar extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2226960173435837206L;

	/**
	 * The new value of the field.
	 */
	public final char value;

	/**
	 * Builds an update of an {@code char} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfChar(StorageReference object, FieldSignature field, char value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new CharValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfChar && super.equals(other) && ((UpdateOfChar) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value;
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Character.compare(value, ((UpdateOfChar) other).value);
	}
}