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
		
		portfolioManager.updatePositionForCapitalAllocation(entries, new HashMap<>());
		final double positionSize1 = portfolioManager.calculatePositionSize(entry1);
		
		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.45, positionSize1, DELTA);
	}
	
	@Test
	public void testCapitalAllocationExchange2() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();

		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOTA_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOTA_USD, entry2);
		portfolioManager.updatePositionForCapitalAllocation(entries, new HashMap<>());
		final double positionSize1 = portfolioManager.calculatePositionSize(entry1);
		final double positionSize2 = portfolioManager.calculatePositionSize(entry2);

		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.45, positionSize1, DELTA);
		Assert.assertEquals(0.45, positionSize2, DELTA);
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
		
		portfolioManager.updatePositionForCapitalAllocation(entries, new HashMap<>());
		
		final double positionSize1 = portfolioManager.calculatePositionSize(entry1);
		final double positionSize2 = portfolioManager.calculatePositionSize(entry2);
		final double positionSize3 = portfolioManager.calculatePositionSize(entry3);

		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.3, positionSize1, DELTA);
		Assert.assertEquals(0.3, positionSize2, DELTA);		
		Assert.assertEquals(0.3, positionSize3, DELTA);
	}
	
	@Test
	public void testCapitalAllocationExchangeMaxPositionLoss1() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();
		
		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 0);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);
		
		portfolioManager.updatePositionForCapitalAllocation(entries, new HashMap<>());
		final double positionSize1 = portfolioManager.calculatePositionSize(entry1);
		
		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.05, positionSize1, DELTA);
	}
	
	
	@Test
	public void testCapitalAllocationExchangeMaxPositionLoss2() throws APIException {
		
		final PortfolioManager portfolioManager = buildPortfolioManager();

		final Map<BitfinexCurrencyPair, CurrencyEntry> entries = new HashMap<>();
		final CurrencyEntry entry1 = new CurrencyEntry(BitfinexCurrencyPair.BTC_USD, 1000, 0);
		entries.put(BitfinexCurrencyPair.BTC_USD, entry1);

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOTA_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOTA_USD, entry2);
		portfolioManager.updatePositionForCapitalAllocation(entries, new HashMap<>());
		final double positionSize1 = portfolioManager.calculatePositionSize(entry1);
		final double positionSize2 = portfolioManager.calculatePositionSize(entry2);

		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.05, positionSize1, DELTA);
		Assert.assertEquals(0.45, positionSize2, DELTA);
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
		
		portfolioManager.updatePositionForCapitalAllocation(entries, new HashMap<>());
		
		final double positionSize1 = portfolioManager.calculatePositionSize(entry1);
		final double positionSize2 = portfolioManager.calculatePositionSize(entry2);
		final double positionSize3 = portfolioManager.calculatePositionSize(entry3);

		// Max loss = 10, max capital allocation 50%
		Assert.assertEquals(0.05, positionSize1, DELTA);
		Assert.assertEquals(0.3, positionSize2, DELTA);		
		Assert.assertEquals(0.1, positionSize3, DELTA);
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
		
		return new BasePortfolioManager(apiBroker);
	}
}
