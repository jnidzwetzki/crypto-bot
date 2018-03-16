package net.achfrag.crypto.strategy;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.StopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

public class BBreakoutStrategy extends TradeStrategyFactory {

	private final int bbPeriod;
	
	private final int deviationUp;
	
	private final int deviationDown;

	public BBreakoutStrategy(final int bbPeriod, final int deviationUp, final int deviationDown, final TimeSeries timeSeries) {
		super(timeSeries);
		this.bbPeriod = bbPeriod;
		this.deviationUp = deviationUp;
		this.deviationDown = deviationDown;
	}

	public Strategy getStrategy() {
		
		final ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);
		final SMAIndicator sma = new SMAIndicator(closePrice, bbPeriod);
		
		final BollingerBandsMiddleIndicator bbmiddle = new BollingerBandsMiddleIndicator(sma);
		final StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, bbPeriod);
		 
		final BollingerBandsUpperIndicator bbup = new BollingerBandsUpperIndicator(bbmiddle, sd, Decimal.valueOf(deviationUp));
		final BollingerBandsUpperIndicator bbdown = new BollingerBandsUpperIndicator(bbmiddle, sd, Decimal.valueOf(deviationDown));

		final Rule buyingRule = new UnderIndicatorRule(closePrice, bbdown);
		final Rule sellingRule = new OverIndicatorRule(closePrice, bbup).or(new StopLossRule(closePrice, Decimal.valueOf(2)));

		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
		return strategy;
	}


	@Override
	public String getName() {
		return "Bolinger-breakout-" + bbPeriod + "-" + deviationUp + "-" + deviationDown;
	}

	@Override
	public double getContracts(double portfolioValue, int barIndex) {
		return portfolioValue / timeSeries.getBar(barIndex).getClosePrice().doubleValue();
	}

}
