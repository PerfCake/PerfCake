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

import java.util.Properties;

/**
 * Provides properties from both {@link java.util.Properties} object and system properties.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MixedPropertyGetter implements PropertyGetter {

   private final Properties p;

   /**
    * Creates a mixed property getter with a given {@link java.util.Properties} instance.
    *
    * @param properties
    *       Object with properties.
    */
   public MixedPropertyGetter(final Properties properties) {
      this.p = properties;
   }

   @Override
   public String getProperty(final String propName, final String defaultValue) {
      return p.getProperty(propName, SystemPropertyGetter.INSTANCE.getProperty(propName, defaultValue));
   }

   @Override
   public String getProperty(final String propName) {
      return getProperty(propName, null);
   }
}
