package org.achfrag.trading.crypto.bitfinex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public abstract class AbstractSimpleCallbackManager<T> {
	
	/**
	 * The order callbacks
	 */
	private final List<Consumer<T>> orderCallbacks;
	
	/**
	 * The executor service
	 */
	private final ExecutorService executorService;
	
	
	public AbstractSimpleCallbackManager(ExecutorService executorService) {
		this.orderCallbacks = new ArrayList<>();
		this.executorService = executorService;
	}
	
	
	/**
	 * Add a order callback
	 * @param callback
	 */
	public void registerOrderCallback(final Consumer<T> callback) {
		synchronized (orderCallbacks) {
			orderCallbacks.add(callback);
		}
	}
	
	/**
	 * Remove a order callback
	 * @param callback
	 * @return
	 */
	public boolean removeOrderCallback(final Consumer<T> callback) {
		synchronized (orderCallbacks) {
			return orderCallbacks.remove(callback);
		}
	}
	
	/**
	 * Update a exchange order
	 * @param exchangeOrder
	 */
	public void notifyCallbacks(final T exchangeOrder) {

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
