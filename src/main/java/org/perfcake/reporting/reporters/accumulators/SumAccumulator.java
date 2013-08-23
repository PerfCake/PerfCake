/**
 * 
 */
package org.perfcake.reporting.reporters.accumulators;

/**
 * Accumulates the sum of values.
 * Atomic types are not used because both values must be set at the same time. Hence the methods are synchronized.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class SumAccumulator implements Accumulator<Double> {
   /**
    * Sum of the reported values
    */
   private double sum = 0d;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.reporting.reporters.accumulators.Accumulator#add(java.lang.Object)
    */
   @Override
   public synchronized void add(final Double number) {
      sum = sum + number;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.reporting.reporters.accumulators.Accumulator#getResult()
    */
   @Override
   public synchronized Double getResult() {
      return sum;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.reporting.reporters.accumulators.Accumulator#reset()
    */
   @Override
   public synchronized void reset() {
      sum = 0d;
   }

}
