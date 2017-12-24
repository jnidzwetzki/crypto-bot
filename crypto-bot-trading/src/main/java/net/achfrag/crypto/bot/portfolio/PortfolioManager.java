package net.achfrag.crypto.bot.portfolio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bboxdb.commons.MathUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import net.achfrag.crypto.bot.CurrencyEntry;
import net.achfrag.crypto.bot.PortfolioOrderManager;
import net.achfrag.crypto.util.HibernateUtil;
import net.achfrag.crypto.util.MathHelper;
import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.BitfinexOrderBuilder;
import net.achfrag.trading.crypto.bitfinex.OrderManager;
import net.achfrag.trading.crypto.bitfinex.entity.APIException;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexCurrencyPair;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexOrder;
import net.achfrag.trading.crypto.bitfinex.entity.BitfinexOrderType;
import net.achfrag.trading.crypto.bitfinex.entity.ExchangeOrder;
import net.achfrag.trading.crypto.bitfinex.entity.ExchangeOrderState;
import net.achfrag.trading.crypto.bitfinex.entity.Wallet;

public abstract class PortfolioManager {

	/**
	 * The bitfinex api broker
	 */
	protected BitfinexApiBroker bitfinexApiBroker;
	
	/**
	 * The order manager
	 */
	protected final OrderManager orderManager;

	/**
	 * The threshold for invested / not invested
	 */
	private static final double INVESTED_THRESHOLD = 0.002;

	/**
	 * Maximum loss per position 
	 */
	private static final double MAX_LOSS_PER_POSITION = 0.05;
	
	/**
	 * The maximum position size
	 */
	private static final double MAX_SINGLE_POSITION_SIZE = 0.5;
	
	/**
	 * Simulate or real trading
	 */
	public final static boolean SIMULATION = false;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PortfolioManager.class);

	public PortfolioManager(BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = bitfinexApiBroker;
		
		this.orderManager = bitfinexApiBroker.getOrderManager();
		
		// Init to store orders in DB
		new PortfolioOrderManager(bitfinexApiBroker);
	}
	
	public void syncOrders(final Map<BitfinexCurrencyPair, CurrencyEntry> entries, 
			final Map<BitfinexCurrencyPair, Double> exits) throws InterruptedException, APIException {
				
		placeEntryOrders(entries);
		placeExitOrders(exits);
		
		updatePortfolioValue();
	}

	/**
	 * Write the USD portfolio value to DB
	 * @throws APIException
	 */
	private void updatePortfolioValue() throws APIException {
		logger.debug("Updating portfolio value");
		final double portfolioValueUSD = getTotalPortfolioValueInUSD();
		final PortfolioValue portfolioValue = new PortfolioValue();
		portfolioValue.setApikey(bitfinexApiBroker.getApiKey());
		portfolioValue.setUsdValue(portfolioValueUSD);
		
		final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
		
		try(final Session session = sessionFactory.openSession()) {
			session.beginTransaction();
			session.save(portfolioValue);
			session.getTransaction().commit();
		}
	}

	/**
	 * Place the entry orders for the market
	 * @param currencyPair
	 * @throws APIException 
	 */
	private void placeEntryOrders(final Map<BitfinexCurrencyPair, CurrencyEntry> entries) 
			throws InterruptedException, APIException {
		
		logger.info("Processing entry orders {}", entries);
		
		// Cancel old open entry orders
		cancelRemovedEntryOrders(entries);
		
		// Calculate the position sizes
		calculatePositionSizes(entries);

		// Place the new entry orders
		placeNewEntryOrders(entries);
	}

	/**
	 * Calculate the position sizes
	 * @param entries
	 * @throws APIException
	 */
	@VisibleForTesting
	public void calculatePositionSizes(final Map<BitfinexCurrencyPair, CurrencyEntry> entries) throws APIException {
		final Wallet wallet = getWalletForCurrency("USD");
		
		// Wallet could be empty
		if(wallet == null) {
			throw new APIException("Unable to find USD wallet");
		}
		
		final double capitalAvailable = getAvailablePortfolioValueInUSD() * getInvestmentRate();
		double capitalNeeded = 0;
		
		for(final BitfinexCurrencyPair currency : entries.keySet()) {
			final CurrencyEntry entry = entries.get(currency);
			final double positionSize = calculatePositionSize(entry);
			entry.setPositionSize(positionSize);
			capitalNeeded = capitalNeeded + (positionSize * entry.getEntryPrice());
		}
		
		// Need the n% risk per position more than the available capital
		if(capitalNeeded > capitalAvailable) {
			
			final double investmentCorrectionFactor = capitalAvailable / capitalNeeded;

			logger.info("Needed capital {}, available capital {} ({})", capitalNeeded, 
					capitalAvailable, investmentCorrectionFactor);
			
			capitalNeeded = 0;
			for(final BitfinexCurrencyPair currency : entries.keySet()) {
				final CurrencyEntry entry = entries.get(currency);
				final double newPositionSize = roundPositionSize(entry.getPositionSize() * investmentCorrectionFactor);
				entry.setPositionSize(newPositionSize);
				capitalNeeded = capitalNeeded + (entry.getPositionSize() * entry.getEntryPrice());
			}
			
			logger.info("Needed capital {} after correction", capitalNeeded);
		}
	}

	/** 
	 * Has the entry order changed?
	 */
	private boolean hasEntryOrderChanged(final BitfinexCurrencyPair currency, final ExchangeOrder order, 
			final double entryPrice, final double positionSize) {
		
		if(order.getAmount() != positionSize) {
			return true;
		}
		
		if(order.getPrice() > entryPrice && ! MathHelper.almostEquals(order.getPrice(), entryPrice)) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Place the new entry orders
	 * @param entries
	 * @throws APIException 
	 * @throws InterruptedException 
	 */
	private void placeNewEntryOrders(final Map<BitfinexCurrencyPair, CurrencyEntry> entries) 
			throws APIException, InterruptedException  {
		
		// Check current limits and position sizes
		for(final BitfinexCurrencyPair currency : entries.keySet()) {
			final ExchangeOrder order = getOpenOrderForSymbol(currency.toBitfinexString());
			
			final CurrencyEntry entry = entries.get(currency);
			final double entryPrice = entry.getEntryPrice();
			final double positionSize = entry.getPositionSize();

			if(positionSize < currency.getMinimalOrderSize()) {
				logger.info("Not placing order for {}, position size is too small {}", currency, positionSize);
				continue;
			}
			
			// Old order present
			if(order != null) {
				if(hasEntryOrderChanged(currency, order, entryPrice, positionSize)) {
					logger.info("Entry order for {}, values changed (amount: {} / {}} (price: {} / {})", 
							currency, order.getAmount(), positionSize, order.getPrice(), entryPrice);	
					
					cancelOrder(order);
				} else {
					logger.info("Not placing a new order for {}, old order still active", currency);
					continue;
				}
			}
			
			final BitfinexOrder newOrder = BitfinexOrderBuilder
					.create(currency, getOrderType(), positionSize)
					.withPrice(entryPrice)
					.setPostOnly()
					.build();
	
			placeOrder(newOrder);	
		}
	}

	/**
	 *  Place an order and catch exceptions
	 * @throws InterruptedException 
	 */
	private void placeOrder(final BitfinexOrder newOrder) throws InterruptedException {
		if(SIMULATION) {
			return;
		}
		
		try {
			orderManager.placeOrderAndWaitUntilActive(newOrder);
		} catch(APIException e) {
			logger.error("Unable to place order", e);
		} 
	}

	/**
	 * Cancel the removed entry orders 
	 * Position is at the moment not interesting for an entry
	 * 
	 * @param entries
	 * @throws APIException
	 * @throws InterruptedException
	 */
	private void cancelRemovedEntryOrders(final Map<BitfinexCurrencyPair, CurrencyEntry> entries)
			throws APIException, InterruptedException {
		
		final List<ExchangeOrder> entryOrders = getAllOpenEntryOrders();
		
		for(final ExchangeOrder order : entryOrders) {
			final String symbol = order.getSymbol();
			final BitfinexCurrencyPair currencyPair = BitfinexCurrencyPair.fromSymbolString(symbol);
			
			if(! entries.containsKey(currencyPair)) {
				logger.info("Entry order for {} is not contained, canceling", currencyPair);
				
				cancelOrder(order);
			}
		}
	}
	
	/**
	 * Place the stop loss orders
	 * @param exits
	 */
	private void placeExitOrders(final Map<BitfinexCurrencyPair, Double> exits) 
			throws InterruptedException, APIException {
		
		logger.info("Process exit orders {}", exits);
		
		cleanupOldExitOrders(exits);
		
		placeNewExitOrders(exits);
	}

	/**
	 * Place the exit orders
	 * @param exits
	 * @throws APIException
	 * @throws InterruptedException
	 */
	private void placeNewExitOrders(final Map<BitfinexCurrencyPair, Double> exits)
			throws APIException, InterruptedException {
		
		for(final BitfinexCurrencyPair currency : exits.keySet()) {
			final ExchangeOrder order = getOpenOrderForSymbol(currency.toBitfinexString());
			final double exitPrice = exits.get(currency);
			
			// Check old orders
			if(order != null) {
				final double orderPrice = order.getPrice();
				
				if(orderPrice >= exitPrice || MathHelper.almostEquals(orderPrice, exitPrice)) {
					logger.info("Old order price for {} is fine (price: order {} model {})", 
							currency, orderPrice, exitPrice);
					continue;
				} 
				
				logger.info("Exit price for {} has moved form {} to {}, canceling old order", 
						currency, orderPrice, exitPrice);
				
				cancelOrder(order);
			} 
			
			final double positionSize = getOpenPositionSizeForCurrency(currency.getCurrency1());
		
			// * -1.0 for sell order
			final double positionSizeSell = positionSize * -1.0;
			
			final BitfinexOrder newOrder = BitfinexOrderBuilder
					.create(currency, getOrderType(), positionSizeSell)
					.withPrice(exitPrice)
					.setPostOnly()
					.build();
	
			placeOrder(newOrder);
		}
	}

	/**
	 * Cleanup the old exit orders (remove duplicates, unknown orders)
	 * @param exits
	 * @throws APIException
	 * @throws InterruptedException
	 */
	private void cleanupOldExitOrders(final Map<BitfinexCurrencyPair, Double> exits)
			throws APIException, InterruptedException {
	
		final List<ExchangeOrder> oldExitOrders = getAllOpenExitOrders();
	
		// Remove unknown orders
		for(final ExchangeOrder order : oldExitOrders) {
			final BitfinexCurrencyPair symbol = BitfinexCurrencyPair.fromSymbolString(order.getSymbol());
			
			if(! exits.containsKey(symbol)) {
				logger.error("Found old and unknown order {}, canceling", order);
				
				cancelOrder(order);
			}
		}
		
		// Remove duplicates
		final Map<String, List<ExchangeOrder>> oldOrders = oldExitOrders.stream()
			             .collect(Collectors.groupingBy(ExchangeOrder::getSymbol));
		
		for(final String symbol : oldOrders.keySet()) {
			final List<ExchangeOrder> symbolOrderList = oldOrders.get(symbol);
			if(symbolOrderList.size() > 1) {
				logger.error("Found duplicates {}", symbolOrderList);
				
				for(final ExchangeOrder order : symbolOrderList) {
					cancelOrder(order);
				}
			}
		}		
	}

	/**
	 * Cancel the order and catch the exception
	 * @param order
	 * @throws APIException
	 * @throws InterruptedException
	 */
	private void cancelOrder(final ExchangeOrder order) throws InterruptedException {
		if(SIMULATION) {
			return;
		}
		
		try {
			orderManager.cancelOrderAndWaitForCompletion(order.getOrderId());
		} catch (APIException e) {
			logger.error("Got an exception while canceling the order", e);
		}
	}
	
	/**
	 * Calculate the position size
	 * @param upperValue
	 * @return
	 * @throws APIException 
	 */
	private double calculatePositionSize(final CurrencyEntry entry) throws APIException  {

		/**
		 * Calculate position size by max capital
		 */		
		// Max position size (capital)
		final double positionSizePerCapital = (getTotalPortfolioValueInUSD() 
				* getInvestmentRate() * MAX_SINGLE_POSITION_SIZE) / entry.getEntryPrice();
		
		/**
		 * Calculate position size by max loss
		 */
		// Max loss per position
		final double maxLossPerPositon = entry.getEntryPrice() - entry.getStopLossPrice();
		
		// The total portfolio value
		final double totalPortfolioValueInUSD = getTotalPortfolioValueInUSD() * getInvestmentRate();
		
		// Max position size per stop loss
		final double positionSizePerLoss = totalPortfolioValueInUSD * MAX_LOSS_PER_POSITION / maxLossPerPositon;

		// =============
		logger.info("Position size {} per capital is {}, position size per max loss is {}", 
				entry.getCurrencyPair(), positionSizePerCapital, positionSizePerLoss);
		
		final double positionSize = Math.min(positionSizePerCapital, positionSizePerLoss);

		return roundPositionSize(positionSize);
	}

	/**
	 * Round the position size
	 * @param positionSize
	 * @return
	 */
	private double roundPositionSize(final double positionSize) {
		return MathUtil.round(positionSize, 6);
	}

	/**
	 * Get the open stop loss order
	 * @param symbol
	 * @param openOrders
	 * @return
	 * @throws APIException 
	 */
	private ExchangeOrder getOpenOrderForSymbol(final String symbol) throws APIException {
		
		final List<ExchangeOrder> openOrders = bitfinexApiBroker.getOrderManager().getOrders();
		
		return openOrders.stream()
			.filter(e -> e.getOrderType() == getOrderType())
			.filter(e -> e.getState() == ExchangeOrderState.STATE_ACTIVE)
			.filter(e -> e.getSymbol().equals(symbol))
			.findAny()
			.orElse(null);
	}
	
	/**
	 * Get all open entry orders
	 * @return 
	 * @throws APIException 
	 */
	private List<ExchangeOrder> getAllOpenEntryOrders() throws APIException {
		final List<ExchangeOrder> openOrders = bitfinexApiBroker.getOrderManager().getOrders();
		
		return openOrders.stream()
			.filter(e -> e.getOrderType() == getOrderType())
			.filter(e -> e.getState() == ExchangeOrderState.STATE_ACTIVE)
			.filter(e -> e.getAmount() > 0)
			.collect(Collectors.toList());
	}
	
	/**
	 * Get all open entry orders
	 * @return 
	 * @throws APIException 
	 */
	private List<ExchangeOrder> getAllOpenExitOrders() throws APIException {
		final List<ExchangeOrder> openOrders = bitfinexApiBroker.getOrderManager().getOrders();
		
		return openOrders.stream()
			.filter(e -> e.getOrderType() == getOrderType())
			.filter(e -> e.getState() == ExchangeOrderState.STATE_ACTIVE)
			.filter(e -> e.getAmount() <= 0)
			.collect(Collectors.toList());
	}
 	
	/**
	 * Get the exchange wallet
	 * @param currency 
	 * @return
	 * @throws APIException 
	 */
	protected Wallet getWalletForCurrency(final String currency) throws APIException {
		return bitfinexApiBroker.getWallets()
			.stream()
			.filter(w -> w.getWalletType().equals(getWalletType()))
			.filter(w -> w.getCurreny().equals(currency))
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * Get all wallets
	 * @param currency 
	 * @return
	 * @throws APIException 
	 */
	protected List<Wallet> getAllWallets() throws APIException {
		return bitfinexApiBroker.getWallets()
			.stream()
			.filter(w -> w.getWalletType().equals(getWalletType()))
			.collect(Collectors.toList());
	}
	
	/**
	 * Is the given position open
	 * @param currency
	 * @return
	 * @throws APIException 
	 */
	public boolean isPositionOpen(final String currency) throws APIException {
		final Wallet wallet = getWalletForCurrency(currency);
		
		if(wallet == null) {
			// Unused wallets are not included in API communication
			logger.debug("Wallet for {} is null", currency);
			return false;
		}
		
		if(wallet.getBalance() > INVESTED_THRESHOLD) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return "Portfolio manager: " + bitfinexApiBroker.getApiKey();
	}
	
	/*
	 * Abstract methods
	 */
	
	/**
	 * Get the used wallet type 
	 * @return
	 */
	protected abstract String getWalletType();
	
	/**
	 * Get the type for the orders
	 */
	protected abstract BitfinexOrderType getOrderType();

	/**
	 * Get the position size for the symbol
	 * @param symbol
	 * @return
	 * @throws APIException 
	 */
	protected abstract double getOpenPositionSizeForCurrency(final String currency) throws APIException;

	/**
	 * Get the investment rate
	 * @return
	 */
	protected abstract double getInvestmentRate();
	
	/**
	 * Get the total portfolio value in USD
	 * @throws APIException 
	 */
	protected abstract double getTotalPortfolioValueInUSD() throws APIException;
	
	/**
	 * Get the available portfolio value in USD
	 * @throws APIException 
	 */
	protected abstract double getAvailablePortfolioValueInUSD() throws APIException;
	

}
