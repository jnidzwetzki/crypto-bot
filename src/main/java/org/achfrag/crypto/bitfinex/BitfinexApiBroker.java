package org.achfrag.crypto.bitfinex;

import java.net.URI;
import java.util.function.Consumer;

import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.misc.APIException;
import org.achfrag.crypto.bitfinex.misc.WebsocketClientEndpoint;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitfinexApiBroker {

	/**
	 * The bitfinex api
	 */
	public final static String BITFINEX_URI = "wss://api.bitfinex.com/ws/2";
	
	/**
	 * The API callback
	 */
	final Consumer<String> apiCallback = ((c) -> handleAPICallback(c));
	
	/**
	 * The websocket endpoint
	 */
	private WebsocketClientEndpoint websocketEndpoint;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BitfinexApiBroker.class);


	public void connect() throws APIException {
		try {
			final URI bitfinexURI = new URI(BITFINEX_URI);
			websocketEndpoint = new WebsocketClientEndpoint(bitfinexURI);
			websocketEndpoint.addConsumer(apiCallback);
			websocketEndpoint.connect();
			
		} catch (Exception e) {
			throw new APIException(e);
		}
	}
	
	public void disconnect() {
		if(websocketEndpoint != null) {
			websocketEndpoint.removeConsumer(apiCallback);
			websocketEndpoint = null;
		}
	}

	public void sendCommand(final AbstractAPICommand apiCommand) {
		websocketEndpoint.sendMessage(apiCommand.getCommand());
	}
	
	public void handleAPICallback(final String message) {
		System.out.println("Got message: " + message);
		
		if(message.startsWith("{")) {
			// JSON callback
			final JSONTokener tokener = new JSONTokener(message);
			final JSONObject jsonObject = new JSONObject(tokener);
		} else if(message.startsWith("[")) {
			// Channel callback
			System.out.println("Channel callback");
		} else {
			logger.error("Got unknown callback: {}", message);
		}
	}
}
