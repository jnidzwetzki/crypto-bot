package org.achfrag.crypto;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements Runnable {

	public final static String BITFINEX_URI = "wss://api.bitfinex.com/ws/2";
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	@Override
	public void run() {
		try {
			final URI bitfinexURI = new URI(BITFINEX_URI);
			final WebsocketClientEndpoint websocketEndpoint = new WebsocketClientEndpoint(bitfinexURI);
			
			Thread.sleep(TimeUnit.SECONDS.toMillis(2));

			final JSONObject subscribeJson = new JSONObject();
			subscribeJson.put("event", "subscribe");
			subscribeJson.put("channel", "ticker");
			subscribeJson.put("symbol", "tETHBTC");
			
			final String sucscribeMessage = subscribeJson.toString();
			
			System.out.println("Register ticker: " + sucscribeMessage);

			websocketEndpoint.sendMessage(sucscribeMessage);
			
			Thread.sleep(TimeUnit.MINUTES.toMillis(5));
			
		} catch (Exception e) {
			logger.error("Got exception", e);
		}
		
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
