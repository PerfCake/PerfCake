/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
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
package org.perfcake.util;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class to work with strings.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class StringUtil {

   /**
    * Does a string start with a second string ignoring case?
    *
    * @param haystack
    *       A string to be searched.
    * @param needle
    *       A potential prefix.
    * @return True if the needle is a prefix of the haystack ignoring case.
    */
   public static boolean startsWithIgnoreCase(final String haystack, final String needle) {
      return needle.equalsIgnoreCase(haystack.substring(0, needle.length()));
   }

   /**
    * Does a string end with a second string ignoring case?
    *
    * @param haystack
    *       A string to be searched.
    * @param needle
    *       A potential suffix.
    * @return True if the needle is a suffix of thehaystack ignoring case.
    */
   public static boolean endsWithIgnoreCase(final String haystack, final String needle) {
      return needle.equalsIgnoreCase(haystack.substring(haystack.length() - needle.length(), haystack.length()));
   }

   /**
    * Does a string contain a substring ignoring case?
    *
    * @param haystack
    *       A string to be searched.
    * @param needle
    *       A potential substring.
    * @return True if the needle is a substring of the haystack ignoring case.
    */
   public static boolean containsIgnoreCase(final String haystack, final String needle) {
      return haystack.toLowerCase().indexOf(needle.toLowerCase()) >= 0;
   }

   /**
    * Trims new line, tabulator, apostrophe and double quotes from a string.
    *
    * @param str
    *       A string to be trimmed.
    * @return The trimmed string.
    */
   public static String trim(final String str) {
      return trim(str, " \n\t'\"");
   }

   /**
    * Trims characters from the begging and end of a string.
    *
    * @param str
    *       A string to be trimmed.
    * @param trimStr
    *       A string with characters to be trimmed. Any of the characters in this string are removed from the first parameter.
    * @return The trimmed string.
    */
   public static String trim(final String str, final String trimStr) {
      if (trimStr == null || trimStr.isEmpty()) {
         return str;
      }

      final String quotedTrimStr = Pattern.quote(trimStr);
      String result = str.replaceAll("^[" + quotedTrimStr + "]*", "");
      result = result.replaceAll("[" + quotedTrimStr + "]*$", "");

      return result;
   }

   /**
    * Removes white spaces from beginning and end of each line. Lines are terminated by LF, or CR LF. Also removes empty lines.
    * Last line will be terminated with LF.
    *
    * @param multiline
    *       A string with multiple lines.
    * @return A new string with trimmed lines.
    */
   public static String trimLines(final String multiline) {
      return Pattern.compile("\\r?\\n").splitAsStream(multiline).map(String::trim).filter(l -> !l.isEmpty()).collect(Collectors.joining("\n")) + "\n";
   }
}
