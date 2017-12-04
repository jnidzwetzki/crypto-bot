package org.achfrag.crypto.bot;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.entity.APIException;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.crypto.bitfinex.entity.ExchangeOrder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderManager {

	/**
	 * The bitfinex API
	 */
	private BitfinexApiBroker bitfinexApiBroker;

	/**
	 * The session factory for persistence
	 */
	private final SessionFactory sessionFactory;

	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(OrderManager.class);

	public OrderManager(final BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = bitfinexApiBroker;
		
		// Persistence
		this.sessionFactory = new Configuration().configure().buildSessionFactory();

		// Register order callbacks
		bitfinexApiBroker.addOrderCallback((o) -> handleOrderCallback(o));
	}
	
	/**
	 * Handle a new order callback
	 * @param o
	 * @return
	 */
	private void handleOrderCallback(final ExchangeOrder order) {
		
		try(final Session session = sessionFactory.openSession()) {
			session.beginTransaction();
			session.save(order);
			session.getTransaction().commit();
		}
	}

	/**
	 * Execute a new order
	 * @param order
	 */
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
