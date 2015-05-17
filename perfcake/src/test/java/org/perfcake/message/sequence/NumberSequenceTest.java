package org.perfcake.message.sequence;

import org.perfcake.PerfCakeException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class NumberSequenceTest {

   @Test
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

   @Test
   public void overUnderFlowTest() throws PerfCakeException {
      NumberSequence s = new NumberSequence();
      s.setStart(Long.MAX_VALUE - 8);
      s.setStep(5);
      s.reset();

      Assert.assertEquals(s.getNext(), String.valueOf(Long.MAX_VALUE - 8));
      Assert.assertEquals(s.getNext(), String.valueOf(Long.MAX_VALUE - 3));
      Assert.assertEquals(s.getNext(), String.valueOf(Long.MIN_VALUE + 1));

      s.setStart(Long.MIN_VALUE + 8);
      s.setStep(-5);
      s.reset();

      Assert.assertEquals(s.getNext(), String.valueOf(Long.MIN_VALUE + 8));
      Assert.assertEquals(s.getNext(), String.valueOf(Long.MIN_VALUE + 3));
      Assert.assertEquals(s.getNext(), String.valueOf(Long.MAX_VALUE - 1));
   }

   @Test
   public void incTest() throws PerfCakeException {
      NumberSequence s = new NumberSequence();
      s.setStart(10);
      s.setEnd(20);
      s.setStep(3);
      s.reset();

      Assert.assertEquals(s.getNext(), "10");
      Assert.assertEquals(s.getNext(), "13");
      Assert.assertEquals(s.getNext(), "16");
      Assert.assertEquals(s.getNext(), "19");
      Assert.assertEquals(s.getNext(), "10");
      Assert.assertEquals(s.getNext(), "13");
   }

   @Test
   public void decTest() throws PerfCakeException {
      NumberSequence s = new NumberSequence();
      s.setStart(20);
      s.setEnd(10);
      s.setStep(-3);
      s.reset();

      Assert.assertEquals(s.getNext(), "20");
      Assert.assertEquals(s.getNext(), "17");
      Assert.assertEquals(s.getNext(), "14");
      Assert.assertEquals(s.getNext(), "11");
      Assert.assertEquals(s.getNext(), "20");
      Assert.assertEquals(s.getNext(), "17");
   }

}