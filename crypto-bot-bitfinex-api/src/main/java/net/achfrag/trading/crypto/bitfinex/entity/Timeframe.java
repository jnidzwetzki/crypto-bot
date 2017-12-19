package net.achfrag.trading.crypto.bitfinex.entity;

import java.util.concurrent.TimeUnit;

public enum Timeframe {

	SECONDS_30(TimeUnit.SECONDS.toMillis(30), null),
	MINUTES_1(TimeUnit.MINUTES.toMillis(1), "1m"),
	MINUTES_5(TimeUnit.MINUTES.toMillis(5), "5m"),
	MINUTES_15(TimeUnit.MINUTES.toMillis(15), "15m"),
	MINUTES_30(TimeUnit.MINUTES.toMillis(3), "30m"),
	HOUR_1(TimeUnit.HOURS.toMillis(1), "1h");
	
	private Timeframe(final long milliseconds, final String bitfinexString) {
		this.milliseconds = milliseconds;
		this.bitfinexString = bitfinexString;
	}
	
	private final long milliseconds;
	
	private final String bitfinexString;

	public long getMilliSeconds() {
		return milliseconds;
	}
	
	public String getBitfinexString() {
		return bitfinexString;
	}
}
