/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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

import org.perfcake.scenario.ReplayResults;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.util.TimerBenchmark;
import org.perfcake.util.Utils;
import org.perfcake.validation.ValidationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
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
    * Raw file with results to be replayed.
    */
   private String rawFile = null;

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

      if (se.rawFile == null) {
         se.executeScenario();
      } else {
         se.replayScenario();
      }

      log.info("=== Goodbye! ===");
   }

   /**
    * Executes the given scenario in the same way as PerfCake was started from the command line.
    * It just allows easy usage in TestNG and jUnit frameworks for instance.
    *
    * @param scenarioFile
    *       The file with scenario definition.
    * @param properties
    *       Any additional properties that would be normally set using -Dprop=value (most command line arguments can be set like this).
    * @throws PerfCakeException
    *       When it was not possible to load the scenario.
    */
   public static void execute(final String scenarioFile, final Properties properties) throws PerfCakeException {
      final Properties backup = new Properties();

      properties.forEach((k, v) -> {
         if (System.getProperty(k.toString()) != null) {
            backup.setProperty(k.toString(), System.getProperty(k.toString()));
         }

         System.setProperty(k.toString(), v.toString());
      });

      final Scenario scenario = ScenarioLoader.load(scenarioFile);
      scenario.init();
      scenario.run();
      scenario.close();

      backup.forEach((k, v) -> {
         System.setProperty(k.toString(), v.toString());
      });
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

      options.addOption(Option.builder("s").longOpt(PerfCakeConst.SCENARIO_OPT).desc("scenario to be executed").hasArg().argName("SCENARIO").build());
      options.addOption(Option.builder("sd").longOpt(PerfCakeConst.SCENARIOS_DIR_OPT).desc("directory for scenarios").hasArg().argName("SCENARIOS_DIR").build());
      options.addOption(Option.builder("md").longOpt(PerfCakeConst.MESSAGES_DIR_OPT).desc("directory for messages").hasArg().argName("MESSAGES_DIR").build());
      options.addOption(Option.builder("pd").longOpt(PerfCakeConst.PLUGINS_DIR_OPT).desc("directory for plugins").hasArg().argName("PLUGINS_DIR").build());
      options.addOption(Option.builder("pf").longOpt(PerfCakeConst.PROPERTIES_FILE_OPT).desc("custom system properties file").hasArg().argName("PROPERTIES_FILE").build());
      options.addOption(Option.builder("log").longOpt(PerfCakeConst.LOGGING_LEVEL_OPT).desc("logging level").hasArg().argName("LOG_LEVEL").build());
      options.addOption(Option.builder("r").longOpt(PerfCakeConst.REPLAY_OPT).desc("raw file to be replayed").hasArg().argName("RAW_FILE").build());
      options.addOption(Option.builder("skip").longOpt(PerfCakeConst.SKIP_TIMER_BENCHMARK_OPT).desc("skip system timer benchmark").build());
      options.addOption(Option.builder("D").argName("property=value").numberOfArgs(2).valueSeparator().desc("system properties").build());

      final CommandLineParser commandLineParser = new DefaultParser();
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

      if (commandLine.hasOption(PerfCakeConst.REPLAY_OPT)) {
         rawFile = commandLine.getOptionValue(PerfCakeConst.REPLAY_OPT);
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

      if (System.getProperty(PerfCakeConst.KEYSTORES_DIR_PROPERTY) == null) {
         System.setProperty(PerfCakeConst.KEYSTORES_DIR_PROPERTY, Utils.determineDefaultLocation("keystores"));
      }
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
         final ClassLoader currentClassLoader = ScenarioExecution.class.getClassLoader();
         final URL[] curls = ((URLClassLoader) currentClassLoader).getURLs();

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
               + " This is usually caused by deadlocks or race conditions in the application under test.");
         System.exit(PerfCakeConst.ERR_BLOCKED_THREADS);
      }
   }

   /**
    * Replays the previously recorded raw results.
    */
   private void replayScenario() {
      log.info("Replaying raw results recorded in {}.", rawFile);

      try (final ReplayResults replay = new ReplayResults(scenario, rawFile)) {
         replay.replay();
      } catch (IOException ioe) {
         log.fatal("Unable to replay scenario: ", ioe);
         System.exit(PerfCakeConst.ERR_SCENARIO_REPLAY);
      }
   }
}
