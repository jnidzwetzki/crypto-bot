package org.achfrag.crypto.bitfinex.test;

import java.util.List;

import org.achfrag.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrderType;
import org.achfrag.crypto.bitfinex.entity.Trade;
import org.achfrag.crypto.bitfinex.entity.TradeDirection;
import org.achfrag.crypto.bitfinex.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class PersistenceTest {

	public static void main(String[] args) throws ClassNotFoundException {

		Class.forName("org.h2.Driver");
		BitfinexOrder order = new BitfinexOrder(BitfinexCurrencyPair.BTC_USD, BitfinexOrderType.EXCHANGE_LIMIT, 0, 0, 0,
				0, false, false, -1);

		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
		Session session = sessionFactory.openSession();

		final Trade trade = new Trade(TradeDirection.LONG	, BitfinexCurrencyPair.BTC_USD, 1);
		trade.getOrdersOpen().add(order);
		
		session.beginTransaction();
		session.save(trade);
		session.getTransaction().commit();

		// now lets pull events from the database and list them
		session = sessionFactory.openSession();
		session.beginTransaction();

		@SuppressWarnings("unchecked")
		List<Trade> result = session.createQuery("from Trade").list();

		for (Trade oneOrder : result) {
			System.out.println(oneOrder);
		}

		session.getTransaction().commit();
		session.close();
	}
}
