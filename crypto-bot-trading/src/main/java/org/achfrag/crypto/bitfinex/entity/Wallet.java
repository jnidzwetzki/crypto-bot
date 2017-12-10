package org.achfrag.crypto.bitfinex.entity;

public class Wallet {

	private String walletType;
	private String curreny;
	private double balance;
	private double unsettledInterest;
	private double balanceAvailable;
	
	public final static String WALLET_TYPE_EXCHANGE = "exchange";
	
	public final static String WALLET_TYPE_MARGIN = "margin";
	
	public final static String WALLET_TYPE_FUNDING = "funding";

	public Wallet(final String walletType, final String curreny, final double balance, final double unsettledInterest,
			final double balanceAvailable) {
		this.walletType = walletType;
		this.curreny = curreny;
		this.balance = balance;
		this.unsettledInterest = unsettledInterest;
		this.balanceAvailable = balanceAvailable;
	}

	@Override
	public String toString() {
		return "Wallet [walletType=" + walletType + ", curreny=" + curreny + ", balance=" + balance
				+ ", unsettledInterest=" + unsettledInterest + ", balanceAvailable=" + balanceAvailable + "]";
	}

	public String getWalletType() {
		return walletType;
	}

	public void setWalletType(final String walletType) {
		this.walletType = walletType;
	}

	public String getCurreny() {
		return curreny;
	}

	public void setCurreny(final String curreny) {
		this.curreny = curreny;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(final double balance) {
		this.balance = balance;
	}

	public double getUnsettledInterest() {
		return unsettledInterest;
	}

	public void setUnsettledInterest(final double unsettledInterest) {
		this.unsettledInterest = unsettledInterest;
	}

	public double getBalanceAvailable() {
		return balanceAvailable;
	}

	public void setBalanceAvailable(final double balanceAvailable) {
		this.balanceAvailable = balanceAvailable;
	}
}
