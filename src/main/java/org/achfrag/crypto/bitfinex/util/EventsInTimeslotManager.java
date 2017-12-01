package org.achfrag.crypto.bitfinex.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class limits the amount of events to t_events in a 
 * definable time period. recordNewEvent() records a new 
 * event, waitForNewTimeslot() blocks until less then
 * t_events occurred in the period of time.
 *
 */
public class EventsInTimeslotManager {
	
	/**
	 * The number of events in the timeslot
	 */
	private final int numberOfEvents;
	
	/**
	 * The size of the timeslot
	 */
	private final long timeslotInMilliseconds;

	/**
	 * The event list
	 */
	private final List<Long> events;

	public EventsInTimeslotManager(final int numberOfEvents, final int timeslot, final TimeUnit timeUnit) {
		
		if(numberOfEvents <= 0) {
			throw new IllegalArgumentException("Number of events must be >= 1");
		}
		
		if(timeslot <= 0) {
			throw new IllegalArgumentException("Timeslot must be >= 1");
		}
		
		this.numberOfEvents = numberOfEvents;
		this.timeslotInMilliseconds = timeUnit.toMillis(timeslot);
		this.events = new ArrayList<>();
	}
	
	/**
	 * Record a new event
	 */
	public void recordNewEvent() {
		
		// Remove old events from record
		final double thresholdTime = System.currentTimeMillis() - (timeslotInMilliseconds * 2.0);
		events.removeIf(e -> e < thresholdTime);
		
		// Record new event
		events.add(System.currentTimeMillis());
	}
	
	/**
	 * Wait for a new timeslow
	 * @return 
	 * @throws InterruptedException 
	 */
	public boolean waitForNewTimeslot() throws InterruptedException {
		
		boolean hasWaited = false;
		
		while(true) {
			final double thresholdTime = System.currentTimeMillis() - (timeslotInMilliseconds);

			final long numberOfEventsInTimeSlot = events.stream()
					.filter(e -> e >= thresholdTime)
					.count();
			
			if(numberOfEventsInTimeSlot > numberOfEvents) {
				hasWaited = true;
				Thread.sleep(timeslotInMilliseconds / 10);
			} else {
				return hasWaited;
			}
		}
	}
}
