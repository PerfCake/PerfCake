package org.perfcake.reporting.destination.anomalyDetection;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.perfcake.reporting.Measurement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of simple regression analysis for detecting anomalies in data set.
 * Degradation of some metrics or regular spikes can be identified.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public class SimpleRegressionAnalysis {


   /**
    * The simple regression analysis.
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
    * The regression coefficient - intercept.
    */
   private double intercept;

   /**
    * The regression coefficient - slope.
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
    *
    */
   private int window;

   /**
    * Threshold exceeded
    */
   private boolean thresholdExceeded = false;

   /**
    * The list of Measurements to analyze.
    */
   private List<Measurement> dataSet = null;
   private List<Measurement> optimizedDataSet = null;

   /**
    *
    */
   private List<RAItem> raItems = new ArrayList<>();

   /**
    *
    */
   private List<PerformanceIssue> issues = null;

   /**
    *
    */
   private boolean degradation = false;

   /**
    *
    */
   private boolean regularSpikes = false;

   /**
    *
    */
   private boolean trafficSpike = false;

   /**
    *
    */
   private int degPerc = 0;

   /**
    *
    */
   private int regsPerc = 0;

   /**
    *
    */
   private int trafSPerc = 0;


   /**
    * Analyzes results of regression analysis - testing statistical hypotheses
    * and important coefficients observation for anomaly identification.
    *
    * Investigated coefficients: b1 (the slope/regression coefficient),
    *                            p (the significance level of the slope),
    *                            RSquare (the determination coefficient).
    */
   private PerformanceIssueType analyzeResults(){
      // investigated coefficients
      slope = regression.getSlope();
      r = regression.getR();
      rSquare = regression.getRSquare();
      t = regression.getSlope()/regression.getSlopeStdErr();
      p = regression.getSignificance();

      // decision tree
      // significant slope value -> increasing/decreasing trend
      if(slope > 0.02){
         // testing statistical hypotheses - dependence between x and y value
         if(p < alpha){
            // dependence -> possibly degradation
            return PerformanceIssueType.DEGRADATION;
         }else{
            // no dependence -> possible spikes
            return PerformanceIssueType.TRAFIC_SPIKE;
         }
      }
      // slope sufficiently far from zero value -> consistent trend
      else{
         // testing statistical hypotheses - dependence between x and y value
         if(p > alpha){
            // no dependence - diverse values -> possible spikes
            if(rSquare < 0.05) { // confirmation
               return PerformanceIssueType.REGULAR_SPIKES;
            }
         }
         else if(p < alpha){
            // dependence - relative constant values of y - OK
            if(rSquare > 0.001){ // confirmation
               return PerformanceIssueType.OK;
            }
         }
      }
      // cannot decide
      return PerformanceIssueType.OK;
   }

   /**
    * Computes regression with the given data set values.
    */
   public void run(){
      // skip first 10 records
      //getOptimizedDataSet();

      // no window set or not enough records in it -> RA over an entire dataset
      // TODO test this (the minimum records)
      if(window != 0 && window > 2){
         performRAinSlidingWindows();
      }

      // cely vzorek
      performRAOverEntireDataset();

      // set performance issues occurrence
      setDetectedPerfIssues();

      // set performance issues occurrence probability
      setPerfIssuesProbabilities();
   }

   /**
    * Performs regression analysis for given regions in data set and stores detected performance issues
    * for their later highlighting in a chart.
    */
   private void performRAinSlidingWindows(){

      List<Measurement> dset = new ArrayList<>();
      if(optimizedDataSet != null) dset = optimizedDataSet;
      else dset = dataSet;
      issues = new ArrayList<>();
      System.out.println("ra: dataset size: " + dset.size());
      System.out.println("ra: window: " + window);
      // sliding window
      CircularFifoQueue<Measurement> slidingWindow = new CircularFifoQueue(window);
      // set initial values
      int lastIndex = window;
      for(int i =0; i<window; i++){
         slidingWindow.add(dset.get(i));
      }
      // sliding and analyzing
      for(int i = lastIndex; i< dset.size(); i++){
         // create RA item
         RAItem raItem = new RAItem();
         // set regression data set
         Iterator itr = slidingWindow.iterator();
         while(itr.hasNext()) {
            Measurement m = (Measurement) itr.next();
            raItem.getDataSet().add(m);
         }
         // perform regression analysis
         raItem.run();
         // check threshold
         if(checkThreshold(raItem.getDataSet(), threshold)) {
            PerformanceIssueType piTType = PerformanceIssueType.THRESHOLD_EXCEEDED;
            PerformanceIssue performanceIssue = new PerformanceIssue(piTType);
            long fromTime = slidingWindow.get(0).getTime();
            performanceIssue.setFrom(fromTime);
            long toTime = slidingWindow.get(window - 1).getTime();
            performanceIssue.setTo(toTime);
            // store PI for later reporting
            issues.add(performanceIssue);
            System.out.println(i + ": from " + fromTime + " to " + toTime + " pi: " + piTType);
         }
         // get detected PI
         PerformanceIssueType piType = raItem.getIssueType();
         if(!PerformanceIssueType.OK.equals(piType)){
            PerformanceIssue performanceIssue = new PerformanceIssue(piType);
            long fromTime = slidingWindow.get(0).getTime();
            performanceIssue.setFrom(fromTime);
            long toTime = slidingWindow.get(window-1).getTime();
            performanceIssue.setTo(toTime);
            // store PI for later reporting
            issues.add(performanceIssue);
            System.out.println(i + ": from " + fromTime + " to " + toTime + " pi: " + piType);

         }
         raItems.add(raItem);
         // add next, remove the last and perform RA wit the nex set of values again
         slidingWindow.add(dset.get(i));
      }
   }

   /**
    * Computes regression analysis for all data set at once (all measured values).
    */
   //regression analysis over the entire dataset.
   private void performRAOverEntireDataset(){
      // cely vzorek
      regression = new SimpleRegression();
      addDataSet(10,dataSet.size());
      intercept = regression.getIntercept();
      slope = regression.getSlope();
      r = regression.getR();
      rSquare = regression.getRSquare();
      p = regression.getSignificance();
   }

   /**
    * Set flags of the detected performance issues for the final report.
    */
   private void setDetectedPerfIssues(){
      // set occurred PI for the whole set
      System.out.println("issues len: " + issues.size());
      degradation = issues.stream().anyMatch(it -> PerformanceIssueType.DEGRADATION.equals(it.getType()));
      regularSpikes = issues.stream().anyMatch(it -> PerformanceIssueType.REGULAR_SPIKES.equals(it.getType()));
      trafficSpike = issues.stream().anyMatch(it -> PerformanceIssueType.TRAFIC_SPIKE.equals(it.getType()));
      System.out.println("Degradation: " + degradation);
      System.out.println("Traffic spikes: " + trafficSpike);
      System.out.println("Regular spikes: " + regularSpikes);
   }

   /**
    * Compute probabilities of the detected performance issues for a final report.
    */
   private void setPerfIssuesProbabilities(){
      int numberOfPossibilities;
      if(optimizedDataSet != null) numberOfPossibilities= optimizedDataSet.size();
      else numberOfPossibilities = dataSet.size();
      System.out.println("NoOfPossibilities: " + numberOfPossibilities);
      if(degradation){
         int occurrence = (int) issues.stream().filter(PerformanceIssue::isDegradation).count();
         degPerc = (occurrence*100)/numberOfPossibilities;
         System.out.println("DEG occurance: " + occurrence + " prob: " + degPerc + "%");

      }
      if(regularSpikes){
         int regOccurrence = (int)issues.stream().filter(PerformanceIssue::isRegularSpike).count();
         regsPerc = (100*regOccurrence)/numberOfPossibilities;
         System.out.println("REG occurance: " + regOccurrence + " prob: " + regsPerc + "%");

      }
      if(trafficSpike){
         int trafOccurrence = (int) issues.stream().filter(PerformanceIssue::isTrafficSpike).count();
         trafSPerc = (100*trafOccurrence)/numberOfPossibilities;
         System.out.println("TRAF occurance: " + trafOccurrence + " prob: " + trafSPerc + "%");

      }
      System.out.println("DEG: " + degPerc + "%");
      System.out.println("REG: " + regsPerc + "%");
      System.out.println("TRAFF: " + trafSPerc + "%");
   }

   /**
    *
    *
    * @param from
    * @param to
    */
   private void addDataSet(int from, int to){
      for(int i=from; i<to; i++){
         Measurement m = dataSet.get(i);
         x = m.getTime();
         Object value = m.get("Result");
         if (value == null) continue;
         y = Double.valueOf(value.toString().split(" ")[0]).doubleValue();
         // compare y with some THRESHOLD value
         if(y > threshold) {
            thresholdExceeded = true;
         }
         regression.addData(x, y);
      }
   }

   private void getOptimizedDataSet(){
      optimizedDataSet = new ArrayList<>();
      for(int i=10; i<dataSet.size(); i++){
         Measurement m = dataSet.get(i);
         x = m.getTime();
         Object value = m.get("Result");
         if (value == null) continue;
         optimizedDataSet.add(m);
      }
   }

   private boolean checkThreshold(List<Measurement> dataSet, double threshold){
      if(dataSet != null) {
         int size = dataSet.size();
         for(int i=0; i<size; i++) {
            Measurement m = dataSet.get(i);
            Object value = m.get("Result");
            if (value == null) continue;
            double y = Double.valueOf(value.toString().split(" ")[0]).doubleValue();
            if (y > threshold) {
               return true;
            }
         }
      }
      return false;
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

   public int getWindow() {
      return window;
   }

   public void setWindow(int window) {
      this.window = window;
   }

   public List<Measurement> getDataSet() {
      return dataSet;
   }

   public void setDataSet(List<Measurement> dataSet) {
      if(dataSet == null){
         dataSet = new ArrayList<>();
      }
      this.dataSet = dataSet;
   }

   public List<PerformanceIssue> getIssues() {
      return issues;
   }

   public void setIssues(List<PerformanceIssue> issues) {
      this.issues = issues;
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

   public double getIntercept() {
      return intercept;
   }

   public void setIntercept(double intercept) {
      this.intercept = intercept;
   }

   public int getDegPerc() {
      return degPerc;
   }

   public void setDegPerc(int degPerc) {
      this.degPerc = degPerc;
   }

   public int getRegsPerc() {
      return regsPerc;
   }

   public void setRegsPerc(int regsPerc) {
      this.regsPerc = regsPerc;
   }

   public int getTrafSPerc() {
      return trafSPerc;
   }

   public void setTrafSPerc(int trafSPerc) {
      this.trafSPerc = trafSPerc;
   }

}

