package org.achfrag.crypto.bitfinex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

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
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Decimal;
import org.ta4j.core.Order;
import org.ta4j.core.Strategy;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TradingRecord;

public class Main implements Runnable {

	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	protected final Map<String, TickMerger> tickMerger;

	protected final Map<String, TimeSeries> timeSeries;

	protected final Map<String, TradingRecord> tradingRecord;

	protected final Map<String, Strategy> strategies;

	protected final List<CurrencyPair> currencies; 
	
	protected static final Timeframe TIMEFRAME = Timeframe.MINUTES_15;

	public Main() {
		tickMerger = new HashMap<>();
		timeSeries = new HashMap<>();
		tradingRecord = new HashMap<>();
		strategies = new HashMap<>();
		currencies = Arrays.asList(CurrencyPair.BTC_USD, CurrencyPair.ETH_USD, CurrencyPair.LTC_USD);
	}

	@Override
	public void run() {
		try {
			final BitfinexApiBroker bitfinexApiBroker = new BitfinexApiBroker();
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
		for(final CurrencyPair currency : currencies) {
			
			final String bitfinexString = currency.toBitfinexString();
			final BaseTimeSeries currencyTimeSeries = new BaseTimeSeries(bitfinexString);
			timeSeries.put(bitfinexString, currencyTimeSeries);
			final Strategy strategy = EMAStrategy03.getStrategy(currencyTimeSeries, 5, 12, 40);
			strategies.put(bitfinexString, strategy);
			tradingRecord.put(bitfinexString, new BaseTradingRecord());

			// Add bars to timeseries
			final BiConsumer<String, Tick> callback = (symbol, tick) -> timeSeries.get(symbol).addTick(tick);
			
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
		System.out.format("Symbol %s Bar %s\n", symbol, tick);
		
		try {
			timeSeries.get(symbol).addTick(tick);
		} catch(Throwable e) {
			logger.error("Unable to add {} to symbol {}", tick, symbol);
		}
		
		final int endIndex = timeSeries.get(symbol).getEndIndex();
		
		if (strategies.get(symbol).shouldEnter(endIndex)) {
			// Our strategy should enter
			System.out.println("Strategy should ENTER on " + endIndex + " / " + symbol);
			boolean entered = tradingRecord.get(symbol).enter(endIndex, tick.getClosePrice(), Decimal.TEN);
			if (entered) {
				Order entry = tradingRecord.get(symbol).getLastEntry();
				System.out.println("Entered on " + entry.getIndex() + " (price=" + entry.getPrice().toDouble()
						+ ", amount=" + entry.getAmount().toDouble() + ")");
			}
		} else if (strategies.get(symbol).shouldExit(endIndex)) {
			// Our strategy should exit
			System.out.println("Strategy should EXIT on " + endIndex + " / " + symbol);
			boolean exited = tradingRecord.get(symbol).exit(endIndex, tick.getClosePrice(), Decimal.TEN);
			if (exited) {
				Order exit = tradingRecord.get(symbol).getLastExit();
				System.out.println("Exited on " + exit.getIndex() + " (price=" + exit.getPrice().toDouble()
						+ ", amount=" + exit.getAmount().toDouble() + ")");
			}
		}
	}

	private void handleTickCallback(final String symbol, final Tick c) {
		tickMerger.get(symbol).addNewPrice(c.getEndTime().toEpochSecond(), c.getOpenPrice().toDouble(), c.getVolume().toDouble());
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
