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
import org.perfcake.util.Utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "integration" })
public class RawReporterTest extends TestSetup {

   @Test
   public void rawReporterBasicTest() throws PerfCakeException, InterruptedException, IOException {
      final File outputFile = File.createTempFile("perfcake", ".raw");
      final File outputResultFile = new File(outputFile.getAbsolutePath() + "-result.csv");
      final File outputReplayFile = new File(outputFile.getAbsolutePath() + "-replay.csv");
      System.setProperty("output.file", outputFile.getAbsolutePath());
      outputFile.deleteOnExit();
      outputResultFile.deleteOnExit();
      outputReplayFile.deleteOnExit();

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

      List<String> origLines = Utils.readLines(outputResultFile.toURI().toURL());
      List<String> replayLines = Utils.readLines(outputReplayFile.toURI().toURL());

      Assert.assertTrue(origLines.size() == replayLines.size()); // not equals, there is no expected value

      // the time shifts a little bit
      for (int i = 0; i < origLines.size(); i++) {
         String origLine = origLines.get(i).split(";", 2)[1];
         String replayLine = replayLines.get(i).split(";", 2)[1];
         Assert.assertTrue(origLine.equals(replayLine));
      }

      // there must be some time information at the end
      final String time = replayLines.get(replayLines.size() - 1).split(";", 2)[0];
      Assert.assertTrue(time.equals("0:00:02") || time.equals("0:00:03"));
   }
}