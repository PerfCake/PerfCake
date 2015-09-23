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

import org.perfcake.PerfCakeConst;
import org.perfcake.util.properties.SystemPropertyGetter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import httl.Engine;
import httl.Template;

/**
 * Holds a template capable of replacing properties in form of ${property} and #{property} to their values.
 * The properties with the dollar sign are replaced only once, while the properties with the hash sign are replaced with each call
 * to {@link #toString()} with the current values (this simulates JavaEE EL). System properties can be accessed using the props. prefix,
 * and environment properties can be accessed using the env. prefix.
 * Automatically provides environment properties, system properties and user specified properties.
 * Examples: ${propertyA} ${1+1} ${non_existing||existing} ${propertyB - 1} ${env.JAVA_HOME} ${props['java.runtime.name']}
 * Notice: The first call to the constructor and calls to the static method {@link #parseTemplate(String)} might take more time than a simple RegExp
 * match but this is payed back for the subsequent calls to {@link #toString()}. Internally, the fast HTTL templating engine is used.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class StringTemplate {

   /**
    * Prefix to allow usage of "env." and "props." prefixes in the template.
    */
   private static final String PREFIX = "#set(Map env)#set(Map props)";

   /**
    * Pattern to find \@{property}.
    */
   private static final String ESCAPED_PROPERTY_PATTERN = "([\\\\](@\\{[^@\\$\\{]+}))";

   /**
    * Pattern to find ${pattern}.
    */
   private static final String ESCAPED_PATTERN = "(\\$(\\{[^\\$\\{]+}))";

   /**
    * Logger for this class.
    */
   private static final Logger log = LogManager.getLogger(StringTemplate.class);

   /**
    * Template engine can be disable by a system property.
    */
   private static boolean disableTemplateEngine = Boolean.valueOf(SystemPropertyGetter.INSTANCE.getProperty(PerfCakeConst.DISABLE_TEMPLATES_PROPERTY, "false"));

   /**
    * Cached compiled template.
    */
   private Template template = null;

   /**
    * Original version of the template. Used in case there was not anything to replace.
    */
   private String originalTemplate = null;

   /**
    * Variables to be replaced in the template.
    */
   private final Map vars = new HashMap();

   /**
    * Template engine.
    */
   private final Engine engine = getEngine();

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
   @SuppressWarnings("unchecked")
   public StringTemplate(final String template, final Properties properties) {
      this.originalTemplate = template;

      if (!disableTemplateEngine) {
         vars.put("env", System.getenv());
         vars.put("props", System.getProperties());
         if (properties != null) {
            vars.putAll(properties);
         }

         try {
            preParse(template);
         } catch (final ParseException pe) {
            log.error("Unable to parse template. Continue with un-parsed content: ", pe);
         }
      }
   }

   /**
    * Renders the template.
    *
    * @return The rendered template.
    */
   public String toString() {
      return renderTemplate(vars);
   }

   /**
    * Renders the template using the additionally provided properties.
    *
    * @param properties
    *       The additional properties to be replaced in the template.
    * @return The rendered template.
    */
   @SuppressWarnings("unchecked")
   public String toString(final Properties properties) {
      final Map localVars = new HashMap(vars);
      if (properties != null) {
         localVars.putAll(properties);
      }

      return renderTemplate(localVars);
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
    * Gets the rendered template of the provided string interpretation.
    *
    * @param template
    *       The string representation of the template.
    * @return The rendered template.
    */
   private Template parseTemplate(final String template) throws ParseException {
      return engine.parseTemplate(PREFIX + template);
   }

   private String renderTemplate(final Map variables) {
      return renderTemplate(this.template, variables);
   }

   /**
    * Is there anything in the template to be rendered?
    *
    * @return <code>true</code> if and only if the template contains any placeholders to be rendered.
    */
   public boolean hasPlaceholders() {
      return template != null;
   }

   private String renderTemplate(final Template template, final Map variables) {
      if (template != null && !disableTemplateEngine) {
         try {
            return template.evaluate(variables).toString();
         } catch (final ParseException pe) {
            log.error("Cannot parse template. Continue with un-parsed content: ", pe);
         }
      }
      return originalTemplate;
   }

   private Engine getEngine() {
      final Properties config = new Properties();
      config.setProperty("value.filters", "");
      config.setProperty("script.value.filters", "");
      config.setProperty("style.value.filters", "");
      config.setProperty("text.filters", "");
      config.setProperty("preload", "false");
      config.setProperty("null.value", "null");
      config.setProperty("modes", "");
      config.setProperty("compiler", "httl.spi.compilers.JavassistCompiler");

      return Engine.getEngine(config);
   }

   private boolean isUnescapedAtSign(final String suspect) {
      char last = '\0';
      for (int i = 0; i < suspect.length(); i++) {
         final char c = suspect.charAt(i);
         if (c == '@' && last != '\\') {
            return true;
         }
         last = c;
      }
      return false;
   }

   private String replaceUnescapedAtSign(final String suspect) {
      char last = '\0';
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < suspect.length(); i++) {
         final char c = suspect.charAt(i);
         if (c == '@' && last != '\\') {
            sb.append("$");
         } else {
            sb.append(c);
         }
         last = c;
      }
      return sb.toString();
   }

   /**
    * During the first pass, the values of ${property} placeholders are replaced immediately.
    * All occurrences of #{property} are replaced with ${property} and the resulting string is returned for the second pass.
    *
    * @param template
    *       The original template
    * @return The template with first pass placeholders replaced and second pass placeholders ready for further parsing.
    */
   private void preParse(final String template) throws ParseException {
      final Template tmpTemplate = parseTemplate(template);

      // first replace all ${property} with their values
      String parsed = renderTemplate(tmpTemplate, vars); // patterns need to check one character ahead, if this was the string beginning, the first property could have been skipped

      // are there any @{property} patterns? if not, we are done and the result can stay as is, else we must handle the @ sign
      if (isUnescapedAtSign(parsed)) {

         // after the first render, we must return back the escape sign to original \${property} as these backslashes were removed
         Matcher matcher = Pattern.compile(ESCAPED_PATTERN).matcher(parsed);
         int correction = 0; // we are adding characters to the string, while the matcher works with the original version
         while (matcher.find()) {
            parsed = parsed.substring(0, matcher.start(1) + correction) + "\\$" + matcher.group(2) + parsed.substring(matcher.end(1) + correction);
            correction++;
         }

         // now switch @{property} to ${property}
         parsed = replaceUnescapedAtSign(parsed);

         // also change \@{property} to @{property} to achieve the same behaviour as for \${property}
         matcher = Pattern.compile(ESCAPED_PROPERTY_PATTERN).matcher(parsed);
         matcher.reset();
         correction = 0; // we are removing characters from the string, while the matcher works with the original version
         while (matcher.find()) {
            parsed = parsed.substring(0, matcher.start(1) - correction) + matcher.group(2) + parsed.substring(matcher.end(1) - correction);
            correction++; // we removed one character, this must be considered during the next loop
         }

         this.template = parseTemplate(parsed);
      } else {
         this.originalTemplate = parsed;
      }
   }
}
