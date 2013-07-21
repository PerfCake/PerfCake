package org.perfcake.nreporting;

import java.util.HashMap;
import java.util.Map;

public class MeasurementUnit {

   private long iteration;
   private long startTime = -1;
   private long stopTime = -1;
   private long totalTime = 0;

   private Map<String, Object> measurementProperties = new HashMap<>();
   private Map<String, Object> measurementResults = new HashMap<>();

   protected MeasurementUnit(long iteration) {
      this.iteration = iteration;
   }

   public void appendResult(String label, Object value) {
      measurementResults.put(label, value);
   }

   public void setProperty(String key, String value) {
      measurementProperties.put(key, value);
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

      return (stopTime - startTime) / 1000;
   }

   public long getIteration() {
      return iteration;
   }
}
