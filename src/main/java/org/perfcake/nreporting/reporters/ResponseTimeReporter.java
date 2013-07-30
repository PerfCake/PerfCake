/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.perfcake.nreporting.reporters;

import java.util.HashMap;
import java.util.Map;

import org.perfcake.common.PeriodType;
import org.perfcake.nreporting.Measurement;
import org.perfcake.nreporting.MeasurementUnit;
import org.perfcake.nreporting.Quantity;
import org.perfcake.nreporting.ReportingException;
import org.perfcake.nreporting.destinations.Destination;
import org.perfcake.nreporting.reporters.accumulators.Accumulator;
import org.perfcake.nreporting.reporters.accumulators.AvgAccumulator;
import org.perfcake.nreporting.reporters.accumulators.LastValueAccumulator;

/**
 * Reports average response time of all measure units
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class ResponseTimeReporter extends AbstractReporter {

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      Map<String, Object> result = new HashMap<>();
      result.put(Measurement.DEFAULT_RESULT, Double.valueOf(mu.getTotalTime()));
      accumulateResults(result);
   }

   @Override
   protected void doPublishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      Measurement m = new Measurement(Math.round(runInfo.getPercentage()), runInfo.getRunTime(), runInfo.getIteration());
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
      if (clazz.getName().equals(Double.class)) {
         return new AvgAccumulator();
      } else {
         return new LastValueAccumulator();
      }
   }

}
