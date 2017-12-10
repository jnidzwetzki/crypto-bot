package org.achfrag.trading.crypto.bitfinex.util;

import java.io.Closeable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.achfrag.trading.crypto.bitfinex.Const;
import org.achfrag.trading.crypto.bitfinex.entity.Timeframe;
import org.ta4j.core.BaseTick;
import org.ta4j.core.Tick;

public class TickMerger implements Closeable {


	private Timeframe timeframe;

	private BiConsumer<String, Tick> tickConsumer;
	
	private long timeframeBegin = -1;
	
	private double totalVolume = 0;
	
	private final List<Double> prices = new ArrayList<>();

	private String symbol;

	public TickMerger(final String symbol, final Timeframe timeframe, final BiConsumer<String, Tick> tickConsumer) {
		this.symbol = symbol;
		this.timeframe = timeframe;
		this.tickConsumer = tickConsumer;
	}
	
	public void addNewPrice(final long timestamp, final double price, final double volume)  {

		if (timeframeBegin == -1) {
			// Align timeframe
			final long millisecondsFromEpoch = timestamp / timeframe.getMilliSeconds();
			timeframeBegin = millisecondsFromEpoch * timeframe.getMilliSeconds();
		} 
		
		final long periodEnd = timeframeBegin + timeframe.getMilliSeconds();

		if (timestamp >= periodEnd) {
			
			if (prices.isEmpty()) {
				System.err.println("Error: prices for series are empty: " + timeframeBegin);
			}

			closeBar();
			
			while (timestamp >= timeframeBegin + timeframe.getMilliSeconds()) {
				timeframeBegin = timeframeBegin + timeframe.getMilliSeconds();
			}
		}

		prices.add(price);
		
		totalVolume = totalVolume + volume;
	}

	protected void closeBar() {
		if(prices.isEmpty()) {
			return;
		}
		
		final double open = prices.get(0);
		final double close = prices.get(prices.size() - 1);
		final double high = prices.stream().mapToDouble(e -> e).max().orElse(-1);
		final double low = prices.stream().mapToDouble(e -> e).min().orElse(-1);

		final Instant i = Instant.ofEpochMilli(timeframeBegin + timeframe.getMilliSeconds() - 1);
		final ZonedDateTime withTimezone = ZonedDateTime.ofInstant(i, Const.BITFINEX_TIMEZONE);
	
		final Tick tick = new BaseTick(withTimezone, open, high, low, close, totalVolume);

		try {
			tickConsumer.accept(symbol, tick);
		} catch (IllegalArgumentException e) {
			// ignore time shift
		}

		totalVolume = 0;
		prices.clear();
	}

	@Override
	public void close() {
		closeBar();
	}

}
