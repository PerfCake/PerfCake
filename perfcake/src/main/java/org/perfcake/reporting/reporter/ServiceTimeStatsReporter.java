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

import org.perfcake.reporting.MeasurementUnit;

/**
 * Reports statistics of service time. This is the time between enqueuing the sender task and its completion.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @see StatsReporter
 */
public class ServiceTimeStatsReporter extends StatsReporter {

   @Override
   protected Double computeResult(final MeasurementUnit measurementUnit) {
      return measurementUnit.getServiceTime();
   }

   @Override
   protected String getResultUnit() {
      return "ms";
   }
}
