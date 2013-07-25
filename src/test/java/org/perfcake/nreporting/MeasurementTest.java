package org.perfcake.nreporting;

import org.perfcake.nreporting.util.HMSNumberFormat;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MeasurementTest {
   private static final long HOURS = 729;
   private static final long MINUTES = 42;
   private static final long SECONDS = 9;

   private static final long PERCENTAGE = 15;
   private static final long TIMESTAMP = HOURS * HMSNumberFormat.MILLIS_IN_HOUR + MINUTES * HMSNumberFormat.MILLIS_IN_MINUTE + SECONDS * HMSNumberFormat.MILLIS_IN_SECOND;
   private static final long ITERATIONS = 12345;

   @Test
   public void testMeasurement() {
      Measurement m = new Measurement(PERCENTAGE, TIMESTAMP, ITERATIONS);
      m.set("18523.269 it/s");
      m.set("current", "257.58 it/s");
      m.set("average", "300.25 it/s");

      Assert.assertEquals(m.toString(), "[" + HOURS + ":" + MINUTES + ":0" + SECONDS + "][" + ITERATIONS + " iterations][" + PERCENTAGE + "%] [18523.269 it/s] [current => 257.58 it/s] [average => 300.25 it/s]");
   }
   
}
