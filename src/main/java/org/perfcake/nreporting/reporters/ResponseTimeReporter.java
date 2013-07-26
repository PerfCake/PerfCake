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

import java.util.concurrent.atomic.AtomicLong;

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

   // TODO replace manual counting with AvgAccumulator

   /**
    * Iterations observed by this reporter
    */
   private final AtomicLong iterations = new AtomicLong(0);

   /**
    * Total response time of all iterations
    */
   private final AtomicLong responseTime = new AtomicLong(0);

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      iterations.incrementAndGet();
      responseTime.addAndGet(mu.getTotalTime());
   }

   @Override
   protected void doPublishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      Measurement m = new Measurement(Math.round(runInfo.getPercentage()), runInfo.getRunTime(), runInfo.getIteration());
      if (responseTime.get() == 0 || iterations.get() == 0) {
         m.set(new Quantity<Double>(0d, "ms"));
      } else {
         m.set(new Quantity<Double>((double) responseTime.get() / iterations.get(), "ms"));
      }

      // TODO publish accumulated results

      d.report(m);
   }

   @Override
   protected void doReset() {
      iterations.set(0);
      responseTime.set(0);
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
