package org.perfcake.reporting.destination.anomalyDetection;

import org.perfcake.reporting.Measurement;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic statistical analysis.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public class BasicStatisticsAnalysis {

   /**
    * Descriptive statistics.
    */
   private DescriptiveStatistics stats;

   /**
    * The mean value.
    */
   private Double mean;

   /**
    * The standard deviation value
    */
   private double standardDeviation;

   /**
    * The 95th percentile value.
    */
   private Double percentile95;

   /**
    * The 99th percentile value.
    */
   private Double percentile99;

   /**
    * The variance value.
    */
   private Double variance;

   /**
    * The threshold value defined by a user.
    */
   private double threshold;

   /**
    * The flag for exceeding the given threshold by any measured value.
    */
   private boolean thresholdResultExceeded = false;

   /**
    * The flag for exceeding the given threshold by an average value.
    */
   private boolean thresholdAverageExceeded = false;

   /**
    * The flag for exceeding the given threshold by an 95th percentile value.
    */
   private boolean threshold95thExceeded = false;

   /**
    * The flag for exceeding the given threshold by an 99th percentile value.
    */
   private boolean threshold99thExceeded = false;

   /**
    * The list of Measurements to analyze.
    */
   private List<Measurement> dataSet;

   /**
    * Sets values to analyze.
    */
   private void setDataSet() {
      if (dataSet != null) {
         int size = dataSet.size();
         for (int i = 0; i < size; i++) {
            // skip first 10 values
            if (i > 10) {
               Measurement m = dataSet.get(i);
               Object value = m.get("Result");
               if (value == null) {
                  continue;
               }
               double y = Double.valueOf(value.toString().split(" ")[0]).doubleValue();
               if (y > threshold) {
                  thresholdResultExceeded = true;
               }
               stats.addValue(y);
            }
         }
      }
   }

   /**
    * Sets statistics results.
    */
   private void setStatisticsResults() {
      mean = stats.getMean();
      standardDeviation = stats.getStandardDeviation();
      percentile95 = stats.getPercentile(95);
      percentile99 = stats.getPercentile(99);
      variance = stats.getVariance();
      thresholdAverageExceeded = (mean.doubleValue() > threshold);
      threshold95thExceeded = (percentile95.doubleValue() > threshold);
      threshold99thExceeded = (percentile99.doubleValue() > threshold);
   }

   /**
    * Runs the basic statistical analysis.
    */
   public void run() {
      stats = new DescriptiveStatistics();
      setDataSet();
      setStatisticsResults();
   }

   public DescriptiveStatistics getStats() {
      return stats;
   }

   public void setStats(DescriptiveStatistics stats) {
      this.stats = stats;
   }

   public Double getMean() {
      return mean;
   }

   public void setMean(Double mean) {
      this.mean = mean;
   }

   public Double getStandardDeviation() {
      return standardDeviation;
   }

   public void setStandardDeviation(Double standardDeviation) {
      this.standardDeviation = standardDeviation;
   }

   public Double getPercentile95() {
      return percentile95;
   }

   public void setPercentile95(Double percentile95) {
      this.percentile95 = percentile95;
   }

   public Double getPercentile99() {
      return percentile99;
   }

   public void setPercentile99(Double percentile99) {
      this.percentile99 = percentile99;
   }

   public Double getVariance() {
      return variance;
   }

   public void setVariance(Double variance) {
      this.variance = variance;
   }

   public void setStandardDeviation(double standardDeviation) {
      this.standardDeviation = standardDeviation;
   }

   public List<Measurement> getDataSet() {
      return dataSet;
   }

   public void setDataSet(List<Measurement> dataSet) {
      if (dataSet == null) {
         dataSet = new ArrayList<>();
      }
      this.dataSet = dataSet;
   }

   public double getThreshold() {
      return threshold;
   }

   public void setThreshold(double threshold) {
      this.threshold = threshold;
   }

   public boolean isThresholdResultExceeded() {
      return thresholdResultExceeded;
   }

   public void setThresholdResultExceeded(boolean thresholdResultExceeded) {
      this.thresholdResultExceeded = thresholdResultExceeded;
   }

   public boolean isThresholdAverageExceeded() {
      return thresholdAverageExceeded;
   }

   public void setThresholdAverageExceeded(boolean thresholdAverageExceeded) {
      this.thresholdAverageExceeded = thresholdAverageExceeded;
   }

   public boolean isThreshold95thExceeded() {
      return threshold95thExceeded;
   }

   public void setThreshold95thExceeded(boolean threshold95thExceeded) {
      this.threshold95thExceeded = threshold95thExceeded;
   }

   public boolean isThreshold99thExceeded() {
      return threshold99thExceeded;
   }

   public void setThreshold99thExceeded(boolean threshold99thExceeded) {
      this.threshold99thExceeded = threshold99thExceeded;
   }
}