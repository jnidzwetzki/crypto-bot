package org.achfrag.trading.crypto.bitfinex.entity;

public class TradingOrderbookEntry {
	private final double price;
	private final double count;
	private final double amount;
	
	public TradingOrderbookEntry(final double price, final double count, final double amount) {
		this.price = price;
		this.count = count;
		this.amount = amount;
	}

	public double getPrice() {
		return price;
	}

	public double getCount() {
		return count;
	}

	public double getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return "TradingOrderbook [price=" + price + ", count=" + count + ", amount=" + amount + "]";
	}

}
