/*******************************************************************************
 *
 *    Copyright (C) 2015-2018 Jan Kristof Nidzwetzki
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package com.github.jnidzwetzki.cryptobot.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.jnidzwetzki.bitfinex.v2.BitfinexApiBroker;
import com.github.jnidzwetzki.bitfinex.v2.entity.APIException;
import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexCurrencyPair;
import com.github.jnidzwetzki.bitfinex.v2.entity.Wallet;
import com.github.jnidzwetzki.bitfinex.v2.manager.WalletManager;
import com.github.jnidzwetzki.cryptobot.CurrencyEntry;
import com.github.jnidzwetzki.cryptobot.portfolio.BasePortfolioManager;
import com.github.jnidzwetzki.cryptobot.portfolio.PortfolioManager;

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

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOT_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOT_USD, entry2);
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

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOT_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOT_USD, entry2);
		
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

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOT_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOT_USD, entry2);
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

		final CurrencyEntry entry2 = new CurrencyEntry(BitfinexCurrencyPair.IOT_USD, 1000, 990);
		entries.put(BitfinexCurrencyPair.IOT_USD, entry2);
		
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
		final WalletManager walletManager = Mockito.mock(WalletManager.class);
		
		Mockito.when(walletManager.getWallets()).thenReturn(wallets);
		Mockito.when(apiBroker.getWalletManager()).thenReturn(walletManager);

		return new BasePortfolioManager(apiBroker, 0.05);
	}
}
