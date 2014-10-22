package org.perfcake.validation;

import org.perfcake.message.Message;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public class FileQueueTest {

   @Test
   public void fileQueueTest() throws Exception {
      final File tmpFile = File.createTempFile("perfcake", "queue");
      tmpFile.deleteOnExit();
      FileQueue<Message> queue = new FileQueue<>(tmpFile);
      Message m = new Message("A tribute to Benjamin C. Bradlee");

      queue.add(m);
      queue.add(m);

      Assert.assertEquals(queue.size(), 2);
      Assert.assertFalse(queue.isEmpty());

      m = queue.poll();
      Assert.assertNotNull(m);
      Assert.assertEquals(queue.size(), 1);

      queue.add(m);
      Assert.assertEquals(queue.size(), 2);

      m = queue.poll();
      m = queue.poll();
      Assert.assertNotNull(m);
      m = queue.poll();
      Assert.assertNull(m);
      Assert.assertEquals(queue.size(), 0);
      Assert.assertTrue(queue.isEmpty());
   }
}