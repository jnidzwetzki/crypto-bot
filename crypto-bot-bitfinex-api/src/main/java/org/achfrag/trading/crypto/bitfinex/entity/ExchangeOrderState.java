package org.achfrag.trading.crypto.bitfinex.entity;

public enum ExchangeOrderState {
	
	STATE_ACTIV("ACTIVE"),
	STATE_EXECUTED("EXECUTED"),
	STATE_PARTIALLY_FILLED("PARTIALLY FILLED"),
	STATE_CANCELED("CANCELED"),
	STATE_POSTONLY_CANCELED("POSTONLY CANCELED");
	
	private String bitfinexString;
	
	private ExchangeOrderState(final String bitfinexString) {
		this.bitfinexString = bitfinexString;
	}
	
	public String getBitfinexString() {
		return bitfinexString;
	}
	
	public static ExchangeOrderState fromString(final String string) {
		for (ExchangeOrderState state : ExchangeOrderState.values()) {
			if (state.getBitfinexString().equalsIgnoreCase(string)) {
				return state;
			}
		}
		throw new IllegalArgumentException("Unable to find order type for: " + string);
	}
}
