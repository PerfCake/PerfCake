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
public class WeightedMeanAccumulatorTest {

   @Test
   public void testWeightedMeanAccumulator() {
      final WeightedMeanAccumulator wma = new WeightedMeanAccumulator();
      wma.add(new WeightedValue<Double>(1d, 1l));
      wma.add(new WeightedValue<Double>(2d, 2l));
      wma.add(new WeightedValue<Double>(3d, 3l));

      Assert.assertEquals(wma.getResult(), new WeightedValue<Double>((1d * 1l + 2d * 2l + 3d * 3l) / (1l + 2l + 3l), 1l));

      wma.reset();
      Assert.assertEquals(wma.getResult(), new WeightedValue<Double>(Double.NaN, 1l));
   }

}
