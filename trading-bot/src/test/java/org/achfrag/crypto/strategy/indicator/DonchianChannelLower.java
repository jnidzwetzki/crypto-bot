package org.achfrag.crypto.strategy.indicator;

import org.ta4j.core.Decimal;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;

/**
 * Lower donchian channel indicator. 
 * <p>
 * Returns the lowest value of the time series within the tiemframe.
 *
 */
public class DonchianChannelLower extends CachedIndicator<Decimal> {

	private static final long serialVersionUID = 6109484986843725281L;

	/**
	 * The price indicator
	 */
    private Indicator<Decimal> indicator;
    
    /**
     * The time frame of the channel
     */
	private int timeFrame;

	public DonchianChannelLower(Indicator<Decimal> indicator, int timeFrame) {
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
			
			if(value.isLessThanOrEqual(result)) {
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
