package org.achfrag.crypto.bot;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.bitfinex.BitfinexOrderBuilder;
import org.achfrag.crypto.bitfinex.entity.APIException;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.crypto.bitfinex.entity.ExchangeOrder;
import org.achfrag.crypto.bitfinex.entity.Trade;
import org.achfrag.crypto.bitfinex.entity.TradeState;
import org.achfrag.crypto.bitfinex.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
		
		// Persistence session factory
		this.sessionFactory = HibernateUtil.getSessionFactory();

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
	 * Open a new trade
	 * @param trade
	 */
	public void openTrade(final Trade trade) {
		
		if(! bitfinexApiBroker.isAuthenticated()) {
			logger.error("Unable to execute trade {} on marketplace, conecction is not authenticated", trade);
			return;
		}
		
		final double amount = trade.getAmount();
		
		final BitfinexOrder order = BitfinexOrderBuilder
				.create(trade.getSymbol(), BitfinexOrderType.EXCHANGE_MARKET, amount)
				.build();
		
		try {
			trade.setTradeState(TradeState.OPENING);
			trade.addOpenOrder(order);
			bitfinexApiBroker.placeOrder(order);
			trade.setTradeState(TradeState.OPEN);
		} catch (APIException e) {
			logger.error("Got an exception while opening trade {}", trade);
			trade.setTradeState(TradeState.ERROR);
		} finally {
			persistTrade(trade);
		}
	}

	/**
	 * Close a trade
	 * @param trade
	 */
	public void closeTrade(final Trade trade) {
		
		if(! bitfinexApiBroker.isAuthenticated()) {
			logger.error("Unable to execute trade {} on marketplace, conecction is not authenticated", trade);
			return;
		}
		
		final double amount = trade.getAmount() * -1.0;
		
		final BitfinexOrder order = BitfinexOrderBuilder
				.create(trade.getSymbol(), BitfinexOrderType.EXCHANGE_MARKET, amount)
				.build();
		
		try {
			trade.setTradeState(TradeState.CLOSING);
			trade.addCloseOrder(order);
			bitfinexApiBroker.placeOrder(order);
			trade.setTradeState(TradeState.CLOSED);
		} catch (APIException e) {
			logger.error("Got an exception while closing trade {}", trade);
			trade.setTradeState(TradeState.ERROR);
		} finally {
			persistTrade(trade);
		}
	}

	/**
	 * Persist the trade in the database
	 * @param trade
	 */
	private void persistTrade(final Trade trade) {
		// Store order in database
		try(final Session session = sessionFactory.openSession()) {
			session.beginTransaction();
			session.save(trade);
			session.getTransaction().commit();
		}
	}
}
