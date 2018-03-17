package com.github.jnidzwetzki.cryptobot.bot.portfolio;

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
	
	private long timestamp;
	
	public PortfolioValue() {
		this.timestamp = System.currentTimeMillis();
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

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "PortfolioValue [id=" + id + ", apikey=" + apikey + ", usdValue=" + usdValue + ", timestamp=" + timestamp
				+ "]";
	}


	
}
