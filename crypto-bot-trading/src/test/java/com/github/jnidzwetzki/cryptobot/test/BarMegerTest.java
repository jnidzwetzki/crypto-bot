/*******************************************************************************
 *
 *    Copyright (C) 2015-2018 Jan Kristof Nidzwetzki
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package com.github.jnidzwetzki.cryptobot.test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import org.junit.Assert;
import org.junit.Test;
import org.ta4j.core.Bar;

import com.github.jnidzwetzki.bitfinex.v2.entity.BitfinexCurrencyPair;
import com.github.jnidzwetzki.bitfinex.v2.entity.Timeframe;
import com.github.jnidzwetzki.cryptobot.util.BarMerger;

public class BarMegerTest {
	
	private final static double DELTA = 0.00001;

	/**
	 * Test close Barmerger without any Bar
	 * @throws IOException
	 */
	public void testEmptyBarMerger() throws IOException {
		final BarMerger BarMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, (s, t) -> {});
		BarMerger.close();
	}
	
	/**
	 * Test one Bar
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test(timeout=5000)
	public void testBarMerger1() throws InterruptedException, IOException {
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> BarConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final BarMerger BarMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, BarConsumer);
		BarMerger.addNewPrice(1000000, 1.0, 5.0);
		BarMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test two Bar merge
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=5000)
	public void testBarMerger2() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> BarConsumer = (s, t) -> {
			Assert.assertEquals(10, t.getVolume().doubleValue(), DELTA);
			Assert.assertEquals(1.0, t.getMinPrice().doubleValue(), DELTA);
			Assert.assertEquals(2.0, t.getMaxPrice().doubleValue(), DELTA);
			latch.countDown();
		};
		
		final BarMerger BarMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, BarConsumer);
		BarMerger.addNewPrice(parser.parse("01:01:13").getTime(), 1.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:23").getTime(), 2.0, 5.0);
		BarMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test three Bar merge
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=6000)
	public void testBarMerger3() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(2);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> BarConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final BarMerger BarMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, BarConsumer);
		BarMerger.addNewPrice(parser.parse("01:01:23").getTime(), 1.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:33").getTime(), 2.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:02:53").getTime(), 2.0, 5.0);

		BarMerger.close();
		
		latch.await();
	}

	/**
	 * Test three Bar merge with other timestamps
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=6000)
	public void testBarMerger4() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(2);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> BarConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final BarMerger BarMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, BarConsumer);
		BarMerger.addNewPrice(parser.parse("01:01:23").getTime(), 1.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:33").getTime(), 2.0, 5.0);
		BarMerger.addNewPrice(parser.parse("02:02:53").getTime(), 2.0, 5.0);

		BarMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test Bar merger min max
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=6000)
	public void testBarMergerMinMax() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> BarConsumer = (s, t) -> {
			Assert.assertEquals(1.0, t.getMinPrice().doubleValue(), DELTA);
			Assert.assertEquals(8.0, t.getMaxPrice().doubleValue(), DELTA);
			Assert.assertEquals(3.0, t.getOpenPrice().doubleValue(), DELTA);
			Assert.assertEquals(4.5, t.getClosePrice().doubleValue(), DELTA);
			latch.countDown();
		};
		
		final BarMerger BarMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, BarConsumer);
		BarMerger.addNewPrice(parser.parse("01:01:01").getTime(), 3.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:02").getTime(), 2.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:03").getTime(), 8.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:04").getTime(), 1.5, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:05").getTime(), 2.5, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:06").getTime(), 1.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:01:07").getTime(), 4.5, 5.0);

		BarMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test the alignment of the Bars
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=10000)
	public void testBarAlignment1() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(3);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> BarConsumer = (s, t) -> {
			Assert.assertTrue(t.getEndTime().getSecond() == 59);
			latch.countDown();
		};
		
		final BarMerger BarMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, BarConsumer);
		BarMerger.addNewPrice(parser.parse("01:01:23").getTime(), 1.0, 5.0);
		BarMerger.addNewPrice(parser.parse("01:02:33").getTime(), 2.0, 5.0);
		BarMerger.addNewPrice(parser.parse("02:03:53").getTime(), 2.0, 5.0);
		BarMerger.addNewPrice(parser.parse("22:22:53").getTime(), 2.0, 5.0);

		BarMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test the alignment of the Bars
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=10000)
	public void testBarAlignment2() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(4);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> BarConsumer = (s, t) -> {
			Assert.assertTrue(t.getEndTime().getMinute() == 14 
					|| t.getEndTime().getMinute() == 29
					|| t.getEndTime().getMinute() == 44
					|| t.getEndTime().getMinute() == 59);
	
			Assert.assertEquals(59, t.getEndTime().getSecond());
			latch.countDown();
		};
		
		final BarMerger BarMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_15, BarConsumer);
		BarMerger.addNewPrice(parser.parse("01:01:00").getTime(), 1.0, 5.0);
		BarMerger.addNewPrice(parser.parse("02:41:33").getTime(), 2.0, 5.0);
		BarMerger.addNewPrice(parser.parse("10:33:11").getTime(), 2.0, 5.0);
		BarMerger.addNewPrice(parser.parse("22:22:53").getTime(), 2.0, 5.0);

		BarMerger.close();
		
		latch.await();
	}

}
