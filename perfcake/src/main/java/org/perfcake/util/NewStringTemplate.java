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
package org.perfcake.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Backslash (\) = escape character, escapes anything
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class NewStringTemplate {

   private Properties properties;

   private String[] parts, replacements, defaults;

   private int patternSize;

   public NewStringTemplate(final String template) {
      this(template, null);
   }

   public NewStringTemplate(final String template, final Properties properties) {
      this.properties = properties == null ? new Properties() : properties;
      compilePattern(firstPhase(template));
   }

   public String toString() {
      return toString(null);
   }

   public String toString(final Properties localProperties) {
      final StringBuilder result = new StringBuilder(parts[0]);

      for (int i = 0; i < patternSize; i++) {
         if (localProperties == null) {
            result.append(getProperty(replacements[i], this.properties, defaults[i]));
         } else {
            final String globalProperty = this.properties.getProperty(replacements[i]);
            result.append(getProperty(replacements[i], localProperties, globalProperty == null ? defaults[i] : globalProperty));
         }
         result.append(parts[i + 1]);
      }

      return result.toString();
   }

   private static void readPropertyName(final StringMill mill, final StringBuilder buffer) {
      while(!mill.end() && mill.cur() != '}') {
         if (mill.cur() == '\\') {
            buffer.append(mill.next());
            mill.cut();
            mill.cut();
         } else {
            buffer.append(mill.cur());
            mill.step();
         }
      }
   }

   // only search for unescaped ${...:...}
   private String firstPhase(final String template) {
      final StringMill mill = new StringMill(template);
      final StringBuilder result = new StringBuilder();
      StringBuilder buffer = new StringBuilder();

      while (!mill.end()) {
         if (mill.cur() == '\\' && mill.next() != '@') {
            result.append(mill.next());
            mill.cut();
            mill.cut();
         } else if (mill.pre() != '\\' && mill.cur() == '$' && mill.next() == '{') {
            mill.step();
            mill.step();
            readPropertyName(mill, buffer);

            if (mill.cur() == '}') {
               mill.step();

               final String[] curly = buffer.toString().split(":", 2);
               String value = getProperty(curly[0], properties);
               if (value == null && curly.length > 1) {
                  value = curly[1];
               }
               result.append(value);
            } else {
               result.append("${");
               result.append(buffer);
            }
            buffer = new StringBuilder();
         } else {
            result.append(mill.cur());
            mill.step();
         }
      }

      if (buffer.length() > 0) {
         result.append(buffer);
      }

      return result.toString();
   }

   private void compilePattern(final String template) {
      final List<String> parts = new LinkedList<>();
      final List<String> replacements = new LinkedList<>();
      final List<String> defaults = new LinkedList<>();

      final StringMill mill = new StringMill(template);
      StringBuilder result = new StringBuilder();
      StringBuilder buffer = new StringBuilder();

      while (!mill.end()) {
         if (mill.cur() == '\\' && mill.next() == '@') {
            result.append(mill.next());
            mill.cut();
            mill.cut();
         } else if (mill.pre() != '\\' && mill.cur() == '@' && mill.next() == '{') {
            mill.step();
            mill.step();
            readPropertyName(mill, buffer);

            if (mill.cur() == '}') {
               mill.step();

               final String[] curly = buffer.toString().split(":", 2);
               parts.add(result.toString());
               result = new StringBuilder();
               replacements.add(curly[0]);
               defaults.add(curly.length > 1 ? curly[1] : null);
            } else {
               result.append("@{");
               result.append(buffer);
            }
            buffer = new StringBuilder();
         } else {
            result.append(mill.cur());
            mill.step();
         }
      }

      if (buffer.length() > 0) {
         result.append(buffer);
      }

      if (result.length() > 0) {
         parts.add(result.toString());
      } else {
         parts.add(""); // avoid out of bounds exception later
      }

      this.parts = parts.toArray(new String[parts.size()]);
      this.replacements = replacements.toArray(new String[replacements.size()]);
      this.defaults = defaults.toArray(new String[defaults.size()]);
      this.patternSize = replacements.size();
   }

   private static String getProperty(final String property, final Properties properties) {
      if (property.startsWith("env.")) {
         return System.getenv(property.substring(4));
      }

      if (property.startsWith("props['")) {
         return System.getProperty(property.substring(7, property.length() - 2)); // we expect it to end with ']
      }

      if (property.startsWith("props[")) {
         return System.getProperty(property.substring(6, property.length() - 1));
      }

      if (property.startsWith("props.")) {
         return System.getProperty(property.substring(6));
      }

      return properties != null ? properties.getProperty(property) : null;
   }

   private static String getProperty(final String property, final Properties properties, final String defaultValue) {
      final String value = getProperty(property, properties);
      return value == null ? defaultValue : value;
   }

   private static class StringMill {
      private final String str;
      private int i = 0;
      private boolean preNull = false;

      private StringMill(final String str) {
         this.str = str;
      }

      private char charRel(final int rel) {
         if (i + rel < 0 || i + rel >= str.length()) {
            return 0;
         }

         return str.charAt(i + rel);
      }

      private void step() {
         i++;
         preNull = false;
      }

      private void cut() {
         i++;
         preNull = true;
      }

      private boolean end() {
         return i >= str.length();
      }

      private char cur() {
         return charRel(0);
      }

      private char pre() {
         return preNull ? 0 : charRel(-1);
      }

      private char next() {
         return charRel(1);
      }
   }
}
