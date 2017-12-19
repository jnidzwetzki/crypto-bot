package net.achfrag.crypto.bot.portfolio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bboxdb.commons.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	public void syncOrders(final Map<BitfinexCurrencyPair, Double> entries, 
			final Map<BitfinexCurrencyPair, Double> exits) throws InterruptedException, APIException {
		
		positionsForCapitalAllocation = calculateTotalPositionsForCapitalAllocation(entries, exits);
		
		placeEntryOrders(entries);
		placeExitOrders(exits);
	}
	
	/**
	 * Place the entry orders for the market
	 * @param currencyPair
	 * @throws APIException 
	 */
	private void placeEntryOrders(final Map<BitfinexCurrencyPair, Double> entries) 
			throws InterruptedException, APIException {
		
		logger.info("Processing entry orders {}", entries);
		
		// Cancel old open entry orders
		cancelRemovedEntryOrders(entries);
		
		// Cancel old and changed orders
		cancelOldChangedOrders(entries);
		
		// Place the new entry orders
		placeNewEntryOrders(entries);
	}

	/**
	 * Adjust the entry orders
	 * @param entries
	 * @throws APIException
	 * @throws InterruptedException
	 */
	private void cancelOldChangedOrders(final Map<BitfinexCurrencyPair, Double> entries)
			throws APIException, InterruptedException {
		
		final double positionSizeUSD = positionSizeInUSD();
		
		// Check current limits and position sizes
		for(final BitfinexCurrencyPair currency : entries.keySet()) {
			final ExchangeOrder order = getOpenOrderForSymbol(currency.toBitfinexString());
			
			// No old order present
			if(order == null) {
				continue;
			}

			final double entryPrice = entries.get(currency);
			final double positionSize = calculatePositionSize(entryPrice, positionSizeUSD);

			if(order.getAmount() == positionSize && order.getPrice() == entryPrice) {
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
	private void placeNewEntryOrders(final Map<BitfinexCurrencyPair, Double> entries) throws APIException, InterruptedException {
		
		final double positionSizeUSD = positionSizeInUSD();
		
		if(positionSizeUSD < USD_INVESTMENT_THRESHOLD) {
			logger.info("Dont place entry orders, USD per position is too small {} < {}", 
					positionSizeUSD, USD_INVESTMENT_THRESHOLD);
			
			return;
		}
		
		// Check current limits and position sizes
		for(final BitfinexCurrencyPair currency : entries.keySet()) {
			final ExchangeOrder order = getOpenOrderForSymbol(currency.toBitfinexString());
			final double entryPrice = entries.get(currency);
			
			final double positionSize = calculatePositionSize(entryPrice, positionSizeUSD);

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
	private void cancelRemovedEntryOrders(final Map<BitfinexCurrencyPair, Double> entries)
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
	 * Cleanup the old exit orders (remove duplicates, unknwon orders)
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
	private double positionSizeInUSD() throws APIException {
		final Wallet wallet = getWalletForCurrency("USD");
		
		// Wallet could be empty
		if(wallet == null) {
			return 0;
		}
		
		return (wallet.getBalance() * getInvestmentRate()) / positionsForCapitalAllocation;
	}

	/**
	 * Calculate the positon size
	 * @param upperValue
	 * @return
	 * @throws APIException 
	 */
	private double calculatePositionSize(final double entryPrice, final double positionSizeInUSD)  {
		final double positionSize = (positionSizeInUSD / entryPrice);
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
	 * Caluclate the amount of open positions
	 * @param entries
	 * @return
	 */
	protected abstract int calculateTotalPositionsForCapitalAllocation(
			final Map<BitfinexCurrencyPair, Double> entries, 
			final Map<BitfinexCurrencyPair, Double> exits);
	
	/**
	 * Get the investment rate
	 * @return
	 */
	protected abstract double getInvestmentRate();

}
