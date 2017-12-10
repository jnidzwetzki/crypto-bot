package org.achfrag.crypto.bot;

import java.util.Collection;

import org.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.trading.crypto.bitfinex.entity.Wallet;
import org.ta4j.core.Order.OrderType;

public class PositionSizeManager {
	
	/**
	 * Determine the position size
	 * @param bitfinexCurrencyPair
	 * @param orderType 
	 * @param collection
	 * @return
	 */
	public static double getPositionSize(final BitfinexCurrencyPair bitfinexCurrencyPair, 
			final OrderType orderType, final Collection<Wallet> collection) {
		
		return bitfinexCurrencyPair.getMinimalOrderSize();
	}
}
