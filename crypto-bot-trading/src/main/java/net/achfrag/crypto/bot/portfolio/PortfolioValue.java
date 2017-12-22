package net.achfrag.crypto.bot.portfolio;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "portfolio_value")
public class PortfolioValue {

	@Id
	@GeneratedValue
	private long id;
	
	private String apikey;
	
	private double usdValue;
	
	public PortfolioValue() {
	}

	public PortfolioValue(final long id, final String apikey, final double usdValue) {
		this.id = id;
		this.apikey = apikey;
		this.usdValue = usdValue;
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

	public double getUsdValue() {
		return usdValue;
	}

	public void setUsdValue(final double usdValue) {
		this.usdValue = usdValue;
	}

	@Override
	public String toString() {
		return "PortfolioValue [id=" + id + ", apikey=" + apikey + ", usdValue=" + usdValue + "]";
	}
	
}
