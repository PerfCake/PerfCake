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

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Properties;
import java.util.Scanner;

import org.perfcake.PerfCakeConst;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.util.ObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CSVDestinationTest {

   private File csvFile, defaultCSVFile;
   private Properties destinationProperties;
   private Measurement measurement;
   private Measurement measurement2;
   private Measurement measurementWithoutDefault;
   private Measurement measurementStringResult;
   private static final long ITERATION = 12345;
   private static final String testOutputDir = "test-output";
   private File csvOutputPath = new File(testOutputDir);
   private static final String PATH = testOutputDir + "/out.csv";
   private static final String TIMESTAMP = String.valueOf(Calendar.getInstance().getTimeInMillis());
   private static final String DEFAULT_PATH = "perfcake-results-" + TIMESTAMP + ".csv";

   @BeforeClass
   public void beforeClass() {
      System.setProperty(PerfCakeConst.TIMESTAMP_PROPERTY, TIMESTAMP);

      measurement = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
      measurement.set(new Quantity<Double>(1111.11, "it/s"));
      measurement.set("another", new Quantity<Double>(222.22, "ms"));

      measurement2 = new Measurement(43, 123457000, ITERATION); // first iteration index is 0
      measurement2.set(new Quantity<Double>(2222.22, "it/s"));
      measurement2.set("another", new Quantity<Double>(333.33, "ms"));

      measurementWithoutDefault = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
      measurementWithoutDefault.set("singleResult", new Quantity<Number>(100, "units"));

      measurementStringResult = new Measurement(42, 123456000, ITERATION - 1); // first iteration index is 0
      measurementStringResult.set("StringValue");
      measurementStringResult.set("StringResult", "StringValue2");

      if (!csvOutputPath.exists()) {
         csvOutputPath.mkdir();
      }

      csvFile = new File(csvOutputPath, "out.csv");
      defaultCSVFile = new File(DEFAULT_PATH);
   }

   @BeforeMethod
   public void beforeMethod() {
      destinationProperties = new Properties();

      if (csvFile.exists()) {
         csvFile.delete();
      }

      if (defaultCSVFile.exists()) {
         defaultCSVFile.delete();
      }
   }

   @Test
   public void testDestinationReport() {
      destinationProperties.put("delimiter", ",");
      destinationProperties.put("path", PATH);
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.open();
         destination.report(measurement);
         destination.close();

         assertCSVFileContent("Time,Iterations," + Measurement.DEFAULT_RESULT + ",another\n34:17:36," + ITERATION + ",1111.11,222.22");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testDefaultProperties() {
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         Assert.assertEquals(destination.getPath(), DEFAULT_PATH);
         Assert.assertEquals(destination.getDelimiter(), ";");

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
      destinationProperties.put("path", PATH);
      final String CHANGED_PATH = "second-path.csv";
      final File secondPath = new File(CHANGED_PATH);
      secondPath.deleteOnExit();
      if (secondPath.exists()) {
         secondPath.delete();
      }
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.open();
         Assert.assertEquals(destination.getPath(), PATH);
         Assert.assertFalse(secondPath.exists());
         destination.report(measurementWithoutDefault);
         destination.setPath(CHANGED_PATH);
         Assert.assertEquals(destination.getPath(), CHANGED_PATH);
         destination.report(measurementWithoutDefault);
         Assert.assertTrue(secondPath.exists());
         destination.close();

         assertCSVFileContent("Time;Iterations;singleResult\n34:17:36;" + ITERATION + ";100");
         assertCSVFileContent(secondPath, "Time;Iterations;singleResult\n34:17:36;" + ITERATION + ";100");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testMultipleRecords() {
      destinationProperties.setProperty("path", PATH);
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.open();
         destination.report(measurement);
         Thread.sleep(100);
         destination.report(measurement2);
         destination.close();

         assertCSVFileContent("Time;Iterations;" + Measurement.DEFAULT_RESULT + ";another\n34:17:36;" + ITERATION + ";1111.11;222.22\n34:17:37;" + (ITERATION + 1) + ";2222.22;333.33");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException | InterruptedException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testNoDefaultResultMeasurement() {
      destinationProperties.put("path", PATH);
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.open();
         destination.report(measurementWithoutDefault);
         destination.close();

         assertCSVFileContent("Time;Iterations;singleResult\n34:17:36;" + ITERATION + ";100");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testStringResultMeasurement() {
      destinationProperties.put("path", PATH);
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.open();
         destination.report(measurementStringResult);
         destination.close();

         assertCSVFileContent("Time;Iterations;" + Measurement.DEFAULT_RESULT + ";StringResult\n34:17:36;" + ITERATION + ";StringValue;StringValue2");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test(expectedExceptions = { ReportingException.class })
   public void testPathIsNotFile() throws ReportingException {
      destinationProperties.setProperty("path", "");
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.open();
         destination.report(measurement);
         destination.close();
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testReadOnlyFile() throws ReportingException {
      File readOnlyFile = new File(csvOutputPath, "read-only.file");
      readOnlyFile.setReadOnly();
      readOnlyFile.deleteOnExit();
      destinationProperties.setProperty("path", readOnlyFile.getPath());
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.open();
         destination.report(measurement);
         destination.close();
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      } catch (ReportingException re) {
         Assert.assertEquals(re.getMessage(), "Could not append a report to the file: " + readOnlyFile.getPath());
      } finally {
         readOnlyFile.delete();
      }
   }

   private void assertCSVFileContent(String expected) {
      assertCSVFileContent(csvFile, expected);
   }

   private void assertCSVFileContent(File file, String expected) {
      try (Scanner scanner = new Scanner(file).useDelimiter("\\Z")) {
         Assert.assertEquals(scanner.next(), expected, "CSV file's content");
      } catch (FileNotFoundException fnfe) {
         fnfe.printStackTrace();
         Assert.fail(fnfe.getMessage());
      }
   }
}
