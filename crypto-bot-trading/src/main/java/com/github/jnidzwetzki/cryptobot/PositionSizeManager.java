package com.github.jnidzwetzki.cryptobot;

import java.util.Collection;

import org.ta4j.core.Order.OrderType;

import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexCurrencyPair;
import com.github.jnidzwetzki.bitfinex.v2.entity.Wallet;

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
		
		return bitfinexCurrencyPair.getMinimumOrderSize();
	}
}
