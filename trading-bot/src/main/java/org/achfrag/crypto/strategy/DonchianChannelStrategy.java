package org.achfrag.crypto.strategy;

import org.achfrag.crypto.strategy.indicator.DonchianChannelLower;
import org.achfrag.crypto.strategy.indicator.DonchianChannelUpper;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.IsFallingRule;
import org.ta4j.core.trading.rules.IsRisingRule;

public class DonchianChannelStrategy implements TradeStrategyFactory {

	private int periodUpper;
	private int periodLower;

	public DonchianChannelStrategy(final int periodUpper, final int periodLower) {
		this.periodUpper = periodUpper;
		this.periodLower = periodLower;
	}
	
	@Override
	public Strategy getStrategy(final TimeSeries timeSeries) {
		final ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		final DonchianChannelLower donchianChannelLower = new DonchianChannelLower(closePrice, periodLower);
		final DonchianChannelUpper donchianChannelUpper = new DonchianChannelUpper(closePrice, periodUpper);
				
		final Rule buyingRule = new IsRisingRule(donchianChannelUpper, 1);
		final Rule sellingRule = new IsFallingRule(donchianChannelLower, 1);
		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
		return strategy;
	}

	@Override
	public String getName() {
		return "Donchian-Channel-" + periodLower + "-" + periodUpper;
	}

}
