package net.achfrag.trading.crypto.bitfinex.entity;

public enum BitfinexOrderType {

	MARKET("MARKET"), 
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

	public static BitfinexOrderType fromString(String orderTypeText) {
		for (BitfinexOrderType orderType : BitfinexOrderType.values()) {
			if (orderType.getBifinexString().equalsIgnoreCase(orderTypeText)) {
				return orderType;
			}
		}
		throw new IllegalArgumentException("Unable to find order type for: " + orderTypeText);
	}
}
