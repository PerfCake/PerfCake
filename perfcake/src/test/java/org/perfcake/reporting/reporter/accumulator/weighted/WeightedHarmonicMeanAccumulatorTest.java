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
package org.perfcake.reporting.reporter.accumulator.weighted;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class WeightedHarmonicMeanAccumulatorTest {

   @Test
   public void testWeightedHarmonicMeanAccumulator() {
      final WeightedHarmonicMeanAccumulator wma = new WeightedHarmonicMeanAccumulator();
      wma.add(new WeightedValue<>(1d, 3L));
      wma.add(new WeightedValue<>(2d, 4L));
      wma.add(new WeightedValue<>(3d, 5L));

      Assert.assertEquals(wma.getResult(), new WeightedValue<>(
            (3L + 4L + 5L) / ((3L / 1d) + (4L / 2d) + (5L / 3d)), 1L));

      wma.reset();
      Assert.assertEquals(wma.getResult(), new WeightedValue<>(Double.NaN, 1L));

   }
}
