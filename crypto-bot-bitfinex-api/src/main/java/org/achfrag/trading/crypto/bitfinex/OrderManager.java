package org.achfrag.trading.crypto.bitfinex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.achfrag.trading.crypto.bitfinex.entity.APIException;
import org.achfrag.trading.crypto.bitfinex.entity.ExchangeOrder;
import org.achfrag.trading.crypto.bitfinex.entity.ExchangeOrderState;

public class OrderManager {

	/**
	 * The order callbacks
	 */
	private final List<Consumer<ExchangeOrder>> orderCallbacks;
	
	/**
	 * The orders
	 */
	private final List<ExchangeOrder> orders;

	/**
	 * The executor service
	 */
	private ExecutorService executorService;
	
	public OrderManager(ExecutorService executorService) {
		this.executorService = executorService;
		this.orderCallbacks = new ArrayList<>();
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
	 * Add a order callback
	 * @param callback
	 */
	public void registerOrderCallback(final Consumer<ExchangeOrder> callback) {
		synchronized (orderCallbacks) {
			orderCallbacks.add(callback);
		}
	}
	
	/**
	 * Remove a order callback
	 * @param callback
	 * @return
	 */
	public boolean removeOrderCallback(final Consumer<ExchangeOrder> callback) {
		synchronized (orderCallbacks) {
			return orderCallbacks.remove(callback);
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
		
		// Notify callbacks async		
		if(orderCallbacks == null) {
			return;
		}
				
		synchronized(orderCallbacks) {
			if(orderCallbacks.isEmpty()) {
				return;
			}
			
			orderCallbacks.forEach((c) -> {
				final Runnable runnable = () -> c.accept(exchangeOrder);
				executorService.submit(runnable);
			});
		}
	}
}
