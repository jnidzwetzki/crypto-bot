package org.achfrag.crypto.bot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.trading.crypto.bitfinex.commands.SubscribeCandlesCommand;
import org.achfrag.trading.crypto.bitfinex.commands.UnsubscribeCandlesCommand;
import org.achfrag.trading.crypto.bitfinex.entity.APIException;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.trading.crypto.bitfinex.entity.Timeframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;


public class HistoricalCandlesHelper {
		
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(HistoricalCandlesHelper.class);
	
	/**
	 * Request historical candles
	 * 
	 * @param bitfinexApiBroker
	 * @param timeframe
	 * @param tradedCurrencies
	 * @return
	 * @throws InterruptedException
	 * @throws APIException
	 */
	public static Map<String, TimeSeries> requestHistoricalCandles(final BitfinexApiBroker bitfinexApiBroker, 
			final Timeframe timeframe, final List<BitfinexCurrencyPair> tradedCurrencies) 
			throws InterruptedException, APIException {
		
		logger.info("Request historical candles");
		
		final Map<String, TimeSeries> timeSeries = new HashMap<>();

		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			
			final String bitfinexString = currency.toBitfinexString();
			final BaseTimeSeries currencyTimeSeries = new BaseTimeSeries(bitfinexString);
			timeSeries.put(bitfinexString, currencyTimeSeries);

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
			
			final String barSymbol = currency.toBifinexCandlestickString(timeframe);
			
			bitfinexApiBroker.getTickerManager().registerTickCallback(barSymbol, callback);
			bitfinexApiBroker.sendCommand(new SubscribeCandlesCommand(currency, timeframe));

			// Wait for 100 tics or 10 seconds. All snapshot ticks are handled in 
			// a syncronized block, so we receive the full snapshot even if we 
			// call removeTickCallback.
			tickCountdown.await(10, TimeUnit.SECONDS);
			
			bitfinexApiBroker.getTickerManager().removeTickCallback(barSymbol, callback);
			bitfinexApiBroker.sendCommand(new UnsubscribeCandlesCommand(currency, timeframe));
			
			logger.info("Loaded ticks for symbol {} {}", bitfinexString,
					+ timeSeries.get(bitfinexString).getEndIndex());
		}
		
		return timeSeries;
	}
}
