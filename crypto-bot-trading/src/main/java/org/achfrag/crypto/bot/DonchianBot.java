package org.achfrag.crypto.bot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.achfrag.crypto.strategy.indicator.DonchianChannelLower;
import org.achfrag.crypto.strategy.indicator.DonchianChannelUpper;
import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.trading.crypto.bitfinex.BitfinexClientFactory;
import org.achfrag.trading.crypto.bitfinex.BitfinexOrderBuilder;
import org.achfrag.trading.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.trading.crypto.bitfinex.commands.SubscribeTickerCommand;
import org.achfrag.trading.crypto.bitfinex.entity.APIException;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.trading.crypto.bitfinex.entity.ExchangeOrder;
import org.achfrag.trading.crypto.bitfinex.entity.Timeframe;
import org.achfrag.trading.crypto.bitfinex.entity.Wallet;
import org.achfrag.trading.crypto.bitfinex.util.TickMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Decimal;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.MinPriceIndicator;

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
		
		// FIXME: Currently the wallets are USD / BTC only. Implement before change
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
				final Wallet wallet = getExchangeBTCWallet();

				if(wallet.getBalance() > currencyPair.getMinimalOrderSize()) {
					logger.info("We are invested, adjusting stop loss");
					moveStopLossOrder(currencyPair);
				} else {
					logger.info("We are not invested, adjusting entry");
					moveEntryOrder(currencyPair);
				}
			}
		} catch (APIException e) {
			logger.error("Got exception while executing trading system", e);
		} catch (InterruptedException e) {
			logger.error("Got interrupted exception");
			Thread.currentThread().interrupt();
			return;
		}
	}

	/**
	 * Find a good entry order
	 * @param currencyPair
	 */
	private void moveEntryOrder(final BitfinexCurrencyPair currencyPair) {
		final ExchangeOrder entryOrder = getEntryOrder(currencyPair.toBitfinexString());		
		
		String symbol = currencyPair.toBitfinexString();
		TimeSeries currencyTimeSeries = timeSeries.get(symbol);
		final MaxPriceIndicator maxPrice = new MaxPriceIndicator(currencyTimeSeries);

		final DonchianChannelUpper donchianChannelUpper = new DonchianChannelUpper(maxPrice, 96);
		final Decimal upperValue = donchianChannelUpper.getValue(currencyTimeSeries.getEndIndex());

		final double adjustedUpper = Math.round(upperValue.toDouble() + (upperValue.toDouble() / 100 * 0.5));
		logger.info("Calculated upper is at: {} / entry order is {}", adjustedUpper, entryOrder);

		if(entryOrder != null && entryOrder.getPrice() == adjustedUpper) {
			logger.info("Old entry upper is at {}, sleeping", entryOrder.getPrice());
			return;
		}
		
		try {
			if(SIMULATION) {
				return;
			}
			
			if(entryOrder != null) {
				orderManager.cancelOrderAndWaitForCompletion(entryOrder.getOrderId());
			} 
			
			final double amount = calculatePositionSize(upperValue);
			
			final BitfinexOrder order = BitfinexOrderBuilder
					.create(currencyPair, BitfinexOrderType.EXCHANGE_STOP, amount)
					.withPrice(adjustedUpper)
					.setPostOnly()
					.build();
			
			bitfinexApiBroker.placeOrder(order);
		
		} catch (APIException e) {
			logger.error("Unable to place order", e);
		} catch (InterruptedException e) {
			logger.info("Got interrupted exception", e);
			Thread.currentThread().interrupt();
			return;
		} 
	}

	/**
	 * Calculate the positon size
	 * @param upperValue
	 * @return
	 */
	private double calculatePositionSize(final Decimal upperValue) {
		
		return 0.002;
		
		/**
		final Wallet wallet = getExchangeUSDWallet();
		return (wallet.getBalance() / upperValue.toDouble()) * 0.8;
		*/
	}
	
	/**
	 * Get the USD wallet
	 * @return
	 */
	private Wallet getExchangeUSDWallet() {
		return bitfinexApiBroker.getWallets()
			.stream()
			.filter(w -> w.getWalletType().equals(Wallet.WALLET_TYPE_EXCHANGE))
			.filter(w -> w.getCurreny().equals("USD"))
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * Get the USD wallet
	 * @return
	 */
	private Wallet getExchangeBTCWallet() {
		return bitfinexApiBroker.getWallets()
			.stream()
			.filter(w -> w.getWalletType().equals(Wallet.WALLET_TYPE_EXCHANGE))
			.filter(w -> w.getCurreny().equals("BTC"))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Move the stop loss order up
	 * @param symbol
	 * @param newStopLoss
	 * @throws APIException 
	 * @throws InterruptedException 
	 */
	private void moveStopLossOrder(BitfinexCurrencyPair currencyPair) 
			throws APIException, InterruptedException {
		
		final String symbol = currencyPair.toBitfinexString();
		final TimeSeries currencyTimeSeries = timeSeries.get(symbol);
		
		final MinPriceIndicator minPrice = new MinPriceIndicator(currencyTimeSeries);

		final DonchianChannelLower donchianChannelLower = new DonchianChannelLower(minPrice, 48);
		Decimal newStopLoss = donchianChannelLower.getValue(currencyTimeSeries.getEndIndex());
		System.out.println("Low is at: " + newStopLoss);

		final double newStopLossValue = Math.round(newStopLoss.toDouble() - (newStopLoss.toDouble() / 100 * 0.2));
		logger.info("Current stop-loss value is {}", newStopLossValue);
		
		final ExchangeOrder openOrder = getStopLossOrder(symbol);
		
		if(openOrder != null && openOrder.getPrice() > newStopLossValue) {
			logger.info("Stop loss is already set to {} (calculated {})", 
					openOrder.getPrice(), newStopLossValue);
			return;
		}
		
		logger.info("Changing stop-loss order");

		if(SIMULATION) {
			return;
		}
		
		if(openOrder != null) {
			orderManager.cancelOrderAndWaitForCompletion(openOrder.getOrderId());
		}
		
		final BitfinexOrder order = BitfinexOrderBuilder
				.create(currencyPair, BitfinexOrderType.EXCHANGE_STOP, openOrder.getAmount())
				.withPrice(newStopLossValue)
				.setPostOnly()
				.build();
		
		bitfinexApiBroker.placeOrder(order);
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
			.filter(e -> e.getOrderType() == BitfinexOrderType.EXCHANGE_STOP)
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
