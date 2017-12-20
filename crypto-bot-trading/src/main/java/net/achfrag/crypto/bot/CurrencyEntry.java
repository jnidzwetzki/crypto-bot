package net.achfrag.crypto.bot;

import net.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;

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
	
	public CurrencyEntry(final BitfinexCurrencyPair currencyPair, final double entryPrice, final double stopLossPrice) {
		this.currencyPair = currencyPair;
		this.entryPrice = entryPrice;
		this.stopLossPrice = stopLossPrice;
	}
	
	@Override
	public String toString() {
		return "CurrencyEntry [currencyPair=" + currencyPair + ", entryPrice=" + entryPrice + ", stopLossPrice="
				+ stopLossPrice + "]";
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
	
}
