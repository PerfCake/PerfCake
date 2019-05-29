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
package org.perfcake.reporting;

import org.perfcake.PerfCakeConst;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A result of the smallest measurement unit - an iteration.
 * One should obtain a new instance of a MeasurementUnit using {@link org.perfcake.reporting.ReportManager#newMeasurementUnit()}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MeasurementUnit implements Serializable {

   private static final long serialVersionUID = 3596375306594505085L;

   /**
    * Logger.
    */
   private static final Logger log = LogManager.getLogger(MeasurementUnit.class);

   /**
    * Iteration for which this unit was created.
    */
   private final long iteration;

   /**
    * Time when last measurement started. A unit may accumulate more measurements together.
    */
   private long startTime = -1;

   /**
    * Time when last measurement ended.
    */
   private long stopTime = -1;

   /**
    * Total measured time.
    */
   private double totalTime = 0;

   /**
    * Custom results reported by a sender.
    */
   private final Map<String, Object> measurementResults = new HashMap<>();

   /**
    * When the measurement was first started in real time (timestamp value from {@link System#currentTimeMillis()}).
    */
   private long timeStarted = -1;

   /**
    * Failure that happened during processing of this task.
    */
   private Exception failure = null;

   /**
    * Time when the sender request was enqueued. By default we assume this is the creation time.
    */
   private long enqueueTime = System.nanoTime();

   /**
    * Constructor is protected. Use {@link org.perfcake.reporting.ReportManager#newMeasurementUnit()} to obtain a new instance.
    *
    * @param iteration
    *       Current iteration number.
    */
   protected MeasurementUnit(final long iteration) {
      this.iteration = iteration;
      measurementResults.put(PerfCakeConst.FAILURES_TAG, 0L);
   }

   /**
    * Appends a custom result.
    *
    * @param label
    *       The label of the result.
    * @param value
    *       The value of the result.
    */
   public void appendResult(final String label, final Object value) {
      measurementResults.put(label, value);
   }

   /**
    * Gets immutable map with all the custom results.
    *
    * @return An immutable copy of the custom results map.
    */
   public Map<String, Object> getResults() {
      return Collections.unmodifiableMap(measurementResults);
   }

   /**
    * Gets a custom result for the given label.
    *
    * @param label
    *       The label of the custom result.
    * @return The value for the given custom result.
    */
   public Object getResult(final String label) {
      return measurementResults.get(label);
   }

   /**
    * Starts measuring. This is independent on current system time.
    */
   public void startMeasure() {
      timeStarted = System.currentTimeMillis();
      startTime = System.nanoTime();
      stopTime = -1;
   }

   /**
    * Stops measuring.
    */
   public void stopMeasure() {
      stopTime = System.nanoTime();
      totalTime = totalTime + getLastTime();
   }

   /**
    * Gets total time measured during all measurements done by this Measurement Unit (all time periods between calls to {@link #startMeasure()} and {@link #stopMeasure()} in milliseconds.
    *
    * @return The total time measured by this unit in milliseconds.
    */
   public double getTotalTime() {
      return totalTime;
   }

   /**
    * Gets the start time of the measurement in nanoseconds.
    *
    * @return The start time of the measurement in nanoseconds.
    */
   public long getStartTime() {
      return startTime;
   }

   /**
    * Gets the stop time of the measurement in nanoseconds.
    *
    * @return The stop time of the measurement in nanoseconds.
    */
   public long getStopTime() {
      return stopTime;
   }

   /**
    * Gets time of the last measurement (time period between calls to {@link #startMeasure()} and {@link #stopMeasure()} in milliseconds.
    *
    * @return Time of the last measurement in milliseconds, -1 when there was no measurement recorded yet.
    */
   public double getLastTime() {
      if (startTime == -1 || stopTime == -1) {
         return -1;
      }

      if (stopTime - startTime == 0) {
         log.warn("Zero time measured! PerfCake is probably running on a machine where the internal timer does not provide enough resolution (e.g. a virtual machine). "
               + "Please refer to the Troubleshooting section in the User Guide.\nCurrent measurement unit: " + this.toString());
      }

      return (stopTime - startTime) / 1_000_000d;
   }

   /**
    * Gets the total service time between enqueuing the sender task and its completion.
    *
    * @return The total service time between enqueuing the sender task and its completion, -1 when there was no measurement recorded yet.
    */
   public double getServiceTime() {
      if (startTime == -1 || stopTime == -1) {
         return -1;
      }

      return (stopTime - enqueueTime) / 1_000_000d;
   }

   /**
    * Checks whether this measurement unit was first started after the specified time (Unix time in millis).
    *
    * @param ref
    *       The reference time to compare to the start of the measurement.
    * @return <code>true</code> if this measurement unit was first started after the specified reference time.
    */
   public boolean startedAfter(final long ref) {
      return timeStarted > ref;
   }

   /**
    * Gets the number of current iteration of this Measurement Unit.
    *
    * @return The number of iteration.
    */
   public long getIteration() {
      return iteration;
   }

   /**
    * Gets the failure that happened during processing of this task.
    *
    * @return The exception that occurred or null if there was no exception.
    */
   public Exception getFailure() {
      return failure;
   }

   /**
    * Sets the exception that happened during processing of this task to be remembered and reported.
    *
    * @param failure
    *       The exception that happened or null to clear the failure flag.
    */
   public void setFailure(final Exception failure) {
      if (failure != null) {
         measurementResults.put(PerfCakeConst.FAILURES_TAG, 1L);
      } else {
         measurementResults.put(PerfCakeConst.FAILURES_TAG, 0L);
      }
      this.failure = failure;
   }

   /**
    * Gets the time when the current sender request was enqueued.
    *
    * @return The time when the current sender request was enqueued.
    */
   public long getEnqueueTime() {
      return enqueueTime;
   }

   /**
    * Sets the time when the current sender request was enqueued.
    *
    * @param enqueueTime
    *       The time when the current sender request was enqueued.
    */
   public void setEnqueueTime(final long enqueueTime) {
      this.enqueueTime = enqueueTime;
   }

   @Override
   public int hashCode() {
      int result;
      result = (int) (iteration ^ (iteration >>> 32));
      result = 31 * result + (int) (startTime ^ (startTime >>> 32));
      result = 31 * result + (int) (stopTime ^ (stopTime >>> 32));
      long temp = Double.doubleToLongBits(totalTime);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      result = 31 * result + measurementResults.hashCode();
      result = 31 * result + (int) (timeStarted ^ (timeStarted >>> 32));
      result = 31 * result + (int) (enqueueTime ^ (enqueueTime >>> 32));
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }

      final MeasurementUnit that = (MeasurementUnit) obj;

      if (iteration != that.iteration) {
         return false;
      }
      if (startTime != that.startTime) {
         return false;
      }
      if (stopTime != that.stopTime) {
         return false;
      }
      if (timeStarted != that.timeStarted) {
         return false;
      }
      if (enqueueTime != that.enqueueTime) {
         return false;
      }
      if (Double.compare(that.totalTime, totalTime) != 0) {
         return false;
      }
      if (!measurementResults.equals(that.measurementResults)) {
         return false;
      }

      return true;
   }

   @Override
   public String toString() {
      return "MeasurementUnit ["
            + "iteration=" + iteration
            + ", enqueueTime=" + enqueueTime
            + ", startTime=" + startTime
            + ", stopTime=" + stopTime
            + ", totalTime=" + totalTime
            + ", measurementResults=" + measurementResults
            + ", timeStarted=" + timeStarted
            + ']';
   }

   /**
    * Writes this instance into output stream with a minimum data needed. This serves for easy transfer of serialized Mesurement Units.
    *
    * @param oos
    *       Stream to write the data to.
    * @throws IOException
    *       When there was an error writing the data.
    */
   public void streamOut(ObjectOutputStream oos) throws IOException {
      oos.writeLong(iteration);
      oos.writeLong(startTime);
      oos.writeLong(stopTime);
      oos.writeLong(timeStarted);
      oos.writeLong(enqueueTime);
      oos.writeDouble(totalTime);

      // write FAILURES_TAG
      final Long failures = (Long) measurementResults.get(PerfCakeConst.FAILURES_TAG);
      oos.writeLong(failures == null ? 0 : failures);

      // write THREADS_TAG
      final Integer threads = (Integer) measurementResults.get(PerfCakeConst.THREADS_TAG);
      oos.writeInt(threads == null ? 0 : threads);

      // write REQUEST_SIZE_TAG
      final Long requestSize = (Long) measurementResults.get(PerfCakeConst.REQUEST_SIZE_TAG);
      oos.writeLong(requestSize == null ? 0 : requestSize);

      // write RESPONSE_SIZE_TAG
      final Long responseSize = (Long) measurementResults.get(PerfCakeConst.RESPONSE_SIZE_TAG);
      oos.writeLong(responseSize == null ? 0 : responseSize);

      // write all results from the map except for FAILURES_TAG and properties under ATTRIBUTES_TAG
      int size = measurementResults.size() - (measurementResults.get(PerfCakeConst.ATTRIBUTES_TAG) != null ? 1 : 0)
            - (failures != null ? 1 : 0) - (threads != null ? 1 : 0) - (requestSize != null ? 1 : 0) - (responseSize != null ? 1 : 0);
      oos.writeInt(size);
      measurementResults.forEach((key, value) -> {
         if (!PerfCakeConst.ATTRIBUTES_TAG.equals(key) && !PerfCakeConst.FAILURES_TAG.equals(key) && !PerfCakeConst.THREADS_TAG.equals(key)
               && !PerfCakeConst.REQUEST_SIZE_TAG.equals(key) && !PerfCakeConst.RESPONSE_SIZE_TAG.equals(key)) {
            try {
               oos.writeUTF(key);
               oos.writeObject(value);
            } catch (IOException e) {
               log.warn("Unable to serialize Measurement Unit: ", e);
            }
         }
      });

      // write properties under ATTRIBUTES_TAG
      final Properties props = (Properties) measurementResults.get(PerfCakeConst.ATTRIBUTES_TAG);
      if (props == null) {
         oos.writeLong(0);
         oos.writeInt(0);
      } else {

         //write iteration number in the attributes
         final String iteration = props.getProperty(PerfCakeConst.ITERATION_NUMBER_PROPERTY);
         oos.writeLong(iteration == null ? 0 : Long.parseLong(iteration));

         // write all remaining attributes
         oos.writeInt(props.size() - (iteration != null ? 1 : 0));
         props.forEach((key, value) -> {
            if (!PerfCakeConst.ITERATION_NUMBER_PROPERTY.equals(key)) {
               try {
                  oos.writeUTF((String) key);
                  oos.writeUTF((String) value);
               } catch (IOException e) {
                  log.warn("Unable to serialize Measurement Unit: ", e);
               }
            }
         });
      }

      // write exception if it is stored
      oos.writeInt(failure == null ? 0 : 1);
      if (failure != null) {
         oos.writeObject(failure);
      }
   }

   /**
    * Reads the minimalistic serialization of Measurement Unit from the input stream.
    *
    * @param in
    *       The stream to read the object data from.
    * @return The deserialized {@link MeasurementUnit}.
    * @throws ClassNotFoundException
    *       When it is not possible to restore an unknown class from the data.
    * @throws IOException
    *       When there was an I/O error reading data.
    */
   public static MeasurementUnit streamIn(ObjectInputStream in) throws ClassNotFoundException, IOException {
      final MeasurementUnit mu = new MeasurementUnit(in.readLong());
      mu.startTime = in.readLong();
      mu.stopTime = in.readLong();
      mu.timeStarted = in.readLong();
      mu.enqueueTime = in.readLong();
      mu.totalTime = in.readDouble();

      mu.measurementResults.put(PerfCakeConst.FAILURES_TAG, in.readLong());
      mu.measurementResults.put(PerfCakeConst.THREADS_TAG, in.readInt());
      mu.measurementResults.put(PerfCakeConst.REQUEST_SIZE_TAG, in.readLong());
      mu.measurementResults.put(PerfCakeConst.RESPONSE_SIZE_TAG, in.readLong());

      int size = in.readInt();
      for (int i = 0; i < size; i++) {
         mu.measurementResults.put(in.readUTF(), in.readObject());
      }

      final Properties props = new Properties();
      mu.measurementResults.put(PerfCakeConst.ATTRIBUTES_TAG, props);

      props.setProperty(PerfCakeConst.ITERATION_NUMBER_PROPERTY, String.valueOf(in.readLong()));

      size = in.readInt();
      for (int i = 0; i < size; i++) {
         props.setProperty(in.readUTF(), in.readUTF());
      }

      final int isFailure = in.readInt();
      if (isFailure > 0) {
         mu.failure = (Exception) in.readObject();
      }

      return mu;
   }
}