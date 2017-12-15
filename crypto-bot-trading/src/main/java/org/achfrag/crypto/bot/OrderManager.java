package org.achfrag.crypto.bot;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.achfrag.crypto.util.HibernateUtil;
import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.trading.crypto.bitfinex.BitfinexOrderBuilder;
import org.achfrag.trading.crypto.bitfinex.entity.APIException;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.trading.crypto.bitfinex.entity.ExchangeOrder;
import org.achfrag.trading.crypto.bitfinex.entity.ExchangeOrderState;
import org.achfrag.trading.crypto.bitfinex.entity.Trade;
import org.achfrag.trading.crypto.bitfinex.entity.TradeState;
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

	/**
	 * The open trades query
	 */
	private static final String OPEN_TRADES_QUERY = "from Trade t where t.tradeState = '" + TradeState.OPEN.name() + "'";

	public OrderManager(final BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = Objects.requireNonNull(bitfinexApiBroker);
		
		// Persistence session factory
		this.sessionFactory = HibernateUtil.getSessionFactory();

		// Register order callbacks
		bitfinexApiBroker.getOrderManager().registerOrderCallback((o) -> handleOrderCallback(o));
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
			session.saveOrUpdate(trade);
			session.getTransaction().commit();
		}
	}
	
	/**
	 * Get all open trades from database
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Trade> getAllOpenTrades() {
		try (final Session session = sessionFactory.openSession()) {
			return session.createQuery(OPEN_TRADES_QUERY).list();
		}
	}
	
	/**
	 * Cancel a order
	 * @param id
	 * @throws APIException, InterruptedException 
	 */
	public void cancelOrderAndWaitForCompletion(final long id) throws APIException, InterruptedException {
		
		if(! bitfinexApiBroker.isAuthenticated()) {
			logger.error("Unable to cancel order {}, conecction is not authenticated", id);
			return;
		}
		
		final CountDownLatch waitLatch = new CountDownLatch(1);
		
		final Consumer<ExchangeOrder> ordercallback = (o) -> {
			if(o.getOrderId() == id && o.getState() == ExchangeOrderState.STATE_CANCELED) {
				waitLatch.countDown();
			}
		};
		
		bitfinexApiBroker.getOrderManager().registerOrderCallback(ordercallback);
		
		try {
			bitfinexApiBroker.cancelOrder(id);
			waitLatch.await();
		} catch (Exception e) {
			throw e;
		} finally {
			bitfinexApiBroker.getOrderManager().removeOrderCallback(ordercallback);
		}
		
	}
}
