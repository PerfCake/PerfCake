/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.reporting;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests scalable quantities.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class ScalableQuantityTest {
   @Test
   public void testMetricScalableQuantity() {
      Assert.assertEquals(new MetricScalableQuantity(12.3, "b").toString(), "12.30 b");
      Assert.assertEquals(new MetricScalableQuantity(1000.0, "b").toString(), "1.00 kb");
      Assert.assertEquals(new MetricScalableQuantity(1234.5, "b").toString(), "1.23 kb");
      Assert.assertEquals(new MetricScalableQuantity(1234567.8, "b").toString(), "1.23 Mb");
      Assert.assertEquals(new MetricScalableQuantity(1000000.0, "b").toString(), "1.00 Mb");
      // Corner cases
      Assert.assertEquals(new MetricScalableQuantity(1e27, "b").toString(), "1000.00 Yb");
      Assert.assertEquals(new MetricScalableQuantity(1e28, "b").toString(), "10000.00 Yb");
   }

   @Test
   public void testMetricScalableQuantityDown() {
      Assert.assertEquals(new MetricScalableQuantity(0.5, "b").toString(), "500.00 mb");
      Assert.assertEquals(new MetricScalableQuantity(0.0003, "b").toString(), "300.00 μb");
      Assert.assertEquals(new MetricScalableQuantity(1.0, "b").toString(), "1.00 b");
      Assert.assertEquals(new MetricScalableQuantity(0.001, "b").toString(), "1.00 mb");
      // Corner cases
      Assert.assertEquals(new MetricScalableQuantity(1e-26, "b").toString(), "0.01 yb");
      Assert.assertEquals(new MetricScalableQuantity(1e-27, "b").toString(), "0.00 yb");
      Assert.assertEquals(new MetricScalableQuantity(1e-28, "b").toString(), "0.00 yb");
   }

   @Test
   public void testMetricScalableQuantityBasePower() {
      Assert.assertEquals(new MetricScalableQuantity(100.0, 2, "b").toString(), "100.00 Mb");
      Assert.assertEquals(new MetricScalableQuantity(100.0, -2, "b").toString(), "100.00 μb");
   }

   @Test
   public void testBinaryScalableQuantityBasePower() {
      Assert.assertEquals(new BinaryScalableQuantity(100L, 2, "b").toString(), "100.00 Mib");
   }

   @Test
   public void testScalableQuantityBasePowerOutOfRange() {
      try {
         new MetricScalableQuantity(100.0, -9, "b").toString();
         Assert.fail("An " + IllegalArgumentException.class.getName() + " should be thrown here.");
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
      try {
         new MetricScalableQuantity(100.0, 9, "b").toString();
         Assert.fail("An " + IllegalArgumentException.class.getName() + " should be thrown here.");
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
      try {
         new BinaryScalableQuantity(100L, -1, "b").toString();
         Assert.fail("An " + IllegalArgumentException.class.getName() + " should be thrown here.");
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
      try {
         new BinaryScalableQuantity(100L, 9, "b").toString();
         Assert.fail("An " + IllegalArgumentException.class.getName() + " should be thrown here.");
      } catch (IllegalArgumentException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testBinaryScalableQuantity() {
      Assert.assertEquals(new BinaryScalableQuantity(1000L, "b").toString(), "1000.00 b");
      Assert.assertEquals(new BinaryScalableQuantity(1024L, "b").toString(), "1.00 Kib");
      Assert.assertEquals(new BinaryScalableQuantity(2048L, "b").toString(), "2.00 Kib");
      Assert.assertEquals(new BinaryScalableQuantity(3L * 1024 * 1024, "b").toString(), "3.00 Mib");
   }
}
