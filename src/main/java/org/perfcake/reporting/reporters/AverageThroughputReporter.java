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

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;

/**
 * The reporter is able to report the current average throughput (in the means of the number of iterations per second)
 * from the beginning of the measuring to the moment when the results are published.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 *
 */
public class AverageThroughputReporter extends AbstractReporter {

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      // nothing to do
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      final Measurement m = newMeasurement();
      Quantity<Double> q = new Quantity<>(1000d * getMaxIteration() / runInfo.getRunTime(), "iterations/s");
      m.set(q);
      d.report(m);
   }

   @Override
   protected void doReset() {
      // nothing needed, the parent does the job
   }
}
