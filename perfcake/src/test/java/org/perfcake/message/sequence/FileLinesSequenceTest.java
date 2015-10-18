package org.perfcake.message.sequence;

import org.perfcake.TestSetup;
import org.perfcake.util.Utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class FileLinesSequenceTest extends TestSetup {

   @Test
   public void testFileLinesSequence() throws Exception {
      final String sequencesDir = Utils.getResource("/sequences");
      final FileLinesSequence fls = new FileLinesSequence();
      final String fileUrl = Utils.locationToUrlWithCheck("seq", null, sequencesDir, ".txt").toString();

      fls.setFileUrl(fileUrl);
      fls.reset();

      Assert.assertEquals(fls.getNext(), "AAA");
      Assert.assertEquals(fls.getNext(), "");
      Assert.assertEquals(fls.getNext(), "bb");
      Assert.assertEquals(fls.getNext(), "c");
      Assert.assertEquals(fls.getNext(), "AAA");
   }

}