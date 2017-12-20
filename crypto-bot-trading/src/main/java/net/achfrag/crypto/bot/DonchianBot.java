package net.achfrag.crypto.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.bboxdb.commons.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Decimal;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.MinPriceIndicator;

import net.achfrag.crypto.bot.portfolio.BasePortfolioManager;
import net.achfrag.crypto.bot.portfolio.PortfolioManager;
import net.achfrag.crypto.strategy.indicator.DonchianChannelLower;
import net.achfrag.crypto.strategy.indicator.DonchianChannelUpper;
import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.BitfinexClientFactory;
import net.achfrag.trading.crypto.bitfinex.commands.AbstractAPICommand;
import net.achfrag.trading.crypto.bitfinex.commands.SubscribeTickerCommand;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import net.achfrag.trading.crypto.bitfinex.entity.Timeframe;
import net.achfrag.trading.crypto.bitfinex.util.TickMerger;

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
	 * The channel period in
	 */
	private final int periodIn;

	/**
	 * The channel period out
	 */
	private final int periodOut;
	
	/**
	 * The portfolio manager
	 */
	private final List<PortfolioManager> portfolioManagers;
	
	/**
	 * The API broker
	 */
	private final List<BitfinexApiBroker> apiBrokerList;
	
	/**
	 * The ticker latch
	 */
	private volatile CountDownLatch tickerLatch;
	
	/**
	 * The timeframe to trade
	 */
	private static final Timeframe TIMEFRAME = Timeframe.MINUTES_15;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(DonchianBot.class);

	
	public DonchianBot(final int periodIn, final int periodOut) {
		this.periodIn = periodIn;
		this.periodOut = periodOut;
		this.tickMerger = new HashMap<>();
		this.timeSeries = new HashMap<>();		
		
		this.tradedCurrencies = Arrays.asList(BitfinexCurrencyPair.BTC_USD,
				BitfinexCurrencyPair.ETH_USD, BitfinexCurrencyPair.LTC_USD, 
				BitfinexCurrencyPair.BCH_USD, BitfinexCurrencyPair.XRP_USD,
				BitfinexCurrencyPair.IOTA_USD, BitfinexCurrencyPair.EOS_USD);
		
		this.portfolioManagers = new ArrayList<>();
		
		this.apiBrokerList = BitfinexClientFactory.buildBifinexClient();

		if(apiBrokerList.isEmpty()) {
			throw new IllegalArgumentException("Unable to get API clients");
		}
				
		for(final BitfinexApiBroker bitfinexApiBroker : apiBrokerList) {
			portfolioManagers.add(new BasePortfolioManager(bitfinexApiBroker));
		}
	}

	@Override
	public void run() {
		try {
			logger.info("===============================================");
			logger.info("Starting with {} API keys", apiBrokerList.size());
			logger.info("===============================================");

			for(final BitfinexApiBroker bitfinexApiBroker : apiBrokerList) {
				bitfinexApiBroker.connect();
			}
			
			final Map<String, TimeSeries> historicalCandles = HistoricalCandlesHelper
					.requestHistoricalCandles(apiBrokerList.get(0), TIMEFRAME, tradedCurrencies);
			timeSeries.putAll(historicalCandles);
			
			registerTicker();

			while(true) {
				
				if(tickerLatch != null) {
					tickerLatch.await();
				}
				
				executeSystem();
				
				tickerLatch = new CountDownLatch(tradedCurrencies.size());
			}
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
			apiBrokerList.get(0).sendCommand(subscribeCommandTicker);

			logger.info("Wait for ticker");

			while (! apiBrokerList.get(0).isTickerActive(currency)) {
				Thread.sleep(100);
			}

			apiBrokerList.get(0)
				.getTickerManager()
				.registerTickCallback(currency.toBitfinexString(), (s, c) -> handleTickCallback(s, c));
		}
	}
	
	/**
	 * Handle the next bar
	 * @param symbol
	 * @param tick
	 */
	private synchronized void barDoneCallback(final String symbol, final Tick tick) {	
		
		final TimeSeries symbolTimeSeries = timeSeries.get(symbol);

		try {
			symbolTimeSeries.addTick(tick);
		} catch(Throwable e) {
			logger.error("Unable to add {} to symbol {}, last bar is {}", 
					tick, symbol, symbolTimeSeries.getLastTick());
		}
		
		logger.info("Newest bar is {}", tick);
		
		// Notify portfolio manager about bar done
		if(tickerLatch != null) {
			tickerLatch.countDown();
		}
	}
	
	/**
	 * Get the last price for the symbol
	 */
	private double getLastPriceForSymbol(final String symbol) {
		return timeSeries.get(symbol).getLastTick().getClosePrice().toDouble();
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
		
		for(final PortfolioManager portfolioManager : portfolioManagers) {
			
			logger.info("Apply orders to portfolio manager {}", portfolioManager);
			logger.info("===============================================");

			applySystemToPortfolioManager(portfolioManager);
			
			if(Thread.interrupted()) {
				return;
			}
		}
		
	}

	/**
	 * Apply the orders to the portfolio manager
	 * @param portfolioManager
	 */
	private void applySystemToPortfolioManager(final PortfolioManager portfolioManager) {
		try {	
			final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
			final Map<BitfinexCurrencyPair, Double> exits = new HashMap<>();
			
			for(final BitfinexCurrencyPair currencyPair : tradedCurrencies) {
				
				final boolean open = portfolioManager.isPositionOpen(currencyPair.getCurrency1());
				
				// The channel values
				final double upperValue = getUpperChannelValue(currencyPair).toDouble();
				final double lowerValue = getLowerChannelValue(currencyPair).toDouble();
				final double channelSize = upperValue - lowerValue;
				
				// The prices
				final double entryPrice = adjustEntryPrice(upperValue);
				final double exitPrice = adjustExitPrice(lowerValue);

				if(! open) {
					final double lastPrice = getLastPriceForSymbol(currencyPair.toBitfinexString());
					
					// Filter entry orders to reduce capital allocation
					final double upperChannelHalf = lowerValue + (channelSize / 2);

					if(lastPrice > upperChannelHalf) {
						final CurrencyEntry currencyEntry = new CurrencyEntry(currencyPair, entryPrice, exitPrice);
						entries.put(currencyPair, currencyEntry);
					} else {
						logger.info("Entry order for {} suppressed because price {} is to low {}", 
								currencyPair, lastPrice, upperChannelHalf);
					}
					
				} else {
					exits.put(currencyPair, exitPrice);
				}
			}
		
			portfolioManager.syncOrders(entries, exits);

		} catch (APIException e) {
			logger.error("Got exception while executing trading system", e);
		} catch (InterruptedException e) {
			logger.error("Got interrupted exception");
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Get the upper channel value
	 * @param currencyPair
	 * @return
	 */
	private Decimal getUpperChannelValue(final BitfinexCurrencyPair currencyPair) {
		final String symbol = currencyPair.toBitfinexString();
		final TimeSeries currencyTimeSeries = timeSeries.get(symbol);
		final MaxPriceIndicator maxPrice = new MaxPriceIndicator(currencyTimeSeries);

		final DonchianChannelUpper donchianChannelUpper = new DonchianChannelUpper(maxPrice, periodIn);
		return donchianChannelUpper.getValue(currencyTimeSeries.getEndIndex());
	}
	
	/**
	 * Get the lower channel value
	 * @param currencyPair
	 * @return
	 */
	private Decimal getLowerChannelValue(final BitfinexCurrencyPair currencyPair) {
		final String symbol = currencyPair.toBitfinexString();
		final TimeSeries currencyTimeSeries = timeSeries.get(symbol);
		
		final MinPriceIndicator minPrice = new MinPriceIndicator(currencyTimeSeries);

		final DonchianChannelLower donchianChannelLower = new DonchianChannelLower(minPrice, periodOut);
		return donchianChannelLower.getValue(currencyTimeSeries.getEndIndex());
	}

	/**
	 * Adjust the entry price
	 * @param upperValue
	 * @return
	 */
	private double adjustEntryPrice(final double upperValue) {
		return MathUtil.round(upperValue + (upperValue / 100.0 * 0.5), 2);
	}
	
	/**
	 * Adjust the exit price
	 * @param newStopLoss
	 * @return
	 */
	private double adjustExitPrice(final double lowerValue) {
		return  MathUtil.round(lowerValue - (lowerValue / 100.0 * 0.2), 2);
	}

	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args) {
		
		if(args.length != 2) {
			System.err.println("Usage: class <period-in> <period-out>");
			System.exit(-1);
		}
		
		final int periodIn = MathUtil.tryParseIntOrExit(args[0], () -> "Unable to parse period in");
		final int periodOut = MathUtil.tryParseIntOrExit(args[1], () -> "Unable to parse period out");

		final DonchianBot donchianBot = new DonchianBot(periodIn, periodOut);
		donchianBot.run();
	}

}
