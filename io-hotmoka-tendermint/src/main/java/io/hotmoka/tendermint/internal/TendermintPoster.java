package io.hotmoka.tendermint.internal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.TendermintValidator;
import io.hotmoka.tendermint.internal.beans.TendermintBroadcastTxResponse;
import io.hotmoka.tendermint.internal.beans.TendermintGenesisResponse;
import io.hotmoka.tendermint.internal.beans.TendermintTxResponse;
import io.hotmoka.tendermint.internal.beans.TendermintTxResult;
import io.hotmoka.tendermint.internal.beans.TendermintValidatorPriority;
import io.hotmoka.tendermint.internal.beans.TendermintValidatorsResponse;
import io.hotmoka.tendermint.internal.beans.TxError;

/**
 * An object that posts requests to a Tendermint process.
 */
public class TendermintPoster {
	private final static Logger logger = LoggerFactory.getLogger(TendermintPoster.class);

	private final TendermintBlockchainConfig config;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	TendermintPoster(TendermintBlockchainConfig config) {
		this.config = config;
	}

	/**
	 * Sends the given {@code request} to the Tendermint process, inside a {@code broadcast_tx_async} Tendermint request.
	 * 
	 * @param request the request to send
	 */
	void postRequest(TransactionRequest<?> request) {
		try {
			String jsonTendermintRequest = "{\"method\": \"broadcast_tx_async\", \"params\": {\"tx\": \"" +  Base64.getEncoder().encodeToString(request.toByteArray()) + "\"}}";
			String response = postToTendermint(jsonTendermintRequest);

			TendermintBroadcastTxResponse parsedResponse = new Gson().fromJson(response, TendermintBroadcastTxResponse.class);
			TxError error = parsedResponse.error;
			if (error != null)
				throw new InternalFailureException("Tendermint transaction failed: " + error.message + ": " + error.data);
		}
		catch (Exception e) {
			logger.error("could not determine the Tendermint chain id for this node", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the Hotmoka request specified in the Tendermint result for the Hotmoka
	 * transaction with the given hash.
	 * 
	 * @param hash the hash of the transaction to look for
	 * @return the Hotmoka transaction request
	 */
	Optional<TransactionRequest<?>> getRequest(String hash) {
		try {
			TendermintTxResponse response = gson.fromJson(tx(hash), TendermintTxResponse.class);
			if (response.error != null)
				// the Tendermint transaction didn't commit successfully
				return Optional.empty();

			String tx = response.result.tx;
			if (tx == null)
				throw new InternalFailureException("no Hotmoka request in Tendermint response");

			byte[] decoded = Base64.getDecoder().decode(tx);
			try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded))) {
				return Optional.of(TransactionRequest.from(ois));
			}
		}
		catch (Exception e) {
			logger.error("could not determine the Tendermint chain id for this node", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the Hotmoka error in the Tendermint transaction with the given hash.
	 * 
	 * @param hash the hash of the transaction to look for
	 * @return the error, if any. If the transaction didn't commit or committed successfully,
	 *         the result is an empty optional
	 */
	Optional<String> getErrorMessage(String hash) {
		try {
			TendermintTxResponse response = gson.fromJson(tx(hash), TendermintTxResponse.class);

			if (response.error != null)
				// the Tendermint transaction didn't commit successfully
				return Optional.empty();
			else {
				// the Tendermint transaction committed successfully
				TendermintTxResult tx_result = response.result.tx_result;
				if (tx_result == null)
					throw new InternalFailureException("no result for Tendermint transacti)on " + hash);
				else if (tx_result.data != null && !tx_result.data.isEmpty())
					return Optional.of(new String(Base64.getDecoder().decode(tx_result.data)));
				else
					// there is no Hotmoka error in this transaction
					return Optional.empty();
			}
		}
		catch (Exception e) {
			logger.error("could not determine the Tendermint chain id for this node", e);
			throw InternalFailureException.of(e);
		}
	}

	/*public String tx_search(String query) throws Exception {
		String jsonTendermintRequest = "{\"method\": \"tx_search\", \"params\": {\"query\": \"" +
			//Base64.getEncoder().encodeToString(
			query + "\", \"prove\": false, \"page\": \"1\", \"per_page\": \"30\", \"order_by\": \"asc\" }}";
	
		return postToTendermint(jsonTendermintRequest);
	}*/
	
	/*public String abci_query(String path, String data) throws Exception {
		String jsonTendermintRequest = "{\"method\": \"abci_query\", \"params\": {\"data\": \""
				+ bytesToHex(data.getBytes())
				//+ Base64.getEncoder().encodeToString(data.getBytes())
				+ "\", \"prove\": true }}";
	
		System.out.println(jsonTendermintRequest);
		return postToTendermint(jsonTendermintRequest);
	}*/
	
	String getTendermintChainId() {
		try {
			TendermintGenesisResponse response = new Gson().fromJson(genesis(), TendermintGenesisResponse.class);
			if (response.error != null)
				throw new InternalFailureException(response.error);
	
			String chainId = response.result.genesis.chain_id;
			if (chainId == null)
				throw new InternalFailureException("no chain id in Tendermint response");
	
			return chainId;
		}
		catch (Exception e) {
			logger.error("could not determine the Tendermint chain id for this node", e);
			throw InternalFailureException.of(e);
		}
	}

	Stream<TendermintValidator> getTendermintValidators() {
		try {
			// the parameters of the validators() query seem to be ignored, no count nor total is returned
			String jsonResponse = validators(1, 100);
			TendermintValidatorsResponse response = new Gson().fromJson(jsonResponse, TendermintValidatorsResponse.class);
			if (response.error != null)
				throw new InternalFailureException(response.error);

			return response.result.validators.stream().map(TendermintPoster::intoTendermintValidator);
		}
		catch (Exception e) {
			logger.error("the Tendermint validators cannot be retrieved for this node", e);
			throw InternalFailureException.of(e);
		} 
	}

	private static TendermintValidator intoTendermintValidator(TendermintValidatorPriority validatorPriority) {
		if (validatorPriority.address == null)
			throw new InternalFailureException("unexpected null address in Tendermint validator");
		else if (validatorPriority.voting_power <= 0L)
			throw new InternalFailureException("unexpected non-positive voting power in Tendermint validator");
		else if (validatorPriority.pub_key.value == null)
			throw new InternalFailureException("unexpected null public key for Tendermint validator");
		else if (validatorPriority.pub_key.type == null)
			throw new InternalFailureException("unexpected null public key type for Tendermint validator");
		else
			return new TendermintValidator(validatorPriority.address, validatorPriority.voting_power, validatorPriority.pub_key.value, validatorPriority.pub_key.type);
	}

	/**
	 * Sends a {@code validators} request to the Tendermint process, to read the
	 * list of current validators of the Tendermint network.
	 * 
	 * @param page the page number
	 * @return number of entries per page (max 100)
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String validators(int page, int perPage) throws IOException, TimeoutException, InterruptedException {
		String jsonTendermintRequest = "{\"method\": \"validators\", \"params\": {\"page\": " + page + ", \"per_page\": " + perPage + "}}";
		return postToTendermint(jsonTendermintRequest);
	}

	/*public String tx_search(String query) throws Exception {
		String jsonTendermintRequest = "{\"method\": \"tx_search\", \"params\": {\"query\": \"" +
			//Base64.getEncoder().encodeToString(
			query + "\", \"prove\": false, \"page\": \"1\", \"per_page\": \"30\", \"order_by\": \"asc\" }}";

		return postToTendermint(jsonTendermintRequest);
	}*/

	/*public String abci_query(String path, String data) throws Exception {
		String jsonTendermintRequest = "{\"method\": \"abci_query\", \"params\": {\"data\": \""
				+ bytesToHex(data.getBytes())
				//+ Base64.getEncoder().encodeToString(data.getBytes())
				+ "\", \"prove\": true }}";

		System.out.println(jsonTendermintRequest);
		return postToTendermint(jsonTendermintRequest);
	}*/

	/**
	 * Sends a {@code tx} request to the Tendermint process, to read the
	 * committed data about the Tendermint transaction with the given hash.
	 * 
	 * @param hash the hash of the Tendermint transaction to look for
	 * @return the response of Tendermint
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String tx(String hash) throws IOException, TimeoutException, InterruptedException {
		String jsonTendermintRequest = "{\"method\": \"tx\", \"params\": {\"hash\": \"" +
			Base64.getEncoder().encodeToString(hexStringToByteArray(hash)) + "\", \"prove\": false }}";
	
		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Sends a {@code genesis} request to the Tendermint process, to read the
	 * genesis information, containing for instance the chain id of the node
	 * and the initial list of validators.
	 * 
	 * @return the response of Tendermint
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing the request failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing the request
	 */
	private String genesis() throws IOException, TimeoutException, InterruptedException {
		String jsonTendermintRequest = "{\"method\": \"genesis\"}";
		return postToTendermint(jsonTendermintRequest);
	}

	/**
	 * Transforms a hexadecimal string into a byte array.
	 * 
	 * @param s the string
	 * @return the byte array
	 */
	private static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2)
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
	
	    return data;
	}

	/**
	 * Sends a POST request to the Tendermint process and yields the response.
	 * 
	 * @param jsonTendermintRequest the request to post, in JSON format
	 * @return the response
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing
	 */
	private String postToTendermint(String jsonTendermintRequest) throws IOException, TimeoutException, InterruptedException {
		HttpURLConnection connection = openPostConnectionToTendermint();
		writeInto(connection, jsonTendermintRequest);
		return readFrom(connection);
	}

	/**
	 * Reads the response from the given connection.
	 * 
	 * @param connection the connection
	 * @return the response
	 * @throws IOException if the response couldn't be read
	 */
	private static String readFrom(HttpURLConnection connection) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
			return br.lines().collect(Collectors.joining());
		}
	}

	/**
	 * Writes the given request into the given connection.
	 * 
	 * @param connection the connection
	 * @param jsonTendermintRequest the request
	 * @throws IOException if an I/O error occurred
	 * @throws TimeoutException if writing failed after repeated trying for some time
	 * @throws InterruptedException if the current thread was interrupted while writing
	 */
	private void writeInto(HttpURLConnection connection, String jsonTendermintRequest) throws IOException, TimeoutException, InterruptedException {
		byte[] input = jsonTendermintRequest.getBytes("utf-8");

		for (int i = 0; i < config.maxPingAttempts; i++) {
			try (OutputStream os = connection.getOutputStream()) {
				os.write(input, 0, input.length);
				return;
			}
			catch (ConnectException e) {
				// not sure why this happens, randomly. It seems that the connection to the Tendermint process is flaky
				Thread.sleep(config.pingDelay);
			}
		}

		throw new TimeoutException("Cannot write into Tendermint's connection. Tried " + config.maxPingAttempts + " times");
	}

	/**
	 * Opens a http POST connection to the Tendermint process.
	 * 
	 * @return the connection
	 * @throws IOException if the connection cannot be opened
	 */
	private HttpURLConnection openPostConnectionToTendermint() throws IOException {
		HttpURLConnection con = (HttpURLConnection) url().openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);

		return con;
	}

	/**
	 * Yields the URL of the Tendermint process.
	 * 
	 * @return the URL
	 * @throws MalformedURLException if the URL is not well formed
	 */
	private URL url() throws MalformedURLException {
		return new URL("http://127.0.0.1:" + config.tendermintPort);
	}
}