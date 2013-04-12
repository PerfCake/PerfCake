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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.perfcake.reporting.reporters.ReportingHelper;

/**
 * <p>
 * Information about test case that should be run. Each Test Run is identified by unique ID that is arbitrary string and by startTime when the test started. Each TestRun can be accompanied by 0 or more tags which add some more information about test case (for example component which was tested or some finer test type name)
 * </p>
 * <p/>
 * <p>
 * These data are set usually in reporting xml element in scenario xml file. Then after parsing from scenario file this TestCase info is provided to each reporter and they use it to create files of appropriate names or insert into databases with appropriate metadata (tags).
 * </p>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 */
public class TestRunInfo {
   private String uniqueId;

   /**
    * How long is this test scheduled to run (some tests may define this) in
    * seconds.
    */
   private long testDuration = -1;

   /**
    * Plan on how many iterations is this test configured.
    */
   private long testIterations = -1;

   private List<String> tags = new ArrayList<String>();

   private long testStartTime = -1;

   private long testEndTime = -1;

   private AtomicLong processedIterations = new AtomicLong(0);

   /**
    * Friendly string format that represents number of seconds that passed from
    * the start of the test. <example> <code>
    * 00:10:23
    * </code> </example>
    */
   public String getTestRunTimeString() {
      long testStart;
      if (getTestStartTime() == -1) {
         return "00:00:00";
      }

      return ReportingHelper.getTimeOfTest(getTestStartTime(), System.currentTimeMillis());
   }

   public long getTestRunTime() {
      long testStart;
      if (getTestStartTime() == -1) {
         return 0;
      }

      return System.currentTimeMillis() - getTestStartTime();

   }

   /**
    * How many percents of the whole test run is actually done. This is computed
    * by 2 ways, depending on whether this test is iteration based or time
    * based.
    * 
    * @return number between 0 and 1 which represents percentage.
    */
   public float getPercentage() {

      if (testDuration != -1) {
         if (System.currentTimeMillis() - testStartTime > testDuration * 1000) {
            return 1;
         }

         return ((System.currentTimeMillis() - testStartTime) / (testDuration * 1000f));
      }

      if (testIterations != -1 && testIterations != 0) {
         long done = processedIterations.get();

         if (done >= testIterations) {
            return 1;
         }

         return ((float) done / testIterations);
      }

      return -1;
   }

   public boolean testStarted() {
      return testStartTime != -1;
   }

   /**
    * Set test duration in seconds.
    */
   public long getTestDuration() {
      return testDuration;
   }

   /**
    * Set test duration in seconds.
    * 
    * @param testDuration
    */
   public void setTestDuration(long testDuration) {
      this.testDuration = testDuration;
   }

   public String getUniqueId() {
      return uniqueId;
   }

   public void setUniqueId(String uniqueId) {
      this.uniqueId = uniqueId;
   }

   public long getTestStartTime() {
      return testStartTime;
   }

   public void setTestStartTime(long testStartTime) {
      this.testStartTime = testStartTime;
   }

   public long getTestEndTime() {
      return testEndTime;
   }

   public void setTestEndTime(long testEndTime) {
      this.testEndTime = testEndTime;
   }

   public List<String> getTags() {
      return tags;
   }

   public void addTag(String tag) {
      tags.add(tag);
   }

   public void addTags(Set<String> commaSeparatedProperty) {
      tags.addAll(commaSeparatedProperty);
   }

   public boolean testFinished() {
      return testEndTime != -1;
   }

   public long getProcessedIterations() {
      return processedIterations.get();
   }

   public long incrementIteration() {
      return processedIterations.incrementAndGet();
   }

   public long getTestIterations() {
      return testIterations;
   }

   public void setTestIterations(long i) {
      testIterations = i;
   }
}
