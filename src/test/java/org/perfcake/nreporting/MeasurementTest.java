package org.perfcake.nreporting;

import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

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
