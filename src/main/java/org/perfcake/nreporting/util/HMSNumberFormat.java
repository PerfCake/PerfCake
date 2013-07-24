package org.perfcake.nreporting.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.ParseException;

/**
 * The number format that is able to format a timestamp (in milliseconds)
 * into a string in a form of "H:M:S", where H is unbounded number of hours, M is number of minutes
 * in the hour and S is number of seconds in the minute.
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class HMSNumberFormat extends NumberFormat {

   /**
    * Serial ID.
    */
   private static final long serialVersionUID = -1L;

   /**
    * Number of milliseconds in an hour.
    */
   public static final long MILLIS_IN_HOUR = 3600000L;

   /**
    * Number of milliseconds in a minute.
    */
   public static final long MILLIS_IN_MINUTE = 60000L;

   /**
    * Number of milliseconds in a second.
    */
   public static final long MILLIS_IN_SECOND = 1000L;

   /**
    * Pattern character representing hours.
    */
   private static final String HOURS = "H";

   /**
    * Pattern character representing minutes.
    */
   private static final String MINUTES = "M";

   /**
    * Pattern character representing seconds.
    */
   private static final String SECONDS = "S";

   /**
    * Number format for minutes and seconds to be rendered in 2 digits with padding 0.
    */
   private static NumberFormat numberFormat = new DecimalFormat("00");

   /**
    * The format pattern.
    */
   private String pattern;

   /**
    * Creates a new instance of HMSNumberFormat using the specified {@link #pattern}.
    * The pattern is converted into the resulting format where 'H' is replaced by the number of hours,
    * 'M' is replaced by the number of minutes in the hour and 'S' is replaces by the number of seconds
    * in the minute.
    * 
    * @param pattern
    *           The formating pattern.
    */
   public HMSNumberFormat(String pattern) {
      super();
      this.pattern = pattern;
   }

   /**
    * Creates a new instance of HMSNumberFormat with the default pattern "H:M:S".
    */
   public HMSNumberFormat() {
      super();
      this.pattern = "H:M:S";
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
      toAppendTo.append(pattern.replaceAll(HOURS, String.valueOf(hours)).replaceAll(MINUTES, String.valueOf(numberFormat.format(minutes))).replaceAll(SECONDS, String.valueOf(numberFormat.format(seconds))));
      return toAppendTo;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.text.NumberFormat#parse(java.lang.String, java.text.ParsePosition)
    */
   /*
    * (non-Javadoc)
    * 
    * @see java.text.NumberFormat#parse(java.lang.String, java.text.ParsePosition)
    */
   @Override
   public Number parse(String source, ParsePosition parsePosition) {
      try {
         return HMSNumberFormat.parseTimeStamp(source);
      } catch (ParseException pe) {
         pe.printStackTrace();
      }
      return null;
   }

   /**
    * Parses the timestamp (in milliseconds) from a string in a form of "H:M:S".
    * 
    * @param source
    *           The string representing the time.
    * @return The timestamp in milliseconds.
    */
   public static long parseTimeStamp(String source) throws ParseException {
      String[] timeTokens = source.split(":");
      if (timeTokens.length == 3) {
         return Long.valueOf(timeTokens[0]) * MILLIS_IN_HOUR + Long.valueOf(timeTokens[1]) * MILLIS_IN_MINUTE + Long.valueOf(timeTokens[2]) * 1000;
      } else {
         throw new ParseException("Couldn't parse \"" + source + "\". It should be in a form of \"H:M:S\".", -1);
      }
   }
}
