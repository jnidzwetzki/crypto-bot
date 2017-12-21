package net.achfrag.crypto.bot.portfolio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bboxdb.commons.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import net.achfrag.crypto.bot.CurrencyEntry;
import net.achfrag.crypto.bot.PortfolioOrderManager;
import net.achfrag.trading.crypto.bitfinex.BitfinexApiBroker;
import net.achfrag.trading.crypto.bitfinex.BitfinexOrderBuilder;
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
	protected final PortfolioOrderManager orderManager;
	
	/**
	 * The positions for capital allocation
	 */
	private int positionsForCapitalAllocation;
	
	/**
	 * The investment threshold
	 */
	private static final double USD_INVESTMENT_THRESHOLD = 20;
	
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
		this.orderManager = new PortfolioOrderManager(bitfinexApiBroker);
	}
	
	public void syncOrders(final Map<BitfinexCurrencyPair, CurrencyEntry> entries, 
			final Map<BitfinexCurrencyPair, Double> exits) throws InterruptedException, APIException {
		
		updatePositionForCapitalAllocation(entries, exits);
		
		placeEntryOrders(entries);
		placeExitOrders(exits);
	}

	/**
	 * Update the positions for capital allocation
	 * @param entries
	 * @param exits
	 */
	@VisibleForTesting
	public void updatePositionForCapitalAllocation(final Map<BitfinexCurrencyPair, CurrencyEntry> entries,
			final Map<BitfinexCurrencyPair, Double> exits) {
		
		positionsForCapitalAllocation = calculateTotalPositionsForCapitalAllocation(entries, exits);
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
		
		// Cancel old and changed orders
		cancelOldChangedEntryOrders(entries);
		
		// Place the new entry orders
		placeNewEntryOrders(entries);
	}

	/**
	 * Adjust the entry orders
	 * @param entries
	 * @throws APIException
	 * @throws InterruptedException
	 */
	private void cancelOldChangedEntryOrders(final Map<BitfinexCurrencyPair, CurrencyEntry> entries)
			throws APIException, InterruptedException {

		// Check current limits and position sizes
		for(final BitfinexCurrencyPair currency : entries.keySet()) {
			final ExchangeOrder order = getOpenOrderForSymbol(currency.toBitfinexString());
			
			// No old order present
			if(order == null) {
				continue;
			}

			final CurrencyEntry entry = entries.get(currency);
			final double entryPrice = entry.getEntryPrice();
			final double positionSize = calculatePositionSize(entry);

			if(order.getAmount() == positionSize && order.getPrice() <= entryPrice) {
				logger.info("Order for {} is fine", currency);
			} else {
				logger.info("Cancel entry order for {}, values changed (amount: {} / {}} (price: {} / {})", 
						currency, order.getAmount(), positionSize, order.getPrice(), entryPrice);	

				if(! SIMULATION) {
					orderManager.cancelOrderAndWaitForCompletion(order.getOrderId());
				}
			}
		}
	}
	
	/**
	 * Place the new entry orders
	 * @param entries
	 * @throws APIException 
	 * @throws InterruptedException 
	 */
	private void placeNewEntryOrders(final Map<BitfinexCurrencyPair, CurrencyEntry> entries) throws APIException, InterruptedException {
		
		final double positionSizeUSD = captialAvailablePerPosition();
		
		if(positionSizeUSD < USD_INVESTMENT_THRESHOLD) {
			logger.info("Dont place entry orders, USD per position is too small {} < {}", 
					positionSizeUSD, USD_INVESTMENT_THRESHOLD);
			
			return;
		}
		
		// Check current limits and position sizes
		for(final BitfinexCurrencyPair currency : entries.keySet()) {
			final ExchangeOrder order = getOpenOrderForSymbol(currency.toBitfinexString());
			
			final CurrencyEntry entry = entries.get(currency);
			final double entryPrice = entry.getEntryPrice();
			
			final double positionSize = calculatePositionSize(entry);

			if(positionSize < currency.getMinimalOrderSize()) {
				logger.info("Not placing order for {}, position size is too small {}", currency, positionSize);
				continue;
			}
			
			// Old order present
			if(order != null) {
				logger.info("Not placing a new order for {}, old order still active", currency);
				continue;
			}
			
			final BitfinexOrder newOrder = BitfinexOrderBuilder
					.create(currency, getOrderType(), positionSize)
					.withPrice(entryPrice)
					.setPostOnly()
					.build();
	
			if(! SIMULATION) {
				orderManager.placeOrderAndWaitUntilActive(newOrder);
			}
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
				
				if(! SIMULATION) {
					orderManager.cancelOrderAndWaitForCompletion(order.getOrderId());
				}
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
				if(order.getPrice() < exitPrice) {
					logger.info("Exit price for {} has moved form {} to {}, canceling old order", 
							currency, order.getPrice(), exitPrice);
					
					if(! SIMULATION) {
						orderManager.cancelOrderAndWaitForCompletion(order.getOrderId());
					}
				} else {
					logger.info("Old order price for {} is fine: {}", currency, exitPrice);
					continue;
				}
			} 
			
			final double positionSize = getOpenPositionSizeForCurrency(currency.getCurrency1());
		
			// * -1.0 for sell order
			final double positionSizeSell = positionSize * -1.0;
			
			final BitfinexOrder newOrder = BitfinexOrderBuilder
					.create(currency, getOrderType(), positionSizeSell)
					.withPrice(exitPrice)
					.setPostOnly()
					.build();
	
			if(! SIMULATION) {
				orderManager.placeOrderAndWaitUntilActive(newOrder);
			}
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
				
				if(! SIMULATION) {
					orderManager.cancelOrderAndWaitForCompletion(order.getOrderId());
				}
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
					if(! SIMULATION) {
						orderManager.cancelOrderAndWaitForCompletion(order.getOrderId());
					}
				}
			}
		}
		
		
	}
	
	/**
	 * Calculate the positions size in USD
	 * @return
	 */
	private double captialAvailablePerPosition() throws APIException {
		final Wallet wallet = getWalletForCurrency("USD");
		
		// Wallet could be empty
		if(wallet == null) {
			return 0;
		}
		
		final double investmentRate = getInvestmentRate();

		// The total portfolio value
		final double totalPortfolioValueInUSD = getTotalPortfolioValueInUSD() * investmentRate;
				
		// Capital per position
		final double capitalAvailablePerPosition = (wallet.getBalance() * investmentRate) / positionsForCapitalAllocation;
		
		logger.debug("Total portfolio value {}, capital per position {} ({} total pos)", 
				totalPortfolioValueInUSD, capitalAvailablePerPosition, positionsForCapitalAllocation);
		
		// Max position size is determined by MAX_POSITION_SIZE
		return Math.min(capitalAvailablePerPosition, 
				totalPortfolioValueInUSD * MAX_SINGLE_POSITION_SIZE);
	}

	/**
	 * Calculate the position size
	 * @param upperValue
	 * @return
	 * @throws APIException 
	 */
	@VisibleForTesting
	public double calculatePositionSize(final CurrencyEntry entry) throws APIException  {

		/**
		 * Calculate position size by max capital
		 */
		// Capital available per position
		final double capitalAvailablePerPosition = captialAvailablePerPosition();
						
		// Max position size (capital)
		final double positionSizePerCapital = (capitalAvailablePerPosition / entry.getEntryPrice());
		
		/**
		 * Calculate position size by max loss
		 */
		// Max loss per position
		final double maxLossPerPositon = entry.getEntryPrice() - entry.getStopLossPrice();
		
		// The total portfolio value
		final double totalPortfolioValueInUSD = getTotalPortfolioValueInUSD();
		
		// Max position size per stop loss
		final double positionSizePerLoss = totalPortfolioValueInUSD * MAX_LOSS_PER_POSITION / maxLossPerPositon;

		// =============
		logger.debug("Position size {} per capital is {}, position size per max loss is {}", 
				entry.getCurrencyPair(), positionSizePerCapital, positionSizePerLoss);
		
		final double positionSize = Math.min(positionSizePerCapital, positionSizePerLoss);

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
	 * Calculate the amount of open positions
	 * @param entries
	 * @return
	 */
	protected abstract int calculateTotalPositionsForCapitalAllocation(
			final Map<BitfinexCurrencyPair, CurrencyEntry> entries, 
			final Map<BitfinexCurrencyPair, Double> exits);
	
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

}
