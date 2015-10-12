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
