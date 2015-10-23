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
package org.perfcake.reporting;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * Tests {@link org.perfcake.reporting.Measurement}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class MeasurementTest {
   private static final long HOURS = 729;
   private static final long MINUTES = 42;
   private static final long SECONDS = 9;

   private static final long PERCENTAGE = 15;
   private static final long TIMESTAMP = TimeUnit.HOURS.toMillis(HOURS) + TimeUnit.MINUTES.toMillis(MINUTES) + TimeUnit.SECONDS.toMillis(SECONDS);
   private static final long ITERATIONS = 12345;

   @Test
   public void testMeasurement() {
      final Measurement m = new Measurement(PERCENTAGE, TIMESTAMP, ITERATIONS - 1); // iterations are indexed from 0 and reported from 1
      m.set("18523.269 it/s");
      m.set("current", "257.58 it/s");
      m.set("average", "300.25 it/s");

      Assert.assertEquals(m.toString(), "[" + HOURS + ":" + MINUTES + ":0" + SECONDS + "][" + ITERATIONS + " iterations][" + PERCENTAGE + "%] [18523.269 it/s] [current => 257.58 it/s] [average => 300.25 it/s]");
   }

}
