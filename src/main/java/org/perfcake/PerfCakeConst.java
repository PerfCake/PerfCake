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
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public final class PerfCakeConst {
   public static final String VERSION = "4.0";
   public static final String XSD_SCHEMA_VERSION = "4.0";

   public static final String MESSAGE_NUMBER_HEADER = "PerfCake_Performance_Message_Number";
   public static final String MESSAGE_NUMBER_PROPERTY = "MessageNumber";

   public static final String SCENARIO_PROPERTY = "perfcake.scenario";
   public static final String DEFAULT_ENCODING_PROPERTY = "perfcake.encoding";

   /**
    * Stores Unix timestamp number.
    */
   public static final String TIMESTAMP_PROPERTY = "perfcake.run.timestamp";

   /**
    * Stores timestamp in the format yyyyMMddHHmmss.
    */
   public static final String NICE_TIMESTAMP_PROPERTY = "perfcake.run.nice.timestamp";
   public static final String SCENARIOS_DIR_PROPERTY = "perfcake.scenarios.dir";
   public static final String MESSAGES_DIR_PROPERTY = "perfcake.messages.dir";
   public static final String PLUGINS_DIR_PROPERTY = "perfcake.plugins.dir";
   public static final String PROPERTIES_FILE_PROPERTY = "perfcake.properties.file";
   public static final String LOGGING_LEVEL_PROPERTY = "perfcake.logging.level";

   public static final String SCENARIO_OPT = "scenario";
   public static final String SCENARIOS_DIR_OPT = "scenarios-dir";
   public static final String MESSAGES_DIR_OPT = "messages-dir";
   public static final String PLUGINS_DIR_OPT = "plugins-dir";
   public static final String PROPERTIES_FILE_OPT = "properties-file";
   public static final String LOGGING_LEVEL_OPT = "log-level";
   public static final String SKIP_TIMER_BENCHMARK_OPT = "skip-timer-benchmark";

   public static final String WARM_UP_TAG = "warmUp";
}
