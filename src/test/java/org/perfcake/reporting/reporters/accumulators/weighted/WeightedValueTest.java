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
package org.perfcake.reporting.reporters.accumulators.weighted;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class WeightedValueTest {

   @Test
   public void weightedValueTest() {
      final WeightedValue<Byte> w1 = new WeightedValue<>((byte) 10, 3l);
      Assert.assertEquals(w1.getValue(), Byte.valueOf((byte) 10));
      Assert.assertEquals(w1.getWeight(), 3l);

      final WeightedValue<Double> w2 = new WeightedValue<>(10d, 3l);
      Assert.assertTrue(w2.equals(w2));
      Assert.assertEquals(w2.getValue(), 10d);
      Assert.assertEquals(w2.getWeight(), 3l);

      Assert.assertNotEquals(w1, w2);

      final WeightedValue<Double> w3 = new WeightedValue<>(w2.getValue(), w2.getWeight());
      Assert.assertEquals(w2, w3);

      Assert.assertEquals(new WeightedValue<Long>(123l, 1l).toString(), "123");
   }

   @Test
   public void testEquals() {
      Assert.assertNotEquals(new WeightedValue<Long>(2l, 3l), new WeightedValue<Long>(4l, 3l));
      Assert.assertNotEquals(new WeightedValue<Long>(2l, 3l), new WeightedValue<Long>(2l, 4l));
      Assert.assertNotEquals(new WeightedValue<Long>(2l, 3l), 4d);
   }

   @Test
   public void testHashCode() {
      Assert.assertEquals(new WeightedValue<Long>(2l, 3l).hashCode(), new WeightedValue<Long>(2l, 3l).hashCode());
      Assert.assertNotEquals(new WeightedValue<Long>(2l, 3l).hashCode(), new WeightedValue<Long>(4l, 3l).hashCode());
      Assert.assertNotEquals(new WeightedValue<Long>(2l, 3l).hashCode(), new WeightedValue<Long>(2l, 4l).hashCode());
      Assert.assertNotEquals(new WeightedValue<Long>(2l, 3l).hashCode(), new WeightedValue<Long>(1l, 4l).hashCode());
   }
}
