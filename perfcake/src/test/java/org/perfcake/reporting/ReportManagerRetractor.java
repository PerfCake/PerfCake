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
package org.perfcake.reporting;

/**
 * Serves as a helper to access the internals of a {@link ReportManager}.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class ReportManagerRetractor {

   /**
    * The {@link ReportManager} to be retracted.
    */
   private ReportManager reportManager;

   /**
    * Creates an instance of the {@link ReportManager} retractpr.
    *
    * @param reportManager
    *       The {@link ReportManager} to be retracted.
    */
   public ReportManagerRetractor(final ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   /**
    * Gets the current {@link ReportManager}.
    *
    * @return The current {@link ReportManager}.
    */
   public ReportManager getReportManager() {
      return reportManager;
   }

   /**
    * Gets the current {@link Thread} used for periodic ticking.
    *
    * @return The current {@link Thread} used for periodic ticking.
    */
   public Thread getPeriodicThread() {
      return reportManager.getPeriodicThread();
   }
}
