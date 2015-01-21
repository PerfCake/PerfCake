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
import org.perfcake.PerfCakeException;
import org.perfcake.common.TimestampedRecord;
import org.perfcake.util.properties.PropertyGetter;
import org.perfcake.util.properties.SystemPropertyGetter;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class Utils {

   public static final File DEFAULT_RESOURCES_DIR = new File("resources");
   public static final File DEFAULT_PLUGINS_DIR = new File("lib/plugins");
   private static final Logger log = LogManager.getLogger(Utils.class);

   /**
    * It takes a string and replaces all ${&lt;property.name&gt;} placeholders
    * by respective value of the property named &lt;property.name&gt; using {@link SystemPropertyGetter}.
    *
    * @param text
    *       Original string.
    * @return Filtered string with.
    * @throws IOException
    */
   public static String filterProperties(final String text) throws IOException {
      String propertyPattern = "[^\\\\](\\$\\{([^\\$\\{:]+)(:[^\\$\\{:]*)?})";
      Matcher matcher = Pattern.compile(propertyPattern).matcher(text);

      return filterProperties(text, matcher, SystemPropertyGetter.INSTANCE);
   }

   public static String filterProperties(final String text, final Matcher matcher, final PropertyGetter pg) {
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
            filteredString = filteredString.replaceAll(Pattern.quote(matcher.group(1)), Matcher.quoteReplacement(pValue));
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
    *       Property name
    * @return Property value or <code>null</code>.
    */
   public static String getProperty(final String name) {
      return getProperty(name, null);
   }

   /**
    * Returns a property value. First it looks at system properties using {@link System#getProperty(String)} if the system property does not exist
    * it looks at environment variables using {@link System#getenv(String)}. If
    * the variable does not exist the method returns <code>defautValue</code>.
    *
    * @param name
    *       Property name
    * @param defaultValue
    *       Default property value
    * @return Property value or <code>defaultValue</code>.
    */
   public static String getProperty(final String name, final String defaultValue) {
      return SystemPropertyGetter.INSTANCE.getProperty(name, defaultValue);
   }

   /**
    * @param properties
    */
   public static void logProperties(final Logger logger, final Level level, final Properties properties) {
      logProperties(logger, level, properties, "");
   }

   /**
    * @param properties
    * @param prefix
    */
   public static void logProperties(final Logger logger, final Level level, final Properties properties, final String prefix) {
      if (logger.isEnabled(level)) {
         for (Entry<Object, Object> property : properties.entrySet()) {
            logger.log(level, prefix + property.getKey() + "=" + property.getValue());
         }
      }
   }

   /**
    * Reads file content into a string. The file content is processed as an UTF-8 encoded text.
    *
    * @param url
    *       specifies the file location as a URL
    * @return the file contents
    * @throws IOException
    */
   public static String readFilteredContent(final URL url) throws IOException {
      try (InputStream is = url.openStream(); Scanner scanner = new Scanner(is, "UTF-8")) {
         return filterProperties(scanner.useDelimiter("\\Z").next());
      } catch (NoSuchElementException nsee) {
         if (log.isWarnEnabled()) {
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
    *       location of the resource
    * @param defaultLocationProperty
    *       property to read the default location prefix
    * @param defaultLocation
    *       default value for defaultLocationProperty if this property is undefined
    * @param defaultSuffix
    *       default suffix of the location
    * @return URL representing the location
    * @throws MalformedURLException
    *       when the location cannot be converted to a URL
    */
   public static URL locationToUrl(final String location, final String defaultLocationProperty, final String defaultLocation, final String defaultSuffix) throws MalformedURLException {
      String uri;

      // if we are looking for a file and there is no path specified, remove the prefix for later automatic directory insertion
      if (location.startsWith("file://") && !location.substring(7).contains(File.separator)) {
         uri = location.substring(7);
      } else {
         uri = location;
      }

      // if there is no protocol specified, try some file locations
      if (!uri.contains("://")) {
         final Path p = Paths.get(Utils.getProperty(defaultLocationProperty, defaultLocation), uri + defaultSuffix);
         uri = p.toUri().toString();
      }

      return new URL(uri);
   }

   /**
    * Convert location to URL with check for the location existence. If location specifies a protocol, it is immediately converted. Without a protocol specified, the following paths
    * are checked for the existence:
    * 1. file://location
    * 2. file://$defaultLocationProperty/location or file://defaultLocation/location (when the property is not set)
    * 3. file://$defaultLocationProperty/location.suffix or file://defaultLocation/location.suffix (when the property is not set) with all the provided suffixes
    * If the file was not found, the result is simply file://location
    *
    * @param location
    *       Location of the resource.
    * @param defaultLocationProperty
    *       Property to read the default location prefix.
    * @param defaultLocation
    *       Default value for defaultLocationProperty if this property is undefined.
    * @param defaultSuffix
    *       Array of default default suffixes to try when searching for the resource.
    * @return URL representing the location.
    * @throws MalformedURLException
    *       When the location cannot be converted to an URL.
    */
   public static URL locationToUrlWithCheck(final String location, final String defaultLocationProperty, final String defaultLocation, final String... defaultSuffix) throws MalformedURLException {
      String uri;

      // if we are looking for a file and there is no path specified, remove the prefix for later automatic directory insertion
      if (location.startsWith("file://")) {
         uri = location.substring(7);
      } else {
         uri = location;
      }

      // if there is no protocol specified, try some file locations
      if (!uri.contains("://")) {
         boolean found = false;
         Path p = Paths.get(uri);

         if (!Files.exists(p)) {
            p = Paths.get(Utils.getProperty(defaultLocationProperty, defaultLocation), uri);

            if (!Files.exists(p)) {
               if (defaultSuffix != null && defaultSuffix.length > 0) {
                  //                  boolean found = false;

                  for (String suffix : defaultSuffix) {
                     p = Paths.get(Utils.getProperty(defaultLocationProperty, defaultLocation), uri + suffix);
                     if (Files.exists(p)) {
                        found = true;
                        break;
                     }
                  }
               }
            } else {
               found = true;
            }
         } else {
            found = true;
         }

         if (found) {
            uri = p.toUri().toString();
         } else {
            uri = "file://" + uri;
         }
      }

      return new URL(uri);
   }

   /**
    * Determines the default location of resources based on the resourcesDir constant.
    *
    * @param locationSuffix
    *       Optional suffix to be added to the path
    * @return the location based on the resourcesDir constant
    */
   public static String determineDefaultLocation(final String locationSuffix) {
      return DEFAULT_RESOURCES_DIR.getAbsolutePath() + "/" + (locationSuffix == null ? "" : locationSuffix);
   }

   /**
    * Converts camelCaseStringsWithACRONYMS to CAMEL_CASE_STRINGS_WITH_ACRONYMS
    *
    * @param camelCase
    *       a camelCase string
    * @return the same string in equivalent format for Java enum values
    */
   public static String camelCaseToEnum(final String camelCase) {
      final String regex = "([a-z])([A-Z])";
      final String replacement = "$1_$2";

      return camelCase.replaceAll(regex, replacement).toUpperCase();
   }

   /**
    * Converts time in milliseconds to H:MM:SS format, where H is unbound.
    *
    * @param time
    *       Timestamp in milliseconds.
    * @return String representing the timestamp in H:MM:SS format.
    */
   public static String timeToHMS(final long time) {
      long hours = TimeUnit.MILLISECONDS.toHours(time);
      long minutes = TimeUnit.MILLISECONDS.toMinutes(time - TimeUnit.HOURS.toMillis(hours));
      long seconds = TimeUnit.MILLISECONDS.toSeconds(time - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes));

      StringBuilder sb = new StringBuilder();
      sb.append(hours).append(":").append(String.format("%02d", minutes)).append(":").append(String.format("%02d", seconds));

      return sb.toString();
   }

   /**
    * Uses {@link PerfCakeConst#DEFAULT_ENCODING_PROPERTY} system property, if this property is not set, <b>UTF-8</b> is used.
    *
    * @return String representation of default encoding for all read and written files
    */
   public static String getDefaultEncoding() {
      return Utils.getProperty(PerfCakeConst.DEFAULT_ENCODING_PROPERTY, "UTF-8");
   }

   /**
    * Computes a linear regression trend of the data set povided.
    *
    * @return Linear regression trend
    */
   public static double computeRegressionTrend(Collection<TimestampedRecord<Number>> data) {
      final SimpleRegression simpleRegression = new SimpleRegression();
      final Iterator<TimestampedRecord<Number>> iterator = data.iterator();
      TimestampedRecord<Number> currentRecord;
      while (iterator.hasNext()) {
         currentRecord = iterator.next();
         simpleRegression.addData(currentRecord.getTimestamp(), currentRecord.getValue().doubleValue());
      }
      return simpleRegression.getSlope();
   }

   /**
    * Sets the property value to the first not-null value from the list.
    *
    * @param props
    *       Properties instance.
    * @param propName
    *       Name of the property to be set.
    * @param values
    *       List of possibilities, the first not-null is used to set the property value.
    */
   public static void setFirstNotNullProperty(Properties props, String propName, String... values) {
      String notNull = getFirstNotNull(values);
      if (notNull != null) {
         props.setProperty(propName, notNull);
      }
   }

   /**
    * Returns the first not-null string in the provided list.
    *
    * @param values
    *       The list of possible values.
    * @return The first non-null value in the list.
    */
   public static String getFirstNotNull(String... values) {
      for (String value : values) {
         if (value != null) {
            return value;
         }
      }

      return null;
   }

   /**
    * Obtains the needed resource with full-path as URL. Works safely on all platforms.
    *
    * @param resource
    *       The name of the resource to obtain
    * @return The fully qualified resource URL location
    * @throws PerfCakeException
    *       in the case of wrong resource name.
    */
   public static URL getResourceAsUrl(final String resource) throws PerfCakeException {
      try {
         return Utils.class.getResource(resource).toURI().toURL();
      } catch (URISyntaxException | MalformedURLException e) {
         throw new PerfCakeException(String.format("Cannot obtain resource %s:", resource), e);
      }
   }

   /**
    * Obtains the needed resource with full-path. Works safely on all platforms.
    *
    * @param resource
    *       The name of the resource to obtain
    * @return The fully qualified resource location
    * @throws PerfCakeException
    *       in the case of wrong resource name.
    */
   public static String getResource(final String resource) throws PerfCakeException {
      try {
         return new File(Utils.class.getResource(resource).toURI()).getAbsolutePath();
      } catch (URISyntaxException e) {
         throw new PerfCakeException(String.format("Cannot obtain resource %s:", resource), e);
      }
   }

   /**
    * Atomically writes given content to a file.
    *
    * @param fileName
    *       Target file name.
    * @param content
    *       Content to be written.
    * @throws IOException
    *       In case of file operations failure.
    */
   public static void writeFileContent(final String fileName, final String content) throws PerfCakeException {
      writeFileContent(new File(fileName), content);
   }

   /**
    * Atomically writes given content to a file.
    *
    * @param file
    *       Target file.
    * @param content
    *       Content to be written.
    * @throws IOException
    *       In case of file operations failure.
    */
   public static void writeFileContent(final File file, final String content) throws PerfCakeException {
      writeFileContent(file.toPath(), content);
   }

   /**
    * Atomically writes given content to a file.
    *
    * @param path
    *       Target file path.
    * @param content
    *       Content to be written.
    * @throws IOException
    *       In case of file operations failure.
    */
   public static void writeFileContent(final Path path, final String content) throws PerfCakeException {
      try {
         if (log.isDebugEnabled()) {
            log.debug(String.format("Writing content to the file %s", path.toString()));
            if (log.isTraceEnabled()) {
               log.trace(String.format("File content: \"%s\"", content));
            }
         }

         final Path workFile = Paths.get(path.toString() + ".work");
         Files.write(workFile, content.getBytes(Utils.getDefaultEncoding()));
         Files.move(workFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException e) {
         String message = String.format("Could not write content to the file %s:", path.toString());
         log.error(message, e);
         throw new PerfCakeException(message, e);
      }
   }

   /**
    * Takes a resource as a StringTemplate, renders the template using the provided properties and stores it to the given path.
    *
    * @param resource
    *       Resource location of a template.
    * @param target
    *       Target path where to store the rendered template file.
    * @param properties
    *       Properties to fill into the template.
    * @throws PerfCakeException
    *       When it is not possible to render the template or store the target file.
    */
   public static void copyTemplateFromResource(final String resource, final Path target, final Properties properties) throws PerfCakeException {
      try {
         if (log.isDebugEnabled()) {
            log.debug(String.format("Copying template from resource %s to the file %s", resource, target.toString()));
            if (log.isTraceEnabled()) {
               final StringWriter sw = new StringWriter();
               properties.list(new PrintWriter(sw));
               log.trace(String.format("Properties for the template: \"%s\"", sw.toString()));
            }
         }

         StringTemplate template = new StringTemplate(new String(Files.readAllBytes(Paths.get(Utils.getResourceAsUrl(resource).toURI())), Utils.getDefaultEncoding()), properties);
         Utils.writeFileContent(target, template.toString());
      } catch (IOException | URISyntaxException e) {
         String message = String.format("Could not render template from resource %s:", resource);
         log.error(message, e);
         throw new PerfCakeException(message, e);
      }
   }

   /**
    * Reconfigures the logging level of the root logger and all suitable appenders.
    *
    * @param level
    *       The desired level.
    */
   public static void setLoggingLevel(final Level level) {
      final org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) log;
      final LoggerContext context = (LoggerContext) coreLogger.getContext();

      coreLogger.setLevel(level);

      for (Logger l : context.getLoggers()) {
         if (l.getName() != null && l.getName().startsWith("org.perfcake")) {
            ((org.apache.logging.log4j.core.Logger) l).setLevel(level);
         }
      }
      //((BaseConfiguration) context.getConfiguration()).ge
      //context.updateLoggers();

      //context.getLogger("org.perfcake").setLevel(level);
   }
}
