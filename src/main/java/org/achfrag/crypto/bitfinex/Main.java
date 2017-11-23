package org.achfrag.crypto.bitfinex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.backtest.TickMerger;
import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeTicker;
import org.achfrag.crypto.pair.CurrencyPair;
import org.achfrag.crypto.strategy.EMAStrategy02;
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

	protected final Map<String, Strategy> strategy;

	protected final List<CurrencyPair> currencies; 

	public Main() {
		tickMerger = new HashMap<>();
		timeSeries = new HashMap<>();
		tradingRecord = new HashMap<>();
		strategy = new HashMap<>();
		currencies = Arrays.asList(CurrencyPair.BTC_USD, CurrencyPair.ETH_USD);
	}

	@Override
	public void run() {
		try {

			final BitfinexApiBroker bitfinexApiBroker = new BitfinexApiBroker();
			bitfinexApiBroker.connect();

			for(final CurrencyPair currency : currencies) {
				
				final String bitfinexString = currency.toBitfinexString();
				final BaseTimeSeries currencyTimeSeries = new BaseTimeSeries(bitfinexString);
				timeSeries.put(bitfinexString, currencyTimeSeries);
				strategy.put(bitfinexString, EMAStrategy02.getStrategy(currencyTimeSeries, 5, 12, 40));
				tradingRecord.put(bitfinexString, new BaseTradingRecord());
				tickMerger.put(bitfinexString, new TickMerger(bitfinexString, TickMerger.MERGE_SECONDS_1M, (s, t) -> barDoneCallback(s, t)));
			
				
				final AbstractAPICommand subscribeCommandTicker = new SubscribeTicker(currency);
				bitfinexApiBroker.sendCommand(subscribeCommandTicker);
	
				System.out.println("Wait for ticker");
	
				while (!bitfinexApiBroker.isTickerActive(currency)) {
					Thread.sleep(100);
				}
	
				System.out.println("Register callback");
				bitfinexApiBroker.registerTickCallback(currency, (s, c) -> handleTickCallback(s, c));
			}
			// final AbstractAPICommand subscribeCommandCandles = new
			// SubscribeCandles(CurrencyPair.BTC_USD, SubscribeCandles.TIMEFRAME_1M);
			// bitfinexApiBroker.sendCommand(subscribeCommandCandles);

			while (true) {
				Thread.sleep(TimeUnit.MINUTES.toMillis(5));
			}
		} catch (Exception e) {
			logger.error("Got exception", e);
		}
	}

	private void barDoneCallback(final String symbol, final Tick tick) {
		System.out.format("Symbol %s Bar %s\n", symbol, tick);
		timeSeries.get(symbol).addTick(tick);

		int endIndex = timeSeries.get(symbol).getEndIndex();
		if (strategy.get(symbol).shouldEnter(endIndex)) {
			// Our strategy should enter
			System.out.println("Strategy should ENTER on " + endIndex + " / " + symbol);
			boolean entered = tradingRecord.get(symbol).enter(endIndex, tick.getClosePrice(), Decimal.TEN);
			if (entered) {
				Order entry = tradingRecord.get(symbol).getLastEntry();
				System.out.println("Entered on " + entry.getIndex() + " (price=" + entry.getPrice().toDouble()
						+ ", amount=" + entry.getAmount().toDouble() + ")");
			}
		} else if (strategy.get(symbol).shouldExit(endIndex)) {
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
		tickMerger.get(symbol).addNewPrice(c.getBeginTime().toEpochSecond(), c.getOpenPrice().toDouble(), c.getVolume().toDouble());
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
