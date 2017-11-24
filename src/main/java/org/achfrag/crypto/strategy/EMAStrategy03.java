package org.achfrag.crypto.strategy;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;

public class EMAStrategy03 {

	public static Strategy getStrategy(TimeSeries timeSeries, int sma1Value, int sma2Value, int sma3Value) {
		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		EMAIndicator sma1 = new EMAIndicator(closePrice, sma1Value);
		EMAIndicator sma2 = new EMAIndicator(closePrice, sma2Value);
		EMAIndicator sma3 = new EMAIndicator(closePrice, sma3Value);
		
		RSIIndicator rsi = new RSIIndicator(closePrice, 14);
		
		Rule buyingRule = new OverIndicatorRule(sma1, sma2)
				.and(new OverIndicatorRule(sma2, sma3))
				.and(new OverIndicatorRule(rsi, Decimal.valueOf(50)));

		Rule sellingRule = new CrossedDownIndicatorRule(sma1, sma3);
		//		.or(new CrossedDownIndicatorRule(sma2, sma3));

		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
		return strategy;
	}

}
