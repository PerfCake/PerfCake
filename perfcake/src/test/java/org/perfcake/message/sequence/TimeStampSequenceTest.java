package org.perfcake.message.sequence;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class TimeStampSequenceTest {

   @Test
   public void testTimeStampSequence() throws Exception {
      final TimeStampSequence tss = new TimeStampSequence();

      int success = 0;
      for (int i = 0; i < 100; i++) {
         long curr = System.currentTimeMillis();
         long seq = Long.parseLong(tss.getNext());

         if (seq - curr <= 1) {
            success++;
         }

         Thread.sleep(1);
      }

      // the invocations are not at the same time precisely, so there might be some differences.
      Assert.assertTrue(success > 90);
   }
}