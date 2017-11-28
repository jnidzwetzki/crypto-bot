package org.achfrag.crypto.bitfinex;

public enum BitfinexOrderType {
	
	MARKET("MARKER"), 
	EXCHANGE_MARKET("EXCHANGE MARKET"), 
	LIMIT("LIMIT"), 
	EXCHANGE_LIMIT("EXCHANGE LIMIT"), 
	STOP("STOP"), 
	EXCHANGE_STOP("EXCHANGE STOP"), 
	TRAILING_STOP("TRAILING STOP"), 
	EXCHANGE_TRAILING_STOP("EXCHANGE TRAILING STOP"), 
	FOK("FOK"), 
	EXCHANGE_FOK("EXCHANGE FOK"), 
	STOP_LIMIT("STOP LIMIT"), 
	EXCHANGE_STOP_LIMIT("EXCHANGE STOP LIMIT");
	
	private final String bifinexString;
	
	private BitfinexOrderType(final String bifinexString) {
		this.bifinexString = bifinexString;
	}
	
	public String getBifinexString() {
		return bifinexString;
	}
}
