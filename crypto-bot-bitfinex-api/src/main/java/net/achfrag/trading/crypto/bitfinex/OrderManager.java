package net.achfrag.trading.crypto.bitfinex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bboxdb.commons.Retryer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexOrder;
import net.achfrag.trading.crypto.bitfinex.entity.ExchangeOrder;
import net.achfrag.trading.crypto.bitfinex.entity.ExchangeOrderState;

public class OrderManager extends AbstractSimpleCallbackManager<ExchangeOrder> {

	/**
	 * The orders
	 */
	private final List<ExchangeOrder> orders;
	
	/**
	 * The api broker
	 */
	private BitfinexApiBroker bitfinexApiBroker;

	/**
	 * The order timeout
	 */
	private final long TIMEOUT_IN_SECONDS = 120;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(OrderManager.class);

	/**
	 * The number of order retries on error
	 */
	private static final int ORDER_RETRIES = 3;

	/**
	 * The delay between two retries
	 */
	private static final int RETRY_DELAY_IN_MS = 1000;

	public OrderManager(final BitfinexApiBroker bitfinexApiBroker) {
		super(bitfinexApiBroker.getExecutorService());
		this.bitfinexApiBroker = bitfinexApiBroker;
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
	
	
	/**
	 * Cancel a order
	 * @param id
	 * @throws APIException, InterruptedException 
	 * @throws InterruptedException 
	 */
	public void placeOrderAndWaitUntilActive(final BitfinexOrder order) throws APIException, InterruptedException {
		
		if(! bitfinexApiBroker.isAuthenticated()) {
			logger.error("Unable to wait for order {}, connection is not authenticated", order);
			return;
		}
		
		order.setApikey(bitfinexApiBroker.getApiKey());
		
		final Callable<Boolean> orderCallable = () -> placeOrderOrderOnAPI(order);
		
		// Bitfinex does not implement a happens-before relationship. Sometimes
		// canceling a stop-loss order and placing a new stop-loss order results 
		// in an 'ERROR, reason is Invalid order: not enough exchange balance' 
		// error for some seconds. The retryer tries to place the order up to 
		// three times
		final Retryer<Boolean> retryer = new Retryer<>(ORDER_RETRIES, RETRY_DELAY_IN_MS, orderCallable);
		retryer.execute();
		
		if(retryer.getNeededExecutions() > 1) {
			logger.info("Nedded {} executions for placing the order", retryer.getNeededExecutions());
		}
		
		if(! retryer.isSuccessfully()) {
			final Exception lastException = retryer.getLastException();
			
			if(lastException == null) {
				throw new APIException("Unable to execute order");
			} else {
				throw new APIException(lastException);
			}
		}
	}

	/**
	 * Execute a new Order
	 * @param order
	 * @return 
	 * @throws Exception
	 */
	private boolean placeOrderOrderOnAPI(final BitfinexOrder order) throws Exception {
		final CountDownLatch waitLatch = new CountDownLatch(1);
		
		final Consumer<ExchangeOrder> ordercallback = (o) -> {
			if(o.getCid() == order.getCid()) {
				waitLatch.countDown();
			}
		};
		
		bitfinexApiBroker.getOrderManager().registerCallback(ordercallback);
		
		try {
			bitfinexApiBroker.placeOrder(order);
			
			waitLatch.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

			if(waitLatch.getCount() != 0) {
				throw new APIException("Timeout while waiting for order");
			}
			
			// Check for order error
			final boolean orderInErrorState = bitfinexApiBroker
					.getOrderManager()
					.getOrders()
					.stream()
					.filter(o -> o.getCid() == order.getCid())
					.anyMatch(o -> o.getState() == ExchangeOrderState.STATE_ERROR);
			
			if(orderInErrorState) {
				throw new APIException("Unable to place order " + order);
			}
			
			return true;
		} catch (Exception e) {
			throw e;
		} finally {
			bitfinexApiBroker.getOrderManager().removeCallback(ordercallback);
		}		
	}
	
	/**
	 * Cancel a order
	 * @param id
	 * @throws APIException, InterruptedException 
	 */
	public void cancelOrderAndWaitForCompletion(final long id) throws APIException, InterruptedException {
		
		if(! bitfinexApiBroker.isAuthenticated()) {
			logger.error("Unable to cancel order {}, connection is not authenticated", id);
			return;
		}
		
		final Callable<Boolean> orderCallable = () -> cancelOrderOnAPI(id);
		
		// See comment in placeOrder()
		final Retryer<Boolean> retryer = new Retryer<>(ORDER_RETRIES, RETRY_DELAY_IN_MS, orderCallable);
		retryer.execute();
		
		if(retryer.getNeededExecutions() > 1) {
			logger.info("Nedded {} executions for canceling the order", retryer.getNeededExecutions());
		}
		
		if(! retryer.isSuccessfully()) {
			final Exception lastException = retryer.getLastException();
			
			if(lastException == null) {
				throw new APIException("Unable to cancel order");
			} else {
				throw new APIException(lastException);
			}
		}
	}

	/**
	 * Cancel the order on the API
	 * @param id
	 * @return
	 * @throws APIException
	 * @throws InterruptedException
	 */
	private boolean cancelOrderOnAPI(final long id) throws APIException, InterruptedException {
		final CountDownLatch waitLatch = new CountDownLatch(1);
		
		final Consumer<ExchangeOrder> ordercallback = (o) -> {
			if(o.getOrderId() == id && o.getState() == ExchangeOrderState.STATE_CANCELED) {
				waitLatch.countDown();
			}
		};
		
		bitfinexApiBroker.getOrderManager().registerCallback(ordercallback);
		
		try {
			logger.info("Cancel order: {}", id);
			bitfinexApiBroker.cancelOrder(id);
			waitLatch.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
			
			if(waitLatch.getCount() != 0) {
				throw new APIException("Timeout while waiting for order");
			}
			
			return true;
			
		} catch (Exception e) {
			throw e;
		} finally {
			bitfinexApiBroker.getOrderManager().removeCallback(ordercallback);
		}
	}
}
