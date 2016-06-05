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

   /*
         <reporter class="ResponseTimeHistogramReporter">
         <property name="detail" value="1" />
         <property name="precision" value="1" />
         <property name="maxExpectedValue" value="100" />
         <property name="correctionMode" value="user" />
         <property name="expectedValue" value="2" />
         <destination class="ConsoleDestination">
            <period type="time" value="1000"/>
         </destination>
         <destination class="ChartDestination">
            <period type="time" value="500"/>
            <property name="yAxis" value="HDR Response time [ms]"/>
            <property name="group" value="${perfcake.scenario}_hdr_resp"/>
            <property name="name" value="HDR Response Time (${threads:25} threads)"/>
            <property name="attributes" value="${attributes}"/>
            <property name="autoCombine" value="false" />
            <property name="chartHeight" value="1000" />
            <property name="outputDir" value="target/${perfcake.scenario}-charts"/>
         </destination>
      </reporter>

    */

   @DataProvider(name = "attributes")
   public Object[][] attributesDataProvider() {
      return new Object[][] {
            { "*", false, true, 101 },
      };
   }

   @Test(dataProvider = "attributes")
   public void integrationTest(final String attributesRequired, final boolean hasWarmUp, final boolean hasThreadsAttribute, final int records) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, PerfCakeException, IOException {
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

         if (ri.getIteration() == 400 && ri.hasTag(PerfCakeConst.WARM_UP_TAG)) {
            ri.removeTag(PerfCakeConst.WARM_UP_TAG);
            ri.reset();
            r.reset();
         }

         r.report(mu);
      }

      r.stop();

      final C3ChartDataFile c3desc = C3ChartFactory.readChartMetaData(dir);
      final C3ChartData c3data = C3ChartFactory.readChartData(dir);

      //C3Chart{baseName='hdr_resp20160605233203', name='HDR Response Time (25 Threads)', xAxis='Time', yAxis='HDR Response Time', xAxisType=TIME, attributes=[Time, Threads, perc0.000000000000, perc0.500000000000, perc0.750000000000, perc0.875000000000, perc0.937500000000, perc0.968750000000, perc0.984375000000, perc0.992187500000, perc0.996093750000, perc0.998046875000, perc0.999023437500,
      // perc0.999511718750, perc0.999755859375, perc0.999877929688, perc1.000000000000], group='hdr_resp'}

      Assert.assertTrue(c3desc.getChart().getAttributes().size() > 5); // there are always enough attributes
      Assert.assertEquals(c3desc.getChart().getAttributes().stream().anyMatch(attribute -> attribute.endsWith("_" + PerfCakeConst.WARM_UP_TAG)), hasWarmUp);
      Assert.assertEquals(c3desc.getChart().getAttributes().contains("Threads"), hasThreadsAttribute);
      Assert.assertEquals(c3data.getData().size(), records);

      FileUtils.deleteDirectory(new File(dir));
   }

}