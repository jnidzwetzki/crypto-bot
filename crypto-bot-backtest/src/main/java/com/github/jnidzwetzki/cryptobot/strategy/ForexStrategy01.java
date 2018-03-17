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
