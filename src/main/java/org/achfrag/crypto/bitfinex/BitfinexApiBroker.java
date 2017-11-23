package org.achfrag.crypto.bitfinex;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.PingCommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeTicker;
import org.achfrag.crypto.bitfinex.misc.APIException;
import org.achfrag.crypto.bitfinex.misc.WebsocketClientEndpoint;
import org.achfrag.crypto.bitfinex.misc.WebsocketCloseHandler;
import org.achfrag.crypto.pair.CurrencyPair;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BaseTick;
import org.ta4j.core.Tick;

public class BitfinexApiBroker implements WebsocketCloseHandler {

	/**
	 * The bitfinex api
	 */
	public final static String BITFINEX_URI = "wss://api.bitfinex.com/ws/2";
	
	/**
	 * The API callback
	 */
	final Consumer<String> apiCallback = ((c) -> websocketCallback(c));
	
	/**
	 * The websocket endpoint
	 */
	private WebsocketClientEndpoint websocketEndpoint;
	
	/**
	 * The ticket map
	 */
	private final Map<String, Integer> tickerMap = new HashMap<>();
	
	/**
	 * The ticker callbacks
	 */
	private final Map<Integer, List<BiConsumer<String, Tick>>> tickerCallbacks = new HashMap<>();
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BitfinexApiBroker.class);

	private Pattern CHANNEL_PATTERN = Pattern.compile("\\[(\\d+),(\\[.*)\\]");

	private Pattern CHANNEL_ELEMENT_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

	private long lastHeatbeat;

	class HeartbeatThread implements Runnable {

		@Override
		public void run() {
			try {
				while(! Thread.interrupted()) {
					if(websocketEndpoint != null) {
						sendCommand(new PingCommand());
						
						if(lastHeatbeat + TimeUnit.SECONDS.toMillis(15) < System.currentTimeMillis()) {
							logger.error("Heartbeat timeout reconnecting");
							handleWebsocketClose();
						}
					}
					
					Thread.sleep(3000);
				}
			} catch(Throwable e) {
				logger.error("Got exception", e);
			}
		}
		
	}

	public void connect() throws APIException {
		try {
			final URI bitfinexURI = new URI(BITFINEX_URI);
			websocketEndpoint = new WebsocketClientEndpoint(bitfinexURI);
			websocketEndpoint.addConsumer(apiCallback);
			websocketEndpoint.addCloseHandler(this);
			websocketEndpoint.connect();
			lastHeatbeat = System.currentTimeMillis();
			
			(new Thread(new HeartbeatThread())).start();
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
	
	public void websocketCallback(final String message) {
		logger.debug("Got message: {}", message);
		
		if(message.startsWith("{")) {
			handleAPICallback(message);
		} else if(message.startsWith("[")) {
			handleChannelCallback(message);
		} else {
			logger.error("Got unknown callback: {}", message);
		}
	}

	protected void handleAPICallback(final String message) {
		// JSON callback
		final JSONTokener tokener = new JSONTokener(message);
		final JSONObject jsonObject = new JSONObject(tokener);
		
		final String eventType = jsonObject.getString("event");
		
		switch(eventType) {
			case "info":
				break;
			case "subscribed":
				final int channelId = jsonObject.getInt("chanId");
				final String symbol = jsonObject.getString("symbol");
				logger.info("Registering symbol {} on channel {}", symbol, channelId);
				tickerMap.put(symbol, channelId);
				tickerCallbacks.put(channelId, new ArrayList<>());
				break;
			case "pong":
				lastHeatbeat = System.currentTimeMillis();
				break;
			default:
				logger.error("Unknown event: {}", message);
		}
	}

	protected void handleChannelCallback(final String message) {
		// Channel callback
		logger.debug("Channel callback");
		
		Matcher matcher = CHANNEL_PATTERN.matcher(message);
		
		if(! matcher.matches()) {
			if(message.contains("\"hb\"")) {
				// Ignore HB
			} else {
				System.out.println("No match found: ");
			}
		} else {
			final int channel = Integer.parseInt(matcher.group(1));
			final String conntent = matcher.group(2);

			final Matcher contentMatcher = CHANNEL_ELEMENT_PATTERN.matcher(conntent);
			
			while (contentMatcher.find()) {
				final String element = contentMatcher.group(1);
				handleTickElement(channel, element);
			}
		}
	}

	protected void handleTickElement(final int channel, final String element) {
		final String[] elements = element.split(",");
		// 0 = BID
		// 2 = ASK
		// 6 = Price
		final float price = Float.parseFloat(elements[6]);
		final Tick tick = new BaseTick(ZonedDateTime.now(), price, price, price, price, price);
		
		final String symbol = tickerMap.entrySet()
			.stream()
			.filter((v) -> v.getValue() == channel)
			.map((v) -> v.getKey())
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("Unable to find symbol"));
		
		final List<BiConsumer<String, Tick>> callbacks = tickerCallbacks.get(channel);
		callbacks.forEach(c -> c.accept(symbol, tick));
	}
	
	public void registerTickCallback(final CurrencyPair currencyPair, final BiConsumer<String, Tick> callback) throws APIException {
		final String currencyString = currencyPair.toBitfinexString();
		
		if(! tickerMap.containsKey(currencyString)) {
			throw new APIException("Unknown ticker string: " + currencyString);
		}
		
		final Integer channel = tickerMap.get(currencyString);
		tickerCallbacks.get(channel).add(callback);
	}
	
	public boolean isTickerActive(final CurrencyPair currencyPair) {
		final String currencyString = currencyPair.toBitfinexString();
		return tickerMap.containsKey(currencyString);
	}
	
	/* (non-Javadoc)
	 * @see org.achfrag.crypto.bitfinex.ReconnectHandler#handleReconnect()
	 */
	@Override
	public void handleWebsocketClose() {
		try {
			logger.info("Performing reconnect");
			
			final Map<String, Integer> oldTickerMap = new HashMap<>();
			oldTickerMap.putAll(tickerMap);
			tickerMap.clear();
			
			final Map<Integer, List<BiConsumer<String, Tick>>> oldConsumerMap = new HashMap<>();
			oldConsumerMap.putAll(tickerCallbacks);
			tickerCallbacks.clear();
			
			websocketEndpoint.connect();
			
			oldTickerMap.keySet().forEach(c -> sendCommand(new SubscribeTicker(c)));
			
			logger.info("Waiting for ticker to resubscribe");
			while(tickerMap.size() != oldTickerMap.size()) {
				Thread.sleep(100);
			}
			
			for(final String oldTicker : oldTickerMap.keySet()) {
				final Integer newChannel = tickerMap.get(oldTicker);
				final Integer oldChannel = oldTickerMap.get(oldTicker);
				
				logger.info("Remapping channel {} / {} -> {}", oldTicker, oldChannel, newChannel);
				
				tickerCallbacks.put(newChannel, oldConsumerMap.get(oldChannel));
			}
			
		} catch (Exception e) {
			logger.error("Got exception while reconnect", e);
		} 
		
	}
}
