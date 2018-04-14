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
package com.github.jnidzwetzki.cryptobot.portfolio;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jnidzwetzki.bitfinex.v2.BitfinexApiBroker;
import com.github.jnidzwetzki.bitfinex.v2.entity.APIException;
import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexCurrencyPair;
import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexOrderType;
import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexTick;
import com.github.jnidzwetzki.bitfinex.v2.entity.Wallet;
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexTickerSymbol;

public class BasePortfolioManager extends PortfolioManager {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BasePortfolioManager.class);


	public BasePortfolioManager(final BitfinexApiBroker bitfinexApiBroker, final double maxLossPerPosition) {
		super(bitfinexApiBroker, maxLossPerPosition);
	}

	/**
	 * Get the used wallet type 
	 * @return
	 */
	@Override
	protected String getWalletType() {
		return Wallet.WALLET_TYPE_EXCHANGE;
	}
	
	/**
	 * Get the used order type
	 * @return 
	 */
	@Override
	protected BitfinexOrderType getOrderType() {
		return BitfinexOrderType.EXCHANGE_STOP;
	}
	
	/**
	 * Get the position size for the symbol
	 * @param symbol
	 * @return
	 * @throws APIException 
	 */
	@Override
	protected BigDecimal getOpenPositionSizeForCurrency(final String currency) throws APIException {
		final Wallet wallet = getWalletForCurrency(currency);
		return wallet.getBalance();
	}
	
	/**
	 * Get the investment rate
	 */
	@Override
	protected BigDecimal getInvestmentRate() {
		return new BigDecimal(0.9);
	}

	@Override
	protected BigDecimal getTotalPortfolioValueInUSD() throws APIException {
		final List<Wallet> wallets = getAllWallets();
				
		BigDecimal totalValue = new BigDecimal(0);
		for(final Wallet wallet : wallets) {
			
			final String curreny = wallet.getCurreny();
			
			if(curreny.equals("USD")) {
				totalValue = totalValue.add(wallet.getBalance());
			} else {
				final String symbol = "t" + curreny + "USD";
				
				try {
					final BitfinexCurrencyPair bitfinexCurrencyPair = BitfinexCurrencyPair.fromSymbolString(symbol);
					final BitfinexTickerSymbol bitfinexSymbol = new BitfinexTickerSymbol(bitfinexCurrencyPair);
					final BitfinexTick lastTick = bitfinexApiBroker.getQuoteManager().getLastTick(bitfinexSymbol);
					
					if(lastTick != null) {
						final BigDecimal rate = lastTick.getClose();
						final BigDecimal value = rate.multiply(wallet.getBalance());
						totalValue = totalValue.add(value);
					} else {
						logger.debug("Unable to find tick for {}, appraise wallet with 0 USD", symbol);
					}
				} catch(IllegalArgumentException e) {
					logger.debug("Unkown symbol {}, ignoring wallet", symbol);
				}
			}
		}
		
		return totalValue;
	}

	@Override
	protected BigDecimal getAvailablePortfolioValueInUSD() throws APIException {
		final List<Wallet> wallets = getAllWallets();
		
		for(final Wallet wallet : wallets) {
			if(wallet.getCurreny().equals("USD")) {
				return wallet.getBalance();
			}
		}
		
		logger.error("Unable to find USD wallet");
		
		return BigDecimal.ZERO;
	}

}
