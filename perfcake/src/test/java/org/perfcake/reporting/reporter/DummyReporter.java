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

import org.perfcake.PerfCakeConst;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.Destination;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A reporter for testing purposes.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class DummyReporter extends AbstractReporter {

   private String lastMethod = null;
   private long lastPercentage = -1;
   private long lastFailures = -1;

   /**
    * The reporter's loger.
    */
   private static final Logger log = LogManager.getLogger(DummyReporter.class);

   @Override
   protected void doReport(final MeasurementUnit measurementUnit) throws ReportingException {
      if (log.isDebugEnabled()) {
         log.debug("Reporting " + measurementUnit.toString());
      }
      lastMethod = "doReport";
   }

   @Override
   public void publishResult(final PeriodType periodType, final Destination destination) throws ReportingException {
      lastPercentage = newMeasurement().getPercentage();
      if (log.isDebugEnabled()) {
         log.debug("Publishing results...");
      }
      lastMethod = "doPublishResult";

      final Long failures = (Long) getAccumulatedResult(PerfCakeConst.FAILURES_TAG);
      lastFailures = (failures == null ? 0 : failures);
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

   public long getLastFailures() {
      return lastFailures;
   }
}
