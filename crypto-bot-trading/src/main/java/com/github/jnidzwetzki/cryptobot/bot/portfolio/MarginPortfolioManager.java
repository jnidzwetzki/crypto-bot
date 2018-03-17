package com.github.jnidzwetzki.cryptobot.bot.portfolio;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jnidzwetzki.bitfinex.v2.BitfinexApiBroker;
import com.github.jnidzwetzki.bitfinex.v2.entity.APIException;
import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexOrderType;
import com.github.jnidzwetzki.bitfinex.v2.entity.Position;
import com.github.jnidzwetzki.bitfinex.v2.entity.Wallet;

public class MarginPortfolioManager extends PortfolioManager {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(MarginPortfolioManager.class);

	public MarginPortfolioManager(final BitfinexApiBroker bitfinexApiBroker, final double maxLossPerPosition) {
		super(bitfinexApiBroker, maxLossPerPosition);
	}

	/**
	 * Get the used wallet type 
	 * @return
	 */
	@Override
	protected String getWalletType() {
		return Wallet.WALLET_TYPE_MARGIN;
	}
	
	/**
	 * Get the used order type
	 * @return 
	 */
	@Override
	protected BitfinexOrderType getOrderType() {
		return BitfinexOrderType.STOP;
	}
	
	/**
	 * Get the position size for the symbol
	 * @param symbol
	 * @return
	 * @throws APIException 
	 */
	@Override
	protected double getOpenPositionSizeForCurrency(final String currency) throws APIException {
		final List<Position> positions = bitfinexApiBroker.getPositionManager().getPositions();
		
		final Position position = positions.stream()
				.filter(p -> p.getCurreny().getCurrency1().equals(currency))
				.findAny()
				.orElse(null);
		
		if(position == null) {
			return 0;
		}
		
		return position.getAmount();
	}

	/**
	 * Get the investment rate
	 */
	@Override
	protected double getInvestmentRate() {
		return 2.0;
	}

	@Override
	protected double getTotalPortfolioValueInUSD() throws APIException {
		final List<Wallet> wallets = getAllWallets();
		
		for(final Wallet wallet : wallets) {
			if(wallet.getCurreny().equals("USD")) {
				return wallet.getBalance();
			}
		}
		
		logger.error("Unable to find USD wallet");
		
		return 0;
	}

	@Override
	protected double getAvailablePortfolioValueInUSD() throws APIException {
		final List<Wallet> wallets = getAllWallets();
		
		for(final Wallet wallet : wallets) {
			if(wallet.getCurreny().equals("USD")) {
				return wallet.getBalanceAvailable();
			}
		}
		
		logger.error("Unable to find USD wallet");
		
		return 0;
	}
	
}
