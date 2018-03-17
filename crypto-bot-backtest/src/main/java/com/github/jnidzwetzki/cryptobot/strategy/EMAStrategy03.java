/*******************************************************************************
 *
 *    Copyright (C) 2015-2018 Jan Kristof Nidzwetzki
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package com.github.jnidzwetzki.cryptobot.strategy;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.StopLossRule;

public class EMAStrategy03 extends TradeStrategyFactory {

	private int sma1Value;
	private int sma2Value;
	private int sma3Value;

	public EMAStrategy03(final int sma1Value, final int sma2Value, final int sma3Value, TimeSeries timeSeries) {
		super(timeSeries);
		this.sma1Value = sma1Value;
		this.sma2Value = sma2Value;
		this.sma3Value = sma3Value;
	}
	
	public Strategy getStrategy() {
		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		EMAIndicator sma1 = new EMAIndicator(closePrice, sma1Value);
		EMAIndicator sma2 = new EMAIndicator(closePrice, sma2Value);
		EMAIndicator sma3 = new EMAIndicator(closePrice, sma3Value);
		
		RSIIndicator rsi = new RSIIndicator(closePrice, 14);
		
		Rule buyingRule = new OverIndicatorRule(sma1, sma2)
				.and(new OverIndicatorRule(sma2, sma3))
				.and(new OverIndicatorRule(rsi, Decimal.valueOf(50)));

		Rule sellingRule = new CrossedDownIndicatorRule(sma1, sma3)
				.or(new CrossedDownIndicatorRule(sma2, sma3))
				.or(new StopLossRule(closePrice, Decimal.valueOf("3")));

		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
		return strategy;
	}

	@Override
	public String getName() {
		return "EMAStrategy03-" + sma1Value + "-" + sma2Value + "-" + sma3Value;
	}

	@Override
	public double getContracts(double portfolioValue, int barIndex) {
		return portfolioValue / timeSeries.getBar(barIndex).getClosePrice().doubleValue();
	}
}
