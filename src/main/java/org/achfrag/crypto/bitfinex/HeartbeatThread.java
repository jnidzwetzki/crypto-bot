package org.achfrag.crypto.bitfinex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.achfrag.crypto.bitfinex.commands.PingCommand;
import org.achfrag.crypto.util.ExceptionSafeThread;

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
	 * @param bitfinexApiBroker
	 */
	HeartbeatThread(BitfinexApiBroker bitfinexApiBroker) {
		this.bitfinexApiBroker = bitfinexApiBroker;
		this.reconnectTimes = new ArrayList<>();
	}

	@Override
	public void runThread() {
		
		while(! Thread.interrupted()) {
			if(bitfinexApiBroker.websocketEndpoint != null) {
				
				final long nextHeartbeat = bitfinexApiBroker.getLastHeatbeat().get() + HEARTBEAT;

				if(nextHeartbeat < System.currentTimeMillis()) {
					bitfinexApiBroker.sendCommand(new PingCommand());
				}
				
				final long heartbeatTimeout = bitfinexApiBroker.getLastHeatbeat().get() + TIMEOUT;
				
				if(heartbeatTimeout < System.currentTimeMillis()) {
					BitfinexApiBroker.logger.error("Heartbeat timeout reconnecting");
					
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
			}
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				return;
			}
		}

	}

	/** 
	 * Wait some time before the next reconnect is executed
	 * @param reconnectState
	 */
	private void waitForReconnectTimeslot() {
		
		while(true) {
			final long connectionsTimeframe 
				= System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(MAX_RECONNECT_SECONDS);
			
			final long connectionsInTimeframe = reconnectTimes.stream().filter(t -> t > connectionsTimeframe).count();
	
			if(connectionsInTimeframe > MAX_RECONNECTS_IN_TIME) {
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(10));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			} else {
				return;
			}
		} 
	}
	
}