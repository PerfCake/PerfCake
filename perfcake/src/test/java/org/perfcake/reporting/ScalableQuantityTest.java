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
 * Fake measurement unit for test usage.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScalableQuantityTest {
   @Test
   public void testMetricScalableQuantity() {
      Assert.assertEquals(new MetricScalableQuantity(12.3, "b").toString(), "12.30 b");
      Assert.assertEquals(new MetricScalableQuantity(1000.0, "b").toString(), "1.00 kb");
      Assert.assertEquals(new MetricScalableQuantity(1234.5, "b").toString(), "1.23 kb");
      Assert.assertEquals(new MetricScalableQuantity(1234567.8, "b").toString(), "1.23 Mb");
      Assert.assertEquals(new MetricScalableQuantity(1000000.0, "b").toString(), "1.00 Mb");
   }

   @Test
   public void testBinaryScalableQuantity() {
      Assert.assertEquals(new BinaryScalableQuantity(1000.0, "b").toString(), "1000.00 b");
      Assert.assertEquals(new BinaryScalableQuantity(1024.0, "b").toString(), "1.00 Kib");
      Assert.assertEquals(new BinaryScalableQuantity(2048.0, "b").toString(), "2.00 Kib");
      Assert.assertEquals(new BinaryScalableQuantity(3.0 * 1024 * 1024, "b").toString(), "3.00 Mib");
   }
}
