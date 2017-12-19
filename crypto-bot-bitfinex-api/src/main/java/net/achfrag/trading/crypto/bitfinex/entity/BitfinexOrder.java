package net.achfrag.trading.crypto.bitfinex.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import net.achfrag.trading.crypto.bitfinex.util.MicroSecondTimestampProvider;

@Entity
@Table(name = "orders_executed")
public class BitfinexOrder {

	@Id
	@GeneratedValue
	private long id;
	
	private final long cid;
	
	private String apikey;
	
	@Enumerated(EnumType.STRING)
	private final BitfinexCurrencyPair symbol; 
	
	@Enumerated(EnumType.STRING)
	private final BitfinexOrderType type;
	
	private final double price;
	private final double priceTrailing;
	private final double priceAuxLimit;
	private final double amount;
	private final boolean postOnly;
	private final boolean hidden;
	private final int groupId;
	
	/**
	 * Needed for hibernate
	 */
	public BitfinexOrder() {
		// The client ID
		this.cid = MicroSecondTimestampProvider.getNewTimestamp();

		this.symbol = null;
		this.apikey = null;
		this.type = null;
		this.price = -1;
		this.priceTrailing = -1;
		this.priceAuxLimit = -1;
		this.amount = -1;
		this.postOnly = false;
		this.hidden = false;
		this.groupId = -1;
	}
	
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
		return "BitfinexOrder [id=" + id + ", cid=" + cid + ", apikey=" + apikey + ", symbol=" + symbol + ", type="
				+ type + ", price=" + price + ", priceTrailing=" + priceTrailing + ", priceAuxLimit=" + priceAuxLimit
				+ ", amount=" + amount + ", postOnly=" + postOnly + ", hidden=" + hidden + ", groupId=" + groupId + "]";
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

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public String getApikey() {
		return apikey;
	}

	public void setApikey(final String apikey) {
		this.apikey = apikey;
	}
	
}
