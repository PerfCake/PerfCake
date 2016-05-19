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

import java.util.Properties;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class NewStringTemplate {

   private String template;

   public NewStringTemplate(final String template) {
      this.template = firstPhase(template, null);
   }

   public NewStringTemplate(final String template, final Properties properties) {
      this.template = firstPhase(template, properties);
   }

   public String toString() {
      return template;
   }

   public String toString(final Properties properties) {
      return template;
   }

   // only search for unescaped ${...:...}
   private static String firstPhase(final String template, final Properties properties) {
      final StringMill mill = new StringMill(template);
      final StringBuilder result = new StringBuilder();
      StringBuilder buffer = new StringBuilder();

      while (!mill.end()) {
         if (mill.cur() == '\\' && mill.next() == '$') {
            result.append('$');
            mill.step();
            mill.step();
         } else if (mill.pre() != '\\' && mill.cur() == '$' && mill.next() == '{') {
            mill.step();
            mill.step();
            while(!mill.end() && mill.cur() != '}') {
               buffer.append(mill.cur());
               mill.step();
            }

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

   private static class StringMill {
      private final String str;
      private int i = 0;

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
      }

      private boolean end() {
         return i >= str.length();
      }

      private char cur() {
         return charRel(0);
      }

      private char pre() {
         return charRel(-1);
      }

      private char prePre() {
         return charRel(-2);
      }

      private char next() {
         return charRel(1);
      }
   }
}
