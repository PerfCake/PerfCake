/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.message.sequence;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.util.Utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.Properties;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "unit")
public class FilesContentSequenceTest extends TestSetup {

   @Test
   public void testSequence() throws PerfCakeException, MalformedURLException {
      final Properties p = new Properties();
      final FilesContentSequence s = new FilesContentSequence();
      s.setFileUrl(Utils.getResource("/sequences") + "/sequence-index.txt");

      s.reset();
      s.publishNext("seq1", p);
      Assert.assertEquals(p.getProperty("seq1"), "I'm the fish!");

      s.publishNext("seq1", p);
      Assert.assertTrue(p.getProperty("seq1").startsWith("Zdeněk"));

      s.publishNext("seq1", p);
      Assert.assertEquals(p.getProperty("seq1"), "I'm the fish!");
   }

   @Test
   public void testFailedSequence() throws PerfCakeException, MalformedURLException {
      final Properties p = new Properties();
      final FilesContentSequence s = new FilesContentSequence();
      s.setFileUrl(Utils.getResource("/sequences") + "/sequence-index-failed.txt");

      s.reset();

      s.publishNext("seq1", p);
      Assert.assertEquals(p.getProperty("seq1"), "I'm the fish!");

      p.remove("seq1");
      s.publishNext("seq1", p);
      Assert.assertNull(p.getProperty("seq1"));

      s.publishNext("seq1", p);
      Assert.assertTrue(p.getProperty("seq1").startsWith("Zdeněk"));

      p.remove("seq1");
      s.publishNext("seq1", p);
      Assert.assertNull(p.getProperty("seq1"));

      s.publishNext("seq1", p);
      Assert.assertEquals(p.getProperty("seq1"), "I'm the fish!");
   }

}