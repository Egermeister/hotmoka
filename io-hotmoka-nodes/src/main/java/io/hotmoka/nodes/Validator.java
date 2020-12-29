package io.hotmoka.nodes;

import java.security.PublicKey;

import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * The description of a validator of network of nodes.
 */
public final class Validator {

	/**
	 * The power of the validator, always positive.
	 */
	public final long power;

	/**
	 * The type of the public key.
	 */
	public final SignatureAlgorithm.TYPES publicKeyType;

	/**
	 * The public key of the account, in the store of the node,
	 * that can be used to send money for paying the
	 * validation work of the validator.
	 */
	public final PublicKey publicKey;

	/**
	 * Builds the description of a validator.
	 * 
	 * @param power the power of the validator
	 * @param publicKey the public key of the account, in the store of the node,
	 *                  that can be used to send money for paying the
	 *                  validation work of the validator
	 * @throws NullPointerException if {@code address} or {@code publicKey} is {@code null}
	 * @throws IllegalArgumentException if {@code power} is not positive
	 */
	public Validator(long power, SignatureAlgorithm.TYPES type, PublicKey publicKey) {
		if (power <= 0L)
			throw new IllegalArgumentException("the power of a validator must be positive");

		if (type == null)
			throw new NullPointerException("the public key type of a validator cannot be null");

		if (publicKey == null)
			throw new NullPointerException("the public key of a validator cannot be null");

		this.power = power;
		this.publicKeyType = type;
		this.publicKey = publicKey;
	}

	@Override
	public String toString() {
		return "validator with power " + power + " and public key " + publicKey + " of type " + publicKeyType;
	}
}