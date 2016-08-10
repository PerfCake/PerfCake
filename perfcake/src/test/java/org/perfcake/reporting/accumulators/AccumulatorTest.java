package org.perfcake.reporting.accumulators;

import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.AvgAccumulator;
import org.perfcake.reporting.reporters.accumulators.HarmonicMeanAccumulator;
import org.perfcake.reporting.reporters.accumulators.MaxAccumulator;
import org.perfcake.reporting.reporters.accumulators.MinAccumulator;
import org.perfcake.reporting.reporters.accumulators.TimeSlidingWindowAvgAccumulator;
import org.perfcake.reporting.reporters.accumulators.TimeSlidingWindowHarmonicMeanAccumulator;
import org.perfcake.reporting.reporters.accumulators.TimeSlidingWindowMaxAccumulator;
import org.perfcake.reporting.reporters.accumulators.TimeSlidingWindowMinAccumulator;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="pavel.macik@gmail.com>Pavel Macík</a>
 */
public class AccumulatorTest {
   @Test
   public void testMaxAccumulator() throws Exception {
      final Accumulator accumulator = new MaxAccumulator();

      for (int i = 1; i <= 5; i++) {
         accumulator.add((double) i);
      }
      Assert.assertEquals(accumulator.getResult(), 5.0);
   }

   @Test
   public void testMinAccumulator() throws Exception {
      final Accumulator accumulator = new MinAccumulator();

      for (int i = 1; i <= 5; i++) {
         accumulator.add((double) i);
      }
      Assert.assertEquals(accumulator.getResult(), 1.0);
   }

   @Test
   public void testAvgAccumulator() throws Exception {
      final Accumulator accumulator = new AvgAccumulator();

      for (int i = 1; i <= 5; i++) {
         accumulator.add((double) i);
      }
      Assert.assertEquals(accumulator.getResult(), (1.0 + 2.0 + 3.0 + 4.0 + 5.0) / 5.0);
   }

   @Test
   public void testHarmonicMeanAccumulator() throws Exception {
      final Accumulator accumulator = new HarmonicMeanAccumulator();

      for (int i = 1; i <= 5; i++) {
         accumulator.add((double) i);
      }
      Assert.assertEquals(accumulator.getResult(), 5.0 / (1.0 + 1.0 / 2.0 + 1.0 / 3.0 + 1.0 / 4.0 + 1.0 / 5.0));
   }

   @Test
   public void testTimeSlidingWindowMaxAccumulator() throws Exception {
      final Accumulator accumulator = new TimeSlidingWindowMaxAccumulator(6000);

      for (int i = 5; i > 0; i--) {
         accumulator.add((double) i);
         Thread.sleep(1000);
      }
      Assert.assertEquals(accumulator.getResult(), 5.0);
      Thread.sleep(2000);
      Assert.assertEquals(accumulator.getResult(), 3.0);
      Thread.sleep(1000);
      Assert.assertEquals(accumulator.getResult(), 2.0);
   }

   @Test
   public void testTimeSlidingWindowMinAccumulator() throws Exception {
      final Accumulator accumulator = new TimeSlidingWindowMinAccumulator(6000);

      for (int i = 1; i <= 5; i++) {
         accumulator.add((double) i);
         Thread.sleep(1000);
      }
      Assert.assertEquals(accumulator.getResult(), 1.0);
      Thread.sleep(2000);
      Assert.assertEquals(accumulator.getResult(), 3.0);
      Thread.sleep(1000);
      Assert.assertEquals(accumulator.getResult(), 4.0);
   }

   @Test
   public void testTimeSlidingWindowAvgAccumulator() throws Exception {
      final Accumulator accumulator = new TimeSlidingWindowAvgAccumulator(6000);

      for (int i = 1; i <= 5; i++) {
         accumulator.add((double) i);
         Thread.sleep(1000);
      }
      Assert.assertEquals(accumulator.getResult(), 3.0);
      Thread.sleep(2000);
      Assert.assertEquals(accumulator.getResult(), 4.0);
      Thread.sleep(1000);
      Assert.assertEquals(accumulator.getResult(), 4.5);
   }

   @Test
   public void testTimeSlidingWindowHarmonicMeanAccumulator() throws Exception {
      final Accumulator accumulator = new TimeSlidingWindowHarmonicMeanAccumulator(6000);

      for (int i = 1; i <= 5; i++) {
         accumulator.add((double) i);
         Thread.sleep(1000);
      }
      Assert.assertEquals(accumulator.getResult(), 5.0 / (1.0 + 1.0 / 2.0 + 1.0 / 3.0 + 1.0 / 4.0 + 1.0 / 5.0));
      Thread.sleep(2000);
      Assert.assertEquals(accumulator.getResult(), 3.0 / (1.0 / 3.0 + 1.0 / 4.0 + 1.0 / 5.0));
      Thread.sleep(1000);
      Assert.assertEquals(accumulator.getResult(), 2.0 / (1.0 / 4.0 + 1.0 / 5.0));
   }
}
