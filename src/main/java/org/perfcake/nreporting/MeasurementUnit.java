package org.perfcake.nreporting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MeasurementUnit {

   private final long iteration;
   private long startTime = -1;
   private long stopTime = -1;
   private long totalTime = 0;

   private final Map<String, Object> measurementResults = new HashMap<>();

   protected MeasurementUnit(final long iteration) {
      this.iteration = iteration;
   }

   public void appendResult(final String label, final Object value) {
      measurementResults.put(label, value);
   }

   public Map<String, Object> getResults() {
      return Collections.unmodifiableMap(measurementResults);
   }

   public Object getResult(final String label) {
      return measurementResults.get(label);
   }

   public void startMeasure() {
      startTime = System.nanoTime();
      stopTime = -1;
   }

   public void stopMeasure() {
      stopTime = System.nanoTime();
      totalTime = totalTime + getLastTime();
   }

   public long getTotalTime() {
      return totalTime;
   }

   public long getLastTime() {
      if (startTime == -1 || stopTime == -1) {
         return -1;
      }

      return (stopTime - startTime) / 1_000_000;
   }

   public long getIteration() {
      return iteration;
   }
}
