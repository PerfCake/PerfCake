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
package org.perfcake.reporting.destinations;

import org.perfcake.PerfCakeConst;
import org.perfcake.TestSetup;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.ObjectFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Properties;
import java.util.Scanner;

/**
 * Tests {@link org.perfcake.reporting.destinations.ChartDestination}.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class CsvDestinationTest {

   private static final Logger log = LogManager.getLogger(CsvDestinationTest.class);
   private static final long ITERATION = 12345;
   private static final String TIMESTAMP = String.valueOf(Calendar.getInstance().getTimeInMillis());
   private static final String DEFAULT_FILE_NAME = "perfcake-results-" + TIMESTAMP + ".csv";
   private File csvFile;

   @BeforeClass
   public void beforeClass() throws IOException {
      System.setProperty(PerfCakeConst.TIMESTAMP_PROPERTY, TIMESTAMP);
      final File csvOutputDir = new File(TestSetup.createTempDir("csvdestination"));
      csvOutputDir.deleteOnExit();
      csvFile = Paths.get(csvOutputDir.getAbsolutePath(), "perfcake-results-" + TIMESTAMP + ".csv").toFile();
   }

   @Test
   public void testDestinationReport() {
      final Properties destinationProperties = new Properties();
      destinationProperties.put("delimiter", ",");
      destinationProperties.put("path", csvFile.getAbsolutePath());

      prepareFile(csvFile);

      try {
         final CsvDestination destination = (CsvDestination) ObjectFactory.summonInstance(CsvDestination.class.getName(), destinationProperties);

         final Measurement measurement = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurement.set(new Quantity<Double>(1111.11, "it/s"));
         measurement.set("another", new Quantity<Double>(222.22, "ms"));

         destination.open();
         destination.report(measurement);
         destination.close();

         assertCSVFileContent(csvFile, "Time,Iterations," + Measurement.DEFAULT_RESULT + ",another\n34:17:36," + ITERATION + ",1111.11,222.22");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testDefaultProperties() throws IOException {
      final File defaultCSVFile = new File(DEFAULT_FILE_NAME);
      if (defaultCSVFile.exists()) {
         defaultCSVFile.delete();
      }

      try {
         final Properties destinationProperties = new Properties();
         final CsvDestination destination = (CsvDestination) ObjectFactory.summonInstance(CsvDestination.class.getName(), destinationProperties);

         Assert.assertEquals(destination.getPath(), DEFAULT_FILE_NAME);
         Assert.assertEquals(destination.getDelimiter(), ";");

         final Measurement measurement = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurement.set(new Quantity<Double>(1111.11, "it/s"));
         measurement.set("another", new Quantity<Double>(222.22, "ms"));

         destination.open();
         destination.report(measurement);
         destination.close();

         assertCSVFileContent(defaultCSVFile, "Time;Iterations;" + Measurement.DEFAULT_RESULT + ";another\n34:17:36;" + ITERATION + ";1111.11;222.22");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      } finally {
         defaultCSVFile.delete();
      }
   }

   @Test
   public void testPathChange() {
      final Properties destinationProperties = new Properties();
      destinationProperties.put("path", csvFile.getAbsolutePath());

      final String CHANGED_PATH = "second-path.csv";
      final File secondPath = new File(CHANGED_PATH);

      secondPath.deleteOnExit();
      if (secondPath.exists()) {
         secondPath.delete();
      }

      prepareFile(csvFile);
      try {
         final CsvDestination destination = (CsvDestination) ObjectFactory.summonInstance(CsvDestination.class.getName(), destinationProperties);

         final Measurement measurementWithoutDefault = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurementWithoutDefault.set("singleResult", new Quantity<Number>(100, "units"));

         destination.open();
         Assert.assertEquals(destination.getPath(), csvFile.getAbsolutePath());
         Assert.assertFalse(secondPath.exists());
         destination.report(measurementWithoutDefault);
         boolean except = false;
         try {
            destination.setPath(CHANGED_PATH);
         } catch (final UnsupportedOperationException expected) {
            except = true;
         }
         Assert.assertTrue(except);
         destination.close();
         destination.setPath(CHANGED_PATH);
         destination.open();
         Assert.assertEquals(destination.getPath(), CHANGED_PATH);
         destination.report(measurementWithoutDefault);
         Assert.assertTrue(secondPath.exists());
         destination.close();

         assertCSVFileContent(csvFile, "Time;Iterations;singleResult\n34:17:36;" + ITERATION + ";100");
         assertCSVFileContent(secondPath, "Time;Iterations;singleResult\n34:17:36;" + ITERATION + ";100");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testMultipleRecords() {
      final Properties destinationProperties = new Properties();
      destinationProperties.setProperty("path", csvFile.getAbsolutePath());

      prepareFile(csvFile);

      try {
         final CsvDestination destination = (CsvDestination) ObjectFactory.summonInstance(CsvDestination.class.getName(), destinationProperties);

         final Measurement measurement = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurement.set(new Quantity<Double>(1111.11, "it/s"));
         measurement.set("another", new Quantity<Double>(222.22, "ms"));

         final Measurement measurement2 = new Measurement(43, 123457000, ITERATION); // first iteration index is 0
         measurement2.set(new Quantity<Double>(2222.22, "it/s"));
         measurement2.set("another", new Quantity<Double>(333.33, "ms"));

         destination.open();
         destination.report(measurement);
         Thread.sleep(100);
         destination.report(measurement2);
         destination.close();

         assertCSVFileContent(csvFile, "Time;Iterations;" + Measurement.DEFAULT_RESULT + ";another\n34:17:36;" + ITERATION + ";1111.11;222.22\n34:17:37;" + (ITERATION + 1) + ";2222.22;333.33");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException | InterruptedException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testNoDefaultResultMeasurement() {
      final Properties destinationProperties = new Properties();
      destinationProperties.put("path", csvFile.getAbsolutePath());

      prepareFile(csvFile);

      try {
         final CsvDestination destination = (CsvDestination) ObjectFactory.summonInstance(CsvDestination.class.getName(), destinationProperties);

         final Measurement measurementWithoutDefault = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurementWithoutDefault.set("singleResult", new Quantity<Number>(100, "units"));

         destination.open();
         destination.report(measurementWithoutDefault);
         destination.close();

         assertCSVFileContent(csvFile, "Time;Iterations;singleResult\n34:17:36;" + ITERATION + ";100");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testStringResultMeasurement() {
      final Properties destinationProperties = new Properties();
      destinationProperties.put("path", csvFile.getAbsolutePath());

      prepareFile(csvFile);

      try {
         final CsvDestination destination = (CsvDestination) ObjectFactory.summonInstance(CsvDestination.class.getName(), destinationProperties);

         final Measurement measurementStringResult = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurementStringResult.set("StringValue");
         measurementStringResult.set("StringResult", "StringValue2");

         destination.open();
         destination.report(measurementStringResult);
         destination.close();

         assertCSVFileContent(csvFile, "Time;Iterations;" + Measurement.DEFAULT_RESULT + ";StringResult\n34:17:36;" + ITERATION + ";StringValue;StringValue2");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test(expectedExceptions = { ReportingException.class })
   public void testPathIsNotFile() throws ReportingException {
      final Properties destinationProperties = new Properties();
      destinationProperties.setProperty("path", "");
      try {
         final CsvDestination destination = (CsvDestination) ObjectFactory.summonInstance(CsvDestination.class.getName(), destinationProperties);

         final Measurement measurement = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurement.set(new Quantity<Double>(1111.11, "it/s"));
         measurement.set("another", new Quantity<Double>(222.22, "ms"));

         destination.open();
         destination.report(measurement);
         destination.close();
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testReadOnlyFile() throws IOException, ReportingException {
      final File readOnlyFile = Files.createTempFile("perfcake", "csvdestination-read-only.file").toFile();
      readOnlyFile.setReadOnly();
      readOnlyFile.deleteOnExit();

      final Properties destinationProperties = new Properties();
      destinationProperties.setProperty("path", readOnlyFile.getPath());

      try {
         final CsvDestination destination = (CsvDestination) ObjectFactory.summonInstance(CsvDestination.class.getName(), destinationProperties);

         final Measurement measurement = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
         measurement.set(new Quantity<Double>(1111.11, "it/s"));
         measurement.set("another", new Quantity<Double>(222.22, "ms"));

         destination.open();
         destination.report(measurement);
         destination.close();
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      } catch (final ReportingException re) {
         Assert.assertEquals(re.getMessage(), "Could not append a report to the file: " + readOnlyFile.getPath());
      } finally {
         readOnlyFile.delete();
      }
   }

   @Test
   public void testPrefixAndSuffix() throws IOException, ReportingException {
      final CsvDestination dest = new CsvDestination();
      final File outf = File.createTempFile("perfcake", "csvdestination-prefixsuffix");
      outf.deleteOnExit();

      dest.setPath(outf.getAbsolutePath());
      dest.setLinePrefix("[ ");
      dest.setLineSuffix("],");
      dest.setAppendStrategy(CsvDestination.AppendStrategy.OVERWRITE);

      final Measurement m = new Measurement(90, 1000, 20);
      m.set("hello");

      dest.open();
      dest.report(m);
      dest.close();

      assertCSVFileContent(outf, "Time;Iterations;Result\n[ 0:00:01;21;hello],");

      delete(outf);
   }

   @Test
   public void testSkipHeaders() throws IOException, ReportingException {
      final CsvDestination dest = new CsvDestination();
      final File outf = File.createTempFile("perfcake", "csvdestination-skipheaders");
      outf.deleteOnExit();

      dest.setPath(outf.getAbsolutePath());
      dest.setSkipHeader(true);
      dest.setAppendStrategy(CsvDestination.AppendStrategy.OVERWRITE);

      final Measurement m = new Measurement(90, 1000, 20);
      m.set("hello");

      dest.open();
      dest.report(m);
      dest.close();

      assertCSVFileContent(outf, "0:00:01;21;hello");

      delete(outf);
   }

   @Test
   public void testCustomDelimiterAndLineBreak() throws IOException, ReportingException {
      final CsvDestination dest = new CsvDestination();
      final File outf = File.createTempFile("perfcake", "csvdestination-delimbreak");
      outf.deleteOnExit();

      dest.setPath(outf.getAbsolutePath());
      dest.setDelimiter("-_-");
      dest.setLineBreak("#*#");
      dest.setAppendStrategy(CsvDestination.AppendStrategy.OVERWRITE);

      final Measurement m = new Measurement(90, 1000, 20);
      m.set("hello");

      dest.open();
      dest.report(m);
      dest.close();

      assertCSVFileContent(outf, "Time-_-Iterations-_-Result#*#0:00:01-_-21-_-hello#*#");

      delete(outf);
   }

   @Test
   public void testFileNumbering() throws IOException, ReportingException {
      final CsvDestination dest = new CsvDestination();
      final File outf = File.createTempFile("perfcake", "file-numbering.csv");
      final File outf1 = new File(outf.getAbsolutePath().replace(".", ".1."));

      outf.deleteOnExit();
      outf1.deleteOnExit();

      dest.setPath(outf.getAbsolutePath());
      final Measurement m = new Measurement(90, 1000, 20);
      m.set("hello");

      dest.open();
      dest.report(m);
      dest.close();

      Assert.assertTrue(outf1.exists());

      delete(outf);
      delete(outf1);
   }

   @Test
   public void testFileNumberingWithoutExt() throws IOException, ReportingException {
      final CsvDestination dest = new CsvDestination();
      final File outf = File.createTempFile("perfcake", "file-numbering-ext");
      final File outf1 = new File(outf + ".1");

      outf.deleteOnExit();
      outf1.deleteOnExit();

      dest.setPath(outf.getAbsolutePath());
      final Measurement m = new Measurement(90, 1000, 20);
      m.set("hello");

      dest.open();
      dest.report(m);
      dest.close();

      Assert.assertTrue(outf1.exists());

      delete(outf);
      delete(outf1);
   }

   private void assertCSVFileContent(final File file, final String expected) {
      try (Scanner scanner = new Scanner(file).useDelimiter("\\Z")) {
         Assert.assertEquals(scanner.next(), expected, "CSV file's content");
      } catch (final FileNotFoundException fnfe) {
         fnfe.printStackTrace();
         Assert.fail(fnfe.getMessage());
      }
   }

   private void prepareFile(final File file) {
      if (!file.getParentFile().exists()) {
         file.getParentFile().mkdirs();
      }
      if (file.exists()) {
         file.delete();
      }
   }

   private void delete(final File f) {
      if (!f.delete()) {
         log.warn(String.format("Temporary file %s could not be deleted.", f.getAbsolutePath()));
      }
   }
}
