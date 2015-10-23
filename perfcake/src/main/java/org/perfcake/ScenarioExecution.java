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
import org.perfcake.util.TimerBenchmark;
import org.perfcake.util.Utils;
import org.perfcake.validation.ValidationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Parses command line parameters, loads the scenario from XML or DSL file and executes it.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_EXIT", justification = "This class is allowed to terminate the JVM.")
public class ScenarioExecution {

   private static final Logger log = LogManager.getLogger(ScenarioExecution.class);

   /**
    * Command line parameters.
    */
   private CommandLine commandLine;

   /**
    * The scenario created from the specified XML file.
    */
   private Scenario scenario;

   /**
    * Skips timer benchmark when set to true.
    */
   private boolean skipTimerBenchmark = false;

   /**
    * Parses command line arguments and creates this class to take care of the Scenario execution.
    *
    * @param args
    *       command line arguments.
    */
   private ScenarioExecution(final String[] args) {
      Utils.initTimeStamps();
      parseCommandLine(args);
      loadScenario();
   }

   /**
    * Creates an instance of {@link org.perfcake.ScenarioExecution} and executes the scenario.
    *
    * @param args
    *       Command line arguments.
    */
   public static void main(final String[] args) {
      final ScenarioExecution se = new ScenarioExecution(args);

      log.info(String.format("=== Welcome to PerfCake %s ===", PerfCakeConst.VERSION));

      // Print system properties
      se.printTraceInformation();

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
      final Properties props = new Properties();
      final String propsFile = System.getProperty(PerfCakeConst.PROPERTIES_FILE_PROPERTY);
      if (propsFile != null) {
         try (final FileInputStream propsInputStream = new FileInputStream(propsFile)) {
            props.load(propsInputStream);
         } catch (final IOException e) {
            // we can still continue without reading file
            log.warn(String.format("Unable to read the properties file '%s': ", propsFile), e);
         }
      }

      props.putAll(commandLine.getOptionProperties("D"));

      for (final Entry<Object, Object> entry : props.entrySet()) {
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
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.LOGGING_LEVEL_OPT).withDescription("logging level").hasArg().withArgName("LOG_LEVEL").create("log"));
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.SKIP_TIMER_BENCHMARK_OPT).withDescription("skip system timer benchmark").create("skip"));
      options.addOption(OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator().withDescription("system properties").create("D"));

      final CommandLineParser commandLineParser = new GnuParser();
      try {
         commandLine = commandLineParser.parse(options, args);
      } catch (final ParseException pe) {
         log.fatal("Cannot parse application parameters: ", pe);
         formatter.printHelp(PerfCakeConst.USAGE_HELP, options);
         System.exit(PerfCakeConst.ERR_PARAMETERS);
         return;
      }

      if (commandLine.hasOption(PerfCakeConst.SCENARIO_OPT)) {
         System.setProperty(PerfCakeConst.SCENARIO_PROPERTY, commandLine.getOptionValue(PerfCakeConst.SCENARIO_OPT));
      } else {
         formatter.printHelp(PerfCakeConst.USAGE_HELP, options);
         System.exit(PerfCakeConst.ERR_NO_SCENARIO);
         return;
      }

      if (commandLine.hasOption(PerfCakeConst.SKIP_TIMER_BENCHMARK_OPT)) {
         skipTimerBenchmark = true;
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
   }

   /**
    * Prints trace information for test debugging purposes.
    */
   private void printTraceInformation() {
      if (log.isTraceEnabled()) {
         log.trace("System properties:");
         final List<String> p = System.getProperties().entrySet().stream().map(entry -> "\t" + entry.getKey() + "=" + entry.getValue()).collect(Collectors.toCollection(() -> new LinkedList<>()));

         Collections.sort(p);

         for (final String s : p) {
            log.trace(s);
         }

         // Print classpath
         log.trace("Classpath:");
         final ClassLoader currentCL = ScenarioExecution.class.getClassLoader();
         final URL[] curls = ((URLClassLoader) currentCL).getURLs();

         for (final URL curl : curls) {
            log.trace("\t" + curl);
         }
      }
   }

   /**
    * Loads the scenario from the XML file specified at the command line.
    */
   private void loadScenario() {
      final String scenarioFile = Utils.getProperty(PerfCakeConst.SCENARIO_PROPERTY);

      try {
         scenario = ScenarioLoader.load(scenarioFile);
      } catch (final Exception e) {
         log.fatal(String.format("Cannot load scenario '%s': ", scenarioFile), e);
         System.exit(PerfCakeConst.ERR_SCENARIO_LOADING);
      }
   }

   /**
    * Executes the loaded scenario.
    */
   private void executeScenario() {
      if (!skipTimerBenchmark) {
         TimerBenchmark.measureTimerResolution();
      }

      boolean err = false;

      try {
         scenario.init();
         scenario.run();
      } catch (final PerfCakeException e) {
         log.fatal("Error running scenario: ", e);
         err = true;
      } finally {
         try {
            scenario.close();
         } catch (final ValidationException ve) {
            log.warn(ve.getMessage());
            System.exit(PerfCakeConst.ERR_VALIDATION);
         } catch (final PerfCakeException e) {
            log.fatal("Scenario did not finish properly: ", e);
            err = true;
         }
      }

      if (err) {
         System.exit(PerfCakeConst.ERR_SCENARIO_EXECUTION);
      }

      if (!scenario.areAllThreadsTerminated()) {
         log.warn("There are some blocked threads that were not possible to terminate. The test results might be flawed."
               + " This is usually caused by deadlocks or raise conditions in the application under test.");
         System.exit(PerfCakeConst.ERR_BLOCKED_THREADS);
      }
   }
}
