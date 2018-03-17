package com.github.jnidzwetzki.cryptobot.util;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;

import com.github.jnidzwetzki.bitfinex.v2.Const;
import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexTick;

public class BarConverter {
	
	/**
	 * Convert a bitfinex tick into a ta4j bar
	 * @param bitfinexTick
	 * @return
	 */
	public static Bar convertBitfinexTick(final BitfinexTick bitfinexTick) {
		final Instant instant = Instant.ofEpochMilli(bitfinexTick.getTimestamp());
		final ZonedDateTime time = ZonedDateTime.ofInstant(instant, Const.BITFINEX_TIMEZONE);

		final Bar bar = new BaseBar(time, bitfinexTick.getOpen(), 
				bitfinexTick.getHigh(), 
				bitfinexTick.getLow(), 
				bitfinexTick.getClose(), 
				bitfinexTick.getVolume() != BitfinexTick.INVALID_VOLUME ? bitfinexTick.getVolume() : 0);
		
		return bar;
	}
}
