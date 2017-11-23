package org.achfrag.crypto.pair;

public class CurrencyPair {
	
	// Ethereum
	public final static CurrencyPair ETH_USD = new CurrencyPair("ETH", "USD");
	
	// Ethereum
	public final static CurrencyPair ETH_BTC = new CurrencyPair("ETH", "BTC");

	// Bitcoin
	public final static CurrencyPair BTC_USD = new CurrencyPair("BTC", "USD");
	
	// Litecoin
	public final static CurrencyPair LTC_USD = new CurrencyPair("LTC", "USD");

	protected final String pair1;
	
	protected final String pair2;

	public CurrencyPair(String pair1, String pair2) {
		this.pair1 = pair1;
		this.pair2 = pair2;
	}
	
	public String toBitfinexString() {
		return "t" + pair1 + pair2;
	}

}
