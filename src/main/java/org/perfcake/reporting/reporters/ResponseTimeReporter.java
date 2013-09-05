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

import java.util.HashMap;
import java.util.Map;

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.AvgAccumulator;

/**
 * Reports average response time of all measure units
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class ResponseTimeReporter extends AbstractReporter {

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      final Map<String, Object> result = new HashMap<>();
      result.put(Measurement.DEFAULT_RESULT, Double.valueOf(mu.getTotalTime()));
      accumulateResults(result);
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      final Measurement m = newMeasurement();
      m.set(new Quantity<Double>((Double) getAccumulatedResult(Measurement.DEFAULT_RESULT), "ms"));
      publishAccumulatedResult(m);

      d.report(m);
   }

   @Override
   protected void doReset() {
      // nothing needed, the parent does the job
   }

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      if (Double.class.equals(clazz)) {
         return new AvgAccumulator();
      } else {
         return super.getAccumulator(key, clazz);
      }
   }

}
