package io.hotmoka.beans.references;

import java.io.Serializable;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A unique identifier for a transaction. This can be anything, from a
 * progressive number to a block/transaction pair to a database reference.
 * Each specific implementation of {@link io.hotmoka.nodes.Node}
 * provides its implementation of this interface. The order of comparison
 * is arbitrary, as long as it is a total order.
 */
@Immutable
public interface TransactionReference extends Serializable, Comparable<TransactionReference> {

	boolean equals(Object other);

	int hashCode();

	/**
	 * Yields the description of this transaction reference. This must be injective, that is,
	 * non-{@code equals()} transaction references must have non-{@code equals()} returned values
	 * for {@code toString()}.
	 * 
	 * @return the description
	 */
	String toString();
}