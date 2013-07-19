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

package org.perfcake.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.util.properties.PropertyGetter;
import org.perfcake.util.properties.SystemPropertyGetter;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Utils {

   public static final File resourcesDir = new File("src/main/resources");
   public static final Logger log = Logger.getLogger(Utils.class);

   /**
    * It takes a string and replaces all ${&lt;property.name&gt;} placeholders
    * by respective value of the property named &lt;property.name&gt; using {@link SystemPropertyGetter}.
    * 
    * @param text
    *           Original string.
    * @return Filtered string with.
    * @throws IOException
    */
   public static String filterProperties(String text) throws IOException {
      String propertyPattern = "[^\\\\](\\$\\{([^\\$\\{:]+)(:[^\\$\\{:]*)?})";
      Matcher matcher = Pattern.compile(propertyPattern).matcher(text);

      return filterProperties(text, matcher, SystemPropertyGetter.INSTANCE);
   }

   public static String filterProperties(String text, Matcher matcher, PropertyGetter pg) {
      String filteredString = text;

      matcher.reset();
      while (matcher.find()) {
         String pValue = null;
         String pName = matcher.group(2);
         String defaultValue = null;
         if (matcher.groupCount() == 3 && matcher.group(3) != null) {
            defaultValue = (matcher.group(3)).substring(1);
         }
         pValue = pg.getProperty(pName, defaultValue);
         if (pValue != null) {
            filteredString = filteredString.replaceAll(Pattern.quote(matcher.group(1)), pValue);
         }
      }

      return filteredString;
   }

   /**
    * Returns a property value. First it looks at system properties using {@link System#getProperty(String)} if the system property does not exist
    * it looks at environment variables using {@link System#getenv(String)}. If
    * the variable does not exist the method returns a <code>null</code>.
    * 
    * @param name
    *           Property name
    * @return Property value or <code>null</code>.
    */
   public static String getProperty(String name) {
      return getProperty(name, null);
   }

   /**
    * Returns a property value. First it looks at system properties using {@link System#getProperty(String)} if the system property does not exist
    * it looks at environment variables using {@link System#getenv(String)}. If
    * the variable does not exist the method returns <code>defautValue</code>.
    * 
    * @param name
    *           Property name
    * @param defaultValue
    *           Default property value
    * @return Property value or <code>defaultValue</code>.
    */
   public static String getProperty(String name, String defaultValue) {
      return SystemPropertyGetter.INSTANCE.getProperty(name, defaultValue);
   }

   /**
    * @param properties
    */
   public static void logProperties(Logger logger, Level level, Properties properties) {
      logProperties(logger, level, properties, "");
   }

   /**
    * @param properties
    * @param prefix
    */
   public static void logProperties(Logger logger, Level level, Properties properties, String prefix) {
      if (logger.isEnabledFor(level)) {
         for (Entry<Object, Object> property : properties.entrySet()) {
            logger.log(level, prefix + property.getKey() + "=" + property.getValue());
         }
      }
   }

   /**
    * Reads file content into a string. The file content is processed as an UTF-8 encoded text.
    * 
    * @param url
    *           specifies the file location as a URL
    * @return the file contents
    * @throws IOException
    */
   public static String readFilteredContent(URL url) throws IOException {
      try (InputStream is = url.openStream(); Scanner scanner = new Scanner(is, "UTF-8")) {
         return filterProperties(scanner.useDelimiter("\\Z").next());
      } catch (NoSuchElementException nsee) {
         if (log.isEnabledFor(Level.WARN)) {
            log.warn("The content of " + url + " is empty.");
         }
         return "";
      }
   }

   /**
    * Convert location to URL. If location specifies a protocol, it is immediately converted. Without a protocol specified, output is
    * file://${&lt;defaultLocationProperty&gt;}/&lt;location&gt;&lt;defaultSuffix&gt; using defaultLocation as a default value for the defaultLocationProperty
    * when the property is undefined.
    * 
    * @param location
    *           location of the resource
    * @param defaultLocationProperty
    *           property to read the default location prefix
    * @param defaultLocation
    *           default value for defaultLocationProperty if this property is undefined
    * @param defaultSuffix
    *           default suffix of the location
    * @return URL representing the location
    * @throws MalformedURLException
    *            when the location cannot be converted to a URL
    */
   public static URL locationToUrl(String location, String defaultLocationProperty, String defaultLocation, String defaultSuffix) throws MalformedURLException {
      // is there a protocol specified? suppose just scenario name
      if (location.indexOf("://") < 0) {
         location = "file://" + Utils.getProperty(defaultLocationProperty, defaultLocation) + "/" + location + defaultSuffix;
      }

      return new URL(location);
   }

   /**
    * Determines the default location of resources based on the resourcesDir constant.
    * 
    * @param locationSuffix
    *           Optional suffix to be added to the path
    * @return the location based on the resourcesDir constant
    */
   public static String determineDefaultLocation(String locationSuffix) {
      return resourcesDir.getAbsolutePath() + "/" + (locationSuffix == null ? "" : locationSuffix);
   }

   /**
    * Converts camelCaseStringsWithACRONYMS to CAMEL_CASE_STRINGS_WITH_ACRONYMS
    * 
    * @param camelCase
    *           a camelCase string
    * @return the same string in equivalent format for Java enum values
    */
   public static String camelCaseToEnum(String camelCase) {
      final String regex = "([a-z])([A-Z])";
      final String replacement = "$1_$2";

      return camelCase.replaceAll(regex, replacement).toUpperCase();
   }

}
