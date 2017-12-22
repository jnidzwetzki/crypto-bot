package net.achfrag.crypto.bot;

import java.util.List;
import java.util.Objects;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.achfrag.crypto.util.HibernateUtil;
import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.BitfinexOrderBuilder;
import net.achfrag.trading.crypto.bitfinex.OrderManager;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexOrder;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexOrderType;
import net.achfrag.trading.crypto.bitfinex.entity.ExchangeOrder;
import net.achfrag.trading.crypto.bitfinex.entity.Trade;
import net.achfrag.trading.crypto.bitfinex.entity.TradeState;

public class PortfolioOrderManager {

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
	private final static Logger logger = LoggerFactory.getLogger(PortfolioOrderManager.class);

	/**
	 * The open trades query
	 */
	private static final String OPEN_TRADES_QUERY = "from Trade t where t.tradeState = '" + TradeState.OPEN.name() + "'";

	public PortfolioOrderManager(final BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = Objects.requireNonNull(bitfinexApiBroker);
		
		// Persistence session factory
		this.sessionFactory = HibernateUtil.getSessionFactory();

		// Register order callbacks
		final OrderManager orderManager = bitfinexApiBroker.getOrderManager();
		
		if(orderManager != null) {
			orderManager.registerCallback((o) -> handleOrderCallback(o));
		}
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
}
