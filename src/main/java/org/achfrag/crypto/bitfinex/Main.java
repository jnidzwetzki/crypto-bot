package org.achfrag.crypto.bitfinex;

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
	
	protected TickMerger tickMerger;

	protected TimeSeries timeSeries = new BaseTimeSeries("BTC");
	
	protected TradingRecord tradingRecord = new BaseTradingRecord();
	
	protected Strategy strategy;
	
	@Override
	public void run() {
		try {
			strategy = EMAStrategy02.getStrategy(timeSeries, 9, 14, 21);
			tickMerger = new TickMerger(TickMerger.MERGE_SECONDS_1M, (t) -> barDoneCallback(t));
			final BitfinexApiBroker bitfinexApiBroker = new BitfinexApiBroker();
			bitfinexApiBroker.connect();
			
			final CurrencyPair currency = CurrencyPair.BTC_USD;
			
			final AbstractAPICommand subscribeCommandTicker = new SubscribeTicker(currency);
			bitfinexApiBroker.sendCommand(subscribeCommandTicker);
			
			System.out.println("Wait for ticker");
			
			while(! bitfinexApiBroker.isTickerActive(currency)) {
				Thread.sleep(100);
			}
			
			System.out.println("Register callback");
			bitfinexApiBroker.registerTickCallback(currency, (c) -> handleTickCallback(c));
			
		//	final AbstractAPICommand subscribeCommandCandles = new SubscribeCandles(CurrencyPair.BTC_USD, SubscribeCandles.TIMEFRAME_1M);
		//	bitfinexApiBroker.sendCommand(subscribeCommandCandles);
			
			while(true) {
				Thread.sleep(TimeUnit.MINUTES.toMillis(5));
			}
		} catch (Exception e) {
			logger.error("Got exception", e);
		}
	}

	private void barDoneCallback(final Tick t) {
		System.out.println("Bar: " + t);
		timeSeries.addTick(t);
		
		int endIndex = timeSeries.getEndIndex();
		if (strategy.shouldEnter(endIndex)) {
            // Our strategy should enter
            System.out.println("Strategy should ENTER on " + endIndex);
            boolean entered = tradingRecord.enter(endIndex, t.getClosePrice(), Decimal.TEN);
            if (entered) {
                Order entry = tradingRecord.getLastEntry();
                System.out.println("Entered on " + entry.getIndex()
                        + " (price=" + entry.getPrice().toDouble()
                        + ", amount=" + entry.getAmount().toDouble() + ")");
            }
        } else if (strategy.shouldExit(endIndex)) {
            // Our strategy should exit
            System.out.println("Strategy should EXIT on " + endIndex);
            boolean exited = tradingRecord.exit(endIndex, t.getClosePrice(), Decimal.TEN);
            if (exited) {
                Order exit = tradingRecord.getLastExit();
                System.out.println("Exited on " + exit.getIndex()
                        + " (price=" + exit.getPrice().toDouble()
                        + ", amount=" + exit.getAmount().toDouble() + ")");
            }
}
	}

	private void handleTickCallback(final Tick c) {
	//	System.out.println("Tick: " + c);
		tickMerger.addNewPrice(c.getBeginTime().toEpochSecond(), c.getOpenPrice().toDouble(), c.getVolume().toDouble());
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
