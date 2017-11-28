package org.achfrag.crypto.bitfinex;

public class BitfinexOrderBuilder {

	private final String symbol; 
	private final BitfinexOrderType type;
	private final double amount;
	private final double price;
	
	private double priceTrailing = 0;
	private double priceAuxLimit = 0;
	private boolean postOnly = false;
	private boolean hidden = false;

	public BitfinexOrderBuilder(String symbol, BitfinexOrderType type, double amount, double price) {
		this.symbol = symbol;
		this.type = type;
		this.amount = amount;
		this.price = price;
	}
	
	public BitfinexOrderBuilder setHidden() {
		hidden = true;
		return this;
	}
	
	public BitfinexOrderBuilder setPostOnly() {
		postOnly = true;
		return this;
	}
	
	public BitfinexOrderBuilder setPriceTrailing(final double price) {
		this.priceTrailing = price;
		return this;
	}
	
	public BitfinexOrderBuilder setPriceAuxLimit(final double price) {
		this.priceAuxLimit = price;
		return this;
	}
	
	public BitfinexOrder build() {
		return new BitfinexOrder(symbol, type, price, amount, priceTrailing, priceAuxLimit, postOnly, hidden);
	}
	

}
