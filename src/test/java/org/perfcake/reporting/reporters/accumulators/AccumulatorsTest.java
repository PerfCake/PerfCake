/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting.reporters.accumulators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 *
 */
public class AccumulatorsTest {

   private static final int STRESS_THREADS = 500;

   @Test
   public void lastValueAccumulatorTest() {
      LastValueAccumulator lva = new LastValueAccumulator();

      Assert.assertNull(lva.getResult(), "Clean LastValueAccumulator must have null result.");

      lva.add("Hello");
      Assert.assertEquals(lva.getResult(), "Hello");

      lva.add("World");
      Assert.assertEquals(lva.getResult(), "World");

      lva.reset();
      Assert.assertNull(lva.getResult(), "LastValueAccumulator must have null result after reset.");
   }

   @Test
   public void avgAccumulatorTest() {
      final int START = 10, END = 100;

      AvgAccumulator aa = new AvgAccumulator();

      Assert.assertEquals(aa.getResult(), 0d);

      for (int i = START; i <= END; i++) {
         aa.add((double) i);
      }

      Assert.assertEquals(aa.getResult(), (START + END) / 2d);

      aa.reset();
      Assert.assertEquals(aa.getResult(), 0d, "AvgAccumulator must be 0 after reset.");
   }

   @Test
   public void sumAccumulatorTest() {
      final int START = 10, END = 100;

      SumAccumulator aa = new SumAccumulator();

      Assert.assertEquals(aa.getResult(), 0d);

      for (int i = START; i <= END; i++) {
         aa.add((double) i);
      }

      Assert.assertEquals(aa.getResult(), (START + END) * (END - START + 1) / 2d);

      aa.reset();
      Assert.assertEquals(aa.getResult(), 0d, "SumAccumulator must be 0 after reset.");
   }

   @Test
   public void slidingWindowAvgAccumulatorTest() {
      final int START = 10, END = 100, WINDOW = 16;

      SlidingWindowAvgAccumulator aa = new SlidingWindowAvgAccumulator(WINDOW);

      Assert.assertEquals(aa.getResult(), 0d);

      aa.add(5d);
      Assert.assertEquals(aa.getResult(), 5d);
      aa.reset();
      Assert.assertEquals(aa.getResult(), 0d, "SlidingWindowAvgAccumulator must be 0 after reset.");

      for (int i = START; i <= END; i++) {
         aa.add((double) i);
      }

      Assert.assertEquals(aa.getResult(), (END - WINDOW + 1 + END) / 2d);

      aa.reset();
      Assert.assertEquals(aa.getResult(), 0d, "SlidingWindowAvgAccumulator must be 0 after reset.");
   }

   @Test
   public void maxLongValueAccumulatorTest() {
      MaxLongValueAccumulator mlva = new MaxLongValueAccumulator();
      Assert.assertEquals((long) mlva.getResult(), (long) Long.MIN_VALUE);

      mlva.add(1l);
      Assert.assertEquals((long) mlva.getResult(), 1l);

      Random r = new Random();
      for (int i = 0; i < 5000; i++) {
         mlva.add(r.nextLong() >> 32);
      }
      Assert.assertTrue(mlva.getResult() <= Long.MAX_VALUE >> 32);

      mlva.add(Long.MAX_VALUE);
      Assert.assertEquals((long) mlva.getResult(), Long.MAX_VALUE);
   }

   @DataProvider(name = "stressTest")
   public Object[][] createDataForStressTest() {
      final Long START = 1L, END = 100_000L;
      final int WINDOW = 1000;

      // accumulator, start, end, result, after reset
      return new Object[][] { { new AvgAccumulator(), START, END, (START + END) / 2d, 0d },
            { new SumAccumulator(), START, END, STRESS_THREADS * (START + END) * (END - START + 1L) / 2d, 0d },
            { new LastValueAccumulator(), START, END, (double) END, null },
            { new SlidingWindowAvgAccumulator(WINDOW), START, END, (END - WINDOW + 1 + END) / 2d, 0d } };
   }

   @Test(dataProvider = "stressTest", groups = { "performance" })
   @SuppressWarnings("rawtypes")
   public void accumulatorStressTest(final Accumulator a, final Long start, final Long end, final Double result, final Double zero) throws InterruptedException {
      List<Thread> stressors = new ArrayList<>();

      for (int i = 0; i < STRESS_THREADS; i++) {
         stressors.add(new Thread(new AccumulatorStressor(a, start, end)));
      }

      long time = System.currentTimeMillis();

      for (Thread t : stressors) {
         t.start();
      }

      for (Thread t : stressors) {
         t.join();
      }

      time = System.currentTimeMillis() - time;

      Reporter.log("Stress test (" + a.getClass().getSimpleName() + ") length " + time + "ms.");

      Assert.assertEquals(a.getResult(), result);

      a.reset();

      Assert.assertEquals(a.getResult(), zero);
   }

   @SuppressWarnings("rawtypes")
   public static class AccumulatorStressor implements Runnable {

      private final Accumulator a;
      private final long start, end;

      public AccumulatorStressor(final Accumulator a, final long start, final long end) {
         this.a = a;
         this.start = start;
         this.end = end;
      }

      @SuppressWarnings("unchecked")
      @Override
      public void run() {
         for (long i = start; i <= end; i = i + 1L) {
            a.add((double) i);
         }
      }
   }
}
