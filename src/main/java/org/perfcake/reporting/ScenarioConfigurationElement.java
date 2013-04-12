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

package org.perfcake.reporting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.perfcake.ObjectWithProperties;

/**
 * <p>
 * Any class that can be configured using scenario xml should extend this class.
 * </p>
 * </p> Each such class can be configured in scenario xml by adding property
 * elements. This class gives convenience methods to retrieve those
 * properties.</p>
 * <p>
 * Further more this class will support unknown property detection. This way we should have high property and configuration checking which is desireable because there are situations when some properties won't be touched just because some element is missing or it get's renamed in which case we want to detect it. Whenever property is touched it will be noted in set named 'touched'. Programmer can
 * check whether there are any untouched properties by calling assertUntouchedProperties and all properties that are in collection properties.keySet().removeAll(touched) will be thrown in exception!.
 * </p>
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public abstract class ScenarioConfigurationElement implements ObjectWithProperties {
   private static final Logger log = Logger.getLogger(ScenarioConfigurationElement.class);

   private Map<String, String> properties = new HashMap<String, String>();

   protected Set<String> touched = new TreeSet<String>();

   /**
    * This method is called for configured class to ask it to validate all his
    * properties in configuration prior to starting up the test. Reporter should
    * do as much logic as possible to validate that configuration is valid
    * before test will be invoked.
    */
   public abstract void loadConfigValues();

   /**
    * All classes that are defined at config-time in scenarios can be configured
    * using property elements. This method is called with each such pair.
    */
   @Override
   public void setProperty(String property, String value) {
      Set<String> set = null;

      if (properties.containsKey(property)) {
         throw new ReportsException("Error configuring " + getClass().getSimpleName() + " (probably from scenario file). The key " + property + " is already set to value: " + getProperty(property));
      }
      log.debug("Configuring " + this.getClass().getSimpleName() + ": [" + property + ":" + value + "]");
      properties.put(property, value);
   }

   /**
    * Calling this method finds throws an exception if and only if following is
    * true:
    * 
    * There at least one property that was set through xml but wasn't read by an
    * application.
    */
   public void assertUntouchedProperties() {

      TreeSet<String> untouched = new TreeSet<String>(properties.keySet());
      untouched.removeAll(touched);

      if (untouched.size() == 0) {
         return;
      }

      StringBuilder sb = new StringBuilder();
      for (String untouch : untouched) {
         sb.append(untouch + " ");
      }

      throw new ReportsException("Following properties were set in scenario xml file for element [" + getClass().getSimpleName() + "] but were never read! [ " + sb.toString() + "]");

   }

   /**
    * Finds property and retreives it's value.
    * 
    * @param propertyName
    *           name attribute of property xml element
    * @return value attribute
    */
   protected String getStringProperty(String propertyName) {
      if (!properties.containsKey(propertyName)) {
         throw new ReportsException("Cannot retrieve string property. The property " + propertyName + " is not set for: " + getClass().getSimpleName());
      }

      String result = getProperty(propertyName);
      return result;
   }

   protected String getStringProperty(String propertyName, String defaultValue) {
      if (!properties.containsKey(propertyName)) {
         return defaultValue;
      }
      String result = getProperty(propertyName);
      return result;
   }

   protected int getIntProperty(String propertyName) {
      try {
         int result = Integer.parseInt(getStringProperty(propertyName));
         return result;
      } catch (NumberFormatException nfe) {
         throw new ReportsException("Cannot retreive int property[" + propertyName + "] for " + getClass().getSimpleName() + ". The property " + propertyName + " exists but value [" + getStringProperty(propertyName) + "] is not valid integer.");
      }
   }

   protected int getIntProperty(String propertyName, int defaultValue) {
      if (!properties.containsKey(propertyName)) {
         return defaultValue;
      }
      return getIntProperty(propertyName);
   }

   protected float getFloatProperty(String propertyName, float f) {
      try {
         String textNum = getStringProperty(propertyName, String.valueOf(f));
         float result = Float.parseFloat(textNum);
         return result;
      } catch (NumberFormatException nfe) {
         throw new ReportsException("Cannot retreive float property[" + propertyName + "] for " + getClass().getSimpleName() + ". The property " + propertyName + " exists but value [" + getStringProperty(propertyName) + "] is not valid float.");
      }
   }

   /**
    * This is convenience method for property that holds comma separated values.
    * For example:
    * 
    * &lt;property name="tags" value="qa, performance,long running" &gt;
    * 
    * @param propertyName
    */
   protected Set<String> getCommaSeparatedProperty(String propertyName) {
      Set<String> result = new TreeSet<String>();
      String[] commaSepProps = getStringProperty(propertyName).split(",");

      for (String item : commaSepProps) {
         result.add(item.trim());
      }

      return result;
   }

   protected Set<String> getCommaSeparatedProperty(String propertyName, Set<String> defaultValue) {
      if (!properties.containsKey(propertyName)) {
         return defaultValue;
      }
      return getCommaSeparatedProperty(propertyName);
   }

   protected boolean getBoolProperty(String propertyName, boolean defaultValue) {
      if (!properties.containsKey(propertyName)) {
         return defaultValue;
      }

      String propVal = getStringProperty(propertyName);
      if (propVal != null) {
         if (propVal.trim().toLowerCase().equals("true")) {
            return true;
         }

         if (propVal.trim().toLowerCase().equals("false")) {
            return false;
         }
      }
      throw new ReportsException("Cannot retreive boolean property[" + propertyName + "] for " + getClass().getSimpleName() + ". The property " + propertyName + " exists but value [" + propVal + "] is not valid boolean.");
   }

   private String getProperty(String key) {
      if (!touched.contains(key)) {
         touched.add(key);
      }

      return properties.get(key);
   }
}
