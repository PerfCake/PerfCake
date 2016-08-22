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
package org.perfcake.reporting.destinations;

import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.ReportingException;

/**
 * Appends a {@link org.perfcake.reporting.Measurement} to standard output.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ConsoleDestination extends AbstractDestination {

   /**
    * ANSI Control Sequence Introducer.
    */
   private static final String CSI = "\u001B[";

   /**
    * ANSI Reset sequence.
    */
   private static final String ANSI_RESET = CSI + "0m";

   /**
    * Foreground color code. Can be an empty string to turn this feature off, a single number 0-7,
    * or three numbers between 0-255 separated by comma representing r, g, b channels.
    */
   private String foreground = null;

   /**
    * Background color code. Can be an empty string to turn this feature off, a single number 0-7,
    * or three numbers between 0-255 separated by comma representing r, g, b channels.
    */
   private String background = null;

   /**
    * Prefix of the console output.
    */
   private String prefix = "";

   /**
    * Precomputed output prefix.
    */
   private String intro = "";

   /**
    * Precomputed output suffix.
    */
   private String outro = "";

   private static String getAnsiFgColor(final int color) {
      if (color > 7 || color < 0) {
         return "";
      }

      return CSI + (30 + color) + "m";
   }

   private static String getAnsiBgColor(final int color) {
      if (color > 7 || color < 0) {
         return "";
      }

      return CSI + (40 + color) + "m";
   }

   private static String getAnsiExtendedFgColor(final byte r, final byte g, final byte b) {
      return CSI + "38;2;" + r + ";" + g + ";" + b + "m";
   }

   private static String getAnsiExtendedBgColor(final byte r, final byte g, final byte b) {
      return CSI + "38;2;" + r + ";" + g + ";" + b + "m";
   }

   @Override
   public void open() {
      intro = prefix;

      if ((background != null && !"".equals(background)) || (foreground != null && !"".equals(foreground))) {
         outro = ANSI_RESET;
      }
   }

   @Override
   public void close() {
      // nop
   }

   @Override
   public void report(final Measurement measurement) throws ReportingException {
      System.out.println(intro + measurement.toString() + outro);
   }

   /**
    * Gets the foreground color code.
    *
    * @return The foreground color code.
    */
   public String getForeground() {
      return foreground;
   }

   /**
    * Sets the foreground color code. Can be an empty string to turn this feature off, a single number 0-7,
    * or three numbers between 0-255 separated by comma representing r, g, b channels.
    *
    * @param foreground
    *       The foreground color code.
    */
   public void setForeground(final String foreground) {
      this.foreground = foreground;
   }

   /**
    * Gets the background color code.
    *
    * @return The background color code.
    */
   public String getBackground() {
      return background;
   }

   /**
    * Sets the background color code. Can be an empty string to turn this feature off, a single number 0-7,
    * or three numbers between 0-255 separated by comma representing r, g, b channels.
    *
    * @param background
    *       The background color code.
    */
   public void setBackground(final String background) {
      this.background = background;
   }

   /**
    * Gets the prefix of the console output.
    *
    * @return The prefix of the console output.
    */
   public String getPrefix() {
      return prefix;
   }

   /**
    * Sets the prefix of the console output.
    *
    * @param prefix
    *       The prefix of the console output.
    */
   public void setPrefix(final String prefix) {
      this.prefix = prefix;
   }
}
