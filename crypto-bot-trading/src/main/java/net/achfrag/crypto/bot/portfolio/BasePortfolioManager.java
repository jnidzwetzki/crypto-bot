package net.achfrag.crypto.bot.portfolio;

import java.util.Map;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexOrderType;
import net.achfrag.trading.crypto.bitfinex.entity.Wallet;

public class BasePortfolioManager extends PortfolioManager {

	public BasePortfolioManager(final BitfinexApiBroker bitfinexApiBroker) {
		super(bitfinexApiBroker);
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
	protected double getOpenPositionSizeForCurrency(final String currency) throws APIException {
		final Wallet wallet = getWalletForCurrency(currency);
		return wallet.getBalance();
	}
	
	/**
	 * Caluclate the amount of open positions
	 * @param entries
	 * @return
	 */
	@Override
	protected int calculateTotalPositionsForCapitalAllocation(
			final Map<BitfinexCurrencyPair, Double> entries, 
			final Map<BitfinexCurrencyPair, Double> exits) {		
		
		// Executed orders are moved to an extra wallet.
		// So we need only to split the balance of the USD wallet 
		return entries.size();
	}
	
	/**
	 * Get the investment rate
	 */
	@Override
	protected double getInvestmentRate() {
		return 0.9;
	}
}
