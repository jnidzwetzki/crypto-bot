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
import java.util.function.Consumer;

import org.ta4j.core.BaseTick;
import org.ta4j.core.Tick;

public class TickMerger implements Closeable {
	
	public final static long MERGE_SECONDS_5M = TimeUnit.MINUTES.toSeconds(5);

	public final static long MERGE_SECONDS_1H = TimeUnit.MINUTES.toSeconds(60);

	private ZoneId TIMEZONE = ZoneId.of("Europe/Berlin");

	private long mergeSeconds;

	private Consumer<Tick> tickConsumer;
	
	private long timeframeBegin = -1;
	
	private double totalVolume = 0;
	
	private final List<Double> prices = new ArrayList<>();

	public TickMerger(final long mergeSeconds, final Consumer<Tick> tickConsumer) {
		this.mergeSeconds = mergeSeconds;
		this.tickConsumer = tickConsumer;
	}
	
	public void addNewPrice(final long timestamp, final double price, final double volume)  {
		
		if (timeframeBegin == -1) {
			timeframeBegin = timestamp;
		} else if (timestamp > timeframeBegin + mergeSeconds) {

			if (prices.isEmpty()) {
				System.err.println("Error: prices for series are empty: " + timeframeBegin);
			}

			closeBar();
			
			while (timestamp > timeframeBegin) {
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
			tickConsumer.accept(tick);
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
