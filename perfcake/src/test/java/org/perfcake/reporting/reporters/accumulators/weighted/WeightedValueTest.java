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
package org.perfcake.reporting.reporters.accumulators.weighted;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class WeightedValueTest {

   @Test
   public void weightedValueTest() {
      final WeightedValue<Byte> w1 = new WeightedValue<>((byte) 10, 3L);
      Assert.assertEquals(w1.getValue(), Byte.valueOf((byte) 10));
      Assert.assertEquals(w1.getWeight(), 3L);

      final WeightedValue<Double> w2 = new WeightedValue<>(10d, 3L);
      Assert.assertTrue(w2.equals(w2));
      Assert.assertEquals(w2.getValue(), 10d);
      Assert.assertEquals(w2.getWeight(), 3L);

      Assert.assertNotEquals(w1, w2);

      final WeightedValue<Double> w3 = new WeightedValue<>(w2.getValue(), w2.getWeight());
      Assert.assertEquals(w2, w3);

      Assert.assertEquals(new WeightedValue<>(123L, 1L).toString(), "123");
   }

   @Test
   public void testEquals() {
      Assert.assertNotEquals(new WeightedValue<>(2L, 3L), new WeightedValue<>(4L, 3L));
      Assert.assertNotEquals(new WeightedValue<>(2L, 3L), new WeightedValue<>(2L, 4L));
      Assert.assertNotEquals(new WeightedValue<>(2L, 3L), 4d);
   }

   @Test
   public void testHashCode() {
      Assert.assertEquals(new WeightedValue<>(2L, 3L).hashCode(), new WeightedValue<>(2L, 3L).hashCode());
      Assert.assertNotEquals(new WeightedValue<>(2L, 3L).hashCode(), new WeightedValue<>(4L, 3L).hashCode());
      Assert.assertNotEquals(new WeightedValue<>(2L, 3L).hashCode(), new WeightedValue<>(2L, 4L).hashCode());
      Assert.assertNotEquals(new WeightedValue<>(2L, 3L).hashCode(), new WeightedValue<>(1L, 4L).hashCode());
   }
}
