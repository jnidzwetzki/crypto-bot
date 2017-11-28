package org.achfrag.crypto.bitfinex;

public class BitfinexOrder {

	private final String symbol; 
	private final BitfinexOrderType type;
	private final double price;
	private final double priceTrailing;
	private final double priceAuxLimit;
	private final double amount;
	private final boolean postOnly;
	private final boolean hidden;
	
	public BitfinexOrder(final String symbol, final BitfinexOrderType type, final double price, final double amount,
			final double priceTrailing, final double priceAuxLimit, final boolean postOnly, final boolean hidden) {
		this.symbol = symbol;
		this.type = type;
		this.price = price;
		this.priceTrailing = priceTrailing;
		this.priceAuxLimit = priceAuxLimit;
		this.amount = amount;
		this.postOnly = postOnly;
		this.hidden = hidden;
	}

	@Override
	public String toString() {
		return "BitfinexOrder [symbol=" + symbol + ", type=" + type + ", price=" + price + ", priceTrailing="
				+ priceTrailing + ", priceAuxLimit=" + priceAuxLimit + ", amount=" + amount + ", postOnly=" + postOnly
				+ ", hidden=" + hidden + "]";
	}

	public String getSymbol() {
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

}