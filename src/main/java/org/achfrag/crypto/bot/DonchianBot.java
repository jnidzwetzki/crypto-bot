package org.achfrag.crypto.bot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.BitfinexClientFactory;
import org.achfrag.crypto.bitfinex.BitfinexOrderBuilder;
import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeTickerCommand;
import org.achfrag.crypto.bitfinex.entity.APIException;
import org.achfrag.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.crypto.bitfinex.entity.ExchangeOrder;
import org.achfrag.crypto.bitfinex.entity.Timeframe;
import org.achfrag.crypto.bitfinex.util.TickMerger;
import org.achfrag.crypto.strategy.indicator.DonchianChannelLower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Decimal;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class DonchianBot implements Runnable {
	
	/**
	 * The ticker merger
	 */
	private final Map<String, TickMerger> tickMerger;
	
	/**
	 * The time series
	 */
	private final Map<String, TimeSeries> timeSeries;
	
	/**
	 * The traded currencies
	 */
	private final List<BitfinexCurrencyPair> tradedCurrencies;

	/**
	 * The bitfinex api broker
	 */
	private BitfinexApiBroker bitfinexApiBroker; 
	
	/**
	 * The order manager
	 */
	protected final OrderManager orderManager;

	/**
	 * The timeframe to trade
	 */
	private static final Timeframe TIMEFRAME = Timeframe.MINUTES_15;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(DonchianBot.class);
	
	/**
	 * Simulate
	 */
	public final static boolean SIMULATION = false;

	
	public DonchianBot() {
		this.tickMerger = new HashMap<>();
		this.timeSeries = new HashMap<>();
		this.tradedCurrencies = Arrays.asList(BitfinexCurrencyPair.BTC_USD);
		this.bitfinexApiBroker = BitfinexClientFactory.buildBifinexClient();
		this.orderManager = new OrderManager(bitfinexApiBroker);
	}

	@Override
	public void run() {
		try {
			bitfinexApiBroker.connect();
			
			final Map<String, TimeSeries> historicalCandles = HistoricalCandlesHelper
					.requestHistoricalCandles(bitfinexApiBroker, TIMEFRAME, tradedCurrencies);
			timeSeries.putAll(historicalCandles);
			
			executeSystem();
			
			registerTicker();
		} catch (Throwable e) {
			logger.error("Got exception", e);
		}
	}
	
	/**
	 * Register the ticker
	 * @throws InterruptedException
	 * @throws APIException
	 */
	protected void registerTicker() throws InterruptedException, APIException {
		
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
	
	/**
	 * Handle the next bar
	 * @param symbol
	 * @param tick
	 */
	private void barDoneCallback(final String symbol, final Tick tick) {		
		try {
			timeSeries.get(symbol).addTick(tick);
		} catch(Throwable e) {
			logger.error("Unable to add {} to symbol {}", tick, symbol);
		}
		
		logger.info("Newest bar is {}", tick);
		
		executeSystem();
	}


	/**
	 * Handle the tick callback
	 * @param symbol
	 * @param tick
	 */
	private void handleTickCallback(final String symbol, final Tick tick) {		
		tickMerger.get(symbol).addNewPrice(
				tick.getEndTime().toEpochSecond() * 1000, 
				tick.getOpenPrice().toDouble(), 
				tick.getVolume().toDouble());		
	}
	
	/**
	 * Execute the trading system
	 * @throws APIException
	 */
	private void executeSystem() {
		try {
			for(final BitfinexCurrencyPair currencyPair : tradedCurrencies) {
				String symbol = currencyPair.toBitfinexString();
				TimeSeries currencyTimeSeries = timeSeries.get(symbol);
				final ClosePriceIndicator closePrice = new ClosePriceIndicator(currencyTimeSeries);

				final DonchianChannelLower donchianChannelLower = new DonchianChannelLower(closePrice, 48);
				Decimal newStopLoss = donchianChannelLower.getValue(currencyTimeSeries.getEndIndex());
				System.out.println("Low is at: " + newStopLoss);
				
				if(getStopLossOrder(symbol) != null) {
					moveStopLossOrder(currencyPair, newStopLoss);
				} else {
					createEntryOrder(currencyPair);
				}
			}
		} catch (APIException e) {
			logger.error("Got exception while executing trading system", e);
		}
	}

	/**
	 * Find a good entry order
	 * @param currencyPair
	 */
	private void createEntryOrder(final BitfinexCurrencyPair currencyPair) {
		final ExchangeOrder entryOrder = getEntryOrder(currencyPair.toBitfinexString());		
		
		if(entryOrder != null) {
			
		} else {
			
		}
	}

	/**
	 * Move the stop loss order up
	 * @param symbol
	 * @param newStopLoss
	 * @throws APIException 
	 */
	private void moveStopLossOrder(BitfinexCurrencyPair currencyPair, Decimal newStopLoss) throws APIException {
		String symbol = currencyPair.toBitfinexString();

		final ExchangeOrder openOrder = getStopLossOrder(symbol);
		
		if(openOrder == null) {
			return;
		}
		
		final double oldStopLoss = openOrder.getPrice();
		logger.info("Current stop loss order at {} ({})", oldStopLoss, openOrder);
		
		final double newStopLossValue = Math.round(newStopLoss.toDouble() - (newStopLoss.toDouble() / 100 * 0.2));
		
		logger.info("Current stop-loss value is {}", newStopLossValue);

		if(SIMULATION) {
			return;
		}
		
		if(oldStopLoss < newStopLossValue) {
			logger.info("Changing stop-loss order");
	
			bitfinexApiBroker.cancelOrder(openOrder.getOrderId());
			
			final BitfinexOrder order = BitfinexOrderBuilder
					.create(currencyPair, BitfinexOrderType.EXCHANGE_STOP, openOrder.getAmount())
					.withPrice(newStopLossValue)
					.build();
			
			bitfinexApiBroker.placeOrder(order);
		} else {
			logger.info("Not changing the old stop-loss order");
		}
	}

	/**
	 * Get the open stop loss order
	 * @param symbol
	 * @param openOrders
	 * @return
	 */
	private ExchangeOrder getStopLossOrder(String symbol) {
		
		final List<ExchangeOrder> openOrders = bitfinexApiBroker.getOrders();
		
		return openOrders.stream()
			.filter(e -> e.getOrderType() == BitfinexOrderType.EXCHANGE_STOP)
			.filter(e -> e.getSymbol().equals(symbol))
			.findAny()
			.orElse(null);
	}
	

	/**
	 * Get the open stop loss order
	 * @param symbol
	 * @param openOrders
	 * @return
	 */
	private ExchangeOrder getEntryOrder(String symbol) {
		
		final List<ExchangeOrder> openOrders = bitfinexApiBroker.getOrders();
		
		return openOrders.stream()
			.filter(e -> e.getOrderType() == BitfinexOrderType.STOP)
			.filter(e -> e.getSymbol().equals(symbol))
			.findAny()
			.orElse(null);
	}
	
	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args) {
		final DonchianBot donchianBot = new DonchianBot();
		donchianBot.run();
	}

}
