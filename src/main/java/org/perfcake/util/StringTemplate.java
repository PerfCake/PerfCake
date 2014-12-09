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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import httl.Engine;
import httl.Template;

/**
 * A string template that can quickly replace properties in form of ${property} and #{property} to their values.
 * The properties with the dollar sign are replaced only once, while the properties with the hash sign are replaced with each call
 * to {@link toString()} with the current values (this simulates JavaEE EL). System properties can be accessed using the props. prefix,
 * and environment properties can be accessed using the env. prefix.
 * Automatically provides environment properties, system properties and user specified properties.
 * Examples: ${propertyA} ${1+1} ${non_existing||existing} ${propertyB - 1} ${env.JAVA_HOME} ${props['java.runtime.name']}
 * Notice: The first call to the constructor and calls to the static method {@link parseTemplate()} might take more time than a simple RegExp
 * match but this is payed back for the subsequent calls to {@link toString()}. Internally, the fast HTTL templating engine is used.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class StringTemplate {

   private static final String PREFIX = "#set(Map env)#set(Map props)";
   private static final int PREFIX_TOKENS = 2;
   private static final String PROPERTY_PATTERN = "[^\\\\](@\\{([^@\\$\\{]+)})";
   private static final String ESCAPED_PROPERTY_PATTERN = "([\\\\](@\\{[^@\\$\\{]+}))";
   private static final String ESCAPED_PATTERN = "(\\$(\\{[^\\$\\{]+}))";
   private static final Logger log = Logger.getLogger(StringTemplate.class);

   private Template template = null;
   private String originalTemplate = null;
   private Map vars = new HashMap();
   private Engine engine = getEngine();

   public StringTemplate(final String template) {
      this(template, null);
   }

   @SuppressWarnings("unchecked")
   public StringTemplate(final String template, final Properties properties) {
      this.originalTemplate = template;

      vars.put("env", System.getenv());
      vars.put("props", System.getProperties());
      if (properties != null) {
         vars.putAll(properties);
      }

      try {
         preParse(template);
      } catch (ParseException pe) {
         log.error("Unable to parse template. Continue with un-parsed content: ", pe);
      }
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
      return new StringTemplate(template, properties).toString(properties);
   }

   private Template parseTemplate(final String template) throws ParseException {
      return engine.parseTemplate(PREFIX + template);
   }

   private String renderTemplate(final Map variables) {
      return renderTemplate(this.template, variables);
   }

   public boolean hasPlaceholders() {
      return template != null;
   }

   private String renderTemplate(final Template template, final Map variables) {
      if (template != null) {
         try {
            return template.evaluate(variables).toString();
         } catch (ParseException pe) {
            log.error("Cannot parse template. Continue with un-parsed content: ", pe);
         }
      }
      return originalTemplate;
   }

   private Engine getEngine() {
      Properties config = new Properties();
      config.setProperty("value.filters", "");
      config.setProperty("preload", "false");

      return Engine.getEngine(config);
   }

   /**
    * During the first pass, the values of ${property} placeholders are replaced immediately.
    * All occurrences of #{property} are replaced with ${property} and the resulting string is returned for the second pass.
    * @param template The original template
    * @return The template with first pass placeholders replaced and second pass placeholders ready for further parsing.
    */
   private void preParse(final String template) throws ParseException {
      final Template tmpTemplate = parseTemplate(template);

      // first replace all ${property} with their values
      String parsed = " " + renderTemplate(tmpTemplate, vars); // patterns need to check one character ahead, if this was the string beginning, the first property could have been skipped

      // are there any @{property} patterns? if not, we are done and the result can stay as is, else we must handle the @ sign
      Pattern propertyPattern = Pattern.compile(PROPERTY_PATTERN);
      Matcher propertyMatcher = propertyPattern.matcher(parsed);
      if (propertyMatcher.find()) {

         // after the first render, we must return back the escape sign to original \${property} as these backslashes were removed
         Matcher matcher = Pattern.compile(ESCAPED_PATTERN).matcher(parsed);
         int correction = 0; // we are adding characters to the string, while the matcher works with the original version
         while (matcher.find()) {
            parsed = parsed.substring(0, matcher.start(1) + correction) + "\\$" + matcher.group(2) + parsed.substring(matcher.end(1) + correction);
            correction++;
         }
         // now switch @{property} to ${property}
         propertyMatcher = propertyPattern.matcher(parsed); // refresh to the updated string
         while (propertyMatcher.find()) {
            parsed = parsed.substring(0, propertyMatcher.start(1)) + "${" + propertyMatcher.group(2) + "}" + parsed.substring(propertyMatcher.end(1));
         }

         // also change \@{property} to @{property} to achieve the same behaviour as for \${property}
         matcher = Pattern.compile(ESCAPED_PROPERTY_PATTERN).matcher(parsed);
         matcher.reset();
         correction = 0; // we are removing characters from the string, while the matcher works with the original version
         while (matcher.find()) {
            parsed = parsed.substring(0, matcher.start(1) - correction) + matcher.group(2) + parsed.substring(matcher.end(1) - correction);
            correction++; // we removed one character, this must be considered during the next loop
         }

         this.template = parseTemplate(parsed.substring(1)); // remove the space we added at the beginning
      } else {
         this.originalTemplate = parsed.substring(1); // remove the space we added at the beginning
      }
   }
}
