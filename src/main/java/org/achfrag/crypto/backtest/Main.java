package org.achfrag.crypto.backtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.StopGainRule;
import org.ta4j.core.trading.rules.StopLossRule;

public class Main implements Runnable {

	protected final static String FILENAME = "/Users/kristofnidzwetzki/Desktop/coinbaseGBP.csv";

	private TimeSeries timeSeries = new BaseTimeSeries("BTC");

	@Override
	public void run() {
		try {
			System.out.println("Load data from file");
			loadDataFromFile();

			System.out.println("Executing trading");
			executeTrading();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void executeTrading() {
		ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);

		SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
		SMAIndicator longSma = new SMAIndicator(closePrice, 30);

		Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);

		Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
				.or(new StopLossRule(closePrice, Decimal.valueOf("3")))
				.or(new StopGainRule(closePrice, Decimal.valueOf("2")));

		TimeSeriesManager seriesManager = new TimeSeriesManager(timeSeries);
		final BaseStrategy strategy = new BaseStrategy(buyingRule, sellingRule);
		TradingRecord tradingRecord = seriesManager.run(strategy);

		double totalPl = 0.0;
		List<Trade> trades = tradingRecord.getTrades();
		for (final Trade trade : trades) {
			final int inIndex = trade.getEntry().getIndex();
			final int outIndex = trade.getExit().getIndex();

			System.out.println("In: " + timeSeries.getTick(inIndex).getBeginTime());
			System.out.println("Out: " + timeSeries.getTick(outIndex).getBeginTime());

			final Decimal priceOut = trade.getExit().getPrice();
			final Decimal priceIn = trade.getEntry().getPrice();

			final double pl = priceOut.minus(priceIn).toDouble();
			System.out.println("P/L: " + pl + " In " + priceIn + " Out " + priceOut);
			totalPl = totalPl + pl;
		}

		System.out.println("Total P/L: " + totalPl);
		System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());

		final Chart chart = new Chart(strategy, timeSeries);
		chart.showChart();
	}

	protected void loadDataFromFile() throws FileNotFoundException, IOException {
		final BufferedReader br = new BufferedReader(new FileReader(new File(FILENAME)));

		final TickMerger tickMerger = new TickMerger(TickMerger.MERGE_SECONDS_1H, (t) -> timeSeries.addTick(t));

		String line = null;
		while ((line = br.readLine()) != null) {
			final String[] parts = line.split(",");
			final long timestamp = Long.parseLong(parts[0]);
			final double price = Double.parseDouble(parts[1]);
			final double volume = Double.parseDouble(parts[2]);

			// Drop unstable data
			if (timestamp < 1498867200) {
				continue;
			}
			
			tickMerger.addNewPrice(timestamp, price, volume);
		}

		tickMerger.close();
		br.close();
	}

	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
