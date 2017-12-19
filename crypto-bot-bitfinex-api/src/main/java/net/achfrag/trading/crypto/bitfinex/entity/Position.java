package net.achfrag.trading.crypto.bitfinex.entity;

public class Position {

	private final BitfinexCurrencyPair curreny;
	private String status;
	private double amount;
	private double basePrice;
	private double marginFunding;
	private double marginFundingType;
	private double pl;
	private double plPercent;
	private double priceLiquidation;
	private double leverage;
	
	public Position(final BitfinexCurrencyPair curreny) {
		this.curreny = curreny;
	}

	public BitfinexCurrencyPair getCurreny() {
		return curreny;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(final String status) {
		this.status = status;
	}
	
	public double getAmount() {
		return amount;
	}
	
	public void setAmount(final double amount) {
		this.amount = amount;
	}
	
	public double getBasePrice() {
		return basePrice;
	}
	
	public void setBasePrice(final double basePrice) {
		this.basePrice = basePrice;
	}
	
	public double getMarginFunding() {
		return marginFunding;
	}
	
	public void setMarginFunding(final double marginFunding) {
		this.marginFunding = marginFunding;
	}
	
	public double getMarginFundingType() {
		return marginFundingType;
	}
	
	public void setMarginFundingType(final double marginFundingType) {
		this.marginFundingType = marginFundingType;
	}
	
	public double getPl() {
		return pl;
	}
	
	public void setPl(final double pl) {
		this.pl = pl;
	}
	
	public double getPriceLiquidation() {
		return priceLiquidation;
	}
	
	public void setPriceLiquidation(final double priceLiquidation) {
		this.priceLiquidation = priceLiquidation;
	}
	
	public double getLeverage() {
		return leverage;
	}
	
	public void setLeverage(final double leverage) {
		this.leverage = leverage;
	}

	public double getPlPercent() {
		return plPercent;
	}

	public void setPlPercent(final double plPercent) {
		this.plPercent = plPercent;
	}

	@Override
	public String toString() {
		return "Position [curreny=" + curreny + ", status=" + status + ", amount=" + amount + ", basePrice=" + basePrice
				+ ", marginFunding=" + marginFunding + ", marginFundingType=" + marginFundingType + ", pl=" + pl
				+ ", plPercent=" + plPercent + ", priceLiquidation=" + priceLiquidation + ", leverage=" + leverage
				+ "]";
	}
	
}
