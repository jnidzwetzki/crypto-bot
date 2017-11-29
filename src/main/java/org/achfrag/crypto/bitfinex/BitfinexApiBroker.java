package org.achfrag.crypto.bitfinex;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.achfrag.crypto.Const;
import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.AuthCommand;
import org.achfrag.crypto.bitfinex.commands.CancelOrderCommand;
import org.achfrag.crypto.bitfinex.commands.CancelOrderGroupCommand;
import org.achfrag.crypto.bitfinex.commands.CommandException;
import org.achfrag.crypto.bitfinex.commands.OrderCommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeTickerCommand;
import org.achfrag.crypto.bitfinex.entity.APIException;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.crypto.bitfinex.entity.CurrencyPair;
import org.achfrag.crypto.bitfinex.entity.ExchangeOrder;
import org.achfrag.crypto.bitfinex.entity.Wallet;
import org.achfrag.crypto.bitfinex.websocket.WebsocketClientEndpoint;
import org.achfrag.crypto.bitfinex.websocket.WebsocketCloseHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BaseTick;
import org.ta4j.core.Tick;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class BitfinexApiBroker implements WebsocketCloseHandler {

	/**
	 * The bitfinex api
	 */
	public final static String BITFINEX_URI = "wss://api.bitfinex.com/ws/2";
	
	/**
	 * The API callback
	 */
	private final Consumer<String> apiCallback = ((c) -> websocketCallback(c));
	
	/**
	 * The websocket endpoint
	 */
	private WebsocketClientEndpoint websocketEndpoint;
	
	/**
	 * The channel map
	 */
	private final Map<Integer, String> channelIdSymbolMap;
	
	/**
	 * The channel callbacks
	 */
	private final Map<String, List<BiConsumer<String, Tick>>> channelCallbacks;
	
	/**
	 * The order callbacks
	 */
	private final List<Consumer<ExchangeOrder>> orderCallbacks;
	
	/**
	 * The last ticks
	 */
	protected final Map<String, Tick> lastTick;

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
	 * Wallets
	 * 
	 *  Currency, Wallet-Type, Wallet
	 */
	private final Table<String, String, Wallet> walletTable;
	
	/**
	 * The orders
	 */
	private final List<ExchangeOrder> orders;
	
	/**
	 * The Logger
	 */
	final static Logger logger = LoggerFactory.getLogger(BitfinexApiBroker.class);

	public BitfinexApiBroker() {
		this.channelIdSymbolMap = new HashMap<>();
		this.channelCallbacks = new HashMap<>();
		this.orderCallbacks = new ArrayList<>();
		this.lastHeatbeat = new AtomicLong();
		this.lastTick = new HashMap<>();
		this.walletTable = HashBasedTable.create();
		this.orders = new ArrayList<>();
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
	
	/**
	 * Get the websocket endpoint
	 * @return
	 */
	public WebsocketClientEndpoint getWebsocketEndpoint() {
		return websocketEndpoint;
	}
	
	/**
	 * We received a websocket callback
	 * @param message
	 */
	private void websocketCallback(final String message) {
		logger.debug("Got message: {}", message);
		
		if(message.startsWith("{")) {
			handleAPICallback(message);
		} else if(message.startsWith("[")) {
			handleChannelCallback(message);
		} else {
			logger.error("Got unknown callback: {}", message);
		}
	}

	/**
	 * Handle a API callback
	 */
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

	/**
	 * Add channel to symbol map
	 * @param channelId
	 * @param symbol
	 */
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

		// JSON callback
		final JSONTokener tokener = new JSONTokener(message);
		final JSONArray jsonArray = new JSONArray(tokener);
				
		final int channel = jsonArray.getInt(0);
		
		if(channel == 0) {
			handleSignalingChannelData(message, jsonArray);
		} else {
			handleChannelData(jsonArray, channel);
		}
	}

	/**
	 * Handle signaling channel data
	 * @param message
	 * @param jsonArray
	 */
	private void handleSignalingChannelData(final String message, final JSONArray jsonArray) {
		logger.info("Got info for channel 0: {}", message);

		final String subchannel = jsonArray.getString(1);
		logger.info("Subchannel is: " + subchannel);
		switch (subchannel) {
		case "hb":
			// Ignore channel heartbeat values
			break;
			
		case "ws":
			handleWalletsCallback(jsonArray);
			break;
			
		case "os":
			handleOrdersCallback(jsonArray);
			break;
			
		case "on":
			// Order notification
			handleOrdersCallback(jsonArray);
			break;
			
		case "oc":
			// Order cancel
			handleOrdersCallback(jsonArray);
			break;
			
		default:
			//logger.error("No match found for message {}", message);
			break;
		}
	}

	/**
	 * Handle the orders callback
	 * @param jsonArray
	 */
	private void handleOrdersCallback(final JSONArray jsonArray) {
		final JSONArray orders = jsonArray.getJSONArray(2);
		
		// No orders active
		if(orders.length() == 0) {
			return;
		}
		
		// Snapshot or update
		if(! (orders.get(0) instanceof JSONArray)) {
			handleOrderCallback(orders);
		} else {
			for(int walletPos = 0; walletPos < orders.length(); walletPos++) {
				final JSONArray orderArray = orders.getJSONArray(walletPos);
				handleOrderCallback(orderArray);
			}
		}
	}

	/**
	 * Handle a single order callback
	 * @param orderArray
	 */
	private void handleOrderCallback(final JSONArray order) {		
		final ExchangeOrder exchangeOrder = new ExchangeOrder();
		exchangeOrder.setOrderId(order.getInt(0));
		exchangeOrder.setGroupId(order.optInt(1, -1));
		exchangeOrder.setCid(order.getLong(2));
		exchangeOrder.setSymbol(order.getString(3));
		exchangeOrder.setCreated(order.getLong(4));
		exchangeOrder.setUpdated(order.getLong(5));
		exchangeOrder.setAmount(order.getDouble(6));
		exchangeOrder.setAmountAtCreation(order.getDouble(7));
		exchangeOrder.setOrderType(BitfinexOrderType.fromString(order.getString(8)));
		exchangeOrder.setState(order.getString(13));
		exchangeOrder.setPrice(order.getDouble(16));
		exchangeOrder.setPriceAvg(order.optDouble(17, -1));
		exchangeOrder.setPriceTrailing(order.optDouble(18, -1));
		exchangeOrder.setPriceAuxLimit(order.optDouble(19, -1));
		exchangeOrder.setNotify(order.getInt(23) == 1 ? true : false);
		exchangeOrder.setHidden(order.getInt(24) == 1 ? true : false);

		synchronized (orders) {
			// Replace order 
			orders.removeIf(o -> o.getCid() == exchangeOrder.getCid());
			orders.add(exchangeOrder);
			
			orders.notifyAll();
		}
		
		// Notify callbacks
		orderCallbacks.forEach(c -> c.accept(exchangeOrder));
	}

	/**
	 * Handle the wallets callback
	 * @param jsonArray
	 */
	private void handleWalletsCallback(final JSONArray jsonArray) {
		
		final JSONArray wallets = jsonArray.getJSONArray(2);
		
		// Snapshot or update
		if(! (wallets.get(0) instanceof JSONArray)) {
			handleWalletcallback(wallets);
		} else {
			for(int walletPos = 0; walletPos < wallets.length(); walletPos++) {
				final JSONArray walletArray = wallets.getJSONArray(walletPos);
				handleWalletcallback(walletArray);
			}
		}
	}

	/**
	 * Handle the callback for a single wallet
	 * @param walletArray
	 */
	private void handleWalletcallback(final JSONArray walletArray) {
		final String walletType = walletArray.getString(0);
		final String currency = walletArray.getString(1);
		final double balance = walletArray.getDouble(2);
		final float unsettledInterest = walletArray.getFloat(3);
		final float balanceAvailable = walletArray.optFloat(4, -1);
		
		final Wallet wallet = new Wallet(walletType, currency, balance, unsettledInterest, balanceAvailable);

		synchronized (walletTable) {
			walletTable.put(walletType, currency, wallet);
			walletTable.notifyAll();
		}
	}

	/**
	 * Handle normal channel data
	 * @param jsonArray
	 * @param channel
	 */
	private void handleChannelData(final JSONArray jsonArray, final int channel) {
		if(jsonArray.get(1) instanceof String) {
			final String value = jsonArray.getString(1);
			
			if("hb".equals(value)) {
				// Ignore heartbeat
			} else {
				logger.error("Unable to process: {}", jsonArray);
			}
		} else {	
			final JSONArray subarray = jsonArray.getJSONArray(1);			
			final String channelSymbol = getFromChannelSymbolMap(channel);
			
			if(channelSymbol.contains("trade")) {
				handleCandlestickCallback(channelSymbol, subarray);
			} else {
				handleTickCallback(channel, subarray);
			}
		}
	}

	/**
	 * Handle a candlestick callback
	 * @param channel
	 * @param subarray
	 */
	private void handleCandlestickCallback(final String channelSymbol, final JSONArray subarray) {

		// channel symbol trade:1m:tLTCUSD
		final String symbol = (channelSymbol.split(":"))[2];

		final List<Tick> ticksBuffer = new ArrayList<>();
		
		// Snapshots contain multiple Bars, Updates only one
		if(subarray.get(0) instanceof JSONArray) {
			for (int pos = 0; pos < subarray.length(); pos++) {
				final JSONArray parts = subarray.getJSONArray(pos);	
				paseCandlestick(ticksBuffer, parts);
			}
		} else {
			paseCandlestick(ticksBuffer, subarray);
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
	 * Parse a candlestick from JSON result
	 */
	private void paseCandlestick(final List<Tick> ticksBuffer, final JSONArray parts) {
		// 0 = Timestamp, 1 = Open, 2 = Close, 3 = High, 4 = Low,  5 = Volume
		final Instant i = Instant.ofEpochMilli(parts.getLong(0));
		final ZonedDateTime withTimezone = ZonedDateTime.ofInstant(i, Const.BITFINEX_TIMEZONE);
		
		final Tick tick = new BaseTick(withTimezone, 
				parts.getDouble(1), 
				parts.getDouble(2), 
				parts.getDouble(3), 
				parts.getDouble(4), 
				parts.getDouble(5));

		ticksBuffer.add(tick);
	}

	/**
	 * Handle a tick callback
	 * @param channel
	 * @param subarray
	 */
	protected void handleTickCallback(final int channel, final JSONArray subarray) {
				
		// 0 = BID
		// 2 = ASK
		// 6 = Price
		final double price = subarray.getDouble(6);
		final Tick tick = new BaseTick(ZonedDateTime.now(Const.BITFINEX_TIMEZONE), price, price, price, price, price);

		final String symbol = getFromChannelSymbolMap(channel);
		
		synchronized (lastTick) {
			lastTick.put(symbol, tick);
		}
		
		final List<BiConsumer<String, Tick>> callbacks = channelCallbacks.get(symbol);

		if(callbacks != null) {
			callbacks.forEach(c -> c.accept(symbol, tick));
		}
		
	}

	/**
	 * Get the channel from the symbol map - thread safe
	 * @param channel
	 * @return
	 */
	private String getFromChannelSymbolMap(final int channel) {
		synchronized (channelIdSymbolMap) {
			return channelIdSymbolMap.get(channel);
		}
	}

	/**
	 * Register a new tick callback
	 * @param symbol
	 * @param callback
	 * @throws APIException
	 */
	public void registerTickCallback(final String symbol, final BiConsumer<String, Tick> callback) throws APIException {
		
		if(! channelCallbacks.containsKey(symbol)) {
			channelCallbacks.put(symbol, new ArrayList<>());
		}
		
		channelCallbacks.get(symbol).add(callback);	
	}
	
	/**
	 * Remove the a tick callback
	 * @param symbol
	 * @param callback
	 * @return
	 * @throws APIException
	 */
	public boolean removeTickCallback(final String symbol, final BiConsumer<String, Tick> callback) throws APIException {
		
		if(! channelCallbacks.containsKey(symbol)) {
			throw new APIException("Unknown ticker string: " + symbol);
		}
			
		return channelCallbacks.get(symbol).remove(callback);
	}
	
	/**
	 * Test whether the ticker is active or not 
	 * @param currencyPair
	 * @return
	 */
	public boolean isTickerActive(final CurrencyPair currencyPair) {
		final String currencyString = currencyPair.toBitfinexString();
		
		return getChannelForSymbol(currencyString) != -1;
	}

	/**
	 * Find the channel for the given symol
	 * @param currencyString
	 * @return
	 */
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
					throw new APIException("Subscription of ticker failed");
				}
				
				channelIdSymbolMap.wait(500);
				execution++;	
			}
		}
	}
	
	/**
	 * Place a new order
	 */
	public void placeOrder(final BitfinexOrder order) {
		final OrderCommand orderCommand = new OrderCommand(order);
		sendCommand(orderCommand);
	}
	
	/**
	 * Cancel the given order
	 * @param cid
	 * @param date
	 */
	public void cancelOrder(final String id) {
		final CancelOrderCommand cancelOrder = new CancelOrderCommand(id);
		sendCommand(cancelOrder);
	}
	
	/**
	 * Cancel the given order group
	 * @param cid
	 * @param date
	 */
	public void cancelOrderGroup(final int id) {
		final CancelOrderGroupCommand cancelOrder = new CancelOrderGroupCommand(id);
		sendCommand(cancelOrder);
	}
	
	
	
	/**
	 * Get a set with active symbols
	 * @return
	 */
	public Set<String> getActiveSymbols() {
		synchronized (lastTick) {
			return lastTick.keySet();
		}
	}

	/**
	 * Is auto reconnecting enabled
	 * @return
	 */
	public boolean isAutoReconnectEnabled() {
		return autoReconnectEnabled;
	}

	/**
	 * Change auto reconnect status
	 * @param autoReconnectEnabled
	 */
	public void setAutoReconnectEnabled(final boolean autoReconnectEnabled) {
		this.autoReconnectEnabled = autoReconnectEnabled;
	}
	
	/**
	 * Get the last heartbeat value
	 * @return
	 */
	public AtomicLong getLastHeatbeat() {
		return lastHeatbeat;
	}
	
	/**
	 * Get the API key
	 * @return
	 */
	public String getApiKey() {
		return apiKey;
	}
	
	/**
	 * Get the API secret
	 * @return
	 */
	public String getApiSecret() {
		return apiSecret;
	}
	
	/**
	 * Get the last tick for a given symbol
	 * @param currencyPair
	 * @return 
	 */
	public Tick getLastTick(final CurrencyPair currencyPair) {
		final String bitfinexString = currencyPair.toBitfinexString();
		return getLastTick(bitfinexString);
	}
	
	/**
	 * Get the last tick for a given symbol
	 * @param currencyPair
	 * @return 
	 */
	public Tick getLastTick(final String bitfinexString) {
		synchronized (lastTick) {
			return lastTick.get(bitfinexString);
		}
	}
	
	/**
	 * Get all wallets
	 * @return 
	 */
	public Collection<Wallet> getWallets() {
		synchronized (walletTable) {
			return Collections.unmodifiableCollection(walletTable.values());
		}
	}
	
	/**
	 * Get the list with exchange orders
	 * @return
	 */
	public List<ExchangeOrder> getOrders() {
		synchronized (orders) {
			return Collections.unmodifiableList(orders);
		}
	}
	
	/**
	 * Add a order callback
	 * @param callback
	 */
	public void addOrderCallback(final Consumer<ExchangeOrder> callback) {
		orderCallbacks.add(callback);
	}
	
	/**
	 * Remove a order callback
	 * @param callback
	 * @return
	 */
	public boolean removeOrderCallback(final Consumer<ExchangeOrder> callback) {
		return orderCallbacks.remove(callback);
	}
}
