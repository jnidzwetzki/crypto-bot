package org.achfrag.crypto.test;

import java.util.Arrays;
import java.util.List;

import org.achfrag.crypto.bot.OrderManager;
import org.achfrag.crypto.util.HibernateUtil;
import org.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.trading.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.trading.crypto.bitfinex.entity.Trade;
import org.achfrag.trading.crypto.bitfinex.entity.TradeDirection;
import org.achfrag.trading.crypto.bitfinex.entity.TradeState;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestPersistence {
	
	private final SessionFactory sessionFactory;
	
	public TestPersistence() {
		sessionFactory = HibernateUtil.getSessionFactory();
	}
	
	/**
	 * Delete old data from tables
	 */
	@Before
	public void before() {
		try(final Session session = sessionFactory.openSession()) {
			session.beginTransaction();

			for(final String tablename : Arrays.asList("BitfinexOrder", "Trade")) {
				@SuppressWarnings("rawtypes")
				final Query query = session.createQuery("delete from " + tablename);
				query.executeUpdate();
			}
			
			session.close();
		}
	}

	/**
	 * Test order persistence
	 */
	@Test
	public void testStoreAndFetchOrder() {
		
		final BitfinexOrder order = new BitfinexOrder(BitfinexCurrencyPair.BTC_USD, 
				BitfinexOrderType.EXCHANGE_LIMIT, 0, 0, 0, 0, false, false, -1);

		final Session session = sessionFactory.openSession();

		@SuppressWarnings("unchecked")
		final List<BitfinexOrder> result1 = session.createQuery("from BitfinexOrder").list();
		//Assert.assertTrue(result1.isEmpty());
		for(final BitfinexOrder orderFetched : result1) {
			System.out.println("Order " +  orderFetched);
		}
		
		session.beginTransaction();
		session.save(order);
		session.getTransaction().commit();
		
		@SuppressWarnings("unchecked")
		final List<BitfinexOrder> result2 = session.createQuery("from BitfinexOrder").list();
		
		Assert.assertEquals(1, result2.size());
		session.close();
	}
	
	/**
	 * Test testade persistence
	 */
	@Test
	public void testTradePersistence() {
		final BitfinexOrder order = new BitfinexOrder(BitfinexCurrencyPair.BTC_USD, 
				BitfinexOrderType.EXCHANGE_LIMIT, 0, 0, 0, 0, false, false, -1);
		
		final Trade trade = new Trade("ABC", TradeDirection.LONG, BitfinexCurrencyPair.BTC_USD, 1);
		trade.getOrdersOpen().add(order);
		trade.setTradeState(TradeState.OPEN);
		
		final Session session = sessionFactory.openSession();
		
		session.beginTransaction();
		
		@SuppressWarnings("unchecked")
		final List<Trade> result1 = session.createQuery("from Trade t").list();
		Assert.assertTrue(result1.isEmpty());
		
		session.save(trade);
		session.getTransaction().commit();

		session.beginTransaction();
		@SuppressWarnings("unchecked")
		final List<Trade> result2 = session.createQuery("from Trade t").list();

		session.getTransaction().commit();
		session.close();
		
		Assert.assertEquals(1, result2.size());
	}
	
	
	/**
	 * Test get open trades from order manager
	 */
	@Test
	public void testGetOpenTrades() {
		
		final BitfinexApiBroker apiBroker = Mockito.mock(BitfinexApiBroker.class);
		final OrderManager ordermanager = new OrderManager(apiBroker);
		
		Assert.assertTrue(ordermanager.getAllOpenTrades().isEmpty());

		final Trade trade = new Trade("ABC", TradeDirection.LONG, BitfinexCurrencyPair.BTC_USD, 1);
		trade.setTradeState(TradeState.CREATED);
		
		final Session session = sessionFactory.openSession();
		
		session.beginTransaction();
		session.save(trade);
		session.getTransaction().commit();

		Assert.assertTrue(ordermanager.getAllOpenTrades().isEmpty());

		session.beginTransaction();
		trade.setTradeState(TradeState.OPEN);
		session.saveOrUpdate(trade);
		session.getTransaction().commit();
		
		session.beginTransaction();
		Assert.assertEquals(1, ordermanager.getAllOpenTrades().size());
		session.getTransaction().commit();
		
		final Trade trade2 = new Trade("ABC", TradeDirection.LONG, BitfinexCurrencyPair.BTC_USD, 1);
		trade2.setTradeState(TradeState.OPEN);
		session.beginTransaction();
		session.save(trade2);
		session.getTransaction().commit();
		
		session.beginTransaction();
		Assert.assertEquals(2, ordermanager.getAllOpenTrades().size());
		session.getTransaction().commit();
		
		session.close();
	}
}
