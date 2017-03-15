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
package org.perfcake.reporting.reporter;

import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;

/**
 * Reports the average number of iterations that were completed since the test beginning per second.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class IterationsPerSecondReporter extends AbstractReporter {
   @Override
   protected void doReset() {
      // nothing is needed here
   }

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      // nothing is needed here
   }

   @Override
   public Measurement computeMeasurement(final PeriodType periodType) throws ReportingException {
      final Measurement m = newMeasurement();
      m.set(Measurement.DEFAULT_RESULT, new Quantity<>(1000.0 * runInfo.getIteration() / runInfo.getRunTime(), "iterations/s"));
      return m;
   }
}
