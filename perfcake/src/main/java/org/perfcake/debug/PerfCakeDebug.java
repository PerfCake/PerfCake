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
package org.perfcake.debug;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.message.correlator.Correlator;
import org.perfcake.message.generator.MessageGenerator;
import org.perfcake.message.receiver.Receiver;
import org.perfcake.message.sequence.Sequence;
import org.perfcake.reporting.destination.Destination;
import org.perfcake.reporting.reporter.Reporter;
import org.perfcake.util.Utils;
import org.perfcake.validation.MessageValidator;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicInformational;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Informational;
import com.netflix.servo.monitor.MonitorConfig;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.avaje.agentloader.AgentLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JMX based debug agent providing information about the running performance test.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class PerfCakeDebug {

   /**
    * The logger.
    */
   private static final Logger log = LogManager.getLogger(PerfCakeDebug.class);

   /**
    * Singleton instance of the debug agent.
    */
   private static PerfCakeDebug INSTANCE;

   /**
    * Name of the debug agent. Determines the property key in JMX. Defaults to perfcake-1. Important when multiple instances were running.
    */
   private static String AGENT_NAME = Utils.getProperty(PerfCakeConst.DEBUG_AGENT_NAME_PROPERTY, PerfCakeConst.DEBUG_AGENT_DEFAULT_NAME);

   /**
    * Class name of the generator used.
    */
   private Informational messageGeneratorClass;

   /**
    * Class name of the sender used.
    */
   private Informational messageSenderClass;

   /**
    * Class name of the receiver used.
    */
   private Informational receiverClass;

   /**
    * Class name of the correlator used.
    */
   private Informational correlatorClass;

   /**
    * Class names of the sequences used.
    */
   private Map<String, Informational> sequenceClasses = new ConcurrentHashMap<>();

   /**
    * Class names of the validators used.
    */
   private Map<String, Informational> validatorClasses = new ConcurrentHashMap<>();

   /**
    * How many sender tasks were created so far.
    */
   private Counter generatedSenderTasks = newCounter("GeneratedSenderTasks");

   /**
    * Number of sent messages.
    */
   private Counter sentMessages = newCounter("SentMessages");

   /**
    * Number of received messages.
    */
   private Counter correlatedMessages = newCounter("CorrelatedMessages");

   /**
    * Number of sequence snapshots taken.
    */
   private Counter sequenceSnapshots = newCounter("SequenceSnapshots");

   /**
    * Internal reference of parent reportes of individual destinations for later reporting.
    */
   private Map<Destination, String> parentReporters = new ConcurrentHashMap<>();

   /**
    * Number of measurement units passed to individual reporter for accumulation.
    * The key is the reporter class name.
    */
   private Map<String, Counter> resultsReported = new ConcurrentHashMap<>();

   /**
    * Number of results published to individual destinations.
    * The key is in the format of &lt;reporter class name&gt;.&lt;destination class name&gt;.
    */
   private Map<String, Counter> resultsWritten = new ConcurrentHashMap<>();

   /**
    * Number of validated messages. The key is validator id.
    */
   private Map<String, Counter> validationResults = new ConcurrentHashMap<>();

   /**
    * Internal map of validators and their ids for later reporting.
    */
   private Map<MessageValidator, String> validatorIds = new ConcurrentHashMap<>();

   /**
    * Initializes and installs the agent.
    */
   private PerfCakeDebug() { // we do not want external creation
      INSTANCE = this;
      log.debug("Creating PerfCakeDebug instance.");

      try {
         final String clazzName = "org.jboss.byteman.agent.Main";
         log.debug(String.format("Looking for class %s", clazzName));
         final Class clazz = Class.forName(clazzName);
         log.debug(String.format("Class found: %s", clazz.toString()));
         final String params = String.format("listener:false,script:%s", saveTmpBytemanRules());
         log.debug(String.format("Loading agent by class name %s with parameters: %s", clazz.getName(), params));
         final boolean agentLoaded = AgentLoader.loadAgentByMainClass(clazz.getName(), params);
         log.debug(String.format("Agent loaded: %s", agentLoaded));
      } catch (Exception e) {
         log.error("Unable to install debug agent, debugging information will not be available: ", e);
      } catch (NoClassDefFoundError ncdfe) {
         log.error("Unable to install debug agent. Make sure you have tools.jar in your JDK installation or copy it to $PERFCAKE_HOME/lib/ext. Debugging information will not be available: ", ncdfe);
      }
   }

   /**
    * Gets the agent instance.
    *
    * @return The agent instance. Can be null when the agent was not initialized.
    */
   public static PerfCakeDebug getInstance() {
      return INSTANCE;
   }

   /**
    * Initializes the debug agent.
    */
   public static synchronized void initialize() {
      System.setProperty("org.jboss.byteman.compileToBytecode", "true");
      System.setProperty("com.netflix.servo.DefaultMonitorRegistry.registryName", "org.perfcake");

      new PerfCakeDebug();
   }

   /**
    * Reports the message generator class name.
    *
    * @param generator
    *       The message generator instance.
    */
   public static void reportGeneratorName(final MessageGenerator generator) {
      if (INSTANCE != null) {
         if (INSTANCE.messageGeneratorClass == null) {
            final String generatorClassName = generator.getClass().getCanonicalName();
            INSTANCE.messageGeneratorClass = newInformational(generatorClassName, "GeneratorClassName");
         }
      }
   }

   /**
    * Reports the sender class name.
    *
    * @param senderClassName
    *       The sender class name.
    */
   public static void reportSenderName(final String senderClassName) {
      if (INSTANCE != null) {
         if (INSTANCE.messageSenderClass == null) {
            INSTANCE.messageSenderClass = newInformational(senderClassName, "SenderClassName");
         }
      }
   }

   /**
    * Reports the receiver class name.
    *
    * @param receiver
    *       The receiver instance.
    */
   public static void reportReceiverName(final Receiver receiver) {
      if (INSTANCE != null) {
         if (INSTANCE.receiverClass == null) {
            final String receiverClassName = receiver.getClass().getCanonicalName();
            INSTANCE.receiverClass = newInformational(receiverClassName, "ReceiverClassName");
         }
      }
   }

   /**
    * Reports the correlator class name.
    *
    * @param correlator
    *       The correlator instance.
    */
   public static void reportCorrelatorName(final Correlator correlator) {
      if (INSTANCE != null) {
         if (INSTANCE.correlatorClass == null) {
            final String correlatorClassName = correlator.getClass().getCanonicalName();
            INSTANCE.correlatorClass = newInformational(correlatorClassName, "CorrelatorClassName");
         }
      }
   }

   /**
    * Reports the sequence class name.
    *
    * @param sequenceId
    *       The sequence id.
    * @param sequence
    *       The sequence instance.
    */
   public static void reportSequenceName(final String sequenceId, final Sequence sequence) {
      if (INSTANCE != null) {
         final String sequenceClassName = sequence.getClass().getCanonicalName();
         INSTANCE.sequenceClasses.computeIfAbsent(sequenceId, k -> newInformational(sequenceClassName, "Sequences", sequenceId));
      }
   }

   /**
    * Reports validator class name.
    *
    * @param validatorId
    *       The validator id.
    * @param validator
    *       The validator instance.
    */
   public static void reportValidatorName(final String validatorId, final MessageValidator validator) {
      if (INSTANCE != null) {
         final String validatorClassName = validator.getClass().getCanonicalName();
         INSTANCE.validatorClasses.computeIfAbsent(validatorId, k -> newInformational(validatorClassName, "Validation", validatorId));
         INSTANCE.validatorIds.put(validator, validatorId);
      }
   }

   /**
    * Reports a new instance of a sender task was created.
    */
   public static void reportNewSenderTask() {
      if (INSTANCE != null) {
         INSTANCE.generatedSenderTasks.increment();
      }
   }

   /**
    * Reports sending of a message.
    */
   public static void reportSentMessage() {
      if (INSTANCE != null) {
         INSTANCE.sentMessages.increment();
      }
   }

   /**
    * Reports a received message.
    */
   public static void reportCorrelatedMessage() {
      if (INSTANCE != null) {
         INSTANCE.correlatedMessages.increment();
      }
   }

   /**
    * Reports a sequences snapshot has been taken.
    */
   public static void reportSequenceSnapshot() {
      if (INSTANCE != null) {
         INSTANCE.sequenceSnapshots.increment();
      }
   }

   /**
    * Reports a measurement unit was sent to report for accumulation.
    *
    * @param reporter
    *       The reporter instance.
    */
   public static void reportReporterUsage(final Reporter reporter) {
      if (INSTANCE != null) {
         final String reporterClassName = reporter.getClass().getCanonicalName();
         INSTANCE.resultsReported.computeIfAbsent(reporterClassName, key -> newCounter("Reporting", reporterClassName)).increment();
      }
   }

   /**
    * Reports the destination's parent reporter for later matching.
    *
    * @param destination
    *       The destination instance.
    * @param reporter
    *       The reporter instance.
    */
   public static void reportParentReporter(final Destination destination, final Reporter reporter) {
      if (INSTANCE != null) {
         INSTANCE.parentReporters.putIfAbsent(destination, reporter.getClass().getCanonicalName());
      }
   }

   /**
    * Reports a result written to a destination.
    *
    * @param destination
    *       The destination instance.
    */
   public static void reportResultWritten(final Destination destination) {
      if (INSTANCE != null) {
         final String reporterClassName = INSTANCE.parentReporters.getOrDefault(destination, "UNKNOWN");
         final String destinationClassName = destination.getClass().getCanonicalName();
         INSTANCE.resultsWritten.computeIfAbsent(reporterClassName + "." + destinationClassName, k -> newCounter("Reporting", reporterClassName, destinationClassName)).increment();
      }
   }

   /**
    * Reports a validation result.
    *
    * @param validator
    *       The validator instance.
    * @param valid
    *       The result of the validation.
    */
   public static void reportValidationResult(final MessageValidator validator, final boolean valid) {
      if (INSTANCE != null) {
         final String id = INSTANCE.validatorIds.get(validator);
         if (id != null) {
            INSTANCE.validationResults.computeIfAbsent(id, key -> newCounter("Validation", id)).increment();
            final Counter invalidCounter = INSTANCE.validationResults.computeIfAbsent(id + ".failed", key -> newCounter("Validation", id, "failed"));
            final Counter validCounter = INSTANCE.validationResults.computeIfAbsent(id + ".passed", key -> newCounter("Validation", id, "passed"));
            if (valid) {
               validCounter.increment();
            } else {
               invalidCounter.increment();
            }
         }
      }
   }

   /**
    * Copies the Byteman rules for the debug agent to a temporary location.
    *
    * @return The temporary location of the Byteman rules.
    * @throws PerfCakeException
    *       When it was not possible to copy the file.
    */
   private static String saveTmpBytemanRules() throws PerfCakeException {
      try {
         final File tmpFile = File.createTempFile("perfCakeAgent", ".btm");
         tmpFile.deleteOnExit();
         Utils.copyTemplateFromResource("/debug/perfCakeAgent.btm", Paths.get(tmpFile.toURI()), null);

         return tmpFile.getAbsolutePath();
      } catch (IOException ioe) {
         throw new PerfCakeException("Unable to create debug agent rules file: ", ioe);
      }
   }

   /**
    * Gets the current process PID on Linux from /proc.
    *
    * @return The PID of the current process.
    */
   private static int getLinuxPid() {
      File file = new File("/proc/self/stat");
      if (!file.exists() || !file.canRead()) {
         return 0;
      }

      FileInputStream fis = null;
      int pid = 0;

      try {
         fis = new FileInputStream(file);
         final byte[] bytes = new byte[10];
         final StringBuilder builder = new StringBuilder();
         final int read = fis.read(bytes);
         for (int i = 0; i < read; i++) {
            char c = (char) bytes[i];
            if (Character.isDigit(c)) {
               builder.append(c);
            } else {
               break;
            }
         }
         pid = Integer.parseInt(builder.toString());
      } catch (IOException e) {
         // ignore
      } finally {
         if (fis != null) {
            try {
               fis.close();
            } catch (IOException e1) {
               // ignore
            }
         }
      }
      return pid;
   }

   /**
    * Gets the current process PID. Based on org.jboss.byteman.contrib.bmunit.BMUnitConfigState by Andrew Dinn.
    *
    * @return The PID of the current process.
    */
   private static String getPid() {
      String id = null;

      // if we can get a proper pid on Linux  we use it
      int pid = getLinuxPid();

      if (pid > 0) {
         id = Integer.toString(pid);
      } else {
         // alternative strategy which will work everywhere
         // set a unique system property and then check each available VM until we find it
         String prop = "org.jboss.byteman.contrib.bmunit.agent.unique";
         String unique = Long.toHexString(System.currentTimeMillis());
         System.setProperty(prop, unique);

         final List<VirtualMachineDescriptor> vmds = VirtualMachine.list();
         for (VirtualMachineDescriptor vmd : vmds) {
            String value = getProperty(vmd.id(), prop);
            if (unique.equals(value)) {
               id = vmd.id();
               break;
            }
         }
         // last ditch effort to obtain pid on Windows where the availableVMs list may be empty
         if (id == null) {
            // last ditch effort to obtain pid on Windows where the availableVMs list may be empty
            String processName = ManagementFactory.getRuntimeMXBean().getName();
            if (processName != null && processName.contains("@")) {
               id = processName.substring(0, processName.indexOf("@"));
               // check we actually have an integer
               try {
                  Integer.parseInt(id);
                  // well, it's a number so now check it identifies the current VM
                  String value = getProperty(id, prop);
                  if (!unique.equals(value)) {
                     // nope, not the right process
                     id = null;
                  }
               } catch (NumberFormatException e) {
                  // nope, not a number
                  id = null;
               }
            }
         }
      }

      return id;
   }

   /**
    * Gets system property from the JVM with the given PID.
    *
    * @param id
    *       The JVM PID.
    * @param property
    *       The property name to obtain.
    * @return The property value or null in case of any error or when the property was not available.
    */
   private static String getProperty(String id, String property) {
      VirtualMachine vm = null;
      try {
         vm = VirtualMachine.attach(id);
         return (String) vm.getSystemProperties().get(property);
      } catch (NoClassDefFoundError | Exception e) {
         return null;
      } finally {
         if (vm != null) {
            try {
               vm.detach();
            } catch (IOException e) {
               // ignore;
            }
         }
      }
   }

   /**
    * Creates and registers new JMX counter.
    *
    * @param name
    *       Name of the counter.
    * @param categories
    *       Other categories under which the counter should be placed in the JMX tree.
    * @return The newly created counter.
    */
   private static Counter newCounter(final String name, final String... categories) {
      final Counter counter = new BasicCounter(getMonitorConfig(name, categories));
      DefaultMonitorRegistry.getInstance().register(counter);

      return counter;
   }

   /**
    * Creates and registers new JMX information record.
    *
    * @param name
    *       Name of the record.
    * @param categories
    *       Other categories under which the record should be placed in the JMX tree.
    * @return The newly created information record.
    */
   private static Informational newInformational(final String value, final String name, final String... categories) {
      final BasicInformational informational = new BasicInformational(getMonitorConfig(name, categories));
      informational.setValue(value);
      DefaultMonitorRegistry.getInstance().register(informational);

      return informational;
   }

   /**
    * Creates a new JMX monitor configuration for counters and information records.
    *
    * @param name
    *       Name of the monitor.
    * @param categories
    *       Categories in which the monitor should be placed in the JMX tree.
    * @return The monitor configuration.
    */
   private static MonitorConfig getMonitorConfig(final String name, final String... categories) {
      final MonitorConfig.Builder config = MonitorConfig.builder(name).withTag("class", AGENT_NAME);
      if (categories.length > 0) {
         for (int i = 0; i < categories.length; i++) {
            config.withTag("category" + (i + 1), categories[i]);
         }
      }
      return config.build();
   }
}
