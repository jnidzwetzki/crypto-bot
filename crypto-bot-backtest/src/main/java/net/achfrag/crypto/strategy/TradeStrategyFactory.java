package net.achfrag.crypto.strategy;

import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public abstract class TradeStrategyFactory {
	
	/**
	 * The time series
	 */
	protected final TimeSeries timeSeries;
	
	/**
	 * The close price indicator
	 */
	protected final ClosePriceIndicator closePriceIndicator;
	
	public TradeStrategyFactory(final TimeSeries timeSeries) {
		this.timeSeries = timeSeries;
		this.closePriceIndicator = new ClosePriceIndicator(timeSeries);
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
