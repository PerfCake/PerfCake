package org.perfcake.reporting.destination.anomalyDetection;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.perfcake.reporting.Measurement;

import java.util.List;

import static jdk.nashorn.internal.objects.NativeMath.sqrt;

/**
 * Implementation of simple regression analysis for detecting anomalies in data set.
 * Degradation of some metrics or regular spikes can be identified.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public class RegressionAnalysisHeuristics implements Heuristics {


   /**
    * The regression analysis.
    */
   SimpleRegression regression;

   /**
    * The value of the independent variable.
    */
   double x;

   /**
    * The value of the dependent variable.
    */
   double y;

   /**
    * The regression coefficient / slope.
    */
   double slope;

   /**
    * The correlation coefficient.
    */
   double r;

   /**
    * The coefficient of determination.
    */
   double rSquare;

   /**
    * The test statistic - slope/standard error of the slope.
    */
   double t;

   /**
    * The significance level of the slope - a low p-value suggests that the slope
    * is not zero, which in turn suggests that changes in the predictor variable
    * are associated with changes in the response variable.
    * (relationship between x and y value)
    */
   double p;

   /**
    * The significance level - a value between 0 and 1.
    */
   double alpha = 0.05;

   /**
    * Analyzes results of regression analysis - testing statistical hypotheses
    * and important coefficients observation for anomaly identification.
    *
    * Investigated coefficients: b1 (the slope/regression coefficient),
    *                            p (the significance level of the slope),
    *                            RSquare (the determination coefficient).
    */
   @Override
   public String analyzeResults(){
      // Pearson's coefficient
      double sr = sqrt((1-regression.getRSquare()),((Object)(regression.getN()-2)));
      double pearson = regression.getR()/sr;

      // investigated coefficients
      slope = regression.getSlope();
      r = regression.getR();
      rSquare = regression.getRSquare();
      t = regression.getSlope()/regression.getSlopeStdErr();
      TDistribution tDistribution = new TDistribution(regression.getN() - 2);
      p = 2*(1.0 - tDistribution.cumulativeProbability(Math.abs(t)));

      // decision tree
      // significant slope value -> increasing/decreasing trend
      if(slope > 0.02){
         // testing statistical hypotheses - dependence between x and y value
         if(p < alpha){
            // dependence -> possibly degradation
            return "possible degradation detected";
         }else{
            // no dependence -> possible spikes
            return "possible increasing (traffic) spikes";
         }
      }
      // slope sufficiently far from zero value -> consistent trend
      else{
         // testing statistical hypotheses - dependence between x and y value
         if(p > alpha){
            // no dependence - diverse values -> possible spikes
            if(rSquare < 0.05) { // confirmation
               return "regular spikes";
            }
         }
         else if(p < alpha){
            // dependence - relative constant values of y - OK
            if(rSquare > 0.001){ // confirmation
               return "OK";
            }
         }
      }
      // cannot decide
      return "NA";
   }

   /**
    * Computes regression with the given data set values.
    * @param dataSet
    */
   @Override
   public void run(List<Measurement> dataSet){
      regression = new SimpleRegression();
      if(dataSet != null) {
         int size = dataSet.size();
         for(int i=0; i<size; i++){
            // workaround for warm-up: skip first 10 values
            if(i > 10){
               Measurement m = dataSet.get(i);
               x = m.getTime();
               Object value = m.get("Result");
               if (value == null) continue;
               y = Double.valueOf(value.toString().split(" ")[0]);
               regression.addData(x, y);
            }
         }
      }
   }
}