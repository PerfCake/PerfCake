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
package org.perfcake.message.generator.profile;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.common.Period;
import org.perfcake.common.PeriodType;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = "unit")
public class CsvProfileTest extends TestSetup {

   @Test
   public void testBasic() throws PerfCakeException {
      final CsvProfile profile = new CsvProfile();
      profile.init(System.getProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY) + "/test-profile-speed.csv");

      ProfileRequest request;

      request = profile.getProfile(new Period(PeriodType.ITERATION, 0));
      Assert.assertEquals(request.getThreads(), 10);
      Assert.assertEquals(request.getSpeed(), 100);

      request = profile.getProfile(new Period(PeriodType.ITERATION, 100));
      Assert.assertEquals(request.getThreads(), 10);
      Assert.assertEquals(request.getSpeed(), 100);

      request = profile.getProfile(new Period(PeriodType.ITERATION, 250));
      Assert.assertEquals(request.getThreads(), 10);
      Assert.assertEquals(request.getSpeed(), 200);

      request = profile.getProfile(new Period(PeriodType.ITERATION, 500));
      Assert.assertEquals(request.getThreads(), 10);
      Assert.assertEquals(request.getSpeed(), 100);
   }
}