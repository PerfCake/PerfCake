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
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.reporters.accumulators.Accumulator;
import org.perfcake.reporting.reporters.accumulators.LastValueAccumulator;

import org.apache.log4j.Logger;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class DummyReporter extends AbstractReporter {

   private String lastMethod = null;
   private long lastPercentage = -1;

   /**
    * The reporter's loger.
    */
   private static final Logger log = Logger.getLogger(DummyReporter.class);

   @Override
   protected void doReport(final MeasurementUnit mu) throws ReportingException {
      if (log.isDebugEnabled()) {
         log.debug("Reporting " + mu.toString());
      }
      lastMethod = "doReport";
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination d) throws ReportingException {
      lastPercentage = newMeasurement().getPercentage();
      if (log.isDebugEnabled()) {
         log.debug("Publishing results...");
      }
      lastMethod = "doPublishResult";
   }

   @SuppressWarnings("rawtypes")
   @Override
   protected Accumulator getAccumulator(final String key, final Class clazz) {
      return new LastValueAccumulator();
   }

   @Override
   protected void doReset() {
      // nothing needed
      lastMethod = "doReset";
   }

   public String getLastMethod() {
      return lastMethod;
   }

   public long getLastPercentage() {
      return lastPercentage;
   }
}
