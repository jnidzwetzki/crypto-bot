package org.achfrag.crypto.backtest;

import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Indicator;
import org.ta4j.core.Strategy;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

/**
 * 
 * Inspired by: https://github.com/ta4j/ta4j/blob/master/ta4j-examples/src/main/java/ta4jexamples/analysis/BuyAndSellSignalsToChart.java
 *
 */
public class Chart {

	private BaseStrategy strategy;
	private TimeSeries timeSeries;

	public Chart(final BaseStrategy strategy, final TimeSeries timeSeries) {
		this.strategy = strategy;
		this.timeSeries = timeSeries;
	}

	/**
	 * Builds a JFreeChart time series from a Ta4j time series and an indicator.
	 * 
	 * @param tickSeries
	 *            the ta4j time series
	 * @param indicator
	 *            the indicator
	 * @param name
	 *            the name of the chart time series
	 * @return the JFreeChart time series
	 */
	private static org.jfree.data.time.TimeSeries buildChartTimeSeries(TimeSeries tickSeries,
			Indicator<Decimal> indicator, String name) {
		org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
		for (int i = 0; i < tickSeries.getTickCount(); i++) {
			Tick tick = tickSeries.getTick(i);
			chartTimeSeries.add(new Minute(Date.from(tick.getEndTime().toInstant())), indicator.getValue(i).toDouble());
		}
		return chartTimeSeries;
	}

	/**
	 * Runs a strategy over a time series and adds the value markers corresponding
	 * to buy/sell signals to the plot.
	 * 
	 * @param series
	 *            a time series
	 * @param strategy
	 *            a trading strategy
	 * @param plot
	 *            the plot
	 */
	private static void addBuySellSignals(TimeSeries series, Strategy strategy, XYPlot plot) {
		// Running the strategy
		TimeSeriesManager seriesManager = new TimeSeriesManager(series);
		List<Trade> trades = seriesManager.run(strategy).getTrades();
		// Adding markers to plot
		for (Trade trade : trades) {
			// Buy signal
			double buySignalTickTime = new Minute(
					Date.from(series.getTick(trade.getEntry().getIndex()).getEndTime().toInstant()))
							.getFirstMillisecond();
			Marker buyMarker = new ValueMarker(buySignalTickTime);
			buyMarker.setPaint(Color.GREEN);
			buyMarker.setLabel("B");
			plot.addDomainMarker(buyMarker);
			// Sell signal
			double sellSignalTickTime = new Minute(
					Date.from(series.getTick(trade.getExit().getIndex()).getEndTime().toInstant()))
							.getFirstMillisecond();
			Marker sellMarker = new ValueMarker(sellSignalTickTime);
			sellMarker.setPaint(Color.RED);
			sellMarker.setLabel("S");
			plot.addDomainMarker(sellMarker);
		}
	}

	/**
	 * Displays a chart in a frame.
	 * 
	 * @param chart
	 *            the chart to be displayed
	 */
	private static void displayChart(JFreeChart chart) {
		// Chart panel
		ChartPanel panel = new ChartPanel(chart);
		panel.setFillZoomRectangle(true);
		panel.setMouseWheelEnabled(true);
		panel.setPreferredSize(new Dimension(1024, 400));
		// Application frame
		ApplicationFrame frame = new ApplicationFrame("Ta4j example - Buy and sell signals to chart");
		frame.setContentPane(panel);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setVisible(true);
	}
	
	protected void showChart() {
		/**
		 * Building chart datasets
		 */
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(
				buildChartTimeSeries(timeSeries, new ClosePriceIndicator(timeSeries), "Bitstamp Bitcoin (BTC)"));

		/**
		 * Creating the chart
		 */
		JFreeChart chart = ChartFactory.createTimeSeriesChart("BTC", // title
				"Date", // x-axis label
				"Price", // y-axis label
				dataset, // data
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
		);
		XYPlot plot = (XYPlot) chart.getPlot();
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("MM-dd-yyyy HH:mm"));

		/**
		 * Running the strategy and adding the buy and sell signals to plot
		 */
		addBuySellSignals(timeSeries, strategy, plot);

		/**
		 * Displaying the chart
		 */
		displayChart(chart);
	}

}
