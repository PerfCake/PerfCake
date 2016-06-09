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
package org.perfcake.reporting.reporters;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.scenario.ReplayResults;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RawReporterTest extends TestSetup {

   @Test(enabled = false)
   public void rawReporterBasicTest() throws PerfCakeException, InterruptedException, IOException {
      final File outputFile = File.createTempFile("perfcake", ".raw");
      System.setProperty("output.file", outputFile.getAbsolutePath());
      //outputFile.deleteOnExit();
      System.out.println(outputFile.getAbsolutePath());

      final Scenario scenario = ScenarioLoader.load("test-raw");

      ScenarioRetractor sr = new ScenarioRetractor(scenario);
      sr.getReportManager();

      scenario.init();
      scenario.run();
      scenario.close();

      final Scenario scenarioCopy = ScenarioLoader.load("test-raw-replay");
      final ReplayResults replay = new ReplayResults(scenarioCopy, outputFile.getAbsolutePath());
      replay.replay();
      replay.close();
   }
}