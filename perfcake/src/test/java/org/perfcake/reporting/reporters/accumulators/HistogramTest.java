package org.perfcake.reporting.reporters.accumulators;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pmacik on 8.10.15.
 */
public class HistogramTest {
   @Test
   public void testValidRange() {
      Range range = new Range(-1000, 1000);
      Assert.assertFalse(range.contains(-1001));
      Assert.assertTrue(range.contains(-1000));
      Assert.assertTrue(range.contains(-999));
      Assert.assertTrue(range.contains(999));
      Assert.assertTrue(range.contains(1000));
      Assert.assertFalse(range.contains(1001));
   }

   @Test
   public void testInvalidRange() {
      try {
         Range range = new Range(0, 0);
         Assert.fail(IllegalArgumentException.class.getName() + " is expected to be thrown.");
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
      try {
         Range range = new Range(1, 0);
         Assert.fail(IllegalArgumentException.class.getName() + " is expected to be thrown.");
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testHistogramAccumulator() {

      // Create 4 ranges: -INF....-250....0....+250....+INF
      List<Double> dividers = new LinkedList<>();
      dividers.add(-250.0);
      dividers.add(0.0);
      dividers.add(250.0);
      Histogram acc = new Histogram(dividers);

      for (long i = 500; i > -500; i--) {
         acc.add(i);
      }
      final Map<Range, Long> histogram = acc.getHistogram();
      Assert.assertEquals(histogram.keySet().size(), 4);
      Assert.assertEquals((long) acc.getCount(), 1000L);
      histogram.forEach((Range k, Long v) -> Assert.assertEquals((long) v, 250L));
   }
}
