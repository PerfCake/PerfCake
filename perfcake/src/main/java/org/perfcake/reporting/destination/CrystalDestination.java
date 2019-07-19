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

import static org.perfcake.common.PeriodType.TIME;
import static org.perfcake.reporting.destination.ChartDestination.ChartType.LINE;

import org.perfcake.PerfCakeException;
import org.perfcake.common.BoundPeriod;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;
import org.perfcake.reporting.destination.anomalyDetection.BasicStatisticsAnalysis;
import org.perfcake.reporting.destination.anomalyDetection.SimpleRegressionAnalysis;
import org.perfcake.reporting.destination.c3chart.C3ChartHelper;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    * A structure for storing results of RA.
    */
   private static ConcurrentHashMap<String, CrystalDestination> instances = new ConcurrentHashMap();

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
   protected String resultSetKey = null;

   /**
    * The path to the directory of a final report.
    */
   private String path = "./PerfCake-report";

   /**
    * The title of a test in a final report.
    */
   private String title = "PerfCake Test Result";

   /**
    * The order of the interpreted result within the final report.
    */
   private Integer order = null;

   /**
    * An indicator to perform results analysis.
    */
   private boolean resultsAnalysis = true;

   /**
    * Threshold for monitored metrics
    */
   private Double threshold = null;

   /**
    * Tim sliding window size
    */
   private int window = 0;

   /**
    *
    */
   private SimpleRegressionAnalysis ra;

   /**
    *
    */
   private BasicStatisticsAnalysis stats;

   /**
    * Attributes that should be stored in the chart.
    */
   private List<String> chartAttributes = new ArrayList<>(Arrays.asList("*".split("\\s*,\\s*")));

   /**
    * Group of this chart. Charts in the same group can be later matched for the column names.
    */
   private String chartGroup = "default";

   /**
    * Y axis legend.
    */
   private String chartYAxis = "Value";

   /**
    * X axis legend.
    */
   private String chartXAxis = "Time";

   /**
    * The type of the X axis. It can display the overall progress of the test in Percents, Time, or Iteration numbers.
    */
   private PeriodType chartXAxisType = TIME;

   /**
    * Height of the resulting chart in pixels.
    */
   private int chartHeight = 400;

   /**
    * The chart can be either of line or bar type. Line is the default.
    */
   private ChartDestination.ChartType chartType = LINE;

   @Override
   public void open() {
      System.out.println("PerfTestName: " + title);
      if (title == null) {
         resultSetKey = "mock";
         title = resultSetKey;
      } else {
         resultSetKey = title;
      }
      sharedResultsMap.putIfAbsent(resultSetKey, new ArrayList<>());
      instances.putIfAbsent(resultSetKey, this);
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
   private void processAnomalyDetection() {
      String actualReporter = this.getParentReporter().getClass().getSimpleName();
      switch (actualReporter) {
         case "ResponseTimeHistogramReporter":
         case "ResponseTimeStatsReporter":
            //case "ThroughputStatsReporter":
            //case "IterationsPerSecondReporter":
            // perform regression analysis
            List<Measurement> dataSet = sharedResultsMap.get(resultSetKey);
            ra = new SimpleRegressionAnalysis();
            ra.setThreshold(threshold.doubleValue());
            Set<BoundPeriod<Destination>> reportingPeriods = getParentReporter().getReportingPeriods();
            int numberOfRecordsInWindow = 0;
            for (BoundPeriod<Destination> bpd : reportingPeriods) {
               if ("CrystalDestination".equals(bpd.getBinding().getClass().getSimpleName())) {
                  switch (bpd.getPeriodType()) {
                     case TIME:
                        numberOfRecordsInWindow = window / (int) bpd.getPeriod();
                        System.out.println("window: " + window);
                        System.out.println("period: " + String.valueOf((int) bpd.getPeriod()));
                        System.out.println("numberOfRecordsInWindow: " + numberOfRecordsInWindow);
                        break;
                     default:

                  }
               }
            }
            ra.setWindow(numberOfRecordsInWindow);
            ra.setDataSet(dataSet);
            ra.run();
            System.out.println("For test '" + resultSetKey + "' slope result is '" + ra.getSlope() + "'.");
            System.out.println("For test '" + resultSetKey + "' R is '" + ra.getR() + "'.");
            System.out.println("For test '" + resultSetKey + "' R square is '" + ra.getrSquare() + "'.");
            System.out.println("For test '" + resultSetKey + "' significance is '" + ra.getP() + "'.");
            System.out.println("For test '" + resultSetKey + "' threshold was '" + String.valueOf(ra.isThresholdExceeded()) + "'.");

            stats = new BasicStatisticsAnalysis();
            stats.setDataSet(dataSet);
            stats.setThreshold(threshold.doubleValue());
            stats.run();
            System.out.println("For test '" + resultSetKey + "' mean was '" + String.valueOf(stats.getMean()) + "'.");
            System.out.println("For test '" + resultSetKey + "' stdErr was '" + String.valueOf(stats.getStandardDeviation()) + "'.");
            System.out.println("For test '" + resultSetKey + "' 95percentile was '" + String.valueOf(stats.getPercentile95()) + "'.");
            System.out.println("For test '" + resultSetKey + "' 99percentile was '" + String.valueOf(stats.getPercentile99()) + "'.");
            System.out.println("For test '" + resultSetKey + "' variance was '" + String.valueOf(stats.getVariance()) + "'.");
            break;
         default:
            // nothing to do here
      }
   }

   /**
    * Generates the final unified report by a master thread.
    */
   private void generateUnifiedReport() {
      // for all tests
      for (Map.Entry e : sharedResultsMap.entrySet()) {
         List<Measurement> measurements = sharedResultsMap.get(e.getKey());
         System.out.println(resultSetKey + " : generating chart for : " + e.getKey());
         // generate C3 chart
         C3ChartHelper helper = new C3ChartHelper(instances.get(e.getKey()));
         for (Measurement m : measurements) {
            try {
               helper.appendResult(m);
            } catch (ReportingException e1) {
               new ReportingException(e1.getCause());
            }
         }
         try {
            helper.close();
            helper.compileResults(false);
         } catch (PerfCakeException e1) {
            new PerfCakeException(e1.getCause());
         }
      }
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
      // TODO do not report first 10 values
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
   public String getTitle() {
      return title;
   }

   /**
    * Sets the name of the chart.
    *
    * @param title
    *       The name of the chart.
    * @return Instance of this to support fluent API.
    */
   public CrystalDestination setTitle(final String title) {
      this.title = title;
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
    *       The order of test result.
    * @return Instance of this to support fluent API.
    */
   public CrystalDestination setOrder(Integer order) {
      this.order = order;
      return this;
   }

   /**
    * Gets the indicator to perform results analysis.
    *
    * @return The boolean value of resultsAnalysis.
    */
   public boolean isResultsAnalysis() {
      return resultsAnalysis;
   }

   /**
    * Sets the indicator to perform results analysis.
    *
    * @param resultsAnalysis
    *       The boolean value of resultsAnalysis.
    */
   public void setResultsAnalysis(boolean resultsAnalysis) {
      this.resultsAnalysis = resultsAnalysis;
   }

   /**
    * Gets the threshold for the monitored metrics.
    *
    * @return The threshold value.
    */
   public Double getThreshold() {
      return threshold;
   }

   /**
    * Sets the threshold for monitored metrics.
    *
    * @param threshold
    *       The threshold value.
    * @return Instance of this to support fluent API.
    */
   public CrystalDestination setThreshold(Double threshold) {
      this.threshold = threshold;
      return this;
   }

   /**
    * @return
    */
   public int getWindow() {
      return window;
   }

   /**
    * @param window
    */
   public void setWindow(int window) {
      this.window = window;
   }

   public SimpleRegressionAnalysis getRa() {
      return ra;
   }

   public void setRa(SimpleRegressionAnalysis ra) {
      this.ra = ra;
   }

   public BasicStatisticsAnalysis getStats() {
      return stats;
   }

   public void setStats(BasicStatisticsAnalysis stats) {
      this.stats = stats;
   }

   /**
    * Gets the attributes that will be written to the chart.
    *
    * @return The attributes separated by comma.
    */
   public String getChartAttributes() {
      return StringUtils.join(chartAttributes, ",");
   }

   /**
    * Sets the attributes that will be written to the chart.
    *
    * @param attributes
    *       The attributes separated by comma.
    * @return Instance of this to support fluent API.
    */
   public CrystalDestination setChartAttributes(final String attributes) {
      this.chartAttributes = new ArrayList<>(Arrays.asList(attributes.split("\\s*,\\s*")));
      return this;
   }

   /**
    * Gets the attributes that will be written to the chart as a List.
    *
    * @return The attributes list.
    */
   public List<String> getChartAttributesAsList() {
      return chartAttributes;
   }

   /**
    * @return
    */
   public String getChartGroup() {
      return chartGroup;
   }

   /**
    * @param chartGroup
    */
   public void setChartGroup(String chartGroup) {
      this.chartGroup = chartGroup;
   }

   /**
    * @return
    */
   public String getChartYAxis() {
      return chartYAxis;
   }

   /**
    * @param chartYAxis
    */
   public void setChartYAxis(String chartYAxis) {
      this.chartYAxis = chartYAxis;
   }

   /**
    * @return
    */
   public String getChartXAxis() {
      return chartXAxis;
   }

   /**
    * @param chartXAxis
    */
   public void setChartXAxis(String chartXAxis) {
      this.chartXAxis = chartXAxis;
   }

   /**
    * @return
    */
   public PeriodType getChartXAxisType() {
      return chartXAxisType;
   }

   /**
    * @param chartXAxisType
    */
   public void setChartXAxisType(PeriodType chartXAxisType) {
      this.chartXAxisType = chartXAxisType;
   }

   /**
    * @return
    */
   public int getChartHeight() {
      return chartHeight;
   }

   /**
    * @param chartHeight
    */
   public void setChartHeight(int chartHeight) {
      this.chartHeight = chartHeight;
   }

   /**
    * Gets the chart's graphics type - either line or bar. Line is the default.
    *
    * @return The chart's graphics type.
    */
   public ChartDestination.ChartType getChartType() {
      return chartType;
   }

   /**
    * Sets the chart's graphics type - either line or bar. Line is the default.
    *
    * @param chartType
    *       The chart's graphics type.
    * @return Instance of this to support fluent API.
    */
   public CrystalDestination setChartType(final ChartDestination.ChartType chartType) {
      this.chartType = chartType;
      return this;
   }
}
