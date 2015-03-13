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
package org.perfcake.util.properties;

/**
 * Provides system properties.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public enum SystemPropertyGetter implements PropertyGetter {

   INSTANCE;

   private SystemPropertyGetter() {

   }

   /**
    * Returns a property value.
    * First it checks whether a namespace of 'env.' or 'props.' was given as a property name prefix. If so, the property must be taken from these,
    * otherwise the default value is returned.
    * Second (as no namespace is specified) it looks at system properties using {@link System#getProperty(String)},
    * if the system property does not exist
    * it looks at environment variables using {@link System#getenv(String)}. If
    * the variable does not exist the method returns <code>defautValue</code>.
    *
    * @param name
    *       Property name
    * @param defaultValue
    *       Default property value
    * @return Property value or <code>defaultValue</code>.
    */
   @Override
   public String getProperty(final String name, final String defaultValue) {
      // first find out whether we specified a namespace
      if (name != null) {
         if (name.startsWith("env.")) {
            final String val = System.getenv(name.substring(4));
            if (val != null) {
               return val;
            } else {
               return defaultValue;
            }
         } else if (name.startsWith("props.")) {
            final String val = System.getProperty(name.substring(6));
            if (val != null) {
               return val;
            } else {
               return defaultValue;
            }
         }

         // try to find anywhere
         if (System.getProperty(name) != null) {
            return System.getProperty(name);
         } else if (System.getenv(name) != null) {
            return System.getenv(name);
         }
      }

      return defaultValue;
   }

   @Override
   public String getProperty(final String propName) {
      return getProperty(propName, null);
   }

}
