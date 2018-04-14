package com.github.jnidzwetzki.cryptobot.test;
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


import java.io.IOException;
import java.math.BigDecimal;
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
	 * Test close tickmerger without any tick
	 * @throws IOException
	 */
	public void testEmptyTickMerger() throws IOException {
		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, (s, t) -> {});
		tickMerger.close();
	}
	
	/**
	 * Test one tick
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test(timeout=5000)
	public void testTickMerger1() throws InterruptedException, IOException {
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> tickConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, tickConsumer);
		tickMerger.addNewPrice(1000000, new BigDecimal(1.0), new BigDecimal(5.0));
		tickMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test two tick merge
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=5000)
	public void testTickMerger2() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> tickConsumer = (s, t) -> {
			Assert.assertEquals(10, t.getVolume().doubleValue(), DELTA);
			Assert.assertEquals(1.0, t.getMinPrice().doubleValue(), DELTA);
			Assert.assertEquals(2.0, t.getMaxPrice().doubleValue(), DELTA);
			latch.countDown();
		};
		
		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:13").getTime(), new BigDecimal(1.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:23").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));
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
		
		final BiConsumer<BitfinexCurrencyPair, Bar> tickConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:23").getTime(), new BigDecimal(1.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:33").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:02:53").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));

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
		
		final BiConsumer<BitfinexCurrencyPair, Bar> tickConsumer = (s, t) -> {
			latch.countDown();
		};
		
		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:23").getTime(), new BigDecimal(1.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:33").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("02:02:53").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));

		tickMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test tick merger min max
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=6000)
	public void testTickMergerMinMax() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(1);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> tickConsumer = (s, t) -> {
			Assert.assertEquals(1.0, t.getMinPrice().doubleValue(), DELTA);
			Assert.assertEquals(8.0, t.getMaxPrice().doubleValue(), DELTA);
			Assert.assertEquals(3.0, t.getOpenPrice().doubleValue(), DELTA);
			Assert.assertEquals(4.5, t.getClosePrice().doubleValue(), DELTA);
			latch.countDown();
		};
		
		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:01").getTime(), new BigDecimal(3.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:02").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:03").getTime(), new BigDecimal(8.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:04").getTime(), new BigDecimal(1.5), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:05").getTime(), new BigDecimal(2.5), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:06").getTime(), new BigDecimal(1.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:01:07").getTime(), new BigDecimal(4.5), new BigDecimal(5.0));

		tickMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test the alignment of the ticks
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=10000)
	public void testTickAlignment1() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(3);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> tickConsumer = (s, t) -> {
			Assert.assertTrue(t.getEndTime().getSecond() == 59);
			latch.countDown();
		};
		
		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_1, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:23").getTime(), new BigDecimal(1.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("01:02:33").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("02:03:53").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("22:22:53").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));

		tickMerger.close();
		
		latch.await();
	}
	
	/**
	 * Test the alignment of the ticks
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Test(timeout=10000)
	public void testTickAlignment2() throws InterruptedException, IOException, ParseException {
		
        final SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
        
		final CountDownLatch latch = new CountDownLatch(4);
		
		final BiConsumer<BitfinexCurrencyPair, Bar> tickConsumer = (s, t) -> {
			Assert.assertTrue(t.getEndTime().getMinute() == 14 
					|| t.getEndTime().getMinute() == 29
					|| t.getEndTime().getMinute() == 44
					|| t.getEndTime().getMinute() == 59);
	
			Assert.assertEquals(59, t.getEndTime().getSecond());
			latch.countDown();
		};
		
		final BarMerger tickMerger = new BarMerger(BitfinexCurrencyPair.BTC_USD, Timeframe.MINUTES_15, tickConsumer);
		tickMerger.addNewPrice(parser.parse("01:01:00").getTime(), new BigDecimal(1.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("02:41:33").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("10:33:11").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));
		tickMerger.addNewPrice(parser.parse("22:22:53").getTime(), new BigDecimal(2.0), new BigDecimal(5.0));

		tickMerger.close();
		
		latch.await();
	}

}
