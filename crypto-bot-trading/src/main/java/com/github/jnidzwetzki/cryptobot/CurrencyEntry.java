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
package com.github.jnidzwetzki.cryptobot;

import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexCurrencyPair;

public class CurrencyEntry {
	
	/**
	 * The currency pair
	 */
	private final BitfinexCurrencyPair currencyPair;
	
	/**
	 * The entry price
	 */
	private final double entryPrice;
	
	/**
	 * The stop loss price
	 */
	private final double stopLossPrice;
	
	/**
	 * The position size
	 */
	private double positionSize;
	
	public CurrencyEntry(final BitfinexCurrencyPair currencyPair, final double entryPrice, final double stopLossPrice) {
		this.currencyPair = currencyPair;
		this.entryPrice = entryPrice;
		this.stopLossPrice = stopLossPrice;
	}
	

	@Override
	public String toString() {
		return "CurrencyEntry [currencyPair=" + currencyPair + ", entryPrice=" + entryPrice + ", stopLossPrice="
				+ stopLossPrice + ", positionSize=" + positionSize + "]";
	}

	public BitfinexCurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public double getEntryPrice() {
		return entryPrice;
	}

	public double getStopLossPrice() {
		return stopLossPrice;
	}

	public double getPositionSize() {
		return positionSize;
	}

	public void setPositionSize(final double positionSize) {
		this.positionSize = positionSize;
	}
	
}
