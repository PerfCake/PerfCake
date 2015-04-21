/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.validation;

import org.perfcake.message.Message;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FileQueueTest {

   @Test
   public void fileQueueTest() throws Exception {
      final File tmpFile = File.createTempFile("perfcake", "queue");
      tmpFile.deleteOnExit();
      final FileQueue<Message> queue = new FileQueue<>(tmpFile);
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