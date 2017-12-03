package org.achfrag.crypto.backtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.entity.Timeframe;
import org.achfrag.crypto.bitfinex.util.TickMerger;
import org.achfrag.crypto.strategy.EMAStrategy03;
import org.achfrag.crypto.strategy.ForexStrategy01;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class Main implements Runnable {

	protected final static String FILENAME = "/Users/kristofnidzwetzki/Desktop/coinbaseUSD.csv";

	private TimeSeries timeSeries = new BaseTimeSeries("BTC");
	
	private float USD_AMOUNT = 500;

	@Override
	public void run() {
		try {
			System.out.println("Load data from file");
			loadDataFromFile();

			System.out.println("Executing trading on ticks: " + timeSeries.getEndIndex());
			
		//	final Strategy strategy = EMAStrategy03.getStrategy(timeSeries, 5, 12, 40);
			final Strategy strategy = ForexStrategy01.getStrategy(timeSeries);
			processTrade("Strategy 5-12-40", strategy);	

			//findEma();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void findEma() throws InterruptedException {
		
		final List<Integer> sma1 = Arrays.asList(5, 6, 7, 8, 9, 10, 11, 12, 15);
		final List<Integer> sma2 = Arrays.asList(10, 12, 14, 16, 18, 20, 30);
		final List<Integer> sma3 = Arrays.asList(40, 50, 60);
		
		for(final int sma1Value : sma1) {
			for(final int sma2Value : sma2) {
				for(final int sma3Value : sma3) {
					final Strategy strategy = EMAStrategy03.getStrategy(timeSeries, sma1Value, sma2Value, sma3Value);
					
					processTrade("Strategy " + sma1Value + "-" + sma2Value + "-" + sma3Value,  strategy);	
				}
			}
		}
	}

	private void processTrade(final String strategyName, final Strategy strategy) {
		TimeSeriesManager seriesManager = new TimeSeriesManager(timeSeries);

		//debugTrades(strategy);
		
		
		TradingRecord tradingRecord = seriesManager.run(strategy);

		double totalPl = 0.0;
		int winner = 0;
		int looser = 0;
		double maxWin = 0;
		double maxLoose = 0;
		double looserInARow = 0;
		final List<Double> looserInARowList = new ArrayList<>();
		
		List<Trade> trades = tradingRecord.getTrades();
		for (final Trade trade : trades) {
			final int inIndex = trade.getEntry().getIndex();
			final int outIndex = trade.getExit().getIndex();

		//	System.out.println("In: " + timeSeries.getTick(inIndex).getBeginTime());
		//	System.out.println("Out: " + timeSeries.getTick(outIndex).getBeginTime());

			final Decimal priceOut = trade.getExit().getPrice();
			final Decimal priceIn = trade.getEntry().getPrice();

			final double pl = priceOut.minus(priceIn).toDouble();
			
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
			
		//	System.out.println("P/L: " + pl + " In " + priceIn + " Out " + priceOut);
			totalPl = totalPl + (pl * (priceIn.toDouble() / USD_AMOUNT));
		}
		
		looserInARowList.add(looserInARow);

		System.out.println("Strategy: " + strategyName);
		System.out.println("Total P/L: " + totalPl);
		System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());
		System.out.format("Winner %d, looser %d\n", winner, looser);
		System.out.format("Max win %f, max loose %f\n", maxWin, maxLoose);
		System.out.format("Looser in a row %f\n", looserInARowList.stream().mapToDouble(e -> e).max().orElse(-1));
		
		/*
		final Chart chart = new Chart(strategy, timeSeries);
		chart.showChart();
*/
	}

	protected void debugTrades(final Strategy strategy) {
		for(int i = 0; i < timeSeries.getEndIndex(); i++) {
			boolean enter = strategy.shouldEnter(i);
			boolean exit = strategy.shouldExit(i);
			
			ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

			EMAIndicator sma1Indicator = new EMAIndicator(closePrice, 5);
			EMAIndicator sma2Indicator = new EMAIndicator(closePrice, 10);
			EMAIndicator sma3Indicator = new EMAIndicator(closePrice, 40);

			if(enter) {
			
				System.out.format("Tick before: Price %s, enter=%b, exit=%b, sma1=%s, sma2=%s, sma3=%s\n", 
						timeSeries.getTick(i-1).getOpenPrice().toString(), enter, exit,
						sma1Indicator.getValue(i-1).toString(), 
						sma2Indicator.getValue(i-1).toString(), 
						sma3Indicator.getValue(i-1).toString());
				
			System.out.format("Tick enter: Price %s, enter=%b, exit=%b, sma1=%s, sma2=%s, sma3=%s\n", 
					timeSeries.getTick(i).getOpenPrice().toString(), enter, exit,
					sma1Indicator.getValue(i).toString(), 
					sma2Indicator.getValue(i).toString(), 
					sma3Indicator.getValue(i).toString());
			}
		}
	}

	protected void loadDataFromFile() throws FileNotFoundException, IOException {
		final BufferedReader br = new BufferedReader(new FileReader(new File(FILENAME)));

		final TickMerger tickMerger = new TickMerger("BTC", Timeframe.MINUTES_15, (s, t) -> timeSeries.addTick(t));

		String line = null;
		while ((line = br.readLine()) != null) {
			final String[] parts = line.split(",");
			final long timestamp = Long.parseLong(parts[0]);
			final double price = Double.parseDouble(parts[1]);
			final double volume = Double.parseDouble(parts[2]);

			// Drop unstable data (01.01.2017)
			if (timestamp < 1483228800) {
				continue;
			}
			
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
