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
package org.perfcake.reporting.destinations;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.common.PeriodType;
import org.perfcake.message.sender.TestSender;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.c3chart.C3ChartData;
import org.perfcake.reporting.reporters.Reporter;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;
import org.perfcake.util.StringTemplate;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import io.vertx.core.json.JsonArray;

/**
 * Tests {@link ChartDestination}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "unit" })
public class ChartDestinationTest extends TestSetup {

   private static final Logger log = LogManager.getLogger(ChartDestinationTest.class);

   @Test
   public void basicGroupNameTest() throws Exception {
      final ChartDestination cd = new ChartDestination();
      testGroupName(cd, "", "default");
      testGroupName(cd, "a", "a");
      testGroupName(cd, "1", "_1");
      testGroupName(cd, "group1", "group1");
      testGroupName(cd, "group.1", "group_1");
      testGroupName(cd, "group 1", "group_1");
      testGroupName(cd, "1group", "_1group");
      testGroupName(cd, "1.group", "_1_group");
      testGroupName(cd, "1. group", "_1__group");
      testGroupName(cd, "group one", "group_one");
   }

   private void testGroupName(final ChartDestination cd, final String setGroup, final String expectedGroup) {
      cd.setGroup(setGroup);
      Assert.assertEquals(cd.getGroup(), expectedGroup);
   }

   @Test
   public void basicTest() throws Exception {
      final String tempDir = TestSetup.createTempDir("test-chart-basic");
      log.info("Created temp directory for chart: " + tempDir);

      final ChartDestination cd = new ChartDestination();
      final ChartDestination cd2 = new ChartDestination();
      cd.setOutputDir(tempDir);
      cd2.setOutputDir(tempDir);
      cd.setXAxis("Time of test");
      cd2.setXAxis("Time of test");
      cd.setYAxis("Iterations per second");
      cd2.setYAxis("Iterations per second");
      cd2.setxAxisType(PeriodType.ITERATION);
      cd.setName("Statistics " + (new SimpleDateFormat("HHmmss")).format(new Date()));
      cd2.setName("Performance " + (new SimpleDateFormat("HHmmss")).format(new Date()));
      cd.setGroup("stats");
      cd2.setGroup("perf");
      cd.setAttributes("Average, Result, warmUp");
      cd2.setAttributes("Average, Result, warmUp");

      cd.open();
      cd2.open();

      long base = System.currentTimeMillis();
      final Random rnd = new Random();

      Measurement m = new Measurement(10, System.currentTimeMillis() - base, 1);
      m.set(10.3 + rnd.nextDouble());
      m.set("Average", 9.8 + rnd.nextDouble());
      m.set("warmUp", true);
      cd.report(m);
      cd2.report(m);

      Thread.sleep(100);

      m = new Measurement(10, System.currentTimeMillis() - base, 2);
      m.set(10.3 + rnd.nextDouble());
      m.set("Average", 9.8 + rnd.nextDouble());
      m.set("warmUp", true);
      cd.report(m);
      cd2.report(m);

      Thread.sleep(100);

      m = new Measurement(10, System.currentTimeMillis() - base, 3);
      m.set(10.3 + rnd.nextDouble());
      m.set("Average", 9.8 + rnd.nextDouble());
      m.set("warmUp", true);
      cd.report(m);
      cd2.report(m);

      Thread.sleep(100);

      base = System.currentTimeMillis();
      m = new Measurement(10, System.currentTimeMillis() - base, 1);
      m.set(10.3 + rnd.nextDouble());
      m.set("Average", 9.8 + rnd.nextDouble());
      m.set("warmUp", false);
      cd.report(m);
      cd2.report(m);

      Thread.sleep(100);

      m = new Measurement(13, System.currentTimeMillis() - base, 2);
      m.set(11.1 + rnd.nextDouble());
      m.set("Average", 9.1 + rnd.nextDouble());
      m.set("warmUp", false);
      cd.report(m);
      cd2.report(m);

      Thread.sleep(2000);

      m = new Measurement(100, System.currentTimeMillis() - base, 10);
      m.set(9.2 + rnd.nextDouble());
      m.set("Average", 9.0 + rnd.nextDouble());
      m.set("warmUp", false);
      cd.report(m);
      cd2.report(m);

      cd.close();
      cd2.close();

      final Path dir = Paths.get(tempDir);

      verifyBasicFiles(Paths.get(tempDir));

      Assert.assertTrue(dir.resolve(Paths.get("data", "stats" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "stats" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".json")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "stats" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".html")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "perf" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "perf" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".json")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "perf" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".html")).toFile().exists());

      final C3ChartData data = new C3ChartData(getBaseName(dir), dir);
      Assert.assertEquals(data.getData().get(0).size(), 5);

      try {
         Assert.assertEquals((int) data.getData().get(0).getInteger(0), 1);
         Assert.assertNull(data.getData().get(0).getValue(1));
         Assert.assertNull(data.getData().get(0).getValue(2));
         Assert.assertNotNull(data.getData().get(0).getDouble(3));
         Assert.assertNotNull(data.getData().get(0).getDouble(4));

         Assert.assertEquals((int) data.getData().get(1).getInteger(0), 2);
         Assert.assertEquals((int) data.getData().get(2).getInteger(0), 3);

         Assert.assertEquals((int) data.getData().get(3).getInteger(0), 1);
         Assert.assertNotNull(data.getData().get(3).getDouble(1));
         Assert.assertNotNull(data.getData().get(3).getDouble(2));
         Assert.assertNull(data.getData().get(3).getValue(3));
         Assert.assertNull(data.getData().get(3).getValue(4));

         Assert.assertEquals((int) data.getData().get(4).getInteger(0), 2);
         Assert.assertEquals((int) data.getData().get(5).getInteger(0), 10);
      } catch (ClassCastException cce) {
         Assert.fail("Chart array does not contain expected data. Should be [int, null/double, null/double, double/null, double/null]. " + cce);
      }

      FileUtils.deleteDirectory(dir.toFile());
   }

   @Test
   public void iterationScenarioTest() throws Exception {
      final Scenario scenario;

      System.setProperty(PerfCakeConst.SCENARIO_PROPERTY, "1chart-scenario$");
      TestSender.resetCounter();

      scenario = ScenarioLoader.load("test-scenario-chart");
      scenario.init();
      scenario.run();
      scenario.close();

      final ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      final Reporter reporter = retractor.getReportManager().getReporters().iterator().next();
      ChartDestination chartDestination = null;
      for (final Destination d : reporter.getDestinations()) {
         log.info(d.toString());
         if (d instanceof ChartDestination) {
            chartDestination = (ChartDestination) d;
            break;
         }
      }

      final String correctGroup = "_1chart_scenario__throughput";

      Assert.assertNotNull(chartDestination);
      Assert.assertEquals(chartDestination.getGroup(), correctGroup);
      Assert.assertEquals(TestSender.getCounter(), 1_000_000);

      final Path dir = Paths.get("target/test-chart");

      verifyBasicFiles(dir);

      Assert.assertTrue(dir.resolve(Paths.get("data", correctGroup + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".json")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", correctGroup + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", correctGroup + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".html")).toFile().exists());

      final C3ChartData data = new C3ChartData(getBaseName(dir), dir);
      Assert.assertEquals(data.getData().get(0).size(), 5);

      JsonArray array = data.getData().get(0);
      try {
         Assert.assertNotNull(array.getInteger(0));
         Assert.assertNotNull(array.getDouble(1));
         Assert.assertNotNull(array.getDouble(2));
         Assert.assertNotNull(array.getDouble(3));
         Assert.assertNotNull(array.getDouble(4));
      } catch (ClassCastException cce) {
         Assert.fail("Chart array does not contain expected data. Should be [int, double, double, double, double]. " + cce);
      }

      FileUtils.deleteDirectory(dir.toFile());
   }

   @Test
   public void combinedChartsTest() throws Exception {
      final Scenario scenario;

      System.setProperty(PerfCakeConst.SCENARIO_PROPERTY, "default");
      TestSender.resetCounter();

      scenario = ScenarioLoader.load("test-scenario-chart");

      scenario.init();
      final String origTime = System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY);
      scenario.run();
      scenario.close();

      // change the time to get different timestamp and different data files
      System.setProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY, String.valueOf(Long.parseLong(System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY)) + 10));

      scenario.init();
      final String newTime = System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY);
      scenario.run();
      scenario.close();

      final Path dir = Paths.get("target/test-chart");

      verifyBasicFiles(dir);

      Assert.assertTrue(dir.resolve(Paths.get("data", "default_throughput" + origTime + ".json")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "default_throughput" + origTime + ".js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "default_throughput" + origTime + ".html")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "default_throughput" + newTime + ".json")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "default_throughput" + newTime + ".js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "default_throughput" + newTime + ".html")).toFile().exists());
      int dataArrays = dir.resolve(Paths.get("data")).toFile().listFiles((directory, name) -> name.startsWith("combined_") && name.endsWith(".js")).length;

      Assert.assertEquals(dataArrays, 4);

      FileUtils.deleteDirectory(dir.toFile());
   }

   @Test
   public void dynamicAttributesTest() throws PerfCakeException, InterruptedException, IOException {
      final String tempDir = TestSetup.createTempDir("test-chart-dyna");
      log.info("Created temp directory for chart: " + tempDir);

      final ChartDestination cd = new ChartDestination();
      cd.setOutputDir(tempDir);
      cd.setXAxis("Time of test");
      cd.setYAxis("Iterations per second");
      cd.setName("Statistics " + (new SimpleDateFormat("HHmmss")).format(new Date()));
      cd.setGroup("stats");
      cd.setAttributes("Average, Result, pref*, other, warmUp");

      cd.open();

      long base = System.currentTimeMillis();
      final Random rnd = new Random();
      final Set<String> dynaAttrs = new HashSet<>();


      for (int i = 1; i < 5; i++) {
         Measurement m = new Measurement(i * 10, System.currentTimeMillis() - base, i);
         m.set(10.3 + rnd.nextDouble());
         m.set("Average", 9.8 + rnd.nextDouble());
         m.set("warmUp", true);
         String attr = "pref" + rnd.nextInt(10);
         dynaAttrs.add(attr);
         dynaAttrs.add(attr + "_" + PerfCakeConst.WARM_UP_TAG);
         m.set(attr, rnd.nextInt(100));
         cd.report(m);
         Thread.sleep(10);
      }

      base = System.currentTimeMillis();
      for (int i = 5; i <= 10; i++) {
         Measurement m = new Measurement((i - 4) * 10, System.currentTimeMillis() - base, i - 4);
         m.set(10.3 + rnd.nextDouble());
         m.set("Average", 9.8 + rnd.nextDouble());
         m.set("warmUp", false);
         String attr = "pref" + rnd.nextInt(10);
         dynaAttrs.add(attr);
         m.set(attr, rnd.nextInt(100));
         cd.report(m);
         Thread.sleep(10);
      }

      final Path tempPath = Paths.get(tempDir);
      Assert.assertFalse(tempPath.resolve(Paths.get("src")).toFile().exists());

      cd.close();

      log.info("Final attributes recorded: " + cd.getAttributesAsList());

      Assert.assertEquals(cd.getAttributesAsList().size(), dynaAttrs.size() + 4);
      dynaAttrs.forEach(attr -> {
         Assert.assertTrue(cd.getAttributesAsList().contains(attr));
      });

      verifyBasicFiles(tempPath);

      final C3ChartData data = new C3ChartData(getBaseName(tempPath), tempPath);
      Assert.assertEquals(data.getData().size(), 10);

      JsonArray array = data.getData().get(0);
      Assert.assertEquals(array.size(), dynaAttrs.size() + 5); // 5 = Time, Average, Result, Average_warmUp, Result_warmUp
      try {
         array.getInteger(0);
         array.getDouble(1);
         array.getDouble(2);
      } catch (ClassCastException cce) {
         Assert.fail("Chart array does not contain expected data. Should be [int, double, double, null..., int, null...]. " + cce);
      }

      System.out.println(array);

      int notNulls = 0;
      for (int i = 5; i < array.size(); i++) {
         if (array.getValue(i) != null) {
            notNulls++;
         }
      }
      Assert.assertEquals(notNulls, 1);

     // FileUtils.deleteDirectory(tempPath.toFile());
   }

   @Test(enabled = false) // manual test so far
   public void testHdrChart() throws PerfCakeException, IOException {
      final Scenario scenario;

      System.setProperty(PerfCakeConst.SCENARIO_PROPERTY, "default");
      System.setProperty("attributes", "*");
      TestSender.resetCounter();

      scenario = ScenarioLoader.load("test-hdr-chart");

      scenario.init();
      final String origTime = System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY);
      scenario.run();
      scenario.close();

      final Path dir = Paths.get("target/default-charts");

      FileUtils.deleteDirectory(dir.toFile());
   }

   /**
    * Derive the chart base name from the path. Works only for directories with a single chart.
    *
    * @param tempPath Path to the directory with the chart.
    * @return The chart's base name.
    */
   private String getBaseName(final Path tempPath) {
      String baseName = tempPath.resolve("data").toFile().list()[0];
      baseName = baseName.substring(0, baseName.lastIndexOf("."));

      return baseName;
   }

   private void verifyBasicFiles(final Path dir) {
      Assert.assertTrue(dir.resolve(Paths.get("data")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("src")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("index.html")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("src", "report.js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("src", "report.css")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("src", "c3.min.css")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("src", "c3.min.js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("src", "d3.v3.min.js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("src", "favicon.svg")).toFile().exists());
   }

}
