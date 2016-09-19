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
package org.perfcake.scenario;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.util.Utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Tests loading of the scenario in various ways using PerfCake API.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScenarioLoaderExtraTest {

   @Test
   public void scenarioLoadTest() throws PerfCakeException {
      final String fileName = Utils.getResource("/scenarios/test-scenario.xml");
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, Utils.getResource("/messages"));

      Assert.assertTrue(Files.exists(Paths.get(fileName)));

      ScenarioLoader.load("file://" + fileName);
      ScenarioLoader.load(fileName);
   }
}
