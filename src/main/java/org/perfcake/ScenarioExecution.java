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
package org.perfcake;

import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.util.Utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * The main class to start PerfCake from command line. It parses command line parameters, loads the scenario XML file and runs it.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_EXIT", justification = "This class is allowed to terminate the JVM.")
public class ScenarioExecution {

   private static final Logger log = Logger.getLogger(ScenarioExecution.class);

   /**
    * Command line parameters.
    */
   private CommandLine commandLine;

   /**
    * The scenario created from the specified XML file.
    */
   private Scenario scenario;

   /**
    * Parses command line arguments and creates this class to take care of the Scenario execution.
    *
    * @param args
    *       command line arguments.
    */
   private ScenarioExecution(final String[] args) {
      parseCommandLine(args);
      loadScenario();
   }

   /**
    * The main method which creates an instance of ScenarioExecution and executes the scenario.
    *
    * @param args
    *       Command line arguments.
    */
   public static void main(final String[] args) {
      ScenarioExecution se = new ScenarioExecution(args);

      log.info(String.format("=== Welcome to PerfCake %s ===", PerfCakeConst.VERSION));

      if (log.isEnabledFor(Level.TRACE)) {
         // Print system properties
         se.printTraceInformation();
      }

      se.executeScenario();

      log.info("=== Goodbye! ===");
   }

   /**
    * Parses a single command line parameter/option.
    *
    * @param option
    *       The parameter/option name.
    * @param property
    *       The system property to which the option value should be stored.
    * @param defaultValue
    *       The default value for the option when it is not present at the command line.
    */
   private void parseParameter(final String option, final String property, final String defaultValue) {
      if (commandLine.hasOption(option)) {
         System.setProperty(property, commandLine.getOptionValue(option));
      } else {
         if (defaultValue != null) {
            System.setProperty(property, defaultValue);
         }
      }
   }

   /**
    * Parses additional user properties specified in a property file and at the command line.
    */
   private void parseUserProperties() {
      Properties props = new Properties();
      String propsFile = System.getProperty(PerfCakeConst.PROPERTIES_FILE_PROPERTY);
      if (propsFile != null) {
         try (final FileInputStream propsInputStream = new FileInputStream(propsFile)) {
            props.load(propsInputStream);
         } catch (IOException e) {
            // we can still continue without reading file
            log.warn(String.format("Unable to read the properties file '%s': ", propsFile), e);
         }
      }

      props.putAll(commandLine.getOptionProperties("D"));

      for (Entry<Object, Object> entry : props.entrySet()) {
         System.setProperty(entry.getKey().toString(), entry.getValue().toString());
      }

   }

   /**
    * Parses the command line.
    *
    * @param args
    *       Command line arguments.
    */
   @SuppressWarnings("static-access")
   private void parseCommandLine(final String[] args) {
      final HelpFormatter formatter = new HelpFormatter();
      final Options options = new Options();

      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.SCENARIO_OPT).withDescription("scenario to be executed").hasArg().withArgName("SCENARIO").create("s"));
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.SCENARIOS_DIR_OPT).withDescription("directory for scenarios").hasArg().withArgName("SCENARIOS_DIR").create("sd"));
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.MESSAGES_DIR_OPT).withDescription("directory for messages").hasArg().withArgName("MESSAGES_DIR").create("md"));
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.PLUGINS_DIR_OPT).withDescription("directory for plugins").hasArg().withArgName("PLUGINS_DIR").create("pd"));
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.PROPERTIES_FILE_OPT).withDescription("custom system properties file").hasArg().withArgName("PROPERTIES_FILE").create("pf"));
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.LOGGING_LEVEL_OPT).withDescription("logging level").hasArg().withArgName("PROPERTIES_FILE").create("log"));
      options.addOption(OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator().withDescription("system properties").create("D"));

      final CommandLineParser commandLineParser = new GnuParser();
      try {
         commandLine = commandLineParser.parse(options, args);
      } catch (ParseException pe) {
         pe.printStackTrace();
         System.exit(2);
         return;
      }

      if (commandLine.hasOption(PerfCakeConst.SCENARIO_OPT)) {
         System.setProperty(PerfCakeConst.SCENARIO_PROPERTY, commandLine.getOptionValue(PerfCakeConst.SCENARIO_OPT));
      } else {
         formatter.printHelp("ScenarioExecution -s <SCENARIO> [-sd <SCENARIOS_DIR>] [-md <MESSAGES_DIR>] [-D<property=value>]*", options);
         System.exit(1);
         return;
      }

      parseParameter(PerfCakeConst.SCENARIOS_DIR_OPT, PerfCakeConst.SCENARIOS_DIR_PROPERTY, Utils.determineDefaultLocation("scenarios"));
      parseParameter(PerfCakeConst.MESSAGES_DIR_OPT, PerfCakeConst.MESSAGES_DIR_PROPERTY, Utils.determineDefaultLocation("messages"));
      parseParameter(PerfCakeConst.PLUGINS_DIR_OPT, PerfCakeConst.PLUGINS_DIR_PROPERTY, Utils.DEFAULT_PLUGINS_DIR.getAbsolutePath());
      parseParameter(PerfCakeConst.PROPERTIES_FILE_OPT, PerfCakeConst.PROPERTIES_FILE_PROPERTY, null);
      parseParameter(PerfCakeConst.LOGGING_LEVEL_OPT, PerfCakeConst.LOGGING_LEVEL_PROPERTY, null);
      if (Utils.getProperty(PerfCakeConst.LOGGING_LEVEL_PROPERTY, null) != null) {
         Utils.setLoggingLevel(Level.toLevel(Utils.getProperty(PerfCakeConst.LOGGING_LEVEL_PROPERTY), Level.INFO));
      }

      parseUserProperties();

      System.setProperty(PerfCakeConst.TIMESTAMP_PROPERTY, String.valueOf(Calendar.getInstance().getTimeInMillis()));
      System.setProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY, (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()));
   }

   /**
    * Prints trace information for test debugging purposes.
    */
   private void printTraceInformation() {
      log.trace("System properties:");
      List<String> p = new LinkedList<>();
      for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
         p.add("\t" + entry.getKey() + "=" + entry.getValue());
      }

      Collections.sort(p);

      for (String s : p) {
         log.trace(s);
      }

      // Print classpath
      log.trace("Classpath:");
      ClassLoader currentCL = ScenarioExecution.class.getClassLoader();
      URL[] curls = ((URLClassLoader) currentCL).getURLs();

      for (URL curl : curls) {
         log.trace("\t" + curl);
      }
   }

   /**
    * Loads the scenario from the XML file specified at the command line.
    */
   private void loadScenario() {
      String scenarioFile = Utils.getProperty(PerfCakeConst.SCENARIO_PROPERTY);

      try {
         scenario = ScenarioLoader.load(scenarioFile);
      } catch (Exception e) {
         log.fatal(String.format("Cannot load scenario '%s': ", scenarioFile), e);
         System.exit(3);
      }
   }

   /**
    * Executes the loaded scenario.
    */
   private void executeScenario() {
      try {
         scenario.init();
         scenario.run();
      } catch (PerfCakeException e) {
         log.fatal("Error running scenario: ", e);
      } finally {
         try {
            scenario.close();
         } catch (PerfCakeException e) {
            log.fatal("Scenario did not finish properly: ", e);
         }
      }
   }
}
