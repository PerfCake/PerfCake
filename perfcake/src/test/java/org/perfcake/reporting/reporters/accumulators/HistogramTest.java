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
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting.reporters.accumulators;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class HistogramTest {

   @Test
   public void testValidRange() {
      Range range = new Range(-1000, 1000);
      Assert.assertFalse(range.contains(-1001));
      Assert.assertTrue(range.contains(-1000));
      Assert.assertTrue(range.contains(-999));
      Assert.assertTrue(range.contains(999));
      Assert.assertFalse(range.contains(1000));
      Assert.assertFalse(range.contains(1001));
      Assert.assertFalse(range.contains(Double.NEGATIVE_INFINITY));
      Assert.assertFalse(range.contains(Double.POSITIVE_INFINITY));
      Assert.assertFalse(range.contains(Double.NaN));
      Assert.assertTrue(range.contains(Double.MIN_VALUE));
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
   public void testHistogram() {

      // Create 4 ranges: -INF....-250....0....+250....+INF
      List<Double> dividers = new LinkedList<>();
      dividers.add(-250.0);
      dividers.add(0.0);
      dividers.add(250.0);
      Histogram histogram = new Histogram(dividers);

      // add values 500..-251, -250..-1, 0..249, 250..500
      for (long i = 499; i >= -500; i--) {
         histogram.add(i);
      }
      final Map<Range, Long> histogramMap = histogram.getHistogram();
      Assert.assertEquals(histogramMap.keySet().size(), 4);
      Assert.assertEquals((long) histogram.getCount(), 1000L);
      histogramMap.forEach((Range k, Long v) -> Assert.assertEquals((long) v, 250L, "Not working for range " + k.toString()));

      System.out.println(histogram.getHistogramInPercent());
   }

   @Test
   public void testHistogramScramble() {
      Histogram histogram = new Histogram("0,1,2,3,4,5,6,7,8,9,10");

      Random rnd = new Random();
      for (int i = 0; i < 1_000_000; i++) {
         histogram.add(rnd.nextDouble() * 10.0);
      }

      double total = 0;
      for (double d : histogram.getHistogramInPercent().values()) {
         total = total + d;
      }

      Assert.assertTrue(total > 99.9 && total < 100.1);
   }
}
