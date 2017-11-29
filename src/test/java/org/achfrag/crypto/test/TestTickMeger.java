package org.achfrag.crypto.test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import org.achfrag.crypto.bitfinex.entity.Timeframe;
import org.achfrag.crypto.bitfinex.util.TickMerger;
import org.junit.Assert;
import org.junit.Test;
import org.ta4j.core.Tick;

public class TestTickMeger {
	
	private final static double DELTA = 0.00001;

	/**
	 * Test one tick
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test(timeout=600)
	public void testTickMerger1() throws InterruptedException, IOException {
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<String, Tick> tickConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final TickMerger tickMerger = new TickMerger("abc", Timeframe.SECONDS_30, tickConsumer);
		tickMerger.addNewPrice(1000000, 1.0, 5.0);
		tickMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test two tick merge
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=600)
	public void testTickMerger2() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<String, Tick> tickConsumer = (s, t) -> {
			Assert.assertEquals(10, t.getVolume().toDouble(), DELTA);
			Assert.assertEquals(1.0, t.getMinPrice().toDouble(), DELTA);
			Assert.assertEquals(2.0, t.getMaxPrice().toDouble(), DELTA);
			latch.countDown();
		};
		
		final TickMerger tickMerger = new TickMerger("abc", Timeframe.SECONDS_30, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:23").getTime(), 1.0, 5.0);
		tickMerger.addNewPrice(parser.parse("01:01:33").getTime(), 2.0, 5.0);
		tickMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test three tick merge
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=6000)
	public void testTickMerger3() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(2);
		
		final BiConsumer<String, Tick> tickConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final TickMerger tickMerger = new TickMerger("abc", Timeframe.SECONDS_30, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:23").getTime(), 1.0, 5.0);
		tickMerger.addNewPrice(parser.parse("01:01:33").getTime(), 2.0, 5.0);
		tickMerger.addNewPrice(parser.parse("01:01:53").getTime(), 2.0, 5.0);

		tickMerger.close();
		
		latch.await();
	}

	/**
	 * Test three tick merge with other timestamps
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=6000)
	public void testTickMerger4() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(2);
		
		final BiConsumer<String, Tick> tickConsumer = (s, t) -> {
			latch.countDown();
			System.out.println(t);
		};
		
		final TickMerger tickMerger = new TickMerger("abc", Timeframe.SECONDS_30, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:23").getTime(), 1.0, 5.0);
		tickMerger.addNewPrice(parser.parse("01:01:33").getTime(), 2.0, 5.0);
		tickMerger.addNewPrice(parser.parse("02:01:53").getTime(), 2.0, 5.0);

		tickMerger.close();
		
		latch.await();
	}

}
