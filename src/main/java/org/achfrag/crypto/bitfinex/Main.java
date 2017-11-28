package org.achfrag.crypto.bitfinex;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeCandlesCommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeTickerCommand;
import org.achfrag.crypto.bitfinex.commands.UnsubscribeCandlesCommand;
import org.achfrag.crypto.bitfinex.misc.APIException;
import org.achfrag.crypto.bitfinex.misc.CurrencyPair;
import org.achfrag.crypto.bitfinex.misc.TickMerger;
import org.achfrag.crypto.bitfinex.misc.Timeframe;
import org.achfrag.crypto.strategy.EMAStrategy03;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Order.OrderType;
import org.ta4j.core.Strategy;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.Trade;

public class Main implements Runnable {

	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	protected final Map<String, TickMerger> tickMerger;
	
	protected final Map<String, TimeSeries> timeSeries;

	protected final Map<String, Strategy> strategies;

	protected final List<CurrencyPair> currencies; 
	
	protected final Map<String, List<Trade>> trades;
	
	protected static final Timeframe TIMEFRAME = Timeframe.MINUTES_15;

	/**
	 * The API broker
	 */
	private BitfinexApiBroker bitfinexApiBroker;

	public Main() {
		tickMerger = new HashMap<>();
		timeSeries = new HashMap<>();
		strategies = new HashMap<>();
		this.trades = new HashMap<>();
		currencies = Arrays.asList(CurrencyPair.BTC_USD, CurrencyPair.ETH_USD, CurrencyPair.LTC_USD);
	}

	@Override
	public void run() {
		try {
			bitfinexApiBroker = buildBifinexClient();
			
			bitfinexApiBroker.connect();

			requestHistoricalData(bitfinexApiBroker);			
			registerTicker(bitfinexApiBroker);

			while (true) {
				Thread.sleep(TimeUnit.MINUTES.toMillis(5));
			}
		} catch (Exception e) {
			logger.error("Got exception", e);
		}
	}

	private BitfinexApiBroker buildBifinexClient() {
		final Properties prop = new Properties();
		
		try {
			final InputStream input = Main.class.getClassLoader().getResourceAsStream("auth.properties");
			
			if(input != null) {
				prop.load(input);
				
				if("true".equals(prop.getProperty("authEnabled"))) {
					final String apiKey = prop.getProperty("apiKey");
					final String apiSecret = prop.getProperty("apiSecret");
					
					if(apiKey == null || apiSecret == null) {
						logger.warn("API key or secret are null");
					} else {
						logger.info("Building authenticated client");
						return new BitfinexApiBroker(apiKey, apiSecret);
					}
				}
			}
		} catch(Exception e) {
			logger.error("Unable to load properties", e);
		}
		
		// Unauthenticated client
		logger.info("Building unauthenticated client");
		return new BitfinexApiBroker();
	}

	private void requestHistoricalData(final BitfinexApiBroker bitfinexApiBroker) throws InterruptedException, APIException {
		logger.info("Request historical candles");
		for(final CurrencyPair currency : currencies) {
			
			final String bitfinexString = currency.toBitfinexString();
			final BaseTimeSeries currencyTimeSeries = new BaseTimeSeries(bitfinexString);
			timeSeries.put(bitfinexString, currencyTimeSeries);
			final Strategy strategy = EMAStrategy03.getStrategy(currencyTimeSeries, 5, 12, 40);
			strategies.put(bitfinexString, strategy);
			trades.put(bitfinexString, new ArrayList<>());

			// Add bars to timeseries callback
			final BiConsumer<String, Tick> callback = (symbol, tick) -> {
				final TimeSeries timeSeriesToAdd = timeSeries.get(symbol);
				
				try { 
					timeSeriesToAdd.addTick(tick);
				} catch(IllegalArgumentException e) {
					logger.error("Unable to add tick {} to time series, last tick is {}", 
							tick, 
							timeSeriesToAdd.getLastTick());
				}
			};
			
			final String barSymbol = currency.toBifinexCandlestickString(TIMEFRAME);
			
			bitfinexApiBroker.registerTickCallback(barSymbol, callback);
			bitfinexApiBroker.sendCommand(new SubscribeCandlesCommand(currency, TIMEFRAME));
			Thread.sleep(TimeUnit.SECONDS.toMillis(1));
			
			bitfinexApiBroker.removeTickCallback(barSymbol, callback);
			bitfinexApiBroker.sendCommand(new UnsubscribeCandlesCommand(currency, TIMEFRAME));
			
			System.out.println("Loaded ticks for symbol " 
					+ bitfinexString + " " 
					+ timeSeries.get(bitfinexString).getEndIndex());
			
		/*	final Chart chart = new Chart(bitfinexString, strategy, currencyTimeSeries);
			chart.showChart();*/
		}
	}

	protected void registerTicker(final BitfinexApiBroker bitfinexApiBroker) throws InterruptedException, APIException {
		
		logger.info("Register ticker");
		
		for(final CurrencyPair currency : currencies) {
			
			final String bitfinexString = currency.toBitfinexString();

			tickMerger.put(bitfinexString, new TickMerger(bitfinexString, TIMEFRAME, (s, t) -> barDoneCallback(s, t)));
		
			final AbstractAPICommand subscribeCommandTicker = new SubscribeTickerCommand(currency);
			bitfinexApiBroker.sendCommand(subscribeCommandTicker);

			System.out.println("Wait for ticker");

			while (!bitfinexApiBroker.isTickerActive(currency)) {
				Thread.sleep(100);
			}

			bitfinexApiBroker.registerTickCallback(currency.toBitfinexString(), (s, c) -> handleTickCallback(s, c));
		}
	}

	private void barDoneCallback(final String symbol, final Tick tick) {		
		try {
			timeSeries.get(symbol).addTick(tick);
		} catch(Throwable e) {
			logger.error("Unable to add {} to symbol {}", tick, symbol);
		}
		
		final int endIndex = timeSeries.get(symbol).getEndIndex();
		
		if (strategies.get(symbol).shouldEnter(endIndex)) {
			final Trade openTrade = getOpenTrade(symbol);
			if(openTrade == null) {
				final Trade trade = new Trade(OrderType.BUY);
				trade.operate(endIndex, timeSeries.get(symbol).getLastTick().getClosePrice(), Decimal.valueOf(1));
				trades.get(symbol).add(trade);
			}
		} else if (strategies.get(symbol).shouldExit(endIndex)) {
			final Trade openTrade = getOpenTrade(symbol);
			if(openTrade != null) {
				openTrade.operate(endIndex, timeSeries.get(symbol).getLastTick().getClosePrice(), Decimal.valueOf(1));
			}
		}
		
		updateScreen();
	}

	/**
	 * Get the open trade for symbol or null
	 * @param symbol
	 * @return
	 */
	private Trade getOpenTrade(final String symbol) {
		final List<Trade> tradeList = trades.get(symbol);

		final List<Trade> openTrades = tradeList.stream().filter(t -> ! t.isClosed()).collect(Collectors.toList());
		
		if(openTrades.size() > 1) {
			throw new IllegalArgumentException("More then one open trade for " + symbol + " / " + openTrades);
		}
		
		if(openTrades.isEmpty()) {
			return null;
		}
		
		return openTrades.get(0);
	}
	
	private void handleTickCallback(final String symbol, final Tick tick) {		
		tickMerger.get(symbol).addNewPrice(
				tick.getEndTime().toEpochSecond() * 1000, 
				tick.getOpenPrice().toDouble(), 
				tick.getVolume().toDouble());
		
		updateScreen();
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
	
	public synchronized void updateScreen() {
		clearScreen();
		System.out.println("");
		System.out.println("==========");
		System.out.println("Last ticks");
		System.out.println("==========");
		for(final CurrencyPair currency : currencies) {
			final String symbol = currency.toBitfinexString();
			System.out.println(symbol + " " + bitfinexApiBroker.getLastTick(currency));
		}
		
		System.out.println("");
		System.out.println("==========");
		System.out.println("Last bars");
		System.out.println("==========");
		for(final CurrencyPair currency : currencies) {
			final String symbol = currency.toBitfinexString();
			System.out.println(symbol + " " + timeSeries.get(symbol).getLastTick());
		}
		
		System.out.println("");
		System.out.println("==========");
		System.out.println("P/L");
		System.out.println("==========");
		for(final CurrencyPair currency : currencies) {
			final String symbol = currency.toBitfinexString();
			
			final Trade trade = getOpenTrade(symbol);
			if(trade != null) {
				final double priceIn = trade.getEntry().getPrice().toDouble();
				final double currentPrice = bitfinexApiBroker.getLastTick(currency).getClosePrice().toDouble();
				System.out.println("Price in " + priceIn + " / " + (currentPrice - priceIn));
			}	
		}
		
		System.out.println("");
		System.out.println("==========");
		System.out.println("Trades");
		System.out.println("==========");
		for(final CurrencyPair currency : currencies) {
			final String symbol = currency.toBitfinexString();
			System.out.println(symbol + " " + trades.get(symbol));
		}
	}
	
	public void clearScreen() {  
	    System.out.print("\033[H\033[2J");  
	    System.out.flush();  
	}  
}
