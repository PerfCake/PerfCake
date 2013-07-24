package org.perfcake.nreporting.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class HMSNumberFormat extends NumberFormat {

   /**
    * Serial ID.
    */
   private static final long serialVersionUID = -1L;

   /**
    * 
    */
   public static final long MILLIS_IN_HOUR = 3600000L;

   /**
    * 
    */
   public static final long MILLIS_IN_MINUTE = 60000L;

   /**
    * 
    */
   public static final long MILLIS_IN_SECOND = 1000L;

   /**
    * 
    */
   private static final String HOURS = "H";
   /**
    * 
    */
   private static final String MINUTES = "M";
   /**
    * 
    */
   private static final String SECONDS = "S";

   /**
    * 
    */
   private static NumberFormat numberFormat = new DecimalFormat("00");

   /**
    * 
    */
   private String pattern;

   /**
    * Creates a new instance of HMSNumberFormat.
    * 
    * @param pattern
    */
   public HMSNumberFormat(String pattern) {
      super();
      this.pattern = pattern;
   }

   /**
    * Creates a new instance of HMSNumberFormat.
    */
   public HMSNumberFormat() {
      super();
      this.pattern = null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.text.NumberFormat#format(double, java.lang.StringBuffer, java.text.FieldPosition)
    */
   @Override
   public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
      return format((long) number, toAppendTo, pos);
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.text.NumberFormat#format(long, java.lang.StringBuffer, java.text.FieldPosition)
    */
   @Override
   public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
      long hours = number / MILLIS_IN_HOUR;
      number = number % MILLIS_IN_HOUR;
      long minutes = number / MILLIS_IN_MINUTE;
      long seconds = (number % MILLIS_IN_MINUTE) / 1000;
      if (pattern == null) {
         toAppendTo.append(hours);
         toAppendTo.append(":");
         toAppendTo.append(numberFormat.format(minutes));
         toAppendTo.append(":");
         toAppendTo.append(numberFormat.format(seconds));
      } else {
         toAppendTo.append(pattern.replaceAll(HOURS, String.valueOf(hours)).replaceAll(MINUTES, String.valueOf(numberFormat.format(minutes))).replaceAll(SECONDS, String.valueOf(numberFormat.format(seconds))));
      }
      return toAppendTo;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.text.NumberFormat#parse(java.lang.String, java.text.ParsePosition)
    */
   @Override
   public Number parse(String source, ParsePosition parsePosition) {
      return HMSNumberFormat.parseTimeStamp(source);
   }

   /**
    * @param source
    * @return
    */
   public static long parseTimeStamp(String source) {
      String[] timeTokens = source.split(":");
      return Long.valueOf(timeTokens[0]) * MILLIS_IN_HOUR + Long.valueOf(timeTokens[1]) * MILLIS_IN_MINUTE + Long.valueOf(timeTokens[2]) * 1000;
   }

}
