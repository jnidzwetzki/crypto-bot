package org.achfrag.crypto;

import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
import org.achfrag.crypto.bitfinex.commands.SubscribeCandles;
import org.achfrag.crypto.bitfinex.commands.SubscribeTicker;
import org.achfrag.crypto.pair.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements Runnable {

	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	@Override
	public void run() {
		try {
			final BitfinexApiBroker bitfinexApiBroker = new BitfinexApiBroker();
			bitfinexApiBroker.connect();
			
			final AbstractAPICommand subscribeCommandTicker = new SubscribeTicker(CurrencyPair.BTC_USD);
			bitfinexApiBroker.sendCommand(subscribeCommandTicker);
			
			final AbstractAPICommand subscribeCommandCandles = new SubscribeCandles(CurrencyPair.BTC_USD, SubscribeCandles.TIMEFRAME_1M);
			bitfinexApiBroker.sendCommand(subscribeCommandCandles);
			
			Thread.sleep(TimeUnit.MINUTES.toMillis(5));
		} catch (Exception e) {
			logger.error("Got exception", e);
		}
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
