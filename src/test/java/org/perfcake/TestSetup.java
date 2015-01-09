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
package org.perfcake;

import org.perfcake.util.Utils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Does the necessary configurations for the tests to work.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class TestSetup {

   @BeforeClass(alwaysRun = true)
   public void configureLocations() throws Exception {
      System.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, Utils.getResource("/scenarios"));
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, Utils.getResource("/messages"));
   }

   @AfterClass(alwaysRun = true)
   public void unsetLocations() {
      System.clearProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY);
      System.clearProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY);
   }
}
