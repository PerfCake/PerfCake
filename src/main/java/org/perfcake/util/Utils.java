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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Utils {

   public static final File resourcesDir = new File("src/main/resources");

   /**
    * It takes a string and replaces all ${&lt;property.name&gt;} placeholders
    * by respective value of the property named &lt;property.name&gt; using {@link #getProperty(String)} method.
    * 
    * @param text
    *           Original string.
    * @return Filtered string with.
    * @throws IOException
    */
   public static String filterProperties(String text) throws IOException {
      String filteredString = new String(text);
      String propertyPattern = "\\$\\{([^\\$\\{:]+)(:[^\\$\\{:]*)?}";
      Matcher matcher = Pattern.compile(propertyPattern).matcher(filteredString);

      while (matcher.find()) {
         String pValue = null;
         String pName = matcher.group(1);
         String defaultValue = null;
         if (matcher.groupCount() == 2 && matcher.group(2) != null) {
            defaultValue = (matcher.group(2)).substring(1);
         }
         pValue = Utils.getProperty(pName, defaultValue);
         if (pValue != null) {
            filteredString = filteredString.replaceAll(Pattern.quote(matcher.group()), pValue);
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
      if (System.getProperty(name) != null) {
         return System.getProperty(name);
      } else if (System.getenv(name) != null) {
         return System.getenv(name);
      } else {
         return defaultValue;
      }
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

}
