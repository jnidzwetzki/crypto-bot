package net.achfrag.trading.crypto.bitfinex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public abstract class AbstractSimpleCallbackManager<T> {
	
	/**
	 * The order callbacks
	 */
	private final List<Consumer<T>> callbacks;
	
	/**
	 * The executor service
	 */
	private final ExecutorService executorService;
	
	
	public AbstractSimpleCallbackManager(ExecutorService executorService) {
		this.callbacks = new ArrayList<>();
		this.executorService = executorService;
	}
	
	/**
	 * Add a order callback
	 * @param callback
	 */
	public void registerCallback(final Consumer<T> callback) {
		synchronized (callbacks) {
			callbacks.add(callback);
		}
	}
	
	/**
	 * Remove a order callback
	 * @param callback
	 * @return
	 */
	public boolean removeCallback(final Consumer<T> callback) {
		synchronized (callbacks) {
			return callbacks.remove(callback);
		}
	}
	
	/**
	 * Update a exchange order
	 * @param exchangeOrder
	 */
	public void notifyCallbacks(final T exchangeOrder) {

		// Notify callbacks async		
		if(callbacks == null) {
			return;
		}
				
		synchronized(callbacks) {
			if(callbacks.isEmpty()) {
				return;
			}
			
			callbacks.forEach((c) -> {
				final Runnable runnable = () -> c.accept(exchangeOrder);
				executorService.submit(runnable);
			});
		}
	}
}
