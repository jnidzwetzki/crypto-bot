package net.achfrag.crypto.backtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.achfrag.trading.crypto.bitfinex.entity.Timeframe;
import org.achfrag.trading.crypto.bitfinex.util.TickMerger;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import net.achfrag.crypto.strategy.DonchianChannelStrategy;
import net.achfrag.crypto.strategy.EMAStrategy03;
import net.achfrag.crypto.strategy.TradeStrategyFactory;

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
			
			printHeader();

		//	final TradeStrategyFactory factory = new EMAStrategy03(5, 12, 40);
			
		//	final Strategy strategy = ForexStrategy01.getStrategy(timeSeries);
			
			final TradeStrategyFactory factory = new DonchianChannelStrategy(48, 48);
			processTrade(factory);
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
					final TradeStrategyFactory factory = new EMAStrategy03(sma1Value, sma2Value, sma3Value);
					processTrade(factory);	
				}
			}
		}
	}

	private void processTrade(final TradeStrategyFactory strategyFactory) {
		TimeSeriesManager seriesManager = new TimeSeriesManager(timeSeries);

		//debugTrades(strategy);
		
		final Strategy strategy = strategyFactory.getStrategy(timeSeries);
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
			final double boughtContracts = USD_AMOUNT / priceIn.toDouble();
			totalPl = totalPl + (pl * boughtContracts);
		}
		
		looserInARowList.add(looserInARow);

		double looserInARowTotal = looserInARowList.stream().mapToDouble(e -> e).max().orElse(-1);
		
		System.out.format("%s\t%f\t%d\t%d\t%d\t%f\t%f\t%f\n", strategyFactory.getName(), totalPl, 
				tradingRecord.getTradeCount(), winner, looser, maxWin, maxLoose, looserInARowTotal);

		/*
		final Chart chart = new Chart(strategy, timeSeries);
		chart.showChart();
*/
	}

	private void printHeader() {
		System.out.println("Strategy\tP/L\tTrades\tWinner\tLooser\tMax win\tMax loose\tLooser row\n");
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
			
			// Drop unstable data 
			// 1483228800 - 01.01.2017
			// 1451606400 - 01.01.2016
			
			if (timestamp < 1483228800) {
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
