package org.achfrag.crypto.bot;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.entity.APIException;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderManager {

	private BitfinexApiBroker bitfinexApiBroker;
	

	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(OrderManager.class);


	public OrderManager(final BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = bitfinexApiBroker;
	}
	
	public void executeOrder(final BitfinexOrder order) {

		if(! bitfinexApiBroker.isAuthenticated()) {
			logger.error("Unable to execute order {} on marketplace, conecction is not authenticated", order);
			return;
		}
			
		try {
			bitfinexApiBroker.placeOrder(order);
		} catch (APIException e) {
			// FIXME: Handle the exception
			logger.error("Got an exception while order execution", e);
		}
	
	}
	

	

}
