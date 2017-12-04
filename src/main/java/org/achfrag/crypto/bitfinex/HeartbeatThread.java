package org.achfrag.crypto.bitfinex;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.commands.PingCommand;
import org.achfrag.crypto.bitfinex.util.EventsInTimeslotManager;
import org.achfrag.crypto.util.ExceptionSafeThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Tick;

class HeartbeatThread extends ExceptionSafeThread {

	/**
	 * The ticker timeout
	 */
	private static final long TICKER_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
	
	/**
	 * The API timeout
	 */
	private static final long CPNNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
	
	/**
	 * The API timeout
	 */
	private static final long HEARTBEAT = TimeUnit.SECONDS.toMillis(5);
	
	/**
	 * Max reconnects in 10 minutes
	 */
	private static final int MAX_RECONNECTS_IN_TIME = 10;

	/**
	 * The API broker
	 */
	private final BitfinexApiBroker bitfinexApiBroker;

	/**
	 * The reconnect timeslot manager
	 */
	private final EventsInTimeslotManager eventsInTimeslotManager;
	
	/**
	 * The Logger
	 */
	final static Logger logger = LoggerFactory.getLogger(HeartbeatThread.class);

	
	/**
	 * @param bitfinexApiBroker
	 */
	HeartbeatThread(BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = bitfinexApiBroker;
		this.eventsInTimeslotManager = new EventsInTimeslotManager(MAX_RECONNECTS_IN_TIME, 10, TimeUnit.MINUTES);
	}

	@Override
	public void runThread() throws InterruptedException {
		
		while(! Thread.interrupted()) {
			
			Thread.sleep(TimeUnit.SECONDS.toMillis(3));
			
			if(bitfinexApiBroker.getWebsocketEndpoint() != null) {
				
				sendHeartbeatIfNeeded();

				final boolean tickerUpToDate = checkTickerFreshness();
				
				if(! tickerUpToDate) {
					logger.error("Ticker are outdated, reconnecting");
					executeReconnect();
					continue;
				}
				
				final boolean reconnectNeeded = checkConnectionTimeout();
				
				if(reconnectNeeded) {
					logger.error("Global connection heartbeat time out, reconnecting");
					executeReconnect();
					continue;
				}
			}
		}
	}

	/**
	 * Are all ticker uptodate
	 * @return
	 */
	private boolean checkTickerFreshness() {
		final long currentTime = System.currentTimeMillis();

		final Set<String> activeSymbols = bitfinexApiBroker.getActiveSymbols();
		for(final String symbol : activeSymbols) {
			final Tick lastTick = bitfinexApiBroker.getLastTick(symbol);
			
			if(lastTick == null) {
				continue;
			}
			
			final long lastUpdate = lastTick.getEndTime().toInstant().getEpochSecond() * 1000;
			
			if(lastUpdate + TICKER_TIMEOUT < currentTime) {
				logger.error("Last update for symbol {} is {} current time is {}, the data is outdated",
						symbol, lastUpdate, currentTime);
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Send a heartbeat package on the connection
	 */
	private void sendHeartbeatIfNeeded() {
		final long nextHeartbeat = bitfinexApiBroker.getLastHeatbeat().get() + HEARTBEAT;

		if(nextHeartbeat < System.currentTimeMillis()) {
			bitfinexApiBroker.sendCommand(new PingCommand());
		}
	}

	/**
	 * Check for connection timeout
	 * @return 
	 */
	private boolean checkConnectionTimeout() {
		final long heartbeatTimeout = bitfinexApiBroker.getLastHeatbeat().get() + CPNNECTION_TIMEOUT;
		
		if(heartbeatTimeout < System.currentTimeMillis()) {
			logger.error("Heartbeat timeout reconnecting");
			return true;
		}
		
		return false;
	}

	/**
	 * Execute the reconnect
	 * @throws InterruptedException 
	 */
	private void executeReconnect() throws InterruptedException {
		// Close connection
		bitfinexApiBroker.getWebsocketEndpoint().close();
		
		// Store the reconnect time to prevent to much
		// reconnects in a short timeframe. Otherwise the
		// rate limit will apply and the reconnects are not successfully
		logger.info("Wait for next reconnect timeslot");
		eventsInTimeslotManager.recordNewEvent();
		eventsInTimeslotManager.waitForNewTimeslot();
		logger.info("Wait for next reconnect timeslot DONE");

		// Disable auto reconnect to ignore session closed 
		// events, and preventing duplicate reconnects
		bitfinexApiBroker.setAutoReconnectEnabled(false);
		bitfinexApiBroker.reconnect();
		bitfinexApiBroker.setAutoReconnectEnabled(true);
		
		// Wait some time to let the ticker timestamps update
		Thread.sleep(TimeUnit.SECONDS.toMillis(30));
	}
}