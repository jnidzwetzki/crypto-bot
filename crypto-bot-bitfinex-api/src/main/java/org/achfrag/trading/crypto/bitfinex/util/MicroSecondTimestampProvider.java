package org.achfrag.trading.crypto.bitfinex.util;

public class MicroSecondTimestampProvider {

	/**
	 * The last currentTimeMillis
	 */
	protected static long lastTimestampMillis = -1;
	
	/**
	 * The counter for this millisecond
	 */
	protected static int counter = 0;
	
	/**
	 * Get a faked micro seconds timestamp. Millisecond collisions are avoided
	 * by adding a faked micro seconds counter to the timestamp
	 * @return 
	 */
	public synchronized static long getNewTimestamp() {
		final long currentMillis = System.currentTimeMillis();
		
		if(currentMillis != lastTimestampMillis) {
			counter = 0;
			lastTimestampMillis = currentMillis;
		}
		
		final long resultValue = currentMillis * 1000 + counter;
		
		counter++;
		
		return resultValue;
	}
	
	/**
	 * Main * Main * Main * Main * Main
	 * @param args
	 */
	public static void main(String[] args) {
		for(int i = 0; i < 10000; i++) {
			System.out.println(MicroSecondTimestampProvider.getNewTimestamp());
		}
	}

}
