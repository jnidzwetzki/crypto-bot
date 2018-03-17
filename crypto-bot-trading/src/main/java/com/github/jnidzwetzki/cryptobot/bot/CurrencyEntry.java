package com.github.jnidzwetzki.cryptobot.bot;

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
