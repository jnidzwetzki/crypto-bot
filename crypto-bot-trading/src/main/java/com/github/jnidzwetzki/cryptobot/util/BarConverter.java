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
