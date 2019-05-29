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

import org.perfcake.RunInfo;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.DummyDestination;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class GeolocationReporterTest {

   @Test
   public void geoloactionTest() throws ReportingException {
      final RunInfo runInfo = new RunInfo(new Period(PeriodType.ITERATION, 10));
      final GeolocationReporter geoReporter = new GeolocationReporter();
      final DummyDestination destination = new DummyDestination();
      runInfo.start();
      geoReporter.setRunInfo(runInfo);
      geoReporter.reset();
      geoReporter.publishResult(PeriodType.ITERATION, destination);
      final Measurement m = destination.getLastMeasurement();

      Assert.assertNotNull(m.get("ip"));
      Assert.assertNotNull(m.get("hostname"));
      Assert.assertNotNull(m.get("city"));
      Assert.assertNotNull(m.get("region"));
      Assert.assertNotNull(m.get("country"));
      Assert.assertNotNull(m.get("lon"));
      Assert.assertNotNull(m.get("lat"));
   }
}