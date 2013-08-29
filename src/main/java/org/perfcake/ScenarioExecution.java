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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

/**
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class ScenarioExecution {

   private static final Logger log = Logger.getLogger(ScenarioExecution.class);

   @SuppressWarnings("static-access")
   public static void main(final String[] args) {
      final HelpFormatter formatter = new HelpFormatter();

      final Options options = new Options();

      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.SCENARIO_OPT).withDescription("scenario to be executed").hasArg().withArgName("SCENARIO").create("s"));
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.SCENARIOS_DIR_OPT).withDescription("directory for scenarios").hasArg().withArgName("SCENARIOS_DIR").create("sd"));
      options.addOption(OptionBuilder.withLongOpt(PerfCakeConst.MESSAGES_DIR_OPT).withDescription("directory for messages").hasArg().withArgName("MESSAGES_DIR").create("md"));
      options.addOption(OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator().withDescription("system properties").create("D"));

      final CommandLineParser commandLineParser = new GnuParser();
      final CommandLine commandLine;
      try {
         commandLine = commandLineParser.parse(options, args);
      } catch (ParseException pe) {
         pe.printStackTrace();
         return;
      }

      if (commandLine.hasOption(PerfCakeConst.SCENARIO_OPT)) {
         System.setProperty(PerfCakeConst.SCENARIO_PROPERTY, commandLine.getOptionValue(PerfCakeConst.SCENARIO_OPT));
      } else {
         formatter.printHelp("ScenarioExecution -s <SCENARIO> [-sd <SCENARIO_DIR>] [-md <MESSAGES_DIR>] [-D<property=value>]*", options);
         return;
      }

      if (commandLine.hasOption(PerfCakeConst.SCENARIOS_DIR_OPT)) {
         System.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, commandLine.getOptionValue(PerfCakeConst.SCENARIOS_DIR_OPT));
      }

      if (commandLine.hasOption(PerfCakeConst.MESSAGES_DIR_OPT)) {
         System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, commandLine.getOptionValue(PerfCakeConst.MESSAGES_DIR_OPT));
      }

      Properties props = commandLine.getOptionProperties("D");
      for (Entry<Object, Object> entry : props.entrySet()) {
         System.setProperty(entry.getKey().toString(), entry.getValue().toString());
      }

      System.setProperty(PerfCakeConst.TIMESTAMP_PROPERTY, String.valueOf(Calendar.getInstance().getTimeInMillis()));

      log.info("=== Welcome to PerfCake ===");

      if (log.isDebugEnabled()) {
         // Print system properties
         log.debug("System properties:");
         List<String> p = new LinkedList<>();
         for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            p.add("\t" + entry.getKey() + "=" + entry.getValue());
         }

         Collections.sort(p);

         for (String s : p) {
            log.debug(s);
         }

         // Print classpath
         log.debug("Classpath:");
         ClassLoader currentCL = ScenarioExecution.class.getClassLoader();
         URL[] curls = ((URLClassLoader) currentCL).getURLs();

         for (int i = 0; i < curls.length; i++) {
            log.debug("\t" + curls[i]);
         }

      }

      Scenario scenario = null;

      try {
         scenario = new Scenario(commandLine.getOptionValue(PerfCakeConst.SCENARIO_OPT));
         scenario.parse();
      } catch (PerfCakeException e) {
         log.fatal("Cannot parse scenario: ", e);
         return;
      }

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
