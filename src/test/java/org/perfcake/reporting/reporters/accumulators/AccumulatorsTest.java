package org.perfcake.reporting.reporters.accumulators;

import java.util.ArrayList;
import java.util.List;

import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.AvgAccumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;
import org.perfcake.reporting.reporters.accumulators.SlidingWindowAvgAccumulator;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class AccumulatorsTest {

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

   @DataProvider(name = "stressTest")
   public Object[][] createDataForStressTest() {
      final Long START = new Long(1L), END = new Long(100_000L);
      int WINDOW = 1000;

      // accumulator, start, end, result, after reset
      return new Object[][] { { new AvgAccumulator(), START, END, new Double((START + END) / 2d), new Double(0) }, { new LastValueAccumulator(), START, END, new Double(END), null }, { new SlidingWindowAvgAccumulator(WINDOW), START, END, new Double((END - WINDOW + 1 + END) / 2d), new Double(0) } };
   }

   @Test(dataProvider = "stressTest", groups = { "performance" })
   @SuppressWarnings("rawtypes")
   public void accumulatorStressTest(final Accumulator a, final Long start, final Long end, final Double result, final Double zero) throws InterruptedException {
      List<Thread> stressors = new ArrayList<>();

      for (int i = 0; i < 1000; i++) {
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

      Reporter.log("Stress test length " + time + "ms.");

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
            a.add(Double.valueOf(i));
         }
      }

   }
}
