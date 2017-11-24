package org.achfrag.crypto.bitfinex.misc;

import java.util.concurrent.TimeUnit;

public enum Timeframe {

	SECONDS_30(30, null),
	MINUTES_1(TimeUnit.MINUTES.toSeconds(1), "1m"),
	MINUTES_5(TimeUnit.MINUTES.toSeconds(5), "5m"),
	MINUTES_15(TimeUnit.MINUTES.toSeconds(15), "15m"),
	MINUTES_30(TimeUnit.MINUTES.toSeconds(3), "30m"),
	HOUR_1(TimeUnit.HOURS.toSeconds(1), "1h");
	
	private Timeframe(long seconds, String bitfinexString) {
		this.seconds = seconds;
		this.bitfinexString = bitfinexString;
	}
	
	private final long seconds;
	
	private final String bitfinexString;

	public long getSeconds() {
		return seconds;
	}
	
	public String getBitfinexString() {
		return bitfinexString;
	}
}
