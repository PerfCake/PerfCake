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
import org.perfcake.util.StringUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

/**
 * Appends a {@link org.perfcake.reporting.Measurement} to standard output.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ConsoleDestination extends AbstractDestination {

   /**
    * Logger.
    */
   private static final Logger log = LogManager.getLogger(ConsoleDestination.class);

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

   /**
    * Gets ANSI code for foreground color.
    *
    * @param color
    *       The foreground color in range 0 - 15.
    * @return The ANSI code for foreground color.
    */
   private static String getAnsiFgColor(final int color) {
      return CSI + (30 + (color % 8)) + (color > 7 ? ";1" : "") + "m";
   }

   /**
    * Gets ANSI code for background color.
    *
    * @param color
    *       The background color in range 0 - 7.
    * @return The ANSI code for background color.
    */
   private static String getAnsiBgColor(final int color) {
      return CSI + (40 + color) + "m";
   }

   /**
    * Parses color configuration. Returns either a single number in range 0 - 15, or -1 if the color was wrong.
    *
    * @param color
    *       The color configuration string.
    * @return Individual parts of color configuration. <code>-1</code> when the input did not match the supported formats.
    */
   private static int parseColor(final String color) {
      int colorNo = -1;

      try {
         colorNo = Integer.valueOf(StringUtil.trim(color));
         if (colorNo > 15 || colorNo < 0) {
            colorNo = -1;
         }
      } catch (NumberFormatException e) {
         // nop
      }

      return colorNo;
   }

   @Override
   public void open() {
      intro = "";

      if (background != null && !"".equals(background)) {
         final int color = parseColor(background);
         if (color != -1) {
            intro = getAnsiBgColor(color) + intro;
         } else {
            log.warn("Unable to parse background color '{}'", background);
         }
      }

      if (foreground != null && !"".equals(foreground)) {
         final int color = parseColor(foreground);
         if (color != -1) {
            intro = getAnsiFgColor(color) + intro;
         } else {
            log.warn("Unable to parse foreground color '{}'", foreground);
         }
      }

      if (!"".equals(intro)) {
         outro = ANSI_RESET;
      } else {
         outro = "";
      }

      intro = intro + prefix;
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
