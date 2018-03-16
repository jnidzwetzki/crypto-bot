package net.achfrag.crypto.strategy;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;

public class EMAStrategy02 extends TradeStrategyFactory  {
	
	private final int sma1Value;
	private final int sma2Value;
	private final int sma3Value;

	public EMAStrategy02(final int sma1Value, final int sma2Value, final int sma3Value, final TimeSeries timeSeries) {
		super(timeSeries);
		this.sma1Value = sma1Value;
		this.sma2Value = sma2Value;
		this.sma3Value = sma3Value;
	}

	public Strategy getStrategy() {
		EMAIndicator sma1 = new EMAIndicator(closePriceIndicator, sma1Value);
		EMAIndicator sma2 = new EMAIndicator(closePriceIndicator, sma2Value);
		EMAIndicator sma3 = new EMAIndicator(closePriceIndicator, sma3Value);
		
		Rule buyingRule = new CrossedUpIndicatorRule(sma1, sma2).and(new OverIndicatorRule(sma2, sma3));

		Rule sellingRule = new CrossedDownIndicatorRule(sma1, sma3);
		//		.or(new CrossedDownIndicatorRule(sma2, sma3));

		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
		return strategy;
	}

	@Override
	public String getName() {
		return "EMAStrategy02";
	}
	
	@Override
	public double getContracts(double portfolioValue, int barIndex) {
		return portfolioValue / timeSeries.getBar(barIndex).getClosePrice().doubleValue();
	}

}
