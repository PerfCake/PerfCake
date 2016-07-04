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
 * Holds a template capable of replacing properties in form of ${property} and @{property} to their values.
 * The properties with the dollar sign are replaced only once, while the properties with the "at" sign are replaced with each call
 * to {@link #toString(Properties)} with the current values (this simulates JavaEE EL). System properties can be accessed using the props. prefix,
 * and environment properties can be accessed using the env. prefix.
 * Automatically provides environment properties, system properties and user specified properties.
 * Default values are separated by semicolon (e.g. ${property:defaultValue}). The property name must contain only letters, numbers and underscores
 * (this is not strictly checked but may lead to undefined behaviour).
 * Backslash works as a general escape character and escapes any letter behind it (e.g. \\ is replaced by \, \@ is replaced by @ etc.).
 * Examples: ${propertyA} ${non_existing:default} ${env.JAVA_HOME} ${props['java.runtime.name']}
 * Notice: The first call to the constructor and calls to the static method {@link #parseTemplate(String, Properties)} might take more time than a simple RegExp
 * match but this is payed back for the subsequent calls to {@link #toString()}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class StringTemplate {

   /**
    * Global properties passed in while creating the template.
    */
   private Properties properties;

   /**
    * Compiled data of the template. Static parts of the template.
    */
   private String[] parts;

   /**
    * Compiled data of the template. Property names to be replaced.
    */
   private String[] replacements;

   /**
    * Compiled data of the template. Default values of properties.
    */
   private String[] defaults;

   /**
    * Compiled data of the template. Number of replacements in the template.
    */
   private int patternSize;

   /**
    * True when there were any placeholders in the template.
    */
   private boolean hasPlaceholders = false;

   /**
    * Creates a template using the provided string interpretation.
    *
    * @param template
    *       The string interpretation of the pattern.
    */
   public StringTemplate(final String template) {
      this(template, null);
   }

   /**
    * Creates a template using the provided string interpretation using the additional properties.
    *
    * @param template
    *       The string interpretation of the pattern.
    * @param properties
    *       Properties to be immediately replaced in the template.
    */
   public StringTemplate(final String template, final Properties properties) {
      this.properties = properties == null ? new Properties() : properties;
      compilePattern(firstPhase(template));
   }

   /**
    * Were there any placeholders in the template?
    *
    * @return True if and only if there were any placeholders in the template.
    */
   public boolean hasPlaceholders() {
      return hasPlaceholders;
   }

   /**
    * Were there any placeholders that need to be replaced each time when rendered?
    *
    * @return True if and only if there were any placeholders that need to be replaced each time when rendered.
    */
   public boolean hasDynamicPlaceholders() {
      return patternSize > 0;
   }

   /**
    * Renders the template.
    *
    * @return The rendered template.
    */
   public String toString() {
      return toString(null);
   }

   /**
    * Renders the template using the additionally provided properties.
    *
    * @param localProperties
    *       The additional properties to be replaced in the template.
    * @return The rendered template.
    */
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

   /**
    * Gets the rendered template of the provided string interpretation and properties.
    *
    * @param template
    *       The string representation of the template.
    * @param properties
    *       The properties to be replaced in the template.
    * @return The rendered template.
    */
   public static String parseTemplate(final String template, final Properties properties) {
      return new StringTemplate(template, properties).toString(properties);
   }

   /**
    * Reads the property name from inside ${...} placeholder.
    *
    * @param mill
    *       The string mill crunching the template.
    * @param buffer
    *       The output buffer to append the property name.
    */
   private void readPropertyName(final StringMill mill, final StringBuilder buffer) {
      hasPlaceholders = true; // we are reading a property now

      while (!mill.end() && mill.cur() != '}') {
         if (mill.cur() == '\\' || mill.cur() == '\u0001') {
            buffer.append(mill.next());
            mill.cut();
            mill.cut();
         } else {
            buffer.append(mill.cur());
            mill.step();
         }
      }
   }

   /**
    * Pre-compiles the template string replacing all ${...} and preserving escaped characters.
    *
    * @param template
    *       The template to be compiled.
    * @return The pre-compiled template.
    */
   private String firstPhase(final String template) {
      final StringMill mill = new StringMill(template);
      final StringBuilder result = new StringBuilder();
      StringBuilder buffer = new StringBuilder();

      while (!mill.end()) {
         if (mill.cur() == '\\' && mill.next() != '@') {
            result.append(mill.next());
            mill.cut();
            mill.cut();
         } else if (mill.cur() == '\\' && mill.next() == '@') { // let's mark the escaped @ for the second round
            result.append('\u0001');
            result.append('@');
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

   /**
    * Second phase of pattern processing, compiles the needed data structures to be able to fast render the template.
    *
    * @param template
    *       The pre-compiled template.
    */
   private void compilePattern(final String template) {
      final List<String> parts = new LinkedList<>();
      final List<String> replacements = new LinkedList<>();
      final List<String> defaults = new LinkedList<>();

      final StringMill mill = new StringMill(template);
      StringBuilder result = new StringBuilder();
      StringBuilder buffer = new StringBuilder();

      while (!mill.end()) {
         if (mill.cur() == '\u0001' && mill.next() == '@') {
            result.append(mill.next());
            mill.cut();
            mill.cut();
         } else if (mill.cur() == '@' && mill.next() == '{') {
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

   /**
    * Gets the property value from all known places given the property prefix.
    *
    * @param property
    *       The property name.
    * @param properties
    *       Local properties to be used for default values.
    * @return The property value, or null if the property was not found in any location.
    */
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

   /**
    * Gets the property value from all known places given the property prefix.
    *
    * @param property
    *       The property name.
    * @param properties
    *       Local properties to be used when system resources did not contain the property.
    * @param defaultValue
    *       The default value to be used when the property was not found anywhere.
    * @return The property value, or null if the property was not found in any location.
    */
   private static String getProperty(final String property, final Properties properties, final String defaultValue) {
      final String value = getProperty(property, properties);
      return value == null ? defaultValue : value;
   }

   /**
    * Helper to crunch through a string.
    */
   private static class StringMill {

      /**
       * The string being crunched.
       */
      private final String str;

      /**
       * Current position in the string.
       */
      private int i = 0;

      /**
       * Is the previous character masked? Simulates consumption of the previous character.
       */
      private boolean preNull = false;

      /**
       * Start crunching the given string.
       *
       * @param str
       *       String to be crunched.
       */
      private StringMill(final String str) {
         this.str = str;
      }

      /**
       * Character relative to the current position.
       *
       * @param rel
       *       Index relative to the current position.
       * @return The character from the corresponding index or \u0000 if the index is out of string boundaries.
       */
      private char charRel(final int rel) {
         if (i + rel < 0 || i + rel >= str.length()) {
            return 0;
         }

         return str.charAt(i + rel);
      }

      /**
       * Skip the character and move to the next position.
       */
      private void step() {
         i++;
         preNull = false;
      }

      /**
       * Consume the character and move to the next position. The character cannot be later obtained by the {@link #pre()} method.
       */
      private void cut() {
         i++;
         preNull = true;
      }

      /**
       * Did we reach the end of the string?
       *
       * @return True if and only if we hit the end of the string.
       */
      private boolean end() {
         return i >= str.length();
      }

      /**
       * Gets the character at the current position.
       *
       * @return The character at the current position.
       */
      private char cur() {
         return charRel(0);
      }

      /**
       * Gets the character at the previous position.
       *
       * @return The character at the previous position.
       */
      private char pre() {
         return preNull ? 0 : charRel(-1);
      }

      /**
       * Gets the character at the next position.
       *
       * @return The character at the next position.
       */
      private char next() {
         return charRel(1);
      }

      @Override
      public String toString() {
         return "StringMill: " + pre() + " --->" + cur() + " " + next();
      }
   }
}
