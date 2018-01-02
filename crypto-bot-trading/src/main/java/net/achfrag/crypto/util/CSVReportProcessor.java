package net.achfrag.crypto.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.bboxdb.commons.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVReportProcessor implements Runnable {

	/*
	 * 
	 */
	private final String filename;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(CSVReportProcessor.class);

	
	public CSVReportProcessor(final String filename) {
		this.filename = filename;
	}

	@Override
	public void run() {
		try(final BufferedReader br = new BufferedReader(new FileReader(new File(filename)))) {
			
			double pl = 0;
			double fees = 0;
			String line;
			while((line = br.readLine()) != null) {
				if(line.startsWith("#")) {
					continue;
				}
				
				final String[] elements = line.split(",");
				
				final String amontString = elements[2];
				final String priceString = elements[3];
				final String feeString = elements[4];
				final String feeCurrencyString = elements[5];
				
				final double amount = MathUtil.tryParseDouble(amontString, () -> "Unable to parse" + amontString);
				final double price = MathUtil.tryParseDouble(priceString, () -> "Unable to parse" + priceString);
				
				pl = pl + (amount * -1.0 * price);

				if("USD".equals(feeCurrencyString)) {
					final double fee = MathUtil.tryParseDouble(feeString, () -> "Unable to parse: " + feeString);
					fees += Math.abs(fee);
				} else {
					fees += 0.17;
				}
			}
			
			System.out.format("PL %f\n", pl);
			System.out.format("Total fees %f\n", fees);
			
		} catch (Exception e) {
			logger.error("Got exception", e);
		} 
	}
	
	public static void main(String[] args) {
		
		if(args.length != 1) {
			System.err.println("Please specify the filename");
			System.exit(-1);
		}
		
		final CSVReportProcessor csvReportProcessor = new CSVReportProcessor(args[0]);
		csvReportProcessor.run();
	}


}
