package org.perfcake.reporting.reporters.accumulators;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

/**
 * Accumulates a value over a set number of recently reported values.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public abstract class AbstractSlidingWindowAccumulator implements Accumulator<Double> {

   protected final Buffer fifo;

   /**
    * Creates a new average accumulator with the sliding window of a given size.
    *
    * @param windowSize
    *       Size of the sliding window
    */
   public AbstractSlidingWindowAccumulator(final int windowSize) {
      fifo = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(windowSize));
   }

   @SuppressWarnings("unchecked")
   @Override
   public void add(final Double value) {
      fifo.add(value);
   }

   @Override
   public void reset() {
      fifo.clear();
   }

}
