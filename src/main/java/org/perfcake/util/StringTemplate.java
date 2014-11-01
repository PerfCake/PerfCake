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

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import httl.Engine;
import httl.Template;

/**
 * Wrapper for HTTL templating engine.
 * Automatically provides environment properties, system properties and user specified properties.
 * Examples: ${propertyA} ${1+1} ${non_existing||existing} ${propertyB - 1} ${env.JAVA_HOME} ${props['java.runtime.name']}
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class StringTemplate {

   private static final String PREFIX = "#set(Map env)#set(Map props)";
   private static final int PREFIX_TOKENS = 2;
   public static final Logger log = Logger.getLogger(StringTemplate.class);

   private Template template = null;
   private String originalTemplate = null;
   private Map vars = new HashMap();

   public StringTemplate(final String template) {
      this(template, null);
   }

   @SuppressWarnings("unchecked")
   public StringTemplate(final String template, final Properties properties) {
      this.originalTemplate = template;

      Engine engine = getEngine();

      vars.put("env", System.getenv());
      vars.put("props", System.getProperties());
      if (properties != null) {
         vars.putAll(properties);
      }

      try {
         if (engine.parseTemplate(PREFIX + template).getChildren().size() > PREFIX_TOKENS + 1) {
            this.template = engine.parseTemplate(PREFIX + template);
         }
      } catch (ParseException pe) {
         log.error("Unable to parse template. Continue with un-parsed content: ", pe);
      }
   }

   private String renderTemplate(final Map variables) {
      if (template != null) {
         try {
            return template.evaluate(variables).toString();
         } catch (ParseException pe) {
            log.error("Cannot parse template. Continue with un-parsed content: ", pe);
         }
      }
      return originalTemplate;
   }

   public String toString() {
      return renderTemplate(vars);
   }

   @SuppressWarnings("unchecked")
   public String toString(Properties properties) {
      Map localVars = new HashMap(vars);
      if (properties != null) {
         localVars.putAll(properties);
      }

      return renderTemplate(localVars);
   }

   public static String parseTemplate(final String template, final Properties properties) {
      return new StringTemplate(template, properties).toString();
   }

   private Engine getEngine() {
      Properties config = new Properties();
      config.setProperty("value.filters", "");
      config.setProperty("preload", "false");

      return Engine.getEngine(config);
   }
}
