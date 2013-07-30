package org.perfcake.nreporting.reporters.accumulators;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AccumulatorsTest {

   @Test
   public void lastValueAccumulatorTest() {
      LastValueAccumulator lva = new LastValueAccumulator();

      Assert.assertNull(lva.getResult(), "Clean LastValueAccumulator must have null result.");
   }
}
