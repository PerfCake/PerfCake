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
package org.perfcake.reporting.destination;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.anomalyDetection.RegressionAnalysisHeuristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A destination that performs heuristics for anomalies detection and
 * compiles all tests results into one document report.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public class CrystalDestination extends AbstractDestination {

   /**
    * Logger.
    */
   private static final Logger log = LogManager.getLogger(CrystalDestination.class);

   /**
    * Caching the state of trace logging level to speed up reporting.
    */
   private static final boolean logTrace = log.isTraceEnabled();

   /**
    * A structure for storing measurements, shared among all the threads.
    */
   private static ConcurrentHashMap<String, List<Measurement>> sharedResultsMap = new ConcurrentHashMap();

   /**
    * A structure for electing the master thread.
    */
   private static Map<String, Boolean> finishedThreadList = new HashMap<>();

   /**
    * A lock for accessing the finishedThreadList.
    */
   ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

   /**
    * An elected master thread name.
    */
   private static String masterThreadName = null;

   /**
    * A master thread flag.
    */
   private boolean masterThread = false;

   /**
    * An unique key describing a specific result set.
    */
   private String resultSetKey = null;

   /**
    * The path of a final report.
    */
   private String path = null;

   /**
    * The title of a result in the final report.
    */
   private String perfTestName = null;

   /**
    * The order of the interpreted result within the final report.
    */
   private Integer order = null;

   @Override
   public void open() {
      resultSetKey = perfTestName;
      sharedResultsMap.putIfAbsent(resultSetKey, new ArrayList<>());
      readWriteLock.writeLock().lock();
      finishedThreadList.put(resultSetKey, false);
      readWriteLock.writeLock().unlock();
   }

   @Override
   public void close() {
      readWriteLock.writeLock().lock();
      finishedThreadList.replace(resultSetKey, true);
      readWriteLock.writeLock().unlock();

      // check for anomalies
      processAnomalyDetection();

      readWriteLock.readLock().lock();
      masterThread = !finishedThreadList.containsValue(Boolean.FALSE);
      readWriteLock.readLock().unlock();

      // last thread -> generate final report
      if (masterThread) {
         masterThreadName = resultSetKey;
         generateUnifiedReport();
      }
   }

   /**
    * According to an actual reporter, performs adequate heuristics for detection anomalies.
    */
   private void processAnomalyDetection(){
      String actualReporter = this.getParentReporter().getClass().getSimpleName();
      switch (actualReporter){
         case "ResponseTimeHistogramReporter":
         case "ResponseTimeStatsReporter":
         case "ThroughputStatsReporter":
         case "IterationsPerSecondReporter":
            // regression analysis
            List<Measurement> dataSet = sharedResultsMap.get(resultSetKey);
            RegressionAnalysisHeuristics ra = new RegressionAnalysisHeuristics();
            ra.run(dataSet);
            String reResult = ra.analyzeResults();
            System.out.println("For test '" + resultSetKey + "' RA result is '" + reResult + "'.");
            break;
         default:
            // nothing to do here
      }
   }

   /**
    * Generates the final unified report by a master thread.
    */
   private void generateUnifiedReport() {
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
      // store measurement into shared map according to the result set key name
      sharedResultsMap.get(resultSetKey).add(measurement);
   }

   /**
    * Gets the path of a final report.
    *
    * @return The final report file path.
    */
   public String getPath() {
      return path;
   }

   /**
    * Sets the the path of a final report.
    *
    * @param path
    *       The path string.
    *
    * @return Instance of this to support fluent API.
    */
   public CrystalDestination setPath(String path) {
      this.path = path;
      return this;
   }

   /**
    * Gets the name of the chart.
    *
    * @return The name of the chart.
    */
   public String getName() {
      return perfTestName;
   }

   /**
    * Sets the name of the chart.
    *
    * @param name
    *       The name of the chart.
    * @return Instance of this to support fluent API.
    */
   public CrystalDestination setName(final String name) {
      this.perfTestName = name;
      return this;
   }

   /**
    * Gets the required order of test result within the final report.
    *
    * @return The order of test result.
    */
   public Integer getOrder() {
      return order;
   }

   /**
    * Sets the order of test result within the final report.
    *
    * @param order
    *   The order of test result.
    * @return Instance of this to support fluent API.
    */
   public CrystalDestination setOrder(Integer order) {
      this.order = order;
      return this;
   }
}
