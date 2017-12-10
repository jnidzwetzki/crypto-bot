package org.achfrag.trading.crypto.bitfinex.entity;

public enum BitfinexCurrencyPair {

	// Ethereum
	ETH_USD("ETH", "USD", 0.04),
	
	// Bitcoin
	BTC_USD("BTC", "USD", 0.002),
	
	// Litecoin
	LTC_USD("LTC", "USD", 0.2);

	/**
	 * The name of the first curreny 
	 */
	protected final String pair1;
	
	/**
	 * The name of the second currency
	 */
	protected final String pair2;
	
	/**
	 * The minimal order size
	 */
	protected final double minimalOrderSize;

	private BitfinexCurrencyPair(final String pair1, final String pair2, final double minimalOrderSize) {
		this.pair1 = pair1;
		this.pair2 = pair2;
		this.minimalOrderSize = minimalOrderSize;
	}
	
	public String toBitfinexString() {
		return "t" + pair1 + pair2;
	}
	
	public String toBifinexCandlestickString(final Timeframe timeframe) {
		return "trade:" + timeframe.getBitfinexString() + ":" + toBitfinexString();
	}
	
	public double getMinimalOrderSize() {
		return minimalOrderSize;
	}
	
	public static BitfinexCurrencyPair fromSymbolString(final String symbolString) {
		for (BitfinexCurrencyPair curency : BitfinexCurrencyPair.values()) {
			if (curency.toBitfinexString().equalsIgnoreCase(symbolString)) {
				return curency;
			}
		}
		throw new IllegalArgumentException("Unable to find order type for: " + symbolString);
	}

}
