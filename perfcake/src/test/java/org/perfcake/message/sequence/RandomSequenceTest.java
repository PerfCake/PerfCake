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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "unit")
public class RandomSequenceTest {

   @Test
   public void rangeTest() throws PerfCakeException {
      final RandomSequence seq = new RandomSequence();
      int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
      seq.reset();
      seq.setMin(10);
      seq.setMax(384);

      final Properties props = new Properties();
      for (int i = 0; i < 65535; i++) {
         seq.publishNext("v1", props);
         int r = Integer.valueOf(props.getProperty("v1"));
         min = Math.min(min, r);
         max = Math.max(max, r);
      }

      // with the given number of tries, it is highly probable that we already hit the boundaries
      Assert.assertEquals(min, 10);
      Assert.assertEquals(max, 383);

      Assert.assertEquals(seq.getMin(), 10);
      Assert.assertEquals(seq.getMax(), 384);
   }
}