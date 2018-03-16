package net.achfrag.crypto.strategy;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.StopGainRule;
import org.ta4j.core.trading.rules.StopLossRule;

public class EMAStrategy01 extends TradeStrategyFactory {
	
	public EMAStrategy01(final TimeSeries timeSeries) {
		super(timeSeries);
	}
	
	public Strategy getStrategy() {
		SMAIndicator shortSma = new SMAIndicator(closePriceIndicator, 5);
		SMAIndicator longSma = new SMAIndicator(closePriceIndicator, 30);

		Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);

		Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
				.or(new StopLossRule(closePriceIndicator, Decimal.valueOf("3")))
				.or(new StopGainRule(closePriceIndicator, Decimal.valueOf("2")));

		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
		return strategy;
	}

	@Override
	public String getName() {
		return "EMAStrategy01";
	}

	@Override
	public double getContracts(double portfolioValue, int barIndex) {
		return portfolioValue / timeSeries.getBar(barIndex).getClosePrice().doubleValue();
	}
}
