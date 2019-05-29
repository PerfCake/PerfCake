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
import org.perfcake.reporting.FakeMeasurementUnit;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportManager;
import org.perfcake.reporting.destination.DummyDestination;
import org.perfcake.util.ObjectFactory;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.PercentileIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class ResponseTimeHistogramTest {

   private static final Logger log = LogManager.getLogger(ResponseTimeHistogramTest.class);

   private void display(Histogram histogram, long avg) {
      String valueFormatString = "%12.3f";
      String percentileFormatString = "%d";

      PercentileIterator pi = new PercentileIterator(histogram.copyCorrectedForCoordinatedOmission(avg), 1);
      pi.reset(2);

      JsonObject o = new JsonObject();

      while (pi.hasNext()) {
         HistogramIterationValue val = pi.next();

         String key = String.format(Locale.US, percentileFormatString, val.getPercentileLevelIteratedTo() / 100d);
         String value = String.format(Locale.US, valueFormatString, val.getValueIteratedTo());

         o.put(key, value);

      }

      System.out.println(o.toString());
   }

   @Test
   public void offModeTest() throws Exception {
      final Properties props = new Properties();
      props.setProperty("correctionMode", "off");
      props.setProperty("precision", "3");
      props.setProperty("prefix", "p");
      Measurement m = runTest(props);

      final List<Long> res = new ArrayList<>();

      m.getAll().forEach((k, v) -> {
         if (k.startsWith("p0.998") || k.startsWith("p0.999") || k.startsWith("p1.000")) {
            res.add(Long.valueOf((String) v));
         }
      });

      Assert.assertEquals((long) res.get(0), 2L);
      Assert.assertEquals((long) res.get(1), 2L);
      Assert.assertEquals((long) res.get(2), 100L);
      Assert.assertEquals((long) res.get(3), 100L);
   }

   @Test
   public void autoModeTest() throws Exception {
      final Properties props = new Properties();
      props.setProperty("correctionMode", "auto");
      props.setProperty("precision", "2");
      props.setProperty("prefix", "p");
      Measurement m = runTest(props);

      final List<Long> res = new ArrayList<>();

      m.getAll().forEach((k, v) -> {
         if (k.startsWith("p0.953") || k.startsWith("p0.968") || k.startsWith("p0.976") || k.startsWith("p0.999")) {
            res.add(Long.valueOf((String) v));
         }
      });

      Assert.assertEquals((long) res.get(0), 2L);
      Assert.assertEquals((long) res.get(1), 36L);
      Assert.assertEquals((long) res.get(2), 52L);
      Assert.assertEquals((long) res.get(3), 98L);
      Assert.assertEquals((long) res.get(4), 100L);
   }

   @Test
   public void maxExpectedTest() throws Exception {
      final Properties props = new Properties();
      props.setProperty("correctionMode", "auto");
      props.setProperty("precision", "1");
      props.setProperty("detail", "1");
      props.setProperty("prefix", "p");
      props.setProperty("maxExpectedValue", "1000");
      Measurement m = runTest(props);

      final List<Long> res = new ArrayList<>();

      m.getAll().forEach((k, v) -> {
         if (k.startsWith("p0.937") || k.startsWith("p0.968") || k.startsWith("p0.984") || k.startsWith("p0.992") || k.startsWith("p0.996") || k.startsWith("p0.998") || k.startsWith("p0.999")) {
            res.add(Long.valueOf((String) v));
         }
      });

      int i = 0;
      Assert.assertEquals((long) res.get(i++), 2L);
      Assert.assertEquals((long) res.get(i++), 39L);
      Assert.assertEquals((long) res.get(i++), 71L);
      Assert.assertEquals((long) res.get(i++), 87L);
      Assert.assertEquals((long) res.get(i++), 95L);
      Assert.assertEquals((long) res.get(i++), 99L);
      Assert.assertEquals((long) res.get(i), 103L);
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

      final List<Long> res = new ArrayList<>();

      m.getAll().forEach((k, v) -> {
         if (k.startsWith("p0.937") || k.startsWith("p0.968") || k.startsWith("p0.984") || k.startsWith("p0.992") || k.startsWith("p0.999")) {
            res.add(Long.valueOf((String) v));
         }
      });

      Assert.assertEquals((long) res.get(0), 2);
      Assert.assertEquals((long) res.get(1), 35);
      Assert.assertEquals((long) res.get(2), 68);
      Assert.assertEquals((long) res.get(3), 84);
      Assert.assertEquals((long) res.get(4), 98);
      Assert.assertEquals((long) res.get(5), 99);
      Assert.assertEquals((long) res.get(6), 100);
   }

   @Test(enabled = false)
   public void testResultFiltering() {
      final SortedMap<String, String> results = new TreeMap<>();

      results.put("perc0.000000000000", "2");
      results.put("perc0.500000000000", "2");
      results.put("perc0.750000000000", "2");
      results.put("perc0.875000000000", "2");
      results.put("perc0.937500000000", "2");
      results.put("perc0.968750000000", "2");
      results.put("perc0.984375000000", "2");
      results.put("perc0.992187500000", "2");
      results.put("perc0.996093750000", "2");
      results.put("perc0.998046875000", "3");
      results.put("perc0.999023437500", "3");
      results.put("perc0.999511718750", "4");
      results.put("perc0.999755859375", "5");
      results.put("perc0.999877929688", "5");
      results.put("perc0.999938964844", "6");
      results.put("perc1.000000000000", "6");

      String lastKey = null;
      String lastValue = null;
      SortedMap<String, String> output = new TreeMap<>();
      for (Map.Entry<String, String> entry : results.entrySet()) {
         if (lastValue != null) {
            if (!entry.getValue().equals(lastValue)) {
               output.put(lastKey, lastValue);
            } else if (entry.getKey() == results.lastKey()) {
               output.put(entry.getKey(), entry.getValue());
            }
         }
         lastKey = entry.getKey();
         lastValue = entry.getValue();
      }

      System.out.println(output);

   }

   private Measurement runTest(final Properties props) throws Exception {
      final ReportManager rm = new ReportManager();
      final RunInfo ri = new RunInfo(new Period(PeriodType.ITERATION, 1000));

      final Reporter r = (Reporter) ObjectFactory.summonInstance("org.perfcake.reporting.reporter.ResponseTimeHistogramReporter", props);
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

      Thread.sleep(500);

      Measurement m = d.getLastMeasurement();

      if (log.isDebugEnabled()) {
         m.getAll().forEach((k, v) -> {
            log.debug(k + ": " + v);
         });
      }

      return m;
   }
}
