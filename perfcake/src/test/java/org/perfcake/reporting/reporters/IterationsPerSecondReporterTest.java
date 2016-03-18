package org.perfcake.reporting.reporters;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.destinations.Destination;
import org.perfcake.reporting.destinations.DummyDestination;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tests {@link org.perfcake.reporting.reporters.IterationsPerSecondReporter} implemetation.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Test(groups = { "unit" })
public class IterationsPerSecondReporterTest extends TestSetup {
   @Test
   public void testIterationsPerSecondReporter() throws PerfCakeException, InterruptedException {
      Scenario scenario = ScenarioLoader.load("test-scenario-iterations-per-second");
      ScenarioRetractor retractor = new ScenarioRetractor(scenario);
      List<Reporter> reporterList = new ArrayList<>(retractor.getReportManager().getReporters());
      Assert.assertEquals(reporterList.size(), 1, "Reporter number.");
      Set<Destination> destinationSet = reporterList.get(0).getDestinations();
      Assert.assertEquals(destinationSet.size(), 1, "Destination number");
      DummyDestination destination = null;
      for (Destination dest : destinationSet) {
         if (dest instanceof DummyDestination) {
            destination = (DummyDestination) dest;
            break;
         }
      }
      Assert.assertNotNull(destination, DummyDestination.class.getName() + " is missing from scenario.");
      scenario.init();
      scenario.run();
      final double result = (Double) ((Quantity<Number>) destination.getLastMeasurement().get()).getNumber();
      Assert.assertTrue(result > 80.0 && result < 100.0, "The final result ( " + result + " ) should be > 99 and < 100.");
   }
}
