package org.achfrag.crypto.bitfinex.test;

import java.util.List;

import org.achfrag.crypto.bitfinex.entity.BitfinexCurrencyPair;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrder;
import org.achfrag.crypto.bitfinex.entity.BitfinexOrderType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class PersistenceTest {

	public static void main(String[] args) throws ClassNotFoundException {

		Class.forName("org.h2.Driver");
		BitfinexOrder order = new BitfinexOrder(BitfinexCurrencyPair.BTC_USD, BitfinexOrderType.EXCHANGE_LIMIT, 0, 0, 0,
				0, false, false, -1);

		SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
		Session session = sessionFactory.openSession();

		session.beginTransaction();
		session.save(order);
		session.getTransaction().commit();

		// now lets pull events from the database and list them
		session = sessionFactory.openSession();
		session.beginTransaction();

		@SuppressWarnings("unchecked")
		List<BitfinexOrder> result = session.createQuery("from BitfinexOrder").list();

		for (BitfinexOrder oneOrder : result) {
			System.out.println(oneOrder);
		}

		session.getTransaction().commit();
		session.close();
	}
}
