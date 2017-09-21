package org.perfcake.reporting.reporter.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Filters values using Kernel estimation algorithm.
 *
 * @author <a href="mailto:lenkaheldova@gmail.com">Lenka Heldová</a>
 */
public class KernelEstimationAlgorithm implements FilterAlgorithm {

   /**
    * Width of the window used in the Kernel estimation.
    */
   private Integer smoothingWindow;

   /**
    * Number which indicates power of the reduction. Kernel estimation is computed for every reductionFactor-th value.
    */
   private Integer reductionFactor;

   /**
    * Multipliers for each value - "weight" used to count Kernel estimations counted from smoothingWindow using Epanecnikov kernel.
    */
   private List<Double> multipliers;

   /**
    * List of values needed for the counting of the Kernel estimation.
    */
   private List<Double> valuesForEstimation = new ArrayList<>();

   private int counter = 1;

   private double denominator = 0.0;

   /**
    * Constructs filter and computes multipliers with relative denominator.
    *
    * @param smoothingWindow
    * @param reductionFactor
    */
   KernelEstimationAlgorithm(Integer smoothingWindow, Integer reductionFactor) {
      this.smoothingWindow = smoothingWindow;
      this.reductionFactor = reductionFactor;

      multipliers = new ArrayList<>();
      for (int i = 0; i < 2 * smoothingWindow; i++) {
         Double newValue = 0.0;
         if (i != 0 && i != 2 * smoothingWindow - 1) {
            newValue = 1.0 / smoothingWindow * 3 / 4 * (1.0 - Math.pow(1.0 / smoothingWindow, 2));
         }
         multipliers.add(newValue);
         denominator += newValue;
      }
   }

   @Override
   public Optional<Double> filter(Double value) {
      Optional<Double> estimation = Optional.empty();

      if (reductionFactor > smoothingWindow) {
         if (counter > (reductionFactor - smoothingWindow)) {
            valuesForEstimation.add(value);
            if (counter == reductionFactor + smoothingWindow) {
               estimation = singleKernelEstimation();
               valuesForEstimation.clear();
               counter = smoothingWindow;
            }
         }
      } else {
         valuesForEstimation.add(value);
         if (valuesForEstimation.size() == (2 * smoothingWindow)) {
            estimation = singleKernelEstimation();
            valuesForEstimation.subList(0, reductionFactor).clear();
         }
      }
      counter++;
      return estimation;
   }

   /**
    * Computes kernel estimation using fulfilled valuesForEstimation, multipliers and denominator.
    *
    * @return estimation for reductionFactor-th value.
    */
   private Optional<Double> singleKernelEstimation() {
      Double numerator = 0.0;
      for (int i = 0; i < valuesForEstimation.size(); i++) {
         numerator += multipliers.get(i) * valuesForEstimation.get(i);
      }
      return Optional.of(numerator / denominator);
   }
}
