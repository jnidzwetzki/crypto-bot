package org.achfrag.crypto.bitfinex.entity;

import org.achfrag.crypto.bitfinex.util.MicroSecondTimestampProvider;

public class BitfinexOrder {

	private final long cid;
	private final BitfinexCurrencyPair symbol; 
	private final BitfinexOrderType type;
	private final double price;
	private final double priceTrailing;
	private final double priceAuxLimit;
	private final double amount;
	private final boolean postOnly;
	private final boolean hidden;
	private final int groupId;
	
	public BitfinexOrder(final BitfinexCurrencyPair symbol, final BitfinexOrderType type, final double price, final double amount,
			final double priceTrailing, final double priceAuxLimit, final boolean postOnly, final boolean hidden,
			final int groupId) {
		
		// The client ID
		this.cid = MicroSecondTimestampProvider.getNewTimestamp();

		this.symbol = symbol;
		this.type = type;
		this.price = price;
		this.priceTrailing = priceTrailing;
		this.priceAuxLimit = priceAuxLimit;
		this.amount = amount;
		this.postOnly = postOnly;
		this.hidden = hidden;
		this.groupId = groupId;
	}

	@Override
	public String toString() {
		return "BitfinexOrder [cid=" + cid + ", symbol=" + symbol + ", type=" + type + ", price=" + price
				+ ", priceTrailing=" + priceTrailing + ", priceAuxLimit=" + priceAuxLimit + ", amount=" + amount
				+ ", postOnly=" + postOnly + ", hidden=" + hidden + ", groupId=" + groupId + "]";
	}

	public BitfinexCurrencyPair getSymbol() {
		return symbol;
	}

	public BitfinexOrderType getType() {
		return type;
	}

	public double getPrice() {
		return price;
	}

	public double getPriceTrailing() {
		return priceTrailing;
	}

	public double getPriceAuxLimit() {
		return priceAuxLimit;
	}

	public double getAmount() {
		return amount;
	}

	public boolean isPostOnly() {
		return postOnly;
	}

	public boolean isHidden() {
		return hidden;
	}

	public long getCid() {
		return cid;
	}
	
	public int getGroupId() {
		return groupId;
	}
}
