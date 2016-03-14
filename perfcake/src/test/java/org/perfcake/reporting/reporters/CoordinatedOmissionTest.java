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

import org.perfcake.RunInfo;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.FakeMeasurementUnit;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.util.ObjectFactory;

import org.HdrHistogram.ConcurrentDoubleHistogram;
import org.HdrHistogram.DoubleHistogram;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CoordinatedOmissionTest {

   private static final Logger log = LogManager.getLogger(CoordinatedOmissionTest.class);

   @Test
   public void offModeTest() throws Exception {
      final Properties props = new Properties();
      props.setProperty("correctionMode", "off");
      props.setProperty("precision", "3");
      props.setProperty("prefix", "p");
      Measurement m = runTest(props);

      final List<Double> res = new ArrayList<>();

      m.getAll().forEach((k, v) -> {
         if (k.startsWith("p0.998") || k.startsWith("p0.999") || k.startsWith("p1.000")) {
            res.add(Double.valueOf((String) v));
         }
      });

      Assert.assertEquals(res.get(0), 2d);
      Assert.assertEquals(res.get(1), 2d);
      Assert.assertEquals(res.get(2), 100.48d);
      Assert.assertEquals(res.get(3), 100.48d);
   }

   @Test
   public void autoModeTest() throws Exception {
      final Properties props = new Properties();
      props.setProperty("correctionMode", "auto");
      props.setProperty("precision", "2");
      props.setProperty("prefix", "p");
      Measurement m = runTest(props);

      final List<Double> res = new ArrayList<>();

      m.getAll().forEach((k, v) -> {
         if (k.startsWith("p0.953") || k.startsWith("p0.968") || k.startsWith("p0.976") || k.startsWith("p0.999")) {
            res.add(Double.valueOf((String) v));
         }
      });

      Assert.assertEquals(res.get(0), 2d);
      Assert.assertEquals(res.get(1), 33.48d);
      Assert.assertEquals(res.get(2), 50.23d);
      Assert.assertEquals(res.get(3), 98.48d);
      Assert.assertEquals(res.get(4), 100.48d);
   }

   @Test
   public void userModeTest() throws Exception {
      final Properties props = new Properties();
      props.setProperty("correctionMode", "user");
      props.setProperty("correction", "1.5");
      props.setProperty("precision", "2");
      props.setProperty("detail", "1");
      props.setProperty("prefix", "p");
      Measurement m = runTest(props);

      final List<Double> res = new ArrayList<>();

      m.getAll().forEach((k, v) -> {
         if (k.startsWith("p0.937") || k.startsWith("p0.968") || k.startsWith("p0.984") || k.startsWith("p0.999")) {
            res.add(Double.valueOf((String) v));
         }
      });

      Assert.assertEquals(res.get(0), 2d);
      Assert.assertEquals(res.get(1), 50.98d);
      Assert.assertEquals(res.get(2), 76.48d);
      Assert.assertEquals(res.get(3), 98.98d);
      Assert.assertEquals(res.get(4), 100.48d);
   }

   private Measurement runTest(final Properties props) throws Exception {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));

      final Reporter r = (Reporter) ObjectFactory.summonInstance("org.perfcake.reporting.reporters.CoordinatedOmissionReporter", props);
      final DummyDestination d = new DummyDestination();

      rm.setRunInfo(ri);
      rm.registerReporter(r);
      r.registerDestination(d, new Period(PeriodType.ITERATION, 1000));

      rm.start();

      for (int i = 0; i < 1000; i++) {
         FakeMeasurementUnit mu = new FakeMeasurementUnit(i);
         mu.setTime(i == 999 ? 100 : 2);
         rm.newMeasurementUnit(); // just to increase counter in RunInfo and make the test progress
         mu.startMeasure();
         mu.stopMeasure();
         rm.report(mu);
      }

      rm.stop();

      Measurement m = d.getLastMeasurement();

      if (log.isDebugEnabled()) {
         m.getAll().forEach((k, v) -> {
            log.debug(k + ": " + v);
         });
      }

      return m;
   }
}
