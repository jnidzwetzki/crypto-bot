package org.achfrag.crypto.bitfinex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.commands.PingCommand;
import org.achfrag.crypto.util.ExceptionSafeThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Tick;

class HeartbeatThread extends ExceptionSafeThread {

	/**
	 * The API timeout
	 */
	private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
	
	/**
	 * The API timeout
	 */
	private static final long HEARTBEAT = TimeUnit.SECONDS.toMillis(5);
	
	/**
	 * Max reconnects
	 */
	private static final int MAX_RECONNECTS_IN_TIME = 5;
	
	/**
	 * Timeframe for MAX_RECONNECTS_IN_TIME
	 */
	private static final long MAX_RECONNECT_SECONDS = TimeUnit.MINUTES.toMillis(10);
	
	/**
	 * The API broker
	 */
	private final BitfinexApiBroker bitfinexApiBroker;

	/**
	 * The reconnect times
	 */
	private final List<Long> reconnectTimes;
	
	/**
	 * The Logger
	 */
	final static Logger logger = LoggerFactory.getLogger(HeartbeatThread.class);

	
	/**
	 * @param bitfinexApiBroker
	 */
	HeartbeatThread(BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = bitfinexApiBroker;
		this.reconnectTimes = new ArrayList<>();
	}

	@Override
	public void runThread() {
		
		while(! Thread.interrupted()) {
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				return;
			}
			
			if(bitfinexApiBroker.websocketEndpoint != null) {
				
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
			final long lastUpdate = lastTick.getEndTime().toInstant().getEpochSecond() * 1000;
			
			if(lastUpdate + HEARTBEAT < currentTime) {
				logger.debug("Last update for symbol {} is {} current time is {}, the data is outdated",
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
		final long heartbeatTimeout = bitfinexApiBroker.getLastHeatbeat().get() + TIMEOUT;
		
		if(heartbeatTimeout < System.currentTimeMillis()) {
			logger.error("Heartbeat timeout reconnecting");
			return true;
		}
		
		return false;
	}

	/**
	 * Execute the reconnect
	 */
	private void executeReconnect() {
		waitForReconnectTimeslot();
		
		// Store the reconnect time to prevent to much
		// reconnects in a short timeframe. Otherwise the
		// rate limit will apply and the reconnects are not successfully
		final long currentTime = System.currentTimeMillis();
		reconnectTimes.removeIf((t) -> t + TimeUnit.MINUTES.toMillis(20) < currentTime);
		reconnectTimes.add(currentTime);
		
		// Disable auto reconnect to ignore session closed 
		// events, and preventing duplicate reconnects
		bitfinexApiBroker.setAutoReconnectEnabled(false);
		bitfinexApiBroker.reconnect();
		bitfinexApiBroker.setAutoReconnectEnabled(true);
	}

	/** 
	 * Wait some time before the next reconnect is executed
	 * @param reconnectState
	 */
	private void waitForReconnectTimeslot() {
		
		logger.debug("Waiting for reconnect timeslot");
		
		while(true) {
			final long connectionsTimeframe 
				= System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(MAX_RECONNECT_SECONDS);
			
			final long connectionsInTimeframe = reconnectTimes.stream().filter(t -> t > connectionsTimeframe).count();
	
			if(connectionsInTimeframe > MAX_RECONNECTS_IN_TIME) {
				logger.debug("Thre are {} reconnects in the timeframe", connectionsInTimeframe);
				
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(10));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			} else {
				logger.debug("Waiting for reconnect timeslot DONE");
				return;
			}
		}
	}
}