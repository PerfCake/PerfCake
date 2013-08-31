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
import java.util.Properties;
import java.util.Scanner;

import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destinations.CSVDestination;
import org.perfcake.util.ObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CSVDestinationTest {

   private File csvFile;
   private Properties destinationProperties;
   private Measurement measurement;
   private Measurement measurementWithoutDefault;

   @BeforeClass
   public void beforeClass() {
      measurement = new Measurement(42, 123456000, 12345);
      measurement.set(new Quantity<Double>(1111.11, "it/s"));
      measurement.set("another", new Quantity<Double>(222.22, "ms"));

      measurementWithoutDefault = new Measurement(42, 123456000, 12345);
      measurementWithoutDefault.set("singleResult", new Quantity<Number>(100, "units"));

      File csvOutputPath = new File("test-output");
      if (!csvOutputPath.exists()) {
         csvOutputPath.mkdir();
      }

      csvFile = new File(csvOutputPath, "out.csv");
   }

   @BeforeMethod
   public void beforeMethod() {
      destinationProperties = new Properties();
      destinationProperties.put("path", "test-output/out.csv");

      if (csvFile.exists()) {
         csvFile.delete();
      }
   }

   @Test
   public void testDestinationReport() {
      destinationProperties.put("delimiter", ",");
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.report(measurement);

         assertCSVFileContent("Time,Iterations,Result,another\n34:17:36,12345,1111.11,222.22");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testDefaultProperties() {
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.report(measurement);

         assertCSVFileContent("Time;Iterations;Result;another\n34:17:36;12345;1111.11;222.22");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   @Test
   public void testNoDefaultResultMeasurement() {
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.report(measurementWithoutDefault);

         assertCSVFileContent("Time;Iterations;singleResult\n34:17:36;12345;100");
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | ReportingException e) {
         e.printStackTrace();
         Assert.fail(e.getMessage());
      }
   }

   private void assertCSVFileContent(String expected) {
      try (Scanner scanner = new Scanner(csvFile).useDelimiter("\\Z")) {
         Assert.assertEquals(scanner.next(), expected, "CSV file's content");
      } catch (FileNotFoundException fnfe) {
         fnfe.printStackTrace();
         Assert.fail(fnfe.getMessage());
      }
   }
}
