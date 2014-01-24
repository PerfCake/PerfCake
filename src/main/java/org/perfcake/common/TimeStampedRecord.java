package org.perfcake.common;

/**
 * A record that is valid for a particular time stamp.
 * The record has a time stamp and a value.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 **/
public class TimeStampedRecord<T extends Number> {

   /**
    * Time stamp.
    **/
   private long timeStamp;

   /**
    * The value.
    **/
   private T value;

   /**
    * Creates a new record.
    * 
    * @param timeStamp
    *           The time stamp of the record.
    * @param value
    *           The value of the record.
    **/
   public TimeStampedRecord(long timeStamp, T value) {
      super();
      this.timeStamp = timeStamp;
      this.value = value;
   }

   /**
    * Used to read the value of timeStamp.
    * 
    * @return The timeStamp value.
    */
   public long getTimeStamp() {
      return timeStamp;
   }

   /**
    * Used to set the value of timeStamp.
    * 
    * @param timeStamp
    *           The timeStamp value to set.
    */
   public void setTimeStamp(long timeStamp) {
      this.timeStamp = timeStamp;
   }

   /**
    * Used to read the value.
    * 
    * @return The value.
    */
   public T getValue() {
      return value;
   }

   /**
    * Used to set the value of value.
    * 
    * @param value
    *           The value to set.
    */
   public void setValue(T value) {
      this.value = value;
   }

}
