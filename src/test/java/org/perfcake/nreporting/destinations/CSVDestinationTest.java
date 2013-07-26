package org.perfcake.nreporting.destinations;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Scanner;

import org.perfcake.nreporting.Measurement;
import org.perfcake.nreporting.Quantity;
import org.perfcake.nreporting.ReportingException;
import org.perfcake.util.ObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CSVDestinationTest {

   private File csvFile;
   private Properties destinationProperties;
   private Measurement measurement;

   @BeforeClass
   public void beforeClass() {
      measurement = new Measurement(42, 123456000, 12345);
      measurement.set(new Quantity<Double>(1111.11, "it/s"));
      measurement.set("another", new Quantity<Double>(222.22, "ms"));

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
   public void testDefaultReport() {
      try {
         CSVDestination destination = (CSVDestination) ObjectFactory.summonInstance(CSVDestination.class.getName(), destinationProperties);

         destination.report(measurement);

         assertCSVFileContent("Time;Iterations;Result;another\n34:17:36;12345;1111.11;222.22");
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
