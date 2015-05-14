package org.perfcake.message.sequences;

import static org.testng.Assert.*;

import org.perfcake.TestSetup;
import org.perfcake.util.Utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Martin Večeřa <marvenec@gmail.com>
 */
@Test(groups = { "unit" })
public class FileLinesSequenceTest extends TestSetup {

   @Test
   public void testFileLinesSequence() throws Exception {
      final FileLinesSequence fls = new FileLinesSequence();
      fls.setFileUrl("file://" + Utils.getResource("/sequences/seq.txt"));
      fls.reset();

      Assert.assertEquals(fls.getNext(), "AAA");
      Assert.assertEquals(fls.getNext(), "");
      Assert.assertEquals(fls.getNext(), "bb");
      Assert.assertEquals(fls.getNext(), "c");
      Assert.assertEquals(fls.getNext(), "AAA");
   }

}