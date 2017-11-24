package org.achfrag.crypto.bitfinex;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.achfrag.crypto.Const;
import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
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
	WebsocketClientEndpoint websocketEndpoint;
	
	/**
	 * The channel map
	 */
	private final Map<String, Integer> channelMap = new HashMap<>();
	
	/**
	 * The channel callbacks
	 */
	private final Map<Integer, List<BiConsumer<String, Tick>>> channelCallbacks = new HashMap<>();
	
	/**
	 * The last heartbeat value
	 */
	protected long lastHeatbeat;
	
	/**
	 * The websocket auto reconnect flag
	 */
	protected volatile boolean autoReconnectEnabled = true;
	
	/**
	 * The Logger
	 */
	final static Logger logger = LoggerFactory.getLogger(BitfinexApiBroker.class);

	private Pattern CHANNEL_PATTERN = Pattern.compile("\\[(\\d+),(\\[.*)\\]");

	private Pattern CHANNEL_ELEMENT_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

	private Thread heartbeatThread;

	public void connect() throws APIException {
		try {
			final URI bitfinexURI = new URI(BITFINEX_URI);
			websocketEndpoint = new WebsocketClientEndpoint(bitfinexURI);
			websocketEndpoint.addConsumer(apiCallback);
			websocketEndpoint.addCloseHandler(this);
			websocketEndpoint.connect();
			lastHeatbeat = System.currentTimeMillis();
			
			heartbeatThread = new Thread(new HeartbeatThread(this));
			heartbeatThread.start();
		} catch (Exception e) {
			throw new APIException(e);
		}
	}
	
	public void disconnect() {
		
		if(heartbeatThread != null) {
			heartbeatThread.interrupt();
			heartbeatThread = null;
		}
		
		if(websocketEndpoint != null) {
			websocketEndpoint.removeConsumer(apiCallback);
			websocketEndpoint.close();
			websocketEndpoint = null;
		}
	}

	public void sendCommand(final AbstractAPICommand apiCommand) {
		websocketEndpoint.sendMessage(apiCommand.getCommand(this));
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
				final String channel = jsonObject.getString("channel");
			
			if(channel.equals("ticker")) {
					final int channelId = jsonObject.getInt("chanId");
					final String symbol = jsonObject.getString("symbol");
					logger.info("Registering symbol {} on channel {}", symbol, channelId);
					channelMap.put(symbol, channelId);
					channelCallbacks.put(channelId, new ArrayList<>());
				} else if(channel.equals("candles")) {
					final int channelId = jsonObject.getInt("chanId");
					final String key = jsonObject.getString("key");
					logger.info("Registering key {} on channel {}", key, channelId);
					channelMap.put(key, channelId);
					channelCallbacks.put(channelId, new ArrayList<>());
				} else {
					logger.error("Unknown subscribed callback {}", message);
				}

				break;
			case "pong":
				lastHeatbeat = System.currentTimeMillis();
				break;
			case "unsubscribed":
				final int channelId = jsonObject.getInt("chanId");
				final String symbol = getSymbolForChannel(channelId);
				logger.info("Channel {} ({}) is unsubscribed", channelId, symbol);
				channelCallbacks.remove(channelId);
				channelMap.remove(symbol);
				break;
			default:
				logger.error("Unknown event: {}", message);
		}
	}

	protected void handleChannelCallback(final String message) {
		// Channel callback
		logger.debug("Channel callback");
				
		final Matcher matcher = CHANNEL_PATTERN.matcher(message);
		
		if(! matcher.matches()) {
			if(message.contains("\"hb\"")) {
				lastHeatbeat = System.currentTimeMillis();
			} else {
				System.out.println("No match found: ");
			}
		} else {
			final int channel = Integer.parseInt(matcher.group(1));
			final String content = matcher.group(2);
			
			final String channelSymbol = getSymbolForChannel(channel);
			
			if(channelSymbol.contains("trade")) {
				handleCandlestickCallback(channel, channelSymbol, content);
			} else {
				handleTickCallback(channel, content);
			}
		}
	}

	/**
	 * Handle a candlestick callback
	 * @param channel
	 * @param content
	 */
	private void handleCandlestickCallback(final int channel, final String channelSymbol, final String content) {
		// remove [[..], [...]] -> [..], [..]
		final String ticks = content.substring(1, content.length()-1);
		

		// channel symbol trade:1m:tLTCUSD
		final String symbol = (channelSymbol.split(":"))[2];
		
		final Matcher contentMatcher = CHANNEL_ELEMENT_PATTERN.matcher(ticks);
		
		final List<Tick> ticksBuffer = new ArrayList<>();
		while (contentMatcher.find()) {
			final String element = contentMatcher.group(1);
			final String[] parts = element.split(",");
			
			// 0 = Timestamp
			// 1 = Open
			// 2 = Close
			// 3 = High 
			// 4 = Low
			// 5 = Volume
			
			final Timestamp timestampValue = new Timestamp(Long.parseLong(parts[0]));
			final LocalDateTime localtime = timestampValue.toLocalDateTime();
			final ZonedDateTime withTimezone = localtime.atZone(Const.BITFINEX_TIMEZONE);

			final Tick tick = new BaseTick(withTimezone, 
					Double.parseDouble(parts[1]), 
					Double.parseDouble(parts[2]), 
					Double.parseDouble(parts[3]), 
					Double.parseDouble(parts[4]), 
					Double.parseDouble(parts[5]));

			ticksBuffer.add(tick);
		}
		
		ticksBuffer.sort((t1, t2) -> t1.getEndTime().compareTo(t2.getEndTime()));
		
		final List<BiConsumer<String, Tick>> callbacks = channelCallbacks.get(channel);

		for(final Tick tick : ticksBuffer) {
			callbacks.forEach(c -> c.accept(symbol, tick));
		}
	}

	/**
	 * Handle a tick callback
	 * @param channel
	 * @param content
	 */
	protected void handleTickCallback(final int channel, final String content) {
		final Matcher contentMatcher = CHANNEL_ELEMENT_PATTERN.matcher(content);
		
		while (contentMatcher.find()) {
			final String element = contentMatcher.group(1);
			handleTickElement(channel, element);
		}
	}

	protected void handleTickElement(final int channel, final String element) {
		final String[] elements = element.split(",");
		// 0 = BID
		// 2 = ASK
		// 6 = Price
		final double price = Double.parseDouble(elements[6]);
		final Tick tick = new BaseTick(ZonedDateTime.now(), price, price, price, price, price);
		
		final String symbol = getSymbolForChannel(channel);
		
		final List<BiConsumer<String, Tick>> callbacks = channelCallbacks.get(channel);
		callbacks.forEach(c -> c.accept(symbol, tick));
	}

	/**
	 * Get the symbol for a given chanel
	 * @param channel
	 * @return
	 */
	private String getSymbolForChannel(final int channel) {
		final String symbol = channelMap.entrySet()
			.stream()
			.filter((v) -> v.getValue() == channel)
			.map((v) -> v.getKey())
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("Unable to find symbol: " + channel + " in " + channelMap));
		return symbol;
	}
	
	public void registerTickCallback(final CurrencyPair currencyPair, final BiConsumer<String, Tick> callback) throws APIException {
		final String currencyString = currencyPair.toBitfinexString();
		
		if(! channelMap.containsKey(currencyString)) {
			throw new APIException("Unknown ticker string: " + currencyString);
		}
		
		final Integer channel = channelMap.get(currencyString);
		channelCallbacks.get(channel).add(callback);
	}
	
	public boolean isTickerActive(final CurrencyPair currencyPair) {
		final String currencyString = currencyPair.toBitfinexString();
		return channelMap.containsKey(currencyString);
	}
	
	/* (non-Javadoc)
	 * @see org.achfrag.crypto.bitfinex.ReconnectHandler#handleReconnect()
	 */
	@Override
	public void handleWebsocketClose() {
		
		if(autoReconnectEnabled == false) {
			return;
		}
		
		reconnect(); 
	}

	protected synchronized void reconnect() {
		try {
			logger.info("Performing reconnect");
			
			websocketEndpoint.close();
			
			final Map<String, Integer> oldTickerMap = new HashMap<>();
			oldTickerMap.putAll(channelMap);
			channelMap.clear();
			
			final Map<Integer, List<BiConsumer<String, Tick>>> oldConsumerMap = new HashMap<>();
			oldConsumerMap.putAll(channelCallbacks);
			channelCallbacks.clear();
			
			websocketEndpoint.connect();
			
			oldTickerMap.keySet().forEach(c -> sendCommand(new SubscribeTicker(c)));
			
			logger.info("Waiting for ticker to resubscribe");
			while(channelMap.size() != oldTickerMap.size()) {
				Thread.sleep(100);
			}
			
			for(final String oldTicker : oldTickerMap.keySet()) {
				final Integer newChannel = channelMap.get(oldTicker);
				final Integer oldChannel = oldTickerMap.get(oldTicker);
				
				logger.info("Remapping channel {} / {} -> {}", oldTicker, oldChannel, newChannel);
				
				channelCallbacks.put(newChannel, oldConsumerMap.get(oldChannel));
			}

			lastHeatbeat = System.currentTimeMillis();					

		} catch (Exception e) {
			logger.error("Got exception while reconnect", e);
		}
	}

	public boolean isAutoReconnectEnabled() {
		return autoReconnectEnabled;
	}

	public void setAutoReconnectEnabled(boolean autoReconnectEnabled) {
		this.autoReconnectEnabled = autoReconnectEnabled;
	}
	
	public Map<String, Integer> getChannelMap() {
		return channelMap;
	}
	

}
