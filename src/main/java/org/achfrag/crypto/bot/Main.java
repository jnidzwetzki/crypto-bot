package org.achfrag.crypto.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.BitfinexClientFactory;
import org.achfrag.crypto.bitfinex.BitfinexOrderBuilder;
import org.achfrag.crypto.bitfinex.TickerManager;
import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeCandlesCommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeTickerCommand;
import org.achfrag.crypto.bitfinex.commands.UnsubscribeCandlesCommand;
import org.achfrag.crypto.bitfinex.entity.APIException;
import org.achfrag.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.crypto.bitfinex.entity.ExchangeOrder;
import org.achfrag.crypto.bitfinex.entity.Timeframe;
import org.achfrag.crypto.bitfinex.entity.Trade;
import org.achfrag.crypto.bitfinex.entity.TradeDirection;
import org.achfrag.crypto.bitfinex.entity.TradeState;
import org.achfrag.crypto.bitfinex.util.TickMerger;
import org.achfrag.crypto.strategy.EMAStrategy03;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Order.OrderType;
import org.ta4j.core.Strategy;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;

public class Main implements Runnable {

	protected final Map<String, TickMerger> tickMerger;
	
	protected final Map<String, TimeSeries> timeSeries;

	protected final Map<String, Strategy> strategies;

	protected final List<BitfinexCurrencyPair> tradedCurrencies; 
		
	protected final OrderManager orderManager;
	
	private final Map<BitfinexCurrencyPair, List<Trade>> trades;
	
	public static boolean UPDATE_SCREEN = true;
	
	protected static final Timeframe TIMEFRAME = Timeframe.MINUTES_15;

	/**
	 * The API broker
	 */
	private final BitfinexApiBroker bitfinexApiBroker;

	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	
	public Main() {
		this.tickMerger = new HashMap<>();
		this.timeSeries = new HashMap<>();
		this.strategies = new HashMap<>();
		this.bitfinexApiBroker = BitfinexClientFactory.buildBifinexClient();
		this.orderManager = new OrderManager(bitfinexApiBroker);
		this.trades = new HashMap<>();
		this.tradedCurrencies = Arrays.asList(BitfinexCurrencyPair.BTC_USD);
	}

	@Override
	public void run() {
		try {			
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

	private void requestHistoricalData(final BitfinexApiBroker bitfinexApiBroker) throws InterruptedException, APIException {
		logger.info("Request historical candles");
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			
			final String bitfinexString = currency.toBitfinexString();
			final BaseTimeSeries currencyTimeSeries = new BaseTimeSeries(bitfinexString);
			timeSeries.put(bitfinexString, currencyTimeSeries);
			final Strategy strategy = EMAStrategy03.getStrategy(currencyTimeSeries, 5, 12, 40);
			strategies.put(bitfinexString, strategy);

			final CountDownLatch tickCountdown = new CountDownLatch(100);
			
			// Add bars to timeseries callback
			final BiConsumer<String, Tick> callback = (channelSymbol, tick) -> {
				
				// channel symbol trade:1m:tLTCUSD
				final String symbol = (channelSymbol.split(":"))[2];

				final TimeSeries timeSeriesToAdd = timeSeries.get(symbol);
				
				try { 
					timeSeriesToAdd.addTick(tick);
					tickCountdown.countDown();
				} catch(IllegalArgumentException e) {
					logger.error("Unable to add tick {} to time series, last tick is {}", 
							tick, 
							timeSeriesToAdd.getLastTick());
				}
			};
			
			final String barSymbol = currency.toBifinexCandlestickString(TIMEFRAME);
			
			bitfinexApiBroker.registerTickCallback(barSymbol, callback);
			bitfinexApiBroker.sendCommand(new SubscribeCandlesCommand(currency, TIMEFRAME));

			// Wait for 100 tics or 10 seconds. All snapshot ticks are handled in 
			// a syncronized block, so we receive the full snapshot even if we 
			// call removeTickCallback.
			tickCountdown.await(10, TimeUnit.SECONDS);
			
			bitfinexApiBroker.removeTickCallback(barSymbol, callback);
			bitfinexApiBroker.sendCommand(new UnsubscribeCandlesCommand(currency, TIMEFRAME));
			
			logger.info("Loaded ticks for symbol {} {}", bitfinexString,
					+ timeSeries.get(bitfinexString).getEndIndex());
			
		/*	final Chart chart = new Chart(bitfinexString, strategy, currencyTimeSeries);
			chart.showChart();*/
		}
	}

	protected void registerTicker(final BitfinexApiBroker bitfinexApiBroker) throws InterruptedException, APIException {
		
		logger.info("Register ticker");
		
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			
			final String bitfinexString = currency.toBitfinexString();

			tickMerger.put(bitfinexString, new TickMerger(bitfinexString, TIMEFRAME, (s, t) -> barDoneCallback(s, t)));
		
			final AbstractAPICommand subscribeCommandTicker = new SubscribeTickerCommand(currency);
			bitfinexApiBroker.sendCommand(subscribeCommandTicker);

			logger.info("Wait for ticker");

			while (! bitfinexApiBroker.isTickerActive(currency)) {
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
			openOrder(symbol, endIndex);
		} else if (strategies.get(symbol).shouldExit(endIndex)) {
			closeOrder(symbol, endIndex);
		}
		
		updateScreen();
	}

	/**
	 * Execute a new close order
	 * @param symbol
	 * @param endIndex
	 * @throws APIException
	 */
	private void closeOrder(final String symbol, final int endIndex) {
		
		final BitfinexCurrencyPair currency = BitfinexCurrencyPair.fromSymbolString(symbol);
		//final Decimal orderSize = Decimal.valueOf(currency.getMinimalOrderSize());
		final Decimal lastClosePrice = timeSeries.get(symbol).getLastTick().getClosePrice();
		
		final Trade openTrade = getOpenTrade(currency);
		
		if(openTrade == null) {
			logger.error("Unable to close a trade, there is no trade open");
			return;
		}
				
		final double amount = openTrade.getAmount() * -1.0;
		
		final BitfinexOrder order = BitfinexOrderBuilder
				.create(currency, BitfinexOrderType.EXCHANGE_MARKET, amount)
				.build();
		
		openTrade.setTradeState(TradeState.CLOSING);
		openTrade.setExpectedPriceClose(lastClosePrice.toDouble());
		openTrade.addCloseOrder(order);
		orderManager.executeOrder(order);
		openTrade.setTradeState(TradeState.CLOSED);
	}

	/**
	 * Execute a new open position order
	 * @param symbol
	 * @param endIndex
	 * @throws APIException
	 */
	private void openOrder(final String symbol, final int endIndex) {
		final BitfinexCurrencyPair currency = BitfinexCurrencyPair.fromSymbolString(symbol);
		final Decimal lastClosePrice = timeSeries.get(symbol).getLastTick().getClosePrice();
		
		final Trade openTrade = getOpenTrade(currency);
		
		if(openTrade != null) {
			logger.debug("Unable to open new trade, there is already one active {}", openTrade);
			return;
		}
		
		final double amount = PositionSizeManager.getPositionSize(currency, OrderType.BUY, 
				bitfinexApiBroker.getWallets());
		
		final Trade trade = new Trade(TradeDirection.LONG, currency, amount);

		if(trades.get(currency) == null) {
			trades.put(currency, new ArrayList<>());
		}
		
		trades.get(currency).add(trade);
	
		final BitfinexOrder order = BitfinexOrderBuilder
				.create(currency, BitfinexOrderType.EXCHANGE_MARKET, amount)
				.build();
		
		trade.setTradeState(TradeState.OPENING);
		trade.setExpectedPriceOpen(lastClosePrice.toDouble());
		trade.addOpenOrder(order);
		orderManager.executeOrder(order);
		trade.setTradeState(TradeState.OPENING);
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
		
		if(! UPDATE_SCREEN) {
			return;
		}
		
		final TickerManager tickerManager = bitfinexApiBroker.getTickerManager();

		clearScreen();
		System.out.println("");
		System.out.println("==========");
		System.out.println("Last ticks");
		System.out.println("==========");
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			final String symbol = currency.toBitfinexString();
			System.out.println(symbol + " " + tickerManager.getLastTick(currency));
		}
		
		System.out.println("");
		System.out.println("==========");
		System.out.println("Last bars");
		System.out.println("==========");
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			final String symbol = currency.toBitfinexString();
			System.out.println(symbol + " " + timeSeries.get(symbol).getLastTick());
		}
		
		System.out.println("");
		System.out.println("==========");
		System.out.println("P/L");
		System.out.println("==========");
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			final String symbol = currency.toBitfinexString();
			
			final Trade trade = getOpenTrade(currency);
			if(trade != null) {
				final double priceIn = trade.getExpectedPriceOpen();
				final double currentPrice = tickerManager.getLastTick(currency).getClosePrice().toDouble();
				System.out.println(symbol + ": price in " + priceIn + " / " + (currentPrice - priceIn));
			}	
		}

        System.out.println("");
        System.out.println("==========");
        System.out.println("Trades");
        System.out.println("==========");
        for(final BitfinexCurrencyPair currency : tradedCurrencies) {
                final String symbol = currency.toBitfinexString();
                final List<Trade> lastTrades = trades.get(currency);
                
                if(lastTrades == null) {
                		continue;
                }
                
                lastTrades.sort((t1, t2) -> Long.compare(t2.getTid(), t1.getTid()));
                
                final List<Trade> lastTwoTrades = lastTrades.subList(Math.max(lastTrades.size() - 2, 0), lastTrades.size());

                for(final Trade trade : lastTwoTrades) {
                		System.out.println(symbol + " " + trade);
                }
        }  
		
		System.out.println("");
		System.out.println("==========");
		System.out.println("Orders");
		System.out.println("==========");
		
		final List<ExchangeOrder> orders = new ArrayList<>(bitfinexApiBroker.getOrders());
		orders.sort((o1, o2) -> Long.compare(o2.getCid(), o1.getCid()));
		final List<ExchangeOrder> lastOrders = orders.subList(Math.max(orders.size() - 3, 0), orders.size());

		for(final ExchangeOrder order : lastOrders) {
			System.out.println(order);
		}
	}
	
	public void clearScreen() {  
	    System.out.print("\033[H\033[2J");  
	    System.out.flush();  
	}  
	
	/**
	 * Get the open trade for symbol or null
	 * @param symbol
	 * @return
	 */
	public Trade getOpenTrade(final BitfinexCurrencyPair symbol) {
		final List<Trade> tradeList = trades.get(symbol);
		
		if(tradeList == null) {
			return null;
		}

		final List<Trade> openTrades = tradeList.stream()
				.filter(t -> t.getTradeState() == TradeState.OPEN)
				.collect(Collectors.toList());
		
		if(openTrades.size() > 1) {
			throw new IllegalArgumentException("More then one open trade for " + symbol + " / " + openTrades);
		}
		
		if(openTrades.isEmpty()) {
			return null;
		}
		
		return openTrades.get(0);
	}
	
}
