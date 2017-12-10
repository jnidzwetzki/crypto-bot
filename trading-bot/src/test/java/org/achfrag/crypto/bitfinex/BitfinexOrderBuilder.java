package org.achfrag.crypto.bitfinex;

import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.crypto.bitfinex.entity.BitfinexCurrencyPair;

public class BitfinexOrderBuilder {

	private final BitfinexCurrencyPair symbol; 
	private final BitfinexOrderType type;
	private final double amount;
	
	private double price = -1;
	private double priceTrailing = -1;
	private double priceAuxLimit = -1;
	private boolean postOnly = false;
	private boolean hidden = false;
	private int groupid = -1;

	private BitfinexOrderBuilder(final BitfinexCurrencyPair symbol, final BitfinexOrderType type, 
			final double amount) {
		
		this.symbol = symbol;
		this.type = type;
		this.amount = amount;
	}
	
	public static BitfinexOrderBuilder create(final BitfinexCurrencyPair symbol, final BitfinexOrderType type, 
			final double amount) {
		
		return new BitfinexOrderBuilder(symbol, type, amount);
	}
	
	public BitfinexOrderBuilder setHidden() {
		hidden = true;
		return this;
	}
	
	public BitfinexOrderBuilder setPostOnly() {
		postOnly = true;
		return this;
	}
	
	public BitfinexOrderBuilder withPrice(final double price) {
		this.price = price;
		return this;
	}
	
	public BitfinexOrderBuilder withPriceTrailing(final double price) {
		this.priceTrailing = price;
		return this;
	}
	
	public BitfinexOrderBuilder withPriceAuxLimit(final double price) {
		this.priceAuxLimit = price;
		return this;
	}
	
	public BitfinexOrderBuilder withGroupId(final int groupId) {
		this.groupid = groupId;
		return this;
	}
	
	public BitfinexOrder build() {
		return new BitfinexOrder(symbol, type, price, 
				amount, priceTrailing, priceAuxLimit, postOnly, hidden, groupid);
	}

}
