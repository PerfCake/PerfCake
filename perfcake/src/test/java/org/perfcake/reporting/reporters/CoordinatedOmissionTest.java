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
package org.perfcake.reporting.reporters;

import org.HdrHistogram.ConcurrentDoubleHistogram;
import org.HdrHistogram.DoubleHistogram;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CoordinatedOmissionTest {

   @Test
   public void hdrHistogramTest() {
      DoubleHistogram hdr = new ConcurrentDoubleHistogram(50, 1);
      //hdr.setAutoResize(true);

      for (int i = 0; i < 1000; i++) {
         hdr.recordValue(2d);
      }

      hdr.recordValue(100d);

      hdr.copyCorrectedForCoordinatedOmission(1.5d).outputPercentileDistribution(System.out, 2, 1d, true);
      //hdr.outputPercentileDistribution(System.out, 1d);

      System.out.println(hdr.getPercentileAtOrBelowValue(2d));
   }
}
