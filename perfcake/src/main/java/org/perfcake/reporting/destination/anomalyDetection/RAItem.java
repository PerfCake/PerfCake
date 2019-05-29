package org.perfcake.reporting.destination.anomalyDetection;

import org.perfcake.reporting.Measurement;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.List;

//import static jdk.nashorn.internal.objects.NativeMath.sqrt;

/**
 * Implementation of simple regression analysis for detecting anomalies in data set.
 * Degradation of some metrics or regular spikes can be identified.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public class RAItem {

   public SimpleRegression getRegression() {
      return regression;
   }

   public void setRegression(SimpleRegression regression) {
      this.regression = regression;
   }

   /**
    * The regression analysis.
    */
   private SimpleRegression regression;

   /**
    * The value of the independent variable.
    */
   private double x;

   /**
    * The value of the dependent variable.
    */
   private double y;

   /**
    * The regression coefficient / slope.
    */
   private double slope;

   /**
    * The correlation coefficient.
    */
   private double r;

   /**
    * The coefficient of determination.
    */
   private double rSquare;

   /**
    * The test statistic = slope/standard error of the slope.
    */
   private double t;

   /**
    * The significance level of the slope - a low p-value suggests that the slope
    * is not zero, which in turn suggests that changes in the predictor variable
    * are associated with changes in the response variable.
    * (relationship between x and y value)
    */
   private double p;

   /**
    * The significance level - a value between 0 and 1.
    */
   private double alpha = 0.05;

   /**
    * Threshold defined by user
    */
   private double threshold;

   /**
    * Threshold exceeded
    */
   private boolean thresholdExceeded = false;

   private List<Measurement> dataSet = null;

   private PerformanceIssueType issueType = null;

   private String result = null;

   private boolean degradation = false;
   private boolean regularSpikes = false;
   private boolean trafficSpike = false;

   /**
    * Analyzes results of regression analysis - testing statistical hypotheses
    * and important coefficients observation for anomaly identification.
    *
    * Investigated coefficients: b1 (the slope/regression coefficient),
    * p (the significance level of the slope),
    * RSquare (the determination coefficient).
    */
   private PerformanceIssueType analyzeResults() {
      // Pearson's coefficient
      //ouble sr = sqrt((1-regression.getRSquare()),((Object)(regression.getN()-2)));
      double sr = Math.sqrt(regression.getN() - 2);
      double pearson = regression.getR() / sr;

      // investigated coefficients
      slope = regression.getSlope();
      r = regression.getR();
      rSquare = regression.getRSquare();
      t = regression.getSlope() / regression.getSlopeStdErr();
      //System.out.println("N:" + regression.getN());
      TDistribution tDistribution = new TDistribution(regression.getN());
      p = 2 * (1.0 - tDistribution.cumulativeProbability(Math.abs(t)));

      // decision tree
      // significant slope value -> increasing/decreasing trend
      if (slope > 0.4) {
         // testing statistical hypotheses - dependence between x and y value
         if (rSquare > 0.7) {
            return PerformanceIssueType.DEGRADATION;
         }
      }
      // slope sufficiently far from zero value -> consistent trend
      else if (slope < 0.3) {
         if (slope < 0.001) {
            return PerformanceIssueType.OK;
         }
         if (rSquare < 0.1) {
            // dependence - relative constant values of y - OK
            if (p > alpha) { // confirmation
               return PerformanceIssueType.REGULAR_SPIKES;
            }
         } else if (rSquare > 0.7) {
            return PerformanceIssueType.OK;
         }
         // no dependence - diverse values -> possible spikes
         else if (p < alpha) { // confirmation
            return PerformanceIssueType.OK;
         }
      }
      // testing statistical hypotheses - dependence between x and y value
      //         if(p > alpha){
      //            // no dependence - diverse values -> possible spikes
      //            if(rSquare < 0.50) { // confirmation
      //               return PerformanceIssueType.REGULAR_SPIKES;
      //            }
      //         }
      //         else if(p < alpha){
      //            // dependence - relative constant values of y - OK
      //            if(rSquare > 0.50){ // confirmation
      //               return PerformanceIssueType.OK;
      //            }
      //         }
      // cannot decide
      return PerformanceIssueType.OK;
   }

   /**
    * Computes regression with the given data set values.
    */
   public void run() {
      regression = new SimpleRegression();
      addDataSet();
      slope = regression.getSlope();
      r = regression.getR();
      rSquare = regression.getRSquare();
      p = regression.getSignificance();
      issueType = analyzeResults();
      if (!PerformanceIssueType.OK.equals(issueType)) {
         if (PerformanceIssueType.DEGRADATION.equals(issueType)) {
            degradation = true;
         }
         if (PerformanceIssueType.REGULAR_SPIKES.equals(issueType)) {
            regularSpikes = true;
         }
         if (PerformanceIssueType.TRAFIC_SPIKE.equals(issueType)) {
            trafficSpike = true;
         }
      }

      System.out.println(".");
      System.out.println("' slope result is '" + slope + "'.");
      System.out.println("' R is '" + r + "'.");
      System.out.println("' R square is '" + rSquare + "'.");
      System.out.println("' significance is '" + p + "'.");
   }

   private void addDataSet() {
      for (Measurement m : dataSet) {
         x = m.getTime();
         Object value = m.get("Result");
         if (value == null) {
            continue;
         }
         y = Double.valueOf(value.toString().split(" ")[0]);
         regression.addData(x, y);
      }
   }

   public double getThreshold() {
      return threshold;
   }

   public void setThreshold(double threshold) {
      this.threshold = threshold;
   }

   public boolean isThresholdExceeded() {
      return thresholdExceeded;
   }

   public void setThresholdExceeded(boolean thresholdExceeded) {
      this.thresholdExceeded = thresholdExceeded;
   }

   public double getSlope() {
      return slope;
   }

   public void setSlope(double slope) {
      this.slope = slope;
   }

   public double getR() {
      return r;
   }

   public void setR(double r) {
      this.r = r;
   }

   public double getrSquare() {
      return rSquare;
   }

   public void setrSquare(double rSquare) {
      this.rSquare = rSquare;
   }

   public double getP() {
      return p;
   }

   public void setP(double p) {
      this.p = p;
   }

   public List<Measurement> getDataSet() {
      if (dataSet == null) {
         dataSet = new ArrayList<>();
      }
      return dataSet;
   }

   public void setDataSet(List<Measurement> dataSet) {
      if (dataSet == null) {
         dataSet = new ArrayList<>();
      }
      this.dataSet = dataSet;
   }

   public PerformanceIssueType getIssueType() {
      return issueType;
   }

   public void setIssueType(PerformanceIssueType issueType) {
      this.issueType = issueType;
   }

   public String getResult() {
      return result;
   }

   public void setResult(String result) {
      this.result = result;
   }

   public boolean isDegradation() {
      return degradation;
   }

   public void setDegradation(boolean degradation) {
      this.degradation = degradation;
   }

   public boolean isRegularSpikes() {
      return regularSpikes;
   }

   public void setRegularSpikes(boolean regularSpikes) {
      this.regularSpikes = regularSpikes;
   }

   public boolean isTrafficSpike() {
      return trafficSpike;
   }

   public void setTrafficSpike(boolean trafficSpike) {
      this.trafficSpike = trafficSpike;
   }
}

