package io.hotmoka.beans.values;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A {@code float} value stored in blockchain.
 */
@Immutable
public final class FloatValue implements StorageValue {

	private static final long serialVersionUID = -291587794739536709L;

	/**
	 * The value.
	 */
	public final float value;

	/**
	 * Builds a {@code float} value.
	 * 
	 * @param value the value
	 */
	public FloatValue(float value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Float.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FloatValue && ((FloatValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Float.compare(value, ((FloatValue) other).value);
	}
}