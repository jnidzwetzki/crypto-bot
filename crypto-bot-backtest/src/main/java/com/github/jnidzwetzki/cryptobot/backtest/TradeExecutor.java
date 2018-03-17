package com.github.jnidzwetzki.cryptobot.backtest;

import java.util.ArrayList;
import java.util.List;

import org.ta4j.core.Decimal;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;

import com.github.jnidzwetzki.cryptobot.strategy.TradeStrategyFactory;

public class TradeExecutor {
	

	private final double initialPortfolioValue;
	private double portfolioValue = 0;
	private double totalFees = 0;
	private int winner = 0;
	private int looser = 0;
	private double maxWin = 0;
	private double maxLoose = 0;
	private int executedStopLoss = 0;
	
	private final List<Integer> looserInARowList = new ArrayList<>();

	private int looserInARow = 0;
	
	private double openContracts = 0;
	private int openBarIndex = -1;
	
	private final TimeSeries timeSeries;
	private TradeStrategyFactory tradeStrategyFactory;

	/**
	 * The trading comission (0.2% for market orders)
	 */
	private static final double COMISSION = 0.002;
	
	/**
	 * Hard stop loss at 5% portfolio value
	 */
	private final static double STOP_LOSS = -0.05;
	
	public TradeExecutor(final double portfolioValue, final TradeStrategyFactory tradeStrategyFactory) {
		this.portfolioValue = portfolioValue;
		this.initialPortfolioValue = portfolioValue;
		this.timeSeries = tradeStrategyFactory.getTimeSeries();
		this.tradeStrategyFactory = tradeStrategyFactory;
	}

	public void executeTrades() {
		for (int i = timeSeries.getBeginIndex(); i < timeSeries.getEndIndex(); i++) {
			
			final Strategy strategy = tradeStrategyFactory.getStrategy();
			
			if(strategy.shouldEnter(i) && openContracts == 0) {
				openTrade(i);
			} else if(strategy.shouldExit(i) && openContracts > 0) {
				closeTrade(i);
			}
		}
		
		if(openContracts > 0) {
			closeTrade(timeSeries.getEndIndex());
		}		
	}

	private void closeTrade(final int i) {

		final Decimal priceOut = timeSeries.getBar(i).getOpenPrice();
		final Decimal priceIn = timeSeries.getBar(openBarIndex).getOpenPrice();

		final double positionValue = priceOut.doubleValue() * openContracts;

		calculateFees(positionValue);
		
		double pl = priceOut.minus(priceIn).doubleValue() * openContracts;
		
		// Hard stop loss
		if(pl < portfolioValue * STOP_LOSS) {
			executedStopLoss++;
			pl = portfolioValue * STOP_LOSS;
		}
				
		portfolioValue = portfolioValue + pl;
		
		if(pl < 0) {
			maxLoose = Math.min(maxLoose, pl);
			looser++;
			looserInARow++;
		} else {
			maxWin = Math.max(maxWin, pl);
			winner++;
			looserInARowList.add(looserInARow);
			looserInARow = 0;
		}
		
		openContracts = 0;
		openBarIndex = -1;
	}

	private void openTrade(final int i) {
		openBarIndex = i;
		final double openPrice = timeSeries.getBar(i).getOpenPrice().doubleValue();
		openContracts = tradeStrategyFactory.getContracts(portfolioValue, i);
		
		final double positionSize = openPrice * openContracts;
		
		if(positionSize > portfolioValue) {
			throw new IllegalArgumentException("Unable to open a trade with position size: " 
					+ positionSize + " portfolio size: " + positionSize);
		}
				
		calculateFees(openPrice);
	}

	private void calculateFees(final double price) {
		final double fees = (openContracts * price) * COMISSION;
		totalFees = totalFees + fees;
		portfolioValue = portfolioValue - fees;
	}
	
	public double getFees() {
		return totalFees;
	}
	
	public int getLoserInARow() {
		return looserInARowList.stream().mapToInt(e -> e).max().orElse(-1);
	}
	
	public int getWinner() {
		return winner;
	}
	
	public double getMaxWin() {
		return maxWin;
	}
	
	public int getLooser() {
		return looser;
	}
	
	public double getMaxLoose() {
		return maxLoose;
	}
	
	public int getTotalTrades() {
		return winner + looser;
	}
	
	public double getTotalPL() {
		return portfolioValue - initialPortfolioValue;
	}
	
	public int getExecutedStopLoss() {
		return executedStopLoss;
	}
	
	public double getPortfolioValue() {
		return portfolioValue;
	}
}
