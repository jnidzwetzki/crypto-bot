package net.achfrag.crypto.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.achfrag.crypto.bot.CurrencyEntry;
import net.achfrag.crypto.bot.portfolio.BasePortfolioManager;
import net.achfrag.crypto.bot.portfolio.PortfolioManager;
import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import net.achfrag.trading.crypto.bitfinex.entity.Wallet;

public class TestCapitalAllocation {
	
	private final static double DELTA = 0.001;

	@Test
	public void testCapitalAllocationExchange1() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();
		
		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);
		
		portfolioManager.calculatePositionSizes(entries);
		
		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.45, entry1.getPositionSize(), DELTA);
	}
	
	@Test
	public void testCapitalAllocationExchange2() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();

		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOTA_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOTA_USD, entry2);
		portfolioManager.calculatePositionSizes(entries);
	
		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.45, entry1.getPositionSize(), DELTA);
		Assert.assertEquals(0.45, entry2.getPositionSize(), DELTA);
	}
	
	@Test
	public void testCapitalAllocationExchange3() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();

		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOTA_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOTA_USD, entry2);
		
		final CurrencyEntry entry3 = new CurrencyEntry(BitfinexCurrencyPair.XRP_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.XRP_USD, entry3);
		
		portfolioManager.calculatePositionSizes(entries);
		
		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.3, entry1.getPositionSize(), DELTA);
		Assert.assertEquals(0.3, entry2.getPositionSize(), DELTA);		
		Assert.assertEquals(0.3, entry3.getPositionSize(), DELTA);
	}
	
	@Test
	public void testCapitalAllocationExchangeMaxPositionLoss1() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();
		
		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 0);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);
		
		portfolioManager.calculatePositionSizes(entries);
		
		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.045, entry1.getPositionSize(), DELTA);
	}
	
	
	@Test
	public void testCapitalAllocationExchangeMaxPositionLoss2() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();

		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 0);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOTA_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOTA_USD, entry2);
		portfolioManager.calculatePositionSizes(entries);

		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.045, entry1.getPositionSize(), DELTA);
		Assert.assertEquals(0.45, entry2.getPositionSize(), DELTA);
	}
	
	@Test
	public void testCapitalAllocationExchangeMaxPositionLoss3() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();

		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 0);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOTA_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOTA_USD, entry2);
		
		final CurrencyEntry entry3 = new CurrencyEntry(BitfinexCurrencyPair.XRP_USD, 1000, 500);
		entries.put(BitfinexCurrencyPair.XRP_USD, entry3);
		
		portfolioManager.calculatePositionSizes(entries);

		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.045, entry1.getPositionSize(), DELTA);
		Assert.assertEquals(0.45, entry2.getPositionSize(), DELTA);		
		Assert.assertEquals(0.09, entry3.getPositionSize(), DELTA);
	}
	
	/**
	 * Build the portfolio manager
	 * @return
	 * @throws APIException
	 */
	private PortfolioManager buildPortfolioManager() throws APIException {
		final Collection<Wallet> wallets = new ArrayList<>();
		wallets.add(new Wallet(Wallet.WALLET_TYPE_EXCHANGE, "USD", 1000, 0, 1000));
		
		final BitfinexApiBroker apiBroker = Mockito.mock(BitfinexApiBroker.class);
		Mockito.when(apiBroker.getWallets()).thenReturn(wallets);
		
		return new BasePortfolioManager(apiBroker, 0.05);
	}
}
