package org.achfrag.crypto.strategy;

import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;

public interface TradeStrategyFactory {
	public Strategy getStrategy(TimeSeries timeSeries);
	public String getName();
}
