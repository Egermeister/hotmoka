package io.hotmoka.takamaka;

import io.hotmoka.beans.annotations.Immutable;
import io.takamaka.code.engine.Config;

/**
 * The configuration of a Takamaka blockchain.
 */
@Immutable
public class TakamakaBlockchainConfig extends Config {

	/**
	 * The maximal number of connection attempts to the Takamaka process during ping.
	 * Defaults to 20.
	 */
	public final int maxPingAttempts;

	/**
	 * The delay between two successive ping attempts, in milliseconds. Defaults to 200.
	 */
	public final int pingDelay;

	/**
	 * Full constructor for the builder pattern.
	 */
	protected TakamakaBlockchainConfig(io.takamaka.code.engine.Config superConfig, int maxPingAttemps, int pingDelay) {
		super(superConfig);

		this.maxPingAttempts = maxPingAttemps;
		this.pingDelay = pingDelay;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.takamaka.code.engine.Config.Builder<Builder> {
		private int maxPingAttempts = 20;
		private int pingDelay = 200;

		/**
		 * Sets the maximal number of connection attempts to the Tendermint process during ping.
		 * Defaults to 20.
		 * 
		 * @param maxPingAttempts the max number of attempts
		 * @return this builder
		 */
		public Builder setMaxPingAttempts(int maxPingAttempts) {
			this.maxPingAttempts = maxPingAttempts;
			return this;
		}

		/**
		 * Sets the delay between two successive ping attempts, in milliseconds. Defaults to 200.
		 * 
		 * @param pingDelay the delay
		 * @return this builder
		 */
		public Builder setPingDelay(int pingDelay) {
			this.pingDelay = pingDelay;
			return this;
		}

		@Override
		public TakamakaBlockchainConfig build() {
			return new TakamakaBlockchainConfig(super.build(), maxPingAttempts, pingDelay);
		}

		@Override
		protected Builder getThis() {
			return this;
		}
	}
}