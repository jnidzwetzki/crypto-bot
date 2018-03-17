package com.github.jnidzwetzki.cryptobot.strategy;

import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.MinPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;

public abstract class TradeStrategyFactory {
	
	/**
	 * The time series
	 */
	protected final TimeSeries timeSeries;
	
	/**
	 * The open price indicator
	 */
	protected final OpenPriceIndicator openPriceIndicator;
	
	/**
	 * The close price indicator
	 */
	protected final ClosePriceIndicator closePriceIndicator;
	
	/**
	 * The high price indicator
	 */
	protected final MaxPriceIndicator highPriceIndicator;
	
	/**
	 * The low price indicator
	 */
	protected final MinPriceIndicator lowPriceIndicator;
	
	public TradeStrategyFactory(final TimeSeries timeSeries) {
		this.timeSeries = timeSeries;
		this.closePriceIndicator = new ClosePriceIndicator(timeSeries);
		this.openPriceIndicator = new OpenPriceIndicator(timeSeries);
		this.highPriceIndicator = new MaxPriceIndicator(timeSeries);
		this.lowPriceIndicator = new MinPriceIndicator(timeSeries);
	}

	public TimeSeries getTimeSeries() {
		return timeSeries;
	}
	
	/**
	 * Get the strategy
	 * @param timeSeries
	 * @return
	 */
	public abstract Strategy getStrategy();
	
	/**
	 * Get the name of the strategy
	 * @return
	 */
	public abstract String getName();
	
	/**
	 * Get the amount of contracts for the given portfolio value
	 */
	public abstract double getContracts(final double portfolioValue, final int barIndex);
}
