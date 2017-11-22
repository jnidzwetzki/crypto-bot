package org.achfrag.crypto.bitfinex;

import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.commands.AbstractAPICommand;
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
			
			final CurrencyPair currency = CurrencyPair.BTC_USD;
			
			final AbstractAPICommand subscribeCommandTicker = new SubscribeTicker(currency);
			bitfinexApiBroker.sendCommand(subscribeCommandTicker);
			
			System.out.println("Wait for ticker");
			
			while(! bitfinexApiBroker.isTickerActive(currency)) {
				Thread.sleep(100);
			}
			
			System.out.println("Register callback");
			bitfinexApiBroker.registerTickCallback(currency, (c) -> System.out.println("---> "  + c));
			
		//	final AbstractAPICommand subscribeCommandCandles = new SubscribeCandles(CurrencyPair.BTC_USD, SubscribeCandles.TIMEFRAME_1M);
		//	bitfinexApiBroker.sendCommand(subscribeCommandCandles);
			
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
