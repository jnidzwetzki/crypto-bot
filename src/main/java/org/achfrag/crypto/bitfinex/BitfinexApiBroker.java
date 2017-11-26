package org.achfrag.crypto.bitfinex;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import org.achfrag.crypto.Const;
import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.AuthCommand;
import org.achfrag.crypto.bitfinex.commands.CommandException;
import org.achfrag.crypto.bitfinex.commands.SubscribeTickerCommand;
import org.achfrag.crypto.bitfinex.misc.APIException;
import org.achfrag.crypto.bitfinex.misc.CurrencyPair;
import org.achfrag.crypto.bitfinex.misc.WebsocketClientEndpoint;
import org.achfrag.crypto.bitfinex.misc.WebsocketCloseHandler;
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
	private final Map<Integer, String> channelIdSymbolMap;
	
	/**
	 * The channel callbacks
	 */
	private final Map<String, List<BiConsumer<String, Tick>>> channelCallbacks;
	
	/**
	 * The last heartbeat value
	 */
	protected final AtomicLong lastHeatbeat;
	
	/**
	 * The websocket auto reconnect flag
	 */
	protected volatile boolean autoReconnectEnabled = true;

	/**
	 * The heartbeat thread
	 */
	private Thread heartbeatThread;
	
	/**
	 * The API key
	 */
	private String apiKey;
	
	/**
	 * The API secret
	 */
	private String apiSecret;
	
	/**
	 * The authentification latch
	 */
	private CountDownLatch authentificatedLatch;
	
	/**
	 * The Logger
	 */
	final static Logger logger = LoggerFactory.getLogger(BitfinexApiBroker.class);

	public BitfinexApiBroker() {
		this.channelIdSymbolMap = new HashMap<>();
		this.channelCallbacks = new HashMap<>();
		this.lastHeatbeat = new AtomicLong();
	}
	
	public BitfinexApiBroker(final String apiKey, final String apiSecret) {
		this();
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}
	
	public void connect() throws APIException {
		try {
			final URI bitfinexURI = new URI(BITFINEX_URI);
			websocketEndpoint = new WebsocketClientEndpoint(bitfinexURI);
			websocketEndpoint.addConsumer(apiCallback);
			websocketEndpoint.addCloseHandler(this);
			websocketEndpoint.connect();
			handlePongCallback();
			
			executeAuthentification();
			
			heartbeatThread = new Thread(new HeartbeatThread(this));
			heartbeatThread.start();
		} catch (Exception e) {
			throw new APIException(e);
		}
	}

	private void executeAuthentification() throws InterruptedException {
		authentificatedLatch = new CountDownLatch(1);
		if(apiKey != null && apiSecret != null) {
			sendCommand(new AuthCommand());
			logger.info("Waiting for authentification");
			authentificatedLatch.await(10, TimeUnit.SECONDS);
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
		try {
			websocketEndpoint.sendMessage(apiCommand.getCommand(this));
		} catch (CommandException e) {
			logger.error("Got Exception while sending command", e);
		}
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

		switch (eventType) {
		case "info":
			break;
		case "subscribed":
			handleSubscribedCallback(message, jsonObject);
			break;
		case "pong":
			handlePongCallback();
			break;
		case "unsubscribed":
			handleUnsubscribedCallback(jsonObject);
			break;
		case "auth":
			handleAuthCallback(jsonObject);
			break;
		default:
			logger.error("Unknown event: {}", message);
		}
	}

	private void handleAuthCallback(final JSONObject jsonObject) {
		authentificatedLatch.countDown();
		logger.info("Authentification successfully {}", jsonObject.getString("status"));
	}

	private void handleUnsubscribedCallback(final JSONObject jsonObject) {
		synchronized (channelIdSymbolMap) {
			final int channelId = jsonObject.getInt("chanId");
			final String symbol = getFromChannelSymbolMap(channelId);
			logger.info("Channel {} ({}) is unsubscribed", channelId, symbol);
			channelCallbacks.remove(symbol);
			channelIdSymbolMap.remove(channelId);
			channelIdSymbolMap.notifyAll();
		}
	}

	private void handlePongCallback() {
		lastHeatbeat.set(System.currentTimeMillis());
	}

	private void handleSubscribedCallback(final String message, final JSONObject jsonObject) {
		final String channel = jsonObject.getString("channel");

		if (channel.equals("ticker")) {
			final int channelId = jsonObject.getInt("chanId");
			final String symbol = jsonObject.getString("symbol");
			logger.info("Registering symbol {} on channel {}", symbol, channelId);
			addToChannelSymbolMap(channelId, symbol);
		} else if (channel.equals("candles")) {
			final int channelId = jsonObject.getInt("chanId");
			final String key = jsonObject.getString("key");
			logger.info("Registering key {} on channel {}", key, channelId);
			addToChannelSymbolMap(channelId, key);
		} else {
			logger.error("Unknown subscribed callback {}", message);
		}
	}

	private void addToChannelSymbolMap(final int channelId, final String symbol) {
		synchronized (channelIdSymbolMap) {
			channelIdSymbolMap.put(channelId, symbol);
			channelIdSymbolMap.notifyAll();
		}
	}

	protected void handleChannelCallback(final String message) {
		// Channel callback
		logger.debug("Channel callback");
		handlePongCallback();

		final Matcher matcher = BitfinexApiHelper.CHANNEL_PATTERN.matcher(message);
		
		if(! matcher.matches()) {
			if(message.contains("\"hb\"")) {
				// Ignore channel heartbeat values
			} else if(message.startsWith("[0,")) {
				logger.info("Got info for channel 0: {}", message);
			} else {
				logger.error("No match found for message {}", message);
			}
		} else {
			final int channel = Integer.parseInt(matcher.group(1));
			final String content = matcher.group(2);
			
			final String channelSymbol = getFromChannelSymbolMap(channel);
			
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
		
		final Matcher contentMatcher = BitfinexApiHelper.CHANNEL_ELEMENT_PATTERN.matcher(ticks);
		
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
			final Instant i = Instant.ofEpochMilli(Long.parseLong(parts[0]));
			final ZonedDateTime withTimezone = ZonedDateTime.ofInstant(i, Const.BITFINEX_TIMEZONE);
			
			final Tick tick = new BaseTick(withTimezone, 
					Double.parseDouble(parts[1]), 
					Double.parseDouble(parts[2]), 
					Double.parseDouble(parts[3]), 
					Double.parseDouble(parts[4]), 
					Double.parseDouble(parts[5]));

			ticksBuffer.add(tick);
		}
		
		ticksBuffer.sort((t1, t2) -> t1.getEndTime().compareTo(t2.getEndTime()));

		final List<BiConsumer<String, Tick>> callbacks = channelCallbacks.get(channelSymbol);

		if(callbacks != null) {
			for(final Tick tick : ticksBuffer) {
				callbacks.forEach(c -> c.accept(symbol, tick));
			}
		}
	}

	/**
	 * Handle a tick callback
	 * @param channel
	 * @param content
	 */
	protected void handleTickCallback(final int channel, final String content) {
		final Matcher contentMatcher = BitfinexApiHelper.CHANNEL_ELEMENT_PATTERN.matcher(content);
		
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
		final Tick tick = new BaseTick(ZonedDateTime.now(Const.BITFINEX_TIMEZONE), price, price, price, price, price);

		final String symbol = getFromChannelSymbolMap(channel);
		
		final List<BiConsumer<String, Tick>> callbacks = channelCallbacks.get(symbol);

		if(callbacks != null) {
			callbacks.forEach(c -> c.accept(symbol, tick));
		}
	}

	private String getFromChannelSymbolMap(final int channel) {
		synchronized (channelIdSymbolMap) {
			return channelIdSymbolMap.get(channel);
		}
	}

	
	public void registerTickCallback(final String symbol, final BiConsumer<String, Tick> callback) throws APIException {
		
		if(! channelCallbacks.containsKey(symbol)) {
			channelCallbacks.put(symbol, new ArrayList<>());
		}
		
		channelCallbacks.get(symbol).add(callback);	
	}
	
	public boolean removeTickCallback(final String symbol, final BiConsumer<String, Tick> callback) throws APIException {
		
		if(! channelCallbacks.containsKey(symbol)) {
			throw new APIException("Unknown ticker string: " + symbol);
		}
		
		return channelCallbacks.get(symbol).remove(callback);
	}
	
	public boolean isTickerActive(final CurrencyPair currencyPair) {
		final String currencyString = currencyPair.toBitfinexString();
		
		return getChannelForSymbol(currencyString) != -1;
	}

	public Integer getChannelForSymbol(final String currencyString) {
		synchronized (channelIdSymbolMap) {
			return channelIdSymbolMap.entrySet()
					.stream()
					.filter((v) -> v.getValue().equals(currencyString))
					.map((v) -> v.getKey())
					.findAny().orElse(-1);
		}
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

	/**
	 * Perform a reconnect
	 * @return
	 */
	protected synchronized boolean reconnect() {
		try {
			logger.info("Performing reconnect");
			
			websocketEndpoint.close();
			websocketEndpoint.connect();
			
			executeAuthentification();
			resubscribeChannels();

			handlePongCallback();
			
			return true;
		} catch (Exception e) {
			logger.error("Got exception while reconnect", e);
			return false;
		}
	}

	/**
	 * Resubscribe the old ticker
	 * @throws InterruptedException
	 * @throws APIException
	 */
	private void resubscribeChannels() throws InterruptedException, APIException {
		final Map<Integer, String> oldChannelIdSymbolMap = new HashMap<>();

		synchronized (channelIdSymbolMap) {
			oldChannelIdSymbolMap.putAll(channelIdSymbolMap);
			channelIdSymbolMap.clear();
			channelIdSymbolMap.notifyAll();
		}
		
		oldChannelIdSymbolMap.entrySet().forEach((e) -> sendCommand(new SubscribeTickerCommand(e.getValue())));
		
		logger.info("Waiting for ticker to resubscribe");
		int execution = 0;
		
		synchronized (channelIdSymbolMap) {		
			while(channelIdSymbolMap.size() != oldChannelIdSymbolMap.size()) {
				
				if(execution > 10) {
					throw new APIException("Subscription of tiker failed");
				}
				
				channelIdSymbolMap.wait(500);
				execution++;	
			}
		}
	}

	public boolean isAutoReconnectEnabled() {
		return autoReconnectEnabled;
	}

	public void setAutoReconnectEnabled(boolean autoReconnectEnabled) {
		this.autoReconnectEnabled = autoReconnectEnabled;
	}
	
	/**
	 * Get the last heartbeat value
	 * @return
	 */
	public AtomicLong getLastHeatbeat() {
		return lastHeatbeat;
	}
	
	public String getApiKey() {
		return apiKey;
	}
	
	public String getApiSecret() {
		return apiSecret;
	}
}
