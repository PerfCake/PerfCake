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

import static com.netflix.servo.annotations.DataSourceType.COUNTER;
import static com.netflix.servo.annotations.DataSourceType.INFORMATIONAL;

import org.perfcake.PerfCakeConst;
import org.perfcake.util.Utils;

import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.byteman.agent.install.Install;
import org.jboss.byteman.agent.install.VMInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    * Class name of the generator used.
    */
   @Monitor(name = "GeneratorClass", type = INFORMATIONAL)
   private AtomicReference<String> messageGeneratorClass = new AtomicReference<String>("");

   /**
    * How many sender tasks were created so far.
    */
   @Monitor(name = "GeneratedSenderTasks", type = COUNTER)
   private AtomicInteger generatedSenderTasks = new AtomicInteger(0);

   /**
    * Initializes and installs the agent.
    */
   private PerfCakeDebug() { // we do not want external creation
      INSTANCE = this;

      final String pid = getPid();
      try {
         Install.install(pid, true, false, "", 0, new String[] { "org.jboss.byteman.compileToBytecode=true" });
      } catch (Exception e) {
         log.error("Unable to install debug agent, debugging information will not be available: ", e);
         return;
      }

      Monitors.registerObject(Utils.getProperty(PerfCakeConst.DEBUG_AGENT_NAME_PROPERTY, PerfCakeConst.DEBUG_AGENT_DEFAULT_NAME), this);
   }

   /**
    * Gets the agent instance.
    * @return The agent instance. Can be null when the agent was not initialized.
    */
   public static PerfCakeDebug getInstance() {
      return INSTANCE;
   }

   /**
    * Initializes the debug agent.
    */
   public static synchronized void initialize() {
      new PerfCakeDebug();
   }

   /**
    * Reports the message generator class name.
    * @param generatorClassName The message generator class name.
    */
   public static void reportGeneratorName(final String generatorClassName) {
      if (INSTANCE != null) {
         INSTANCE.messageGeneratorClass.set(generatorClassName);
      }
   }

   /**
    * Reports a new instance of a sender task was created.
    */
   public static void reportNewSenderTask() {
      if (INSTANCE != null) {
         INSTANCE.generatedSenderTasks.incrementAndGet();
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
         byte[] bytes = new byte[10];
         StringBuilder builder = new StringBuilder();
         fis.read(bytes);
         for (int i = 0; i < 10; i++) {
            char c = (char) bytes[i];
            if (Character.isDigit(c)) {
               builder.append(c);
            } else {
               break;
            }
         }
         pid = Integer.valueOf(builder.toString());
      } catch (Exception e) {
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
         VMInfo[] vmInfo = Install.availableVMs();
         for (int i = 0; i < vmInfo.length; i++) {
            String nextId = vmInfo[i].getId();
            String value = Install.getSystemProperty(nextId, prop);
            if (unique.equals(value)) {
               id = nextId;
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
                  String value = Install.getSystemProperty(id, prop);
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
}
