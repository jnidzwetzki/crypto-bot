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
package com.github.jnidzwetzki.cryptobot.strategy.indicator;

import org.ta4j.core.Decimal;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;

/**
 * Lower donchian channel indicator. 
 * <p>
 * Returns the highest value of the time series within the tiemframe.
 *
 */
public class DonchianChannelUpper extends CachedIndicator<Decimal> {

	private static final long serialVersionUID = 6109484986843725281L;

	/**
	 * The price indicator
	 */
    private Indicator<Decimal> indicator;
    
    /**
     * The time frame of the channel
     */
	private int timeFrame;

	public DonchianChannelUpper(Indicator<Decimal> indicator, int timeFrame) {
        super(indicator.getTimeSeries());
		this.indicator = indicator;
		this.timeFrame = timeFrame;
    }

	
	@Override
	protected Decimal calculate(int index) {
		int startIndex = Math.max(0, index - timeFrame + 1);
		
		Decimal result = indicator.getValue(index);
		
		for(int pos = startIndex; pos <= index; pos++) {
			final Decimal value = indicator.getValue(pos);
			
			if(value.isGreaterThan(result)) {
				result = value;
			}
		}
		
		return result;
	}
	
    @Override
    public String toString() {
        return getClass().getSimpleName() + "timeFrame: " + timeFrame;
    }

}
