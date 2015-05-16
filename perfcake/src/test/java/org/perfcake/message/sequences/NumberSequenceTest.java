package org.perfcake.message.sequences;

import org.perfcake.PerfCakeException;

import org.testng.Assert;

/**
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class NumberSequenceTest {

   public void defaultTest() throws PerfCakeException {
      final NumberSequence s = new NumberSequence();
      s.reset();

      Assert.assertEquals(s.getStart(), 0);
      Assert.assertEquals(s.getEnd(), Long.MIN_VALUE);
      Assert.assertEquals(s.getStep(), 1);
      Assert.assertEquals(s.isCycle(), true);

      Assert.assertEquals(s.getNext(), "0");
      Assert.assertEquals(s.getNext(), "1");
      Assert.assertEquals(s.getNext(), "2");
   }

   public void overUnderFlowTest() throws PerfCakeException {
      NumberSequence s = new NumberSequence();
      s.setStart(Long.MAX_VALUE - 8);
      s.setStep(5);
      s.reset();

      Assert.assertEquals(s.getNext(), String.valueOf(Long.MAX_VALUE - 8));
      Assert.assertEquals(s.getNext(), String.valueOf(Long.MAX_VALUE - 3));
      Assert.assertEquals(s.getNext(), String.valueOf(Long.MIN_VALUE + 2));


      s.setStart(Long.MIN_VALUE + 8);
      s.setStep(-5);
      s.reset();
   }

}