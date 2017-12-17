package org.achfrag.trading.crypto.bitfinex.entity;

import java.util.Objects;

public enum ExchangeOrderState {
	
	STATE_ACTIVE("ACTIVE"),
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
		
		Objects.requireNonNull(string);
		
		for (ExchangeOrderState state : ExchangeOrderState.values()) {
			if (string.startsWith(state.getBitfinexString())) {
				return state;
			}
		}
		
		throw new IllegalArgumentException("Unable to find order type for: " + string);
	}
}
