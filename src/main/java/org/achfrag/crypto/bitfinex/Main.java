package org.achfrag.crypto.bitfinex;

import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.backtest.TickMerger;
import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeTicker;
import org.achfrag.crypto.pair.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Tick;

public class Main implements Runnable {

	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(Main.class);
	
	protected TickMerger tickMerger;

	@Override
	public void run() {
		try {
			
			tickMerger = new TickMerger(TickMerger.MERGE_SECONDS_30S, (t) -> barDoneCallback(t));
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
			
			Thread.sleep(TimeUnit.MINUTES.toMillis(5));
		} catch (Exception e) {
			logger.error("Got exception", e);
		}
	}

	private void barDoneCallback(Tick t) {
		System.out.println("Bar: " + t);
	}

	private void handleTickCallback(Tick c) {
		System.out.println("Tick: " + c);
		tickMerger.addNewPrice(c.getBeginTime().toEpochSecond(), c.getOpenPrice().toDouble(), c.getVolume().toDouble());
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
