package net.achfrag.crypto.bot.portfolio;

import java.util.List;
import java.util.Map;

import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexOrderType;
import net.achfrag.trading.crypto.bitfinex.entity.Position;
import net.achfrag.trading.crypto.bitfinex.entity.Wallet;

public class MarginPortfolioManager extends PortfolioManager {

	public MarginPortfolioManager(final BitfinexApiBroker bitfinexApiBroker) {
		super(bitfinexApiBroker);
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
	 * Caluclate the amount of open positions
	 * @param entries
	 * @return
	 */
	protected int calculateTotalPositionsForCapitalAllocation(
			final Map<BitfinexCurrencyPair, Double> entries, 
			final Map<BitfinexCurrencyPair, Double> exits) {		
		

		return entries.size() + exits.size();
	}
	
	/**
	 * Get the investment rate
	 */
	@Override
	protected double getInvestmentRate() {
		return 2.0;
	}
	
}
