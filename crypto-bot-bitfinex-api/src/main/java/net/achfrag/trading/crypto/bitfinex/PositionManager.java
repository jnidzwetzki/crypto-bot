package net.achfrag.trading.crypto.bitfinex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import net.achfrag.trading.crypto.bitfinex.entity.Position;

public class PositionManager extends AbstractSimpleCallbackManager<Position> {

	/**
	 * The positions
	 */
	private final List<Position> positions;

	public PositionManager(final ExecutorService executorService) {
		super(executorService);
		this.positions = new ArrayList<>();
	}

	/**
	 * Clear all orders
	 */
	public void clear() {
		synchronized (positions) {
			positions.clear();	
		}
	}
	
	/**
	 * Update a exchange order
	 * @param exchangeOrder
	 */
	public void updatePosition(final Position position) {
		
		synchronized (positions) {
			// Replace position
			positions.removeIf(p -> p.getCurreny() == position.getCurreny());
			positions.notifyAll();
		}
		
		notifyCallbacks(position);
	}
	
	/**
	 * Get the positions
	 * @return
	 */
	public List<Position> getPositions() {
		synchronized (positions) {
			return positions;
		}
	}
	
}
