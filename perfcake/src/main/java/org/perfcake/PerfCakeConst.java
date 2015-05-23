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
   public static final String VERSION = "5.0";

   /**
    * PerfCake scenario XML Schema version that is part of namespace <code>urn:perfcake:scenario:&lt;version&gt;</code>.
    */
   public static final String XSD_SCHEMA_VERSION = "5.0";

   /**
    * Name of the message header that stores message number.
    */
   public static final String MESSAGE_NUMBER_HEADER = "PerfCake_Performance_Message_Number";

   /**
    * Name of the system property that stores message number. It can be used in messages as a placeholder.
    */
   public static final String MESSAGE_NUMBER_PROPERTY = "MessageNumber";

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
    * Name of the system property that stores path to the property file with system properties.
    */
   public static final String PROPERTIES_FILE_PROPERTY = "perfcake.properties.file";

   /**
    * Name of the system property that stores PerfCake logging level.
    */
   public static final String LOGGING_LEVEL_PROPERTY = "perfcake.logging.level";

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
    * Result name indicating whether the scenario
    */
   public static final String WARM_UP_TAG = "warmUp";

   /**
    * Property that can disable templating engine (HTTL) in {@link org.perfcake.util.StringTemplate}.
    */
   public static final String DISABLE_TEMPLATES_PROPERTY = "perfcake.templates.disabled";
}
