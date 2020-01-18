package io.takamaka.code.blockchain.values;

import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A {@code char} value stored in blockchain.
 */
@Immutable
public final class CharValue implements StorageValue {

	private static final long serialVersionUID = 2752558282237382571L;

	/**
	 * The value.
	 */
	public final char value;

	/**
	 * Builds a {@code char} value.
	 * 
	 * @param value the value
	 */
	public CharValue(char value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Character.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CharValue && ((CharValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Character.compare(value, ((CharValue) other).value);
	}
}