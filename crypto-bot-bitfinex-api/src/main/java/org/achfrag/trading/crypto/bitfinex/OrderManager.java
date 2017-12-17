package org.achfrag.trading.crypto.bitfinex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.achfrag.trading.crypto.bitfinex.entity.APIException;
import org.achfrag.trading.crypto.bitfinex.entity.ExchangeOrder;
import org.achfrag.trading.crypto.bitfinex.entity.ExchangeOrderState;

public class OrderManager extends AbstractSimpleCallbackManager<ExchangeOrder> {

	/**
	 * The orders
	 */
	private final List<ExchangeOrder> orders;

	public OrderManager(final ExecutorService executorService) {
		super(executorService);
		this.orders = new ArrayList<>();
	}
	
	/**
	 * Clear all orders
	 */
	public void clear() {
		synchronized (orders) {
			orders.clear();	
		}
	}
	
	
	/**
	 * Get the list with exchange orders
	 * @return
	 * @throws APIException 
	 */
	public List<ExchangeOrder> getOrders() throws APIException {		
		synchronized (orders) {
			return orders;
		}
	}
	
	/**
	 * Update a exchange order
	 * @param exchangeOrder
	 */
	public void updateOrder(final ExchangeOrder exchangeOrder) {
		
		synchronized (orders) {
			// Replace order 
			orders.removeIf(o -> o.getOrderId() == exchangeOrder.getOrderId());
			
			// Remove canceled orders
			if(exchangeOrder.getState() != ExchangeOrderState.STATE_CANCELED) {
				orders.add(exchangeOrder);
			}
						
			orders.notifyAll();
		}
		
		notifyCallbacks(exchangeOrder);
	}
}
