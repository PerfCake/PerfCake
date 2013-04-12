/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * 
 * @author Martin Večeřa <marvec@gmail.com>, Pavel Macík <pavel.macik@gmail.com>
 */
public class StringUtil {
   private static Logger log = Logger.getLogger(StringUtil.class);

   public static boolean startsWithIgnoreCase(String haystack, String needle) {
      return needle.equalsIgnoreCase(haystack.substring(0, needle.length()));
   }

   public static boolean endsWithIgnoreCase(String haystack, String needle) {
      return needle.equalsIgnoreCase(haystack.substring(haystack.length() - needle.length(), haystack.length()));
   }

   public static boolean containsIgnoreCase(String haystack, String needle) {
      return haystack.toLowerCase().indexOf(needle.toLowerCase()) >= 0;
   }

   public static String trim(String str) {
      return trim(str, " \n\t'\"");
   }

   /**
    * Remove leading and ending white lines.
    * 
    * @param str
    * @param trimStr
    * @return
    */
   public static String trim(String str, String trimStr) {
      int start = 0;
      while (start < str.length() && trimStr.indexOf(str.charAt(start)) >= 0) {
         start++;
      }

      int stop = str.length() - 1;
      while (stop > 0 && trimStr.indexOf(str.charAt(stop)) >= 0) {
         stop--;
      }
      if (start > stop) {
         if (log.isEnabledFor(Level.DEBUG)) {
            log.debug("Unable to trim string \"" + str + "\" by trim string \"" + trimStr + "\". The start of trimmed string expected on position " + (start + 1) + " and the end of the trimmed string expect on position " + stop + " which is not valid. Returning an empty string.");
         }
         return "";
      }
      return str.substring(start, stop + 1);
   }

   /**
    * Remove white spaces from beginning of each line.
    * 
    * @param multiline
    * @return
    */
   public static String trimLines(String multiline) {
      StringBuffer sb = new StringBuffer();
      BufferedReader br = new BufferedReader(new StringReader(multiline));
      String line;
      try {
         while ((line = br.readLine()) != null) {
            sb.append(line.trim());
            if (!line.isEmpty()) {
               sb.append("\n");
            }
         }
      } catch (IOException ex) {
         java.util.logging.Logger.getLogger(StringUtil.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      }
      return sb.toString();
   }
}
