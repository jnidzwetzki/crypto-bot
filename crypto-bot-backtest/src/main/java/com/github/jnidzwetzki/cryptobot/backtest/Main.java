package com.github.jnidzwetzki.cryptobot.backtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;

import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexCurrencyPair;
import com.github.jnidzwetzki.bitfinex.v2.entity.Timeframe;
import com.github.jnidzwetzki.cryptobot.strategy.DonchianChannelStrategy;
import com.github.jnidzwetzki.cryptobot.strategy.EMAStrategy03;
import com.github.jnidzwetzki.cryptobot.strategy.TradeStrategyFactory;
import com.github.jnidzwetzki.cryptobot.util.BarMerger;

public class Main implements Runnable {

	protected final static String FILENAME = "/Users/kristofnidzwetzki/Desktop/coinbaseUSD.csv";

	private TimeSeries timeSeries = new BaseTimeSeries("BTC");
	
	private float USD_AMOUNT = 1000;

	@Override
	public void run() {
		try {
			System.out.println("Load data from file");
			loadDataFromFile();

			System.out.println("Executing trading on ticks: " + timeSeries.getEndIndex());
			
			printHeader();

		//	final TradeStrategyFactory factory = new EMAStrategy03(5, 12, 40);
			
		//	final Strategy strategy = ForexStrategy01.getStrategy(timeSeries);
			
			final TradeStrategyFactory factory1 = new DonchianChannelStrategy(24 * 4, 12 * 4, timeSeries);
			processTrade(factory1);
			
			final TradeStrategyFactory factory2 = new DonchianChannelStrategy(24* 4, 24* 4, timeSeries);
			processTrade(factory2);
			
			final TradeStrategyFactory factory3 = new DonchianChannelStrategy(12* 4, 24* 4, timeSeries);
			processTrade(factory3);

			
			//findEma();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void findEma() throws InterruptedException {
		
		final List<Integer> sma1 = Arrays.asList(5, 6, 7, 8, 9, 10, 11, 12, 15);
		final List<Integer> sma2 = Arrays.asList(10, 12, 14, 16, 18, 20, 30, 40);
		final List<Integer> sma3 = Arrays.asList(40, 50, 60, 70, 80, 90);
		
		for(final int sma1Value : sma1) {
			for(final int sma2Value : sma2) {
				for(final int sma3Value : sma3) {
					final TradeStrategyFactory factory = new EMAStrategy03(sma1Value, sma2Value, sma3Value, timeSeries);
					processTrade(factory);	
				}
			}
		}
	}

	private void processTrade(final TradeStrategyFactory strategyFactory) {

		//debugTrades(strategy);
		
		final TradeExecutor tradeExecutor = new TradeExecutor(USD_AMOUNT, strategyFactory);
		tradeExecutor.executeTrades();
	
		System.out.format("%s\t%f\t%f\t%f\t%d\t%d\t%d\t%f\t%f\t%d\t%d\n", strategyFactory.getName(), 
				tradeExecutor.getPortfolioValue(), tradeExecutor.getTotalPL(), 
				tradeExecutor.getFees(), tradeExecutor.getTotalTrades(),
				tradeExecutor.getWinner(), tradeExecutor.getLooser(), tradeExecutor.getMaxWin(), 
				tradeExecutor.getMaxLoose(), tradeExecutor.getLoserInARow(), 
				tradeExecutor.getExecutedStopLoss());

		/*
		final Chart chart = new Chart(strategy, timeSeries);
		chart.showChart();
*/
	}

	private void printHeader() {
		System.out.println("Strategy\tPortfolio value\tP/L\tFees\tTrades\tWinner\tLooser\tMax win\tMax loose\tLooser row\tStop loss hard\n");
	}

	protected void loadDataFromFile() throws FileNotFoundException, IOException {
		final BufferedReader br = new BufferedReader(new FileReader(new File(FILENAME)));

		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, 
				Timeframe.MINUTES_15, (s, t) -> timeSeries.addBar(t));

		String line = null;
		while ((line = br.readLine()) != null) {
			final String[] parts = line.split(",");
			final long timestamp = Long.parseLong(parts[0]);
			final double price = Double.parseDouble(parts[1]);
			final double volume = Double.parseDouble(parts[2]);
			
			// Drop unstable data 
			// 1483228800 - 01.01.2017
			// 1451606400 - 01.01.2016
			
			if (timestamp < 1451606400) {
				continue;
			}
			
			//if(timestamp > 1483228800) {
			//	continue;
			//}

			tickMerger.addNewPrice(TimeUnit.SECONDS.toMillis(timestamp), price, volume);
		}
		
		tickMerger.close();
		br.close();
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
