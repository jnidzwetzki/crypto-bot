/*******************************************************************************
 *
 *    Copyright (C) 2015-2018 Jan Kristof Nidzwetzki
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package net.achfrag.crypto.util;

import java.io.Closeable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.ta4j.core.BaseTick;
import org.ta4j.core.Tick;

import com.github.jnidzwetzki.bitfinex.v2.Const;
import com.github.jnidzwetzki.bitfinex.v2.entity.Timeframe;
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexCurrencyPair;

public class TickMerger implements Closeable {


	private Timeframe timeframe;

	private BiConsumer<BitfinexCurrencyPair, Tick> tickConsumer;
	
	private long timeframeBegin = -1;
	
	private double totalVolume = 0;
	
	private final List<Double> prices = new ArrayList<>();

	private BitfinexCurrencyPair symbol;

	public TickMerger(final BitfinexCurrencyPair symbol, final Timeframe timeframe, final BiConsumer<BitfinexCurrencyPair, Tick> tickConsumer) {
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
