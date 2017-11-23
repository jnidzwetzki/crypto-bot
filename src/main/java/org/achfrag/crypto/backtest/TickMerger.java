package org.achfrag.crypto.backtest;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.ta4j.core.BaseTick;
import org.ta4j.core.Tick;

public class TickMerger implements Closeable {
	
	public final static long MERGE_SECONDS_30S = 30;

	public final static long MERGE_SECONDS_1M = TimeUnit.MINUTES.toSeconds(1);
	
	public final static long MERGE_SECONDS_5M = TimeUnit.MINUTES.toSeconds(5);
	
	public final static long MERGE_SECONDS_15M = TimeUnit.MINUTES.toSeconds(15);

	public final static long MERGE_SECONDS_30M = TimeUnit.MINUTES.toSeconds(30);

	public final static long MERGE_SECONDS_1H = TimeUnit.MINUTES.toSeconds(60);

	private ZoneId TIMEZONE = ZoneId.of("Europe/Berlin");

	private long mergeSeconds;

	private BiConsumer<String, Tick> tickConsumer;
	
	private long timeframeBegin = -1;
	
	private double totalVolume = 0;
	
	private final List<Double> prices = new ArrayList<>();

	private String symbol;

	public TickMerger(final String symbol, final long mergeSeconds, final BiConsumer<String, Tick> tickConsumer) {
		this.symbol = symbol;
		this.mergeSeconds = mergeSeconds;
		this.tickConsumer = tickConsumer;
	}
	
	public void addNewPrice(final long timestamp, final double price, final double volume)  {
		
		final long periodEnd = timeframeBegin + mergeSeconds;
		
		if (timeframeBegin == -1) {
			timeframeBegin = timestamp;
		} else if (timestamp > periodEnd) {

			if (prices.isEmpty()) {
				System.err.println("Error: prices for series are empty: " + timeframeBegin);
			}

			closeBar();
			
			while (timestamp >= timeframeBegin + mergeSeconds) {
				timeframeBegin = timeframeBegin + mergeSeconds;
			}
		}
		
		prices.add(price);
		totalVolume = totalVolume + volume;
	}

	protected void closeBar() {
		final double open = prices.get(0);
		final double close = prices.get(prices.size() - 1);
		final double high = prices.stream().mapToDouble(e -> e).max().orElse(-1);
		final double low = prices.stream().mapToDouble(e -> e).min().orElse(-1);

		final Timestamp timestampValue = new Timestamp(timeframeBegin * 1000);
		final LocalDateTime localtime = timestampValue.toLocalDateTime();
		final ZonedDateTime withTimezone = localtime.atZone(TIMEZONE);

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
	public void close() throws IOException {
		closeBar();
	}

}
