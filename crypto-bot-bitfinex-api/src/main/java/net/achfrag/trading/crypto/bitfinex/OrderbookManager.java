package net.achfrag.trading.crypto.bitfinex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.TradingOrderbookEntry;

public class OrderbookManager {

	/**
	 * The channel callbacks
	 */
	private final Map<String, List<BiConsumer<String, TradingOrderbookEntry>>> channelCallbacks;
	
	/**
	 * The executor service
	 */
	private ExecutorService executorService;

	public OrderbookManager(final ExecutorService executorService) {
		this.executorService = executorService;
		this.channelCallbacks = new HashMap<>();
	}
	
	/**
	 * Register a new trading orderbook callback
	 * @param symbol
	 * @param callback
	 * @throws APIException
	 */
	public void registerTradingOrderbookCallback(final String symbol, 
			final BiConsumer<String, TradingOrderbookEntry> callback) throws APIException {
		
		channelCallbacks.putIfAbsent(symbol, new ArrayList<>());

		final List<BiConsumer<String, TradingOrderbookEntry>> callbacks = channelCallbacks.get(symbol);
		
		synchronized (callbacks) {
			callbacks.add(callback);	
		}
	}
	
	/**
	 * Remove the a trading orderbook callback
	 * @param symbol
	 * @param callback
	 * @return
	 * @throws APIException
	 */
	public boolean removeTradingOrderbookCallback(final String symbol, 
			final BiConsumer<String, TradingOrderbookEntry> callback) throws APIException {
		
		if(! channelCallbacks.containsKey(symbol)) {
			throw new APIException("Unknown ticker string: " + symbol);
		}
			
		final List<BiConsumer<String, TradingOrderbookEntry>> callbacks = channelCallbacks.get(symbol);
		
		synchronized (callbacks) {
			return callbacks.remove(callback);
		}
	}
}
