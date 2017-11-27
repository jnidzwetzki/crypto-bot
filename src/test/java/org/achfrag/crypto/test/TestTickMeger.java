package org.achfrag.crypto.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import org.achfrag.crypto.bitfinex.misc.TickMerger;
import org.achfrag.crypto.bitfinex.misc.Timeframe;
import org.junit.Test;
import org.ta4j.core.Tick;

public class TestTickMeger {

	@Test(timeout=600)
	public void testTickMerger1() throws InterruptedException, IOException {
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<String, Tick> tickConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final TickMerger tickMerger = new TickMerger("abc", Timeframe.SECONDS_30, tickConsumer);
		tickMerger.addNewPrice(1000000, 1.0, 5.0);
		tickMerger.addNewPrice(2000000, 1.0, 5.0);
		tickMerger.close();
		
		latch.await();
	}

}
