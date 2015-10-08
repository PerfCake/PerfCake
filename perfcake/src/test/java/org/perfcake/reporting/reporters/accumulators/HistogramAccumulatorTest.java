package org.perfcake.reporting.reporters.accumulators;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pmacik on 8.10.15.
 */
public class HistogramAccumulatorTest {
   @Test
   public void testValidRange() {
      Range<Comparable> range = new Range<>(-1000L, 1000L);
      Assert.assertFalse(range.contains(-1001L));
      Assert.assertTrue(range.contains(-1000L));
      Assert.assertTrue(range.contains(-999L));
      Assert.assertTrue(range.contains(999L));
      Assert.assertTrue(range.contains(1000L));
      Assert.assertFalse(range.contains(1001L));
   }

   @Test
   public void testInvalidRange() {
      try {
         Range<Comparable> range = new Range<>(0L, 0L);
         Assert.fail(IllegalArgumentException.class.getName() + " is expected to be thrown.");
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
      try {
         Range<Comparable> range = new Range<>(1L, 0L);
         Assert.fail(IllegalArgumentException.class.getName() + " is expected to be thrown.");
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testHistogramAccumulator() {

      // Create 4 ranges: -INF....-250....0....+250....+INF
      List<Comparable> dividers = new LinkedList<>();
      dividers.add(-250L);
      dividers.add(0L);
      dividers.add(250L);
      HistogramAccumulator acc = new HistogramAccumulator(dividers, Long.MIN_VALUE, Long.MAX_VALUE);

      for (long i = 500; i > -500; i--) {
         acc.add(i);
      }
      final Map histogram = acc.getHistogram();
      Assert.assertEquals(histogram.keySet().size(), 4);
      Assert.assertEquals((long) acc.getCount(), 1000L);
      histogram.forEach((k, v) -> Assert.assertEquals((long) v, 250L));
   }
}
