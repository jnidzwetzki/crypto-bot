package org.achfrag.crypto.backtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTick;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Tick;
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

	protected final long MERGE_SECONDS = TimeUnit.MINUTES.toSeconds(60);

	private ZoneId TIMEZONE = ZoneId.of("Europe/Berlin");

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

		long timeframeBegin = -1;
		final List<Float> prices = new ArrayList<>();

		String line = null;
		while ((line = br.readLine()) != null) {
			final String[] parts = line.split(",");
			final long timestamp = Long.parseLong(parts[0]);
			final float price = Float.parseFloat(parts[1]);
			final float volume = Float.parseFloat(parts[2]);

			// Drop unstable data
			if (timestamp < 1498867200) {
				continue;
			}

			if (timeframeBegin == -1) {
				timeframeBegin = timestamp;
			} else if (timestamp > timeframeBegin + MERGE_SECONDS) {

				if (prices.isEmpty()) {
					System.err.println("Error: prices for series are empty: " + timeframeBegin);
				}

				final double open = prices.get(0);
				final double close = prices.get(prices.size() - 1);
				final double high = prices.stream().mapToDouble(e -> e).max().orElse(-1);
				final double low = prices.stream().mapToDouble(e -> e).min().orElse(-1);

				final Timestamp timestampValue = new Timestamp(timeframeBegin * 1000);
				final LocalDateTime localtime = timestampValue.toLocalDateTime();
				final ZonedDateTime withTimezone = localtime.atZone(TIMEZONE);

				final Tick tick = new BaseTick(withTimezone, open, high, low, close, volume);

				try {
					timeSeries.addTick(tick);
				} catch (IllegalArgumentException e) {
					// ignore time shift
				}

				while (timestamp > timeframeBegin) {
					timeframeBegin = timeframeBegin + MERGE_SECONDS;
				}

				prices.clear();
			}

			prices.add(price);
		}

		br.close();
	}


	public static void main(final String[] args) {
		final Main main = new Main();
		main.run();
	}
}
