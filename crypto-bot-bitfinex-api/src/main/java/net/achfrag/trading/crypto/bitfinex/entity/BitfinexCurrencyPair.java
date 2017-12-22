package net.achfrag.trading.crypto.bitfinex.entity;

public enum BitfinexCurrencyPair {

	// Ethereum
	ETH_USD("ETH", "USD", 0.04),
	
	// Bitcoin
	BTC_USD("BTC", "USD", 0.002),
	
	// Litecoin
	LTC_USD("LTC", "USD", 0.2),
	
	// Bitcoin Cash
	BCH_USD("BCH", "USD", 0.2),
	
	// XRP Cash
	XRP_USD("XRP", "USD", 22.0),
	
	// IOTA
	IOTA_USD("IOT", "USD", 6.0),
	
	// EOS
	EOS_USD("EOS", "USD", 0.2),
	
	// NEO
	NEO_USD("NEO", "USD", 0.2);

	/**
	 * The name of the first currency 
	 */
	protected final String currency1;
	
	/**
	 * The name of the second currency
	 */
	protected final String currency2;
	
	/**
	 * The minimal order size
	 */
	protected final double minimalOrderSize;

	private BitfinexCurrencyPair(final String pair1, final String pair2, final double minimalOrderSize) {
		this.currency1 = pair1;
		this.currency2 = pair2;
		this.minimalOrderSize = minimalOrderSize;
	}
	
	public String toBitfinexString() {
		return "t" + currency1 + currency2;
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

	public String getCurrency1() {
		return currency1;
	}
	
	public String getCurrency2() {
		return currency2;
	}
}
