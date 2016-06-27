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

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.RunInfo;
import org.perfcake.TestSetup;
import org.perfcake.TestUtil;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.destinations.c3chart.C3ChartData;
import org.perfcake.reporting.destinations.c3chart.C3ChartDataFile;
import org.perfcake.reporting.destinations.c3chart.C3ChartFactory;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.util.ObjectFactory;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Random;

/**
 * Verifies integration of ResponseTimeHistogramReporter and ChartDestination.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "integration")
public class HistogramChartIntegrationTest extends TestSetup {

   @DataProvider(name = "attributes")
   public Object[][] attributesDataProvider() {
      return new Object[][] {
            { "*", false, true, 101, 5 },
            { "*, " + PerfCakeConst.WARM_UP_TAG, true, true, 142, 5 },
            { "perc*", false, false, 101, 5 },
            { "perc*, " + PerfCakeConst.WARM_UP_TAG, true, false, 142, 5 },
            { "Threads", false, true, 101, 2 },
            { "Threads, " + PerfCakeConst.WARM_UP_TAG, true, true, 142, 3 },
            { PerfCakeConst.WARM_UP_TAG, false, false, 0, 1 },
      };
   }

   @Test(dataProvider = "attributes")
   public void integrationTest(final String attributesRequired, final boolean hasWarmUp, final boolean hasThreadsAttribute, final int records, final int minAttributes) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, PerfCakeException, IOException {
      final String dir = "target/hdr-charts";
      final Properties reporterProperties = TestUtil.props("detail", "1", "precision", "1", "maxExpectedValue", "100", "correctionMode", "user",
            "expectedValue", "2");
      final Properties destinationProperties = TestUtil.props("yAxis", "HDR Response Time", "group", "hdr_resp", "name", "HDR Response Time (25 Threads)",
            "attributes", attributesRequired, "autoCombine", "false", "chartHeight", "1000", "outputDir", dir);

      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 10000));
      final Reporter r = (Reporter) ObjectFactory.summonInstance("org.perfcake.reporting.reporters.ResponseTimeHistogramReporter", reporterProperties);
      final Destination d = (Destination) ObjectFactory.summonInstance("org.perfcake.reporting.destinations.ChartDestination", destinationProperties);
      final Random rnd = new Random();

      r.setRunInfo(ri);
      r.registerDestination(d, new Period(PeriodType.ITERATION, 100));

      ri.addTag(PerfCakeConst.WARM_UP_TAG);
      ri.start();
      r.start();

      while (ri.isRunning()) {
         FakeMeasurementUnit mu = new FakeMeasurementUnit(ri.getNextIteration());
         mu.setTime(rnd.nextInt(10000) > 9990 ? rnd.nextInt(10) + 6 : 2);
         mu.startMeasure();
         mu.appendResult(PerfCakeConst.THREADS_TAG, "10");
         mu.stopMeasure();

         if (ri.getIteration() == 4000 && ri.hasTag(PerfCakeConst.WARM_UP_TAG)) {
            ri.removeTag(PerfCakeConst.WARM_UP_TAG);
            ri.reset();
            r.reset();
         }

         r.report(mu);
      }

      r.stop();

      final C3ChartDataFile c3desc = C3ChartFactory.readChartMetaData(dir);
      final C3ChartData c3data = C3ChartFactory.readChartData(dir);

      // there are always enough attributes
      Assert.assertTrue(c3desc.getChart().getAttributes().size() >= minAttributes);
      // expect perc_warmUp
      Assert.assertEquals(c3desc.getChart().getAttributes().stream().anyMatch(attribute -> attribute.startsWith("perc") && attribute.endsWith("_" + PerfCakeConst.WARM_UP_TAG)), attributesRequired.contains("*") && hasWarmUp);
      // expect Threads
      Assert.assertEquals(c3desc.getChart().getAttributes().contains("Threads"), hasThreadsAttribute);
      // expect Threads_warmUp
      Assert.assertEquals(c3desc.getChart().getAttributes().contains("Threads_" + PerfCakeConst.WARM_UP_TAG), hasThreadsAttribute && hasWarmUp);
      // expect correct number of records depending on whether warmUp was recorded
      Assert.assertEquals(c3data.getData().size(), records);

      FileUtils.deleteDirectory(new File(dir));
   }

}