package com.github.jnidzwetzki.cryptobot.strategy;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.StochasticOscillatorDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

public class ForexStrategy01 extends TradeStrategyFactory {
	
	public ForexStrategy01(final TimeSeries timeSeries) {
		super(timeSeries);
	}
	
	public Strategy getStrategy() {
		EMAIndicator sma1 = new EMAIndicator(closePriceIndicator, 5);
		EMAIndicator sma2 = new EMAIndicator(closePriceIndicator, 10);
		
		RSIIndicator rsi = new RSIIndicator(closePriceIndicator, 14);
		
		StochasticOscillatorKIndicator stochK = new StochasticOscillatorKIndicator(timeSeries, 14);
		StochasticOscillatorDIndicator stochD = new StochasticOscillatorDIndicator(stochK);
		
		Rule buyingRule = new CrossedUpIndicatorRule(sma1, sma2)
				.and(new OverIndicatorRule(rsi, Decimal.valueOf(50)))
				.and(new OverIndicatorRule(stochK, stochD))
				.and(new UnderIndicatorRule(stochD, Decimal.valueOf(80)));

		Rule sellingRule = new CrossedDownIndicatorRule(sma1, sma2)
				.or(new CrossedDownIndicatorRule(rsi, Decimal.valueOf(50)));

		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
		return strategy;
	}

	@Override
	public String getName() {
		return "ForexStrategy01";
	}

	@Override
	public double getContracts(double portfolioValue, int barIndex) {
		return portfolioValue / timeSeries.getBar(barIndex).getClosePrice().doubleValue();
	}
}
