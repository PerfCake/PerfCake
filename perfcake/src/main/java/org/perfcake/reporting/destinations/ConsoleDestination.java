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
    *       The foreground color in range 0 - 7.
    * @return The ANSI code for foreground color.
    */
   private static String getAnsiFgColor(final int color) {
      return CSI + (30 + color) + "m";
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
    * Gets extended ANSI code for foreground color.
    *
    * @param r
    *       Red color value in range 0 - 255.
    * @param g
    *       Green color value in range 0 - 255.
    * @param b
    *       Blue color value in range 0 - 255.
    * @return The extended ANSI code for foreground color.
    */
   private static String getAnsiExtendedFgColor(final int r, final int g, final int b) {
      return CSI + "38;2;" + r + ";" + g + ";" + b + "m";
   }

   /**
    * Gets extended ANSI code for background color.
    *
    * @param r
    *       Red color value in range 0 - 255.
    * @param g
    *       Green color value in range 0 - 255.
    * @param b
    *       Blue color value in range 0 - 255.
    * @return The extended ANSI code for background color.
    */
   private static String getAnsiExtendedBgColor(final int r, final int g, final int b) {
      return CSI + "48;2;" + r + ";" + g + ";" + b + "m";
   }

   /**
    * Parses color configuration. Returns either a single number in range 0 - 7, or three numbers in range 0 - 255.
    *
    * @param color
    *       The color configuration string.
    * @return Individual parts of color configuration. <code>null</code> when the input did not matche the supported formats.
    */
   private static int[] parseColor(final String color) {
      final LongAdder fails = new LongAdder();
      int[] colors = Arrays.stream(color.split(",")).mapToInt(s -> {
         try {
            return Integer.valueOf(StringUtil.trim(s));
         } catch (NumberFormatException e) {
            fails.increment(); // count failures
            return -1;
         }
      }).toArray();

      if (fails.longValue() == 0) { // no failures so far
         if (colors.length == 1) { // we have a single color, check for correct value and return it
            if (colors[0] >= 0 && colors[0] <= 7) {
               return colors;
            }
         } else if (colors.length == 3) { // we have r,g,b, check their values
            for (int i = 0; i < 3; i++) {
               if (colors[i] > 255 || colors[i] < 0) { // if we are out of range, return null
                  return null;
               }
            }
            return colors; // the check has passed, return colors
         }
      }

      return null; // there were failures, get out of here
   }

   @Override
   public void open() {
      intro = "";

      if (background != null && !"".equals(background)) {
         final int[] colors = parseColor(background);
         if (colors != null) {
            if (colors.length == 1) {
               intro = getAnsiBgColor(colors[0]) + intro;
            } else if (colors.length == 3) {
               intro = getAnsiExtendedBgColor(colors[0], colors[1], colors[2]) + intro;
            }
         } else {
            log.warn("Unable to parse background color '{}'", background);
         }
      }

      if (foreground != null && !"".equals(foreground)) {
         final int[] colors = parseColor(foreground);
         if (colors != null) {
            if (colors.length == 1) {
               intro = getAnsiFgColor(colors[0]) + intro;
            } else if (colors.length == 3) {
               intro = getAnsiExtendedFgColor(colors[0], colors[1], colors[2]) + intro;
            }
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
