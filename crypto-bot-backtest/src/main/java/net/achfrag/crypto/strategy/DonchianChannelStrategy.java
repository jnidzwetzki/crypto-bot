package net.achfrag.crypto.strategy;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.trading.rules.AndRule;
import org.ta4j.core.trading.rules.IsFallingRule;
import org.ta4j.core.trading.rules.IsRisingRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;

import net.achfrag.crypto.strategy.indicator.DonchianChannelLower;
import net.achfrag.crypto.strategy.indicator.DonchianChannelUpper;

public class DonchianChannelStrategy extends TradeStrategyFactory {

	private final int periodUpper;
	private final int periodLower;
	private final DonchianChannelLower donchianChannelLower;
	private final DonchianChannelUpper donchianChannelUpper;

	public DonchianChannelStrategy(final int periodUpper, final int periodLower, final TimeSeries timeSeries) {
		super(timeSeries);
		
		this.periodUpper = periodUpper;
		this.periodLower = periodLower;
		this.donchianChannelLower = new DonchianChannelLower(lowPriceIndicator, periodLower);
		this.donchianChannelUpper = new DonchianChannelUpper(highPriceIndicator, periodUpper);
	}
	
	@Override
	public Strategy getStrategy() {		
	    final MACDIndicator macd = new MACDIndicator(closePriceIndicator, 9, 26);
	    final EMAIndicator emaMacd = new EMAIndicator(macd, 9);
				
		final Rule buyingRule = new AndRule(
				new IsRisingRule(donchianChannelUpper, 1),
				new OverIndicatorRule(macd, Decimal.valueOf(0)));
		
		final Rule sellingRule = //new OrRule(
				new IsFallingRule(donchianChannelLower, 1);
				//new UnderIndicatorRule(macd, emaMacd));
		
		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
		return strategy;
	}

	@Override
	public String getName() {
		return "Donchian-Channel-" + periodUpper + "-" + periodLower;
	}
	
	@Override
	public double getContracts(final double portfolioValue, final int barIndex) {
		final double channelUpper = donchianChannelUpper.getValue(barIndex).toDouble();
		final double channelLower = donchianChannelLower.getValue(barIndex).toDouble();
		final double maxLossPerContract = channelUpper - channelLower;
		
		final double closePrice = timeSeries.getTick(barIndex).getClosePrice().toDouble();
		
		// Max position size per stop loss
		final double positionSizePerLoss = (portfolioValue * 0.02) / maxLossPerContract;
		
		return Math.min(positionSizePerLoss, portfolioValue * 0.5 / closePrice);
	}

}
