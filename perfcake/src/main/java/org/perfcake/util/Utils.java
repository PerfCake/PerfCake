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

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds useful utility methods used throughout PerfCake.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Utils {

   /**
    * Default name of resource directory.
    */
   public static final File DEFAULT_RESOURCES_DIR = new File("resources");

   /**
    * Default name of plugin directory.
    */
   public static final File DEFAULT_PLUGINS_DIR = new File("lib/plugins");

   /**
    * Replaces all ${&lt;property.name&gt;} placeholders in a string
    * by respective value of the property named &lt;property.name&gt; using {@link SystemPropertyGetter}.
    *
    * @param text
    *       The original string.
    * @return Filtered string.
    */
   public static String filterProperties(final String text) {
      final String propertyPattern = "[^\\\\](\\$\\{([^\\$\\{:]+)(:[^\\$\\{:]*)?})";
      final Matcher matcher = Pattern.compile(propertyPattern).matcher(text);

      return filterProperties(text, matcher, SystemPropertyGetter.INSTANCE);
   }

   /**
    * Filters properties in the given string. Consider using {@link org.perfcake.util.StringTemplate} instead.
    *
    * @param text
    *       The string to be filtered.
    * @param matcher
    *       The matcher to find the properties, any user specified matcher can be provided.
    * @param pg
    *       The {@link org.perfcake.util.properties.PropertyGetter} to provide values of the properties.
    * @return The filtered text.
    */
   public static String filterProperties(final String text, final Matcher matcher, final PropertyGetter pg) {
      String filteredString = text;

      matcher.reset();
      while (matcher.find()) {
         String pValue = null;
         final String pName = matcher.group(2);
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
    *       Property name.
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
    *       Property name.
    * @param defaultValue
    *       Default property value.
    * @return Property value or <code>defaultValue</code>.
    */
   public static String getProperty(final String name, final String defaultValue) {
      return SystemPropertyGetter.INSTANCE.getProperty(name, defaultValue);
   }

   /**
    * Writes the whole properties map to the given logger at the given level.
    *
    * @param logger
    *       The logger to log the properties.
    * @param level
    *       The level at which to log the properties.
    * @param properties
    *       The properties to log.
    */
   public static void logProperties(final Logger logger, final Level level, final Properties properties) {
      logProperties(logger, level, properties, "");
   }

   /**
    * Writes the whole properties map to the given logger at the given level with the given prefix.
    *
    * @param logger
    *       The logger to log the properties.
    * @param level
    *       The level at which to log the properties.
    * @param properties
    *       The properties to log.
    * @param prefix
    *       The prefix to prepend to each log message (used for aligning with spaces, tabs, etc.).
    */
   public static void logProperties(final Logger logger, final Level level, final Properties properties, final String prefix) {
      if (logger.isEnabled(level)) {
         for (final Entry<Object, Object> property : properties.entrySet()) {
            logger.log(level, prefix + property.getKey() + "=" + property.getValue());
         }
      }
   }

   /**
    * Reads URL (file) content into a string. The file content is processed as an UTF-8 encoded text.
    *
    * @param url
    *       The file location as an URL.
    * @return The file contents.
    * @throws IOException
    *       When it was not possible to read the content.
    */
   public static String readFilteredContent(final URL url) throws IOException {
      try (InputStream is = url.openStream(); Scanner scanner = new Scanner(is, "UTF-8")) {
         return filterProperties(scanner.useDelimiter("\\Z").next());
      } catch (final NoSuchElementException nsee) {
         throw new IOException("The content of " + url + " is empty.");
      }
   }

   /**
    * Reads lines from the given URL as a list of strings.
    *
    * @param url
    *       The URL to read the content from.
    * @return A list of lines in the content in the original order.
    * @throws IOException
    *       When it was not possible to read the content of the given URL.
    */
   public static List<String> readLines(final URL url) throws IOException {
      List<String> results = new ArrayList<>();

      try (InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr)) {

         String line;
         while ((line = br.readLine()) != null) {
            results.add(line);
         }
      }

      return results;
   }

   /**
    * Converts location to URL. If location specifies a protocol, it is immediately converted. Without a protocol specified, output is
    * file://${&lt;defaultLocationProperty&gt;}/&lt;location&gt;&lt;defaultSuffix&gt; using defaultLocation as a default value for the defaultLocationProperty
    * when the property is undefined.
    *
    * @param location
    *       The location of the resource.
    * @param defaultLocationProperty
    *       The property to read the default location prefix.
    * @param defaultLocation
    *       The default value for defaultLocationProperty if this property is undefined.
    * @param defaultSuffix
    *       The default suffix of the location.
    * @return The URL representing the location.
    * @throws MalformedURLException
    *       When the location cannot be converted to a URL.
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
    * Converts location to URL with check for the location existence. If location specifies a protocol, it is immediately converted. Without a protocol specified, the following paths
    * are checked for the existence:
    * 1. file://location
    * 2. file://$defaultLocationProperty/location or file://defaultLocation/location (when the property is not set)
    * 3. file://$defaultLocationProperty/location.suffix or file://defaultLocation/location.suffix (when the property is not set) with all the provided suffixes
    * If the file was not found, the result is simply file://location
    *
    * @param location
    *       The location of the resource.
    * @param defaultLocationProperty
    *       The property to read the default location prefix.
    * @param defaultLocation
    *       The default value for defaultLocationProperty if this property is undefined.
    * @param defaultSuffix
    *       The array of default default suffixes to try when searching for the resource.
    * @return The URL representing the location.
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

         if (!Files.exists(p) || Files.isDirectory(p)) {
            p = Paths.get(Utils.getProperty(defaultLocationProperty, defaultLocation), uri);

            if (!Files.exists(p) || Files.isDirectory(p)) {
               if (defaultSuffix != null && defaultSuffix.length > 0) {
                  //                  boolean found = false;

                  for (final String suffix : defaultSuffix) {
                     p = Paths.get(Utils.getProperty(defaultLocationProperty, defaultLocation), uri + suffix);
                     if (Files.exists(p) && !Files.isDirectory(p)) {
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
    *       The optional suffix to be added to the path.
    * @return The location based on the resourcesDir constant.
    */
   public static String determineDefaultLocation(final String locationSuffix) {
      return DEFAULT_RESOURCES_DIR.getAbsolutePath() + "/" + (locationSuffix == null ? "" : locationSuffix);
   }

   /**
    * Converts camelCaseStringsWithACRONYMS to CAMEL_CASE_STRINGS_WITH_ACRONYMS
    *
    * @param camelCase
    *       The camelCase string.
    * @return The same string in equivalent format for Java enum values.
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
    *       The timestamp in milliseconds.
    * @return The string representing the timestamp in H:MM:SS format.
    */
   public static String timeToHMS(final long time) {
      final long hours = TimeUnit.MILLISECONDS.toHours(time);
      final long minutes = TimeUnit.MILLISECONDS.toMinutes(time - TimeUnit.HOURS.toMillis(hours));
      final long seconds = TimeUnit.MILLISECONDS.toSeconds(time - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes));

      final StringBuilder sb = new StringBuilder();
      sb.append(hours).append(":").append(String.format("%02d", minutes)).append(":").append(String.format("%02d", seconds));

      return sb.toString();
   }

   /**
    * Gets the default encoding. Uses {@link PerfCakeConst#DEFAULT_ENCODING_PROPERTY} system property, if this property is not set, <b>UTF-8</b> is used.
    *
    * @return The string representation of default encoding for all read and written files
    */
   public static String getDefaultEncoding() {
      return Utils.getProperty(PerfCakeConst.DEFAULT_ENCODING_PROPERTY, "UTF-8");
   }

   /**
    * Computes a linear regression trend of the data set provided.
    *
    * @param data
    *       Data on which to compute the trend.
    * @return The linear regression trend.
    */
   public static double computeRegressionTrend(final Collection<TimestampedRecord<Number>> data) {
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
    *       The properties instance.
    * @param propName
    *       The name of the property to be set.
    * @param values
    *       The list of possibilities, the first not-null is used to set the property value.
    */
   public static void setFirstNotNullProperty(final Properties props, final String propName, final String... values) {
      final String notNull = getFirstNotNull(values);
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
   public static String getFirstNotNull(final String... values) {
      for (final String value : values) {
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
    *       The name of the resource to obtain.
    * @return The fully qualified resource URL location.
    * @throws PerfCakeException
    *       In the case of wrong resource name.
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
    *       The name of the resource to obtain.
    * @return The fully qualified resource location.
    * @throws PerfCakeException
    *       In the case of wrong resource name.
    */
   public static String getResource(final String resource) throws PerfCakeException {
      try {
         return new File(Utils.class.getResource(resource).toURI()).getAbsolutePath();
      } catch (final URISyntaxException e) {
         throw new PerfCakeException(String.format("Cannot obtain resource %s:", resource), e);
      }
   }

   /**
    * Atomically writes given content to a file.
    *
    * @param fileName
    *       The target file name.
    * @param content
    *       The content to be written.
    * @throws org.perfcake.PerfCakeException
    *       In the case of s file operations failure.
    */
   public static void writeFileContent(final String fileName, final String content) throws PerfCakeException {
      writeFileContent(new File(fileName), content);
   }

   /**
    * Atomically writes given content to a file.
    *
    * @param file
    *       The target file.
    * @param content
    *       The content to be written.
    * @throws org.perfcake.PerfCakeException
    *       In the case of file operations failure.
    */
   public static void writeFileContent(final File file, final String content) throws PerfCakeException {
      writeFileContent(file.toPath(), content);
   }

   /**
    * Atomically writes given content to a file.
    *
    * @param path
    *       The target file path.
    * @param content
    *       The content to be written.
    * @throws org.perfcake.PerfCakeException
    *       In the case of file operations failure.
    */
   public static void writeFileContent(final Path path, final String content) throws PerfCakeException {
      try {
         final Path workFile = Paths.get(path.toString() + ".work");
         Files.write(workFile, content.getBytes(Utils.getDefaultEncoding()));
         Files.move(workFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (final IOException e) {
         final String message = String.format("Could not write content to the file %s:", path.toString());
         throw new PerfCakeException(message, e);
      }
   }

   /**
    * Takes a resource as a StringTemplate, renders the template using the provided properties and stores it to the given path.
    *
    * @param resource
    *       The resource location of a template.
    * @param target
    *       The target path where to store the rendered template file.
    * @param properties
    *       The properties to fill into the template.
    * @throws PerfCakeException
    *       When it is not possible to render the template or store the target file.
    */
   public static void copyTemplateFromResource(final String resource, final Path target, final Properties properties) throws PerfCakeException {
      try {
         final StringTemplate template = new StringTemplate(IOUtils.toString(Utils.class.getResourceAsStream(resource), Utils.getDefaultEncoding()), properties);
         Utils.writeFileContent(target, template.toString());
      } catch (final IOException e) {
         final String message = String.format("Could not render template from resource %s:", resource);
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
      final Logger log = LogManager.getLogger(Utils.class);
      final org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) log;
      final LoggerContext context = coreLogger.getContext();

      context.getConfiguration().getLoggers().get("org.perfcake").setLevel(level);
      context.updateLoggers();
   }

   /**
    * Initializes system properties that carry time stamps.
    */
   public static void initTimeStamps() {
      if (System.getProperty(PerfCakeConst.TIMESTAMP_PROPERTY) == null) {
         System.setProperty(PerfCakeConst.TIMESTAMP_PROPERTY, String.valueOf(Calendar.getInstance().getTimeInMillis()));
      }

      if (System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) == null) {
         System.setProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY, (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()));
      }
   }

}
