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
package com.github.jnidzwetzki.cryptobot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.Decimal;
import org.ta4j.core.Order.OrderType;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;

import com.github.jnidzwetzki.bitfinex.v2.BitfinexApiBroker;
import com.github.jnidzwetzki.bitfinex.v2.entity.APIException;
import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexCurrencyPair;
import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexTick;
import com.github.jnidzwetzki.bitfinex.v2.entity.ExchangeOrder;
import com.github.jnidzwetzki.bitfinex.v2.entity.Timeframe;
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexCandlestickSymbol;
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexTickerSymbol;
import com.github.jnidzwetzki.bitfinex.v2.manager.QuoteManager;
import com.github.jnidzwetzki.cryptobot.entity.Trade;
import com.github.jnidzwetzki.cryptobot.entity.TradeDirection;
import com.github.jnidzwetzki.cryptobot.entity.TradeState;
import com.github.jnidzwetzki.cryptobot.strategy.EMAStrategy03;
import com.github.jnidzwetzki.cryptobot.strategy.TradeStrategyFactory;
import com.github.jnidzwetzki.cryptobot.util.BarMerger;
import com.github.jnidzwetzki.cryptobot.util.BitfinexClientFactory;

public class EMABot implements Runnable {

	protected final Map<BitfinexCurrencyPair, BarMerger> tickMerger;
	
	protected final Map<BitfinexCurrencyPair, TimeSeries> timeSeries;

	protected final Map<BitfinexCurrencyPair, Strategy> strategies;

	protected final List<BitfinexCurrencyPair> tradedCurrencies; 
		
	protected final PortfolioOrderManager orderManager;
	
	private final Map<BitfinexCurrencyPair, List<Trade>> trades;
	
	public static boolean UPDATE_SCREEN = true;
		
	protected static final Timeframe TIMEFRAME = Timeframe.MINUTES_15;

	/**
	 * The API broker
	 */
	private final BitfinexApiBroker bitfinexApiBroker;

	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(EMABot.class);

	
	public EMABot() {
		
		final List<BitfinexApiBroker> connections = BitfinexClientFactory.buildBifinexClient();

		if(connections.isEmpty()) {
			throw new IllegalArgumentException("Unable to get API clients");
		}
		
		this.bitfinexApiBroker = connections.get(0);
		this.tickMerger = new HashMap<>();
		this.timeSeries = new HashMap<>();
		this.strategies = new HashMap<>();
		this.orderManager = new PortfolioOrderManager(bitfinexApiBroker);
		this.trades = new HashMap<>();
		this.tradedCurrencies = Arrays.asList(BitfinexCurrencyPair.BTC_USD);
	}

	@Override
	public void run() {
		try {			
			trades.clear();
			
			final List<Trade> openTrades = orderManager.getAllOpenTrades();
			openTrades.forEach(t -> addTradeToOpenTradeList(t));
			
			bitfinexApiBroker.connect();
			
			final Map<BitfinexCandlestickSymbol, TimeSeries> historicalCandles = HistoricalCandlesHelper
					.requestHistoricalCandles(bitfinexApiBroker, TIMEFRAME, tradedCurrencies);
			
			historicalCandles.forEach((k, v) -> timeSeries.put(k.getSymbol(), v));
			
			createStrategies();
			registerTicker(bitfinexApiBroker);
						
			while (true) {
				Thread.sleep(TimeUnit.MINUTES.toMillis(5));
			}
		} catch (Exception e) {
			logger.error("Got exception", e);
		}
	}

	/**
	 * Init the trading stretegies
	 */
	private void createStrategies() {
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			TradeStrategyFactory strategyFactory = new EMAStrategy03(5, 12, 40, timeSeries.get(currency));
			final Strategy strategy = strategyFactory.getStrategy();
			strategies.put(currency, strategy);
		}
	}

	protected void registerTicker(final BitfinexApiBroker bitfinexApiBroker) throws InterruptedException, APIException {
		
		logger.info("Register ticker");
		
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {

			tickMerger.put(currency, new BarMerger(currency, TIMEFRAME, (s, t) -> barDoneCallback(s, t)));
		
			final BitfinexTickerSymbol symbol = new BitfinexTickerSymbol(currency);
			bitfinexApiBroker.getQuoteManager().subscribeTicker(symbol);

			logger.info("Wait for ticker");
			
			while (! bitfinexApiBroker.isTickerActive(symbol)) {
				Thread.sleep(100);
			}

			bitfinexApiBroker
				.getQuoteManager()
				.registerTickCallback(symbol, (s, c) -> handleTickCallback(s, c));
		}
	}

	private void barDoneCallback(final BitfinexCurrencyPair symbol, final Bar bar) {		
		try {
			timeSeries.get(symbol).addBar(bar);
		} catch(Throwable e) {
			logger.error("Unable to add {} to symbol {}", bar, symbol);
		}
		
		final int endIndex = timeSeries.get(symbol).getEndIndex();

		try {
			if (strategies.get(symbol).shouldEnter(endIndex)) {
				openOrder(symbol, endIndex);
			} else if (strategies.get(symbol).shouldExit(endIndex)) {
				closeOrder(symbol, endIndex);
			}
		} catch (APIException e) {
			logger.error("Got an exception while executing order", e);
		}
		
		updateScreen();
	}

	/**
	 * Execute a new close order
	 * @param symbol
	 * @param endIndex
	 * @throws APIException
	 */
	private void closeOrder(final BitfinexCurrencyPair symbol, final int endIndex) {
		
		final Decimal lastClosePrice = timeSeries.get(symbol).getLastBar().getClosePrice();
		
		final Trade openTrade = getOpenTrade(symbol);
		
		if(openTrade == null) {
			logger.error("Unable to close a trade, there is no trade open");
			return;
		}
		
		openTrade.setExpectedPriceClose(lastClosePrice.doubleValue());

		orderManager.closeTrade(openTrade);
	}

	/**
	 * Execute a new open position order
	 * @param symbol
	 * @param endIndex
	 * @throws APIException
	 */
	private void openOrder(final BitfinexCurrencyPair symbol, final int endIndex) throws APIException {
		final Decimal lastClosePrice = timeSeries.get(symbol).getLastBar().getClosePrice();
		
		final Trade openTrade = getOpenTrade(symbol);
		
		if(openTrade != null) {
			logger.debug("Unable to open new trade, there is already one active {}", openTrade);
			return;
		}
		
		final double amount = PositionSizeManager.getPositionSize(symbol, OrderType.BUY, 
				bitfinexApiBroker.getWalletManager().getWallets());
		
		final Trade trade = new Trade("EMA Strategy", TradeDirection.LONG, symbol, amount);
		trade.setExpectedPriceOpen(lastClosePrice.doubleValue());

		addTradeToOpenTradeList(trade);
		orderManager.openTrade(trade);
	}


	/**
	 * Open the trade to the open trades list
	 * @param currency
	 * @param trade
	 */
	private void addTradeToOpenTradeList(final Trade trade) {
		
		final BitfinexCurrencyPair currency = trade.getSymbol();
		
		if(! trades.containsKey(currency)) {
			trades.put(currency, new ArrayList<>());
		}
		
		trades.get(currency).add(trade);
	}
	
	private void handleTickCallback(final BitfinexTickerSymbol symbol, final BitfinexTick bar) {		
		tickMerger.get(symbol.getBitfinexCurrencyPair()).addNewPrice(
				bar.getTimestamp(), 
				bar.getOpen(),
				bar.getVolume());
		
		updateScreen();
	}

	public static void main(final String[] args) {
		final EMABot main = new EMABot();
		main.run();
	}
	
	public synchronized void updateScreen() {
		
		if(! UPDATE_SCREEN) {
			return;
		}
		
		final QuoteManager tickerManager = bitfinexApiBroker.getQuoteManager();

		CliTools.clearScreen();
		System.out.println("");
		System.out.println("==========");
		System.out.println("Last ticks");
		System.out.println("==========");
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			final String symbol = currency.toBitfinexString();
			final BitfinexTickerSymbol tickerSymbol = new BitfinexTickerSymbol(currency);
			System.out.println(symbol + " " + tickerManager.getLastTick(tickerSymbol));
		}
		
		System.out.println("");
		System.out.println("==========");
		System.out.println("Last bars");
		System.out.println("==========");
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			System.out.println(currency + " " + timeSeries.get(currency).getLastBar());
		}
		
		System.out.println("");
		System.out.println("==========");
		System.out.println("P/L");
		System.out.println("==========");
		for(final BitfinexCurrencyPair currency : tradedCurrencies) {
			final String symbol = currency.toBitfinexString();
			final BitfinexTickerSymbol tickerSymbol = new BitfinexTickerSymbol(currency);

			final Trade trade = getOpenTrade(currency);
			if(trade != null) {
				final double priceIn = trade.getExpectedPriceOpen();
				final double currentPrice = tickerManager.getLastTick(tickerSymbol).getClose();
				System.out.println(symbol + ": price in " + priceIn + " / " + (currentPrice - priceIn));
			}	
		}

        System.out.println("");
        System.out.println("==========");
        System.out.println("Trades");
        System.out.println("==========");
        for(final BitfinexCurrencyPair currency : tradedCurrencies) {
                final String symbol = currency.toBitfinexString();
                final List<Trade> lastTrades = trades.get(currency);
                
                if(lastTrades == null) {
                		continue;
                }
                
                lastTrades.sort((t1, t2) -> Long.compare(t2.getTid(), t1.getTid()));
                
                final List<Trade> lastTwoTrades = lastTrades.subList(Math.max(lastTrades.size() - 2, 0), lastTrades.size());

                for(final Trade trade : lastTwoTrades) {
                		System.out.println(symbol + " " + trade);
                }
        }  
		
		try {
			System.out.println("");
			System.out.println("==========");
			System.out.println("Orders");
			System.out.println("==========");
			
			final List<ExchangeOrder> orders = new ArrayList<>(bitfinexApiBroker.getOrderManager().getOrders());
			orders.sort((o1, o2) -> Long.compare(o2.getCid(), o1.getCid()));
			final List<ExchangeOrder> lastOrders = orders.subList(Math.max(orders.size() - 3, 0), orders.size());

			for(final ExchangeOrder order : lastOrders) {
				System.out.println(order);
			}
		} catch (APIException e) {
			logger.error("Got eception while reading wallets", e);
		}
	}
	
	/**
	 * Get the open trade for symbol or null
	 * @param symbol
	 * @return
	 */
	public Trade getOpenTrade(final BitfinexCurrencyPair symbol) {
		final List<Trade> tradeList = trades.get(symbol);
		
		if(tradeList == null) {
			return null;
		}

		final List<Trade> openTrades = tradeList.stream()
				.filter(t -> t.getTradeState() == TradeState.OPEN)
				.collect(Collectors.toList());
		
		if(openTrades.size() > 1) {
			throw new IllegalArgumentException("More then one open trade for " + symbol + " / " + openTrades);
		}
		
		if(openTrades.isEmpty()) {
			return null;
		}
		
		return openTrades.get(0);
	}
	
}
