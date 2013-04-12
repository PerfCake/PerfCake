/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.reporting;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.perfcake.reporting.destinations.util.CsvFile;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReportingTestBase {
   protected static final Logger log = Logger.getLogger(ReportingTestBase.class);

   private static boolean log4jconfigured = false;

   public static final String TEST_OUTPUT_DIR = "./target/test-output";

   @BeforeClass
   public static void setupLog4j() {
      if (!log4jconfigured) {
         log4jconfigured = true;

         Element element = null;

         try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbFactory.newDocumentBuilder();
            Document doc = db.parse(ReportingTestBase.class.getResourceAsStream("/testsLog4j.xml"));
            element = doc.getDocumentElement();
         } catch (Exception e) {
            e.printStackTrace();
         }
         DOMConfigurator.configure(element);
      }
   }

   @BeforeMethod
   public void purgeTestFolder() {
      File testOutputdir = new File(TEST_OUTPUT_DIR);
      if (testOutputdir.exists()) {
         boolean success = deleteDirectory(testOutputdir);
         if (!success) {
            Assert.fail("Cannot delete test output directory before test.");
         }
      }

      testOutputdir.mkdir();
   }

   /**
    * This method simulates 10 iterations that happen over 5 seconds
    * 
    * 1. second - 2 iters 2. second - 0 iters 3. second - 1 iters 4. second - 3
    * iters 5. second - 4 iters
    * 
    * Average throughoutput (manually computed) is:
    * 
    * after 1.second - 2 msg/s after 2.second - 1 msg/s after 3.second - 1 msg/s
    * after 4.second - 1.5 msg/s after 5.second - 2 msg/s
    * 
    * Time window size 2 throughoutputs, in case when measurements are outputed
    * every second are:
    * 
    * after 1.second - 2msg/s after 2.second - 1 msg/s after 3.second - 0.5
    * msg/s after 4.second - 2 msg/s after 5.second - 3.5 msg/s
    * 
    * @param rm
    */
   protected void simulateIterations(ReportManager rm) {
      rm.reportTestStarted();
      sleep(1f);
      rm.reportIteration();
      rm.reportIteration();
      sleep(1f);
      // 1 seconds passed
      sleep(1f);
      // 2 seconds
      rm.reportIteration();
      sleep(1f);
      // 3 seconds
      rm.reportIteration();
      rm.reportIteration();
      rm.reportIteration();
      sleep(1f);
      // 4 seconds
      rm.reportIteration();
      rm.reportIteration();
      rm.reportIteration();
      rm.reportIteration();
      // 5 seconds
      rm.reportTestFinished();
   }

   /**
    * Asserts that file contains string.
    * 
    * @param filename
    * @param string
    */
   protected void assertCsvContainsExactly(String filename, String string) {
      CsvFile csvFile = new CsvFile(filename);
      String csvText = csvFile.getAllText();
      Assert.assertEquals(string, csvText);
   }

   /**
    * Asserts that csv file contains 1 number of value higher than minVal.
    * 
    * @param filename
    * @param minVal
    */
   protected void assertCsvContainsNumber(String filename, double minVal) {
      CsvFile csvFile = new CsvFile(filename);
      String csvText = csvFile.getAllText();
      Double d = null;
      try {
         d = Double.parseDouble(csvText.trim());
      } catch (Exception ex) {
         Assert.fail("The file " + filename + " doesn't contain 1 parseable integer. The text in file: " + csvText);
         return;
      }
      Assert.assertTrue(d > minVal, "The csv file: " + filename + " contains double [" + d + "] which is lower than expected[" + minVal + "]");

   }

   /**
    * Asserts that file contains num lines that matches the regex
    */
   protected void assertCsvContainsLines(String filename, int num, String regex) {
      CsvFile csvFile = new CsvFile(filename);
      List<String> csvText = csvFile.getLines();
      int numMatch = 0;

      for (String line : csvText) {
         if (line.matches(regex)) {
            numMatch++;
         }
      }

      Assert.assertEquals(num, numMatch, "The file [" + filename + "] doesn't contain matches of regex [" + regex + "].");
   }

   /**
    * Simulates standard request messages in given number of threads.
    * 
    * There is 100ms delay once in each thread between each sender reports SENT
    * and PROCESSED. That means that if the simulation occurs in 11 threads then
    * there will be 1100ms delay overall.
    */
   protected void stressSimulate(final ReportManager rm, int numThreads, int numMessages) {
      if (numMessages % numThreads != 0) {
         Assert.fail("Number of messages must be divisible by number of threads!");
      }
      final int numForThread = numMessages / numThreads;
      rm.reportTestStarted();
      List<Thread> threads = new ArrayList<Thread>();
      for (int i = 0; i < numThreads; i++) {
         Thread t = new StressThread(rm, i, numForThread);
         threads.add(t);
         t.start();
      }
      for (Thread t : threads) {
         try {
            t.join();
         } catch (InterruptedException e) {
         }
      }

      rm.reportTestFinished();
   }

   /**
    * Stress thread helper class for method simulate.
    * 
    * @author Filip Nguyen <nguyen.filip@gmail.com>
    * 
    */
   public class StressThread extends Thread {
      private int numForThread;

      private ReportManager rm;

      public StressThread(ReportManager rm, int i, int numForThread) {
         this.numForThread = numForThread;
         this.rm = rm;
      }

      @Override
      public void run() {
         boolean slept = false;
         for (int k = 0; k < numForThread; k++) {
            long theTime = System.currentTimeMillis();
            if (k > numForThread / 2 && !slept) {
               try {
                  Thread.sleep(100);
               } catch (InterruptedException e) {
               }
               slept = true;
            }
            theTime = System.currentTimeMillis() - theTime;
            rm.reportResponseTime(theTime);
            rm.reportIteration();
         }

      }
   }

   public static boolean deleteDirectory(File path) {
      if (path.exists()) {
         File[] files = path.listFiles();
         for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
               deleteDirectory(files[i]);
            } else {
               files[i].delete();
            }
         }
      }
      return (path.delete());
   }

   /**
    * Sleeps for specified number of seconds.
    */
   protected void sleep(float i) {
      try {
         Thread.sleep(Math.round(i * 1000));
      } catch (InterruptedException e) {
      }
   }
}
