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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "unit")
public class RandomUuidSequenceTest {

   @Test
   public void basicTest() {
      Sequence s = new RandomUuidSequence();

      final Properties props = new Properties();
      s.publishNext("v1", props);
      s.publishNext("v2", props);

      Assert.assertNotEquals(props.getProperty("v1"), props.getProperty("v2"));
      Assert.assertEquals(props.getProperty("v1").length(), 8 * 4 + 4);
   }

}