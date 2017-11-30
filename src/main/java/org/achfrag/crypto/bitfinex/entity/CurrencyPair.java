package org.achfrag.crypto.bitfinex.entity;

public class CurrencyPair {
	
	// Ethereum
	//public final static CurrencyPair ETH_BTC = new CurrencyPair("ETH", "BTC");
	
	// Ethereum
	public final static CurrencyPair ETH_USD = new CurrencyPair("ETH", "USD", 0.04);
	
	// Bitcoin
	public final static CurrencyPair BTC_USD = new CurrencyPair("BTC", "USD", 0.002);
	
	// Litecoin
	public final static CurrencyPair LTC_USD = new CurrencyPair("LTC", "USD", 0.2);

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

	public CurrencyPair(final String pair1, final String pair2, final double minimalOrderSize) {
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

}
