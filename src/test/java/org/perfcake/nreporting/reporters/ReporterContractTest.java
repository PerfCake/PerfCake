package org.perfcake.nreporting.reporters;

import org.junit.Assert;
import org.perfcake.nreporting.ReportingException;
import org.testng.annotations.Test;

public class ReporterContractTest {

   @Test
   public void noRunInfoTest() throws ReportingException {
      ResponseTimeReporter r = new ResponseTimeReporter();

      Exception e = null;
      try {
         r.report(null);
      } catch (ReportingException ee) {
         e = ee;
      }
      Assert.assertNotNull("An exception was supposed to be thrown as RunInfo was not set.", e);
   }
}
