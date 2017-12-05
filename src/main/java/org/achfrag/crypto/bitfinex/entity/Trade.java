package org.achfrag.crypto.bitfinex.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.achfrag.crypto.bitfinex.util.MicroSecondTimestampProvider;

@Entity
@Table(name = "trades")
public class Trade {
		
	@Id
	@GeneratedValue
	private long tid;
	
	/**
	 * The state of the trade
	 */
	private TradeState tradeState;
	
	/**
	 * The direction of the trade
	 */
	private TradeDirection tradeDirection;
	
	/**
	 * The orders to open the position
	 */
    @OneToMany(cascade=CascadeType.ALL)
    @JoinColumn(nullable=true)
	private final List<BitfinexOrder> ordersOpen;
	
	/**
	 * The orders to close the position
	 */
    @OneToMany(cascade=CascadeType.ALL)
    @JoinColumn(nullable=true)
	private final List<BitfinexOrder> ordersClose;
    
    /** 
     * The symbol to trade
     */
	private BitfinexCurrencyPair symbol; 

	/**
	 * The amount to trade
	 */
	private double amount;
	
	/**
	 * The expeted open price
	 */
	private double expectedPriceOpen;
	
	/**
	 * The expected close price
	 */
	private double expectedPriceClose;

	public Trade() {
		tid = MicroSecondTimestampProvider.getNewTimestamp();
		tradeState = TradeState.CREATED;
		ordersOpen = new ArrayList<>();
		ordersClose = new ArrayList<>();
		expectedPriceOpen = -1;
		expectedPriceClose = -1;
	}
	
	public Trade(final TradeDirection tradeDirection, final BitfinexCurrencyPair symbol, final double amount) {
		this();
		this.tradeDirection = tradeDirection;
		this.symbol = symbol;
		this.amount = amount;
	}

	@Override
	public String toString() {
		return "Trade [tid=" + tid + ", tradeState=" + tradeState + ", tradeDirection=" + tradeDirection
				+ ", ordersOpen=" + ordersOpen + ", ordersClose=" + ordersClose + ", symbol=" + symbol + ", amount="
				+ amount + ", expectedPriceOpen=" + expectedPriceOpen + ", expectedPriceClose=" + expectedPriceClose
				+ "]";
	}

	public long getTid() {
		return tid;
	}


	public void setTid(final long tid) {
		this.tid = tid;
	}


	public TradeState getTradeState() {
		return tradeState;
	}


	public void setTradeState(final TradeState tradeState) {
		this.tradeState = tradeState;
	}


	public TradeDirection getTradeDirection() {
		return tradeDirection;
	}


	public void setTradeDirection(final TradeDirection tradeDirection) {
		this.tradeDirection = tradeDirection;
	}


	public BitfinexCurrencyPair getSymbol() {
		return symbol;
	}


	public void setSymbol(final BitfinexCurrencyPair symbol) {
		this.symbol = symbol;
	}


	public double getAmount() {
		return amount;
	}


	public void setAmount(final double amount) {
		this.amount = amount;
	}


	public List<BitfinexOrder> getOrdersOpen() {
		return ordersOpen;
	}


	public List<BitfinexOrder> getOrdersClose() {
		return ordersClose;
	}
	
	public void addCloseOrder(final BitfinexOrder order) {
		ordersClose.add(order);
	}
	
	public void addOpenOrder(final BitfinexOrder order) {
		ordersOpen.add(order);
	}

	public double getExpectedPriceOpen() {
		return expectedPriceOpen;
	}

	public void setExpectedPriceOpen(final double expectedPriceOpen) {
		this.expectedPriceOpen = expectedPriceOpen;
	}

	public double getExpectedPriceClose() {
		return expectedPriceClose;
	}

	public void setExpectedPriceClose(final double expectedPriceClose) {
		this.expectedPriceClose = expectedPriceClose;
	}
	
}
