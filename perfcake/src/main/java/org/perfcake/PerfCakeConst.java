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

/**
 * Used to store String constants used throughout PerfCake.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public final class PerfCakeConst {
   /**
    * PerfCake version.
    */
   public static final String VERSION = "8.0";

   /**
    * PerfCake scenario XML Schema version that is part of namespace <code>urn:perfcake:scenario:&lt;version&gt;</code>.
    */
   public static final String XSD_SCHEMA_VERSION = "8.0";

   /**
    * Help on PerfCake command line usage.
    */
   public static final String USAGE_HELP = "ScenarioExecution -s <SCENARIO> [options] [-D<property=value>]*";

   /**
    * PerfCake welcome greeting.
    */
   public static final String WELCOME = "=== Welcome to PerfCake %s ===";

   /**
    * Name of the message header that stores the number of the current iteration.
    */
   public static final String ITERATION_NUMBER_PROPERTY = "perfcake.iteration.number";

   /**
    * Name of the system property that stores name of scenario that is executed.
    */
   public static final String SCENARIO_PROPERTY = "perfcake.scenario";

   /**
    * Name of the system property that stores name of the default String encoding.
    */
   public static final String DEFAULT_ENCODING_PROPERTY = "perfcake.encoding";

   /**
    * Name of the system property that stores Unix timestamp.
    */
   public static final String TIMESTAMP_PROPERTY = "perfcake.run.timestamp";

   /**
    * Name of the system property that stores timestamp in the format <code>yyyyMMddHHmmss</code>.
    */
   public static final String NICE_TIMESTAMP_PROPERTY = "perfcake.run.nice.timestamp";

   /**
    * Name or the system property that stores path where scenarios are taken from.
    */
   public static final String SCENARIOS_DIR_PROPERTY = "perfcake.scenarios.dir";

   /**
    * Name of the system property that stores path where messages are taken from.
    */
   public static final String MESSAGES_DIR_PROPERTY = "perfcake.messages.dir";

   /**
    * Name of the system property that stores path where PerfCake plugins are loaded from.
    */
   public static final String PLUGINS_DIR_PROPERTY = "perfcake.plugins.dir";

   /**
    * Name of the system property that specifies SSL key store directory.
    */
   public static final String KEYSTORES_DIR_PROPERTY = "perfcake.keystores.dir";

   /**
    * Name of the system property that stores path to the property file with system properties.
    */
   public static final String PROPERTIES_FILE_PROPERTY = "perfcake.properties.file";

   /**
    * Name of the system property that stores PerfCake logging level.
    */
   public static final String LOGGING_LEVEL_PROPERTY = "perfcake.logging.level";

   /**
    * Name of the system property that stores the skip timer benchmark option.
    */
   public static final String SKIP_TIMER_BENCHMARK_PROPERTY = "perfcake.skip.timer.benchmark";

   /**
    * Name of the system property that stores the path to the raw recording to be replayed.
    */
   public static final String REPLAY_PROPERTY = "perfcake.replay";

   /**
    * Name of the system property that stores the debug agent activation state.
    */
   public static final String DEBUG_PROPERTY = "perfcake.debug";

   /**
    * Name of the system property that stores the debug agent name.
    */
   public static final String DEBUG_AGENT_NAME_PROPERTY = "perfcake.debug.name";

   /**
    * Name of the system property to cause immediate scenario termination when there is an exception thrown by a sender.
    */
   public static final String FAIL_FAST_PROPERTY = "perfcake.fail.fast";

   /**
    * Name of the system property that stores the boot delay for receivers.
    */
   public static final String RECEIVER_BOOT_DELAY_PROPERTY = "perfcake.receiver.boot.delay";

   /**
    * Name of the CLI argument to print help.
    */
   public static final String HELP_OPT = "help";

   /**
    * Name of the CLI argument to specify scenario name.
    */
   public static final String SCENARIO_OPT = "scenario";

   /**
    * Name of the CLI argument to specify path to scenarios.
    */
   public static final String SCENARIOS_DIR_OPT = "scenarios-dir";

   /**
    * Name of the CLI argument to specify path to messages.
    */
   public static final String MESSAGES_DIR_OPT = "messages-dir";

   /**
    * Name of the CLI argument to specify path to plugins.
    */
   public static final String PLUGINS_DIR_OPT = "plugins-dir";

   /**
    * Name of the CLI argument to specify path to the property file.
    */
   public static final String PROPERTIES_FILE_OPT = "properties-file";

   /**
    * Name of the CLI argument to specify PerfCake logging level.
    */
   public static final String LOGGING_LEVEL_OPT = "log-level";

   /**
    * Name of the CLI argument to skip timer benchmark.
    */
   public static final String SKIP_TIMER_BENCHMARK_OPT = "skip-timer-benchmark";

   /**
    * Name of the cli argument to start raw recording replay.
    */
   public static final String REPLAY_OPT = "replay";

   /**
    * Name of the cli argument to start debugging agent.
    */
   public static final String DEBUG_OPT = "debug";

   /**
    * Name of the cli argument to start debugging agent name.
    */
   public static final String DEBUG_AGENT_NAME_OPT = "debug-name";

   /**
    * Default name of the debug agent.
    */
   public static final String DEBUG_AGENT_DEFAULT_NAME = "perfcake-1";

   /**
    * Result name indicating whether the scenario.
    */
   public static final String WARM_UP_TAG = "warmUp";

   /**
    * Result name counting the number of failed sender tasks.
    */
   public static final String FAILURES_TAG = "failures";

   /**
    * Result name of message attributes stored in the measurement unit.
    */
   public static final String ATTRIBUTES_TAG = "attributes";

   /**
    * Result name of the number of threads.
    */
   public static final String THREADS_TAG = "Threads";

   /**
    * Result name of the total size of response.
    */
   public static final String RESPONSE_SIZE_TAG = "ResponseSize";

   /**
    * Result name of the total size of request.
    */
   public static final String REQUEST_SIZE_TAG = "RequestSize";

   /**
    * Result name of the user tags.
    */
   public static final String TAGS_TAG = "tags";

   /**
    * Result name of the test execution real time tag.
    */
   public static final String REAL_TIME_TAG = "rt";

   /**
    * Property that can disable templating engine (HTTL) in {@link org.perfcake.util.StringTemplate}.
    */
   public static final String DISABLE_TEMPLATES_PROPERTY = "perfcake.templates.disabled";

   /**
    * Exit code when there is no scenario specified.
    */
   public static final int ERR_NO_SCENARIO = 1;

   /**
    * Exit code when there are wrong parameters on the command line.
    */
   public static final int ERR_PARAMETERS = 2;

   /**
    * Exit code when it was not possible to parse scenario properly.
    */
   public static final int ERR_SCENARIO_LOADING = 3;

   /**
    * Exit code when there was an error during scenario execution.
    */
   public static final int ERR_SCENARIO_EXECUTION = 4;

   /**
    * Exit code when there were validation errors.
    */
   public static final int ERR_VALIDATION = 5;

   /**
    * Exit code when there are blocked threads after the scenario was executed.
    */
   public static final int ERR_BLOCKED_THREADS = 6;

   /**
    * Error while trying to replay scenario.
    */
   public static final int ERR_SCENARIO_REPLAY = 7;

   /**
    * Error while trying to replay scenario.
    */
   public static final int ERR_PRINT_HELP = 8;

   /**
    * There should be no instance of a utility class.
    */
   private PerfCakeConst() {
   }
}
