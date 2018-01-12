package net.achfrag.crypto.bot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;

import com.github.jnidzwetzki.bitfinex.v2.BitfinexApiBroker;
import com.github.jnidzwetzki.bitfinex.v2.entity.APIException;
import com.github.jnidzwetzki.bitfinex.v2.entity.Timeframe;
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexCandlestickSymbol;
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexCurrencyPair;


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
	public static Map<BitfinexCandlestickSymbol, TimeSeries> requestHistoricalCandles(final BitfinexApiBroker bitfinexApiBroker, 
			final Timeframe timeframe, final List<BitfinexCurrencyPair> tradedCurrencies) 
			throws InterruptedException, APIException {
		
		logger.info("Request historical candles");
		
		final Map<BitfinexCandlestickSymbol, TimeSeries> timeSeries = new HashMap<>();

		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			
			final String bitfinexString = currency.toBitfinexString();
			final BaseTimeSeries currencyTimeSeries = new BaseTimeSeries(bitfinexString);
			final BitfinexCandlestickSymbol barSymbol = new BitfinexCandlestickSymbol(currency, timeframe);

			timeSeries.put(barSymbol, currencyTimeSeries);

			final CountDownLatch tickCountdown = new CountDownLatch(100);
			
			// Add bars to timeseries callback
			final BiConsumer<BitfinexCandlestickSymbol, Tick> callback = (channelSymbol, tick) -> {

				final TimeSeries timeSeriesToAdd = timeSeries.get(channelSymbol);
				
				try { 
					timeSeriesToAdd.addTick(tick);
					tickCountdown.countDown();
				} catch(IllegalArgumentException e) {
					logger.error("Unable to add tick {} to time series, last tick is {}", 
							tick, 
							timeSeriesToAdd.getLastTick());
				}
			};
			
								
			bitfinexApiBroker.getQuoteManager().registerCandlestickCallback(barSymbol, callback);
			bitfinexApiBroker.getQuoteManager().subscribeCandles(barSymbol);
			
			// Wait for 100 tics or 10 seconds. All snapshot ticks are handled in 
			// a syncronized block, so we receive the full snapshot even if we 
			// call removeTickCallback.
			tickCountdown.await(10, TimeUnit.SECONDS);
			
			bitfinexApiBroker.getQuoteManager().registerCandlestickCallback(barSymbol, callback);
			bitfinexApiBroker.getQuoteManager().unsubscribeCandles(barSymbol);
			
			logger.info("Loaded ticks for symbol {} {}", bitfinexString,
					+ timeSeries.get(barSymbol).getEndIndex());
		}
		
		return timeSeries;
	}
}
