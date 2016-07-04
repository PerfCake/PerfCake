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
/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.perfcake.agent;

import com.sun.tools.attach.VirtualMachine;

/**
 * Java implementation of a PerfCake agent that can be used to monitor tested system's JVM.
 *
 * <p>Starts a thread with a socket server listening on a specified host and port and is used
 * by PerfCake's reporting (class org.perfcake.reporting.reporters.MemoryUsageReporter) to gather memory usage data
 * and to control garbage collection and heap dump activities needed by the reporter.</p>
 *
 * <p>To attach the agent to the tested system's JVM, append the following JVM argument to
 * the executing java command or use JAVA_OPTS environment variable:</p>
 *
 * <p><code>"... -javaagent:&lt;perfcake_agent_jar&gt;=hostname=&lt;hostname&gt;,port=&lt;port&gt;</code></p>
 * <p>where <code>perfcake_jar_path</code> is a path to PerfCake JAR archive, <code>hostname</code> and <code>port</code>
 * specifies agent's socket.</p>
 *
 * <p>It is also possible to attach to a running JVM:</p>
 *
 * <p><code>java -cp ${JAVA_HOME}/lib/tools.jar:&lt;perfcake_agent_jar&gt; org.perfcake.agent.PerfCakeAgent &lt;PID&gt; hostname=&lt;hostname&gt;,port=&lt;port&gt;</code></p>
 *
 * <table summary="PerfCakeAgent Commands">
 * <thead>
 * <tr>
 * <th>Command</th>
 * <th>Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>"{@link AgentCommand#FREE FREE}"</td>
 * <td>Returns the amount of free memory in the Java Virtual Machine.</td>
 * </tr>
 * <tr>
 * <td>"{@link AgentCommand#USED USED}"</td>
 * <td>Returns the amount of used memory in the Java Virtual Machine.</td>
 * </tr>
 * <tr>
 * <td>"{@link AgentCommand#TOTAL TOTAL}"</td>
 * <td>Returns the amount of total memory in the Java Virtual Machine.</td>
 * </tr>
 * <tr>
 * <td>"{@link AgentCommand#MAX MAX}"</td>
 * <td>Returns the maximum amount of memory that the Java Virtual Machine will attempt to use.</td>
 * </tr>
 * <tr>
 * <td>"{@link AgentCommand#DUMP DUMP}(:&lt;dump-file&gt;)"</td>
 * <td>Initiates a heap dump into <code>dump-file</code>. <code>dump-file</code> is optional - if not provided,
 * the file name would be generated as <code>"dump-" + {@link java.lang.System#currentTimeMillis()} + ".bin"</code>.</td>
 * </tr>
 * <tr>
 * <td>"{@link AgentCommand#GC GC}"</td>
 * <td>Calls {@link System#gc()}.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @see AgentCommand
 */
public class PerfCakeAgent {

   /**
    * Default encoding of the input and output streams.
    */
   public static final String DEFAULT_ENCODING = "UTF-8";

   /**
    * Default agent port.
    */
   public static final int DEFAULT_PORT = 8850;

   /**
    * There should be no instance of a utility class.
    */
   private PerfCakeAgent() {
   }

   /**
    * {@link PerfCakeAgent}'s pre-main method.
    *
    * @param agentArgs
    *       Agent arguments.
    * @see java.lang.instrument
    */
   public static void premain(final String agentArgs) {
      final Thread agentThread = new Thread(new AgentThread(agentArgs));
      agentThread.setDaemon(true);
      agentThread.start();
   }

   public static void agentmain(final String agentArgs, final java.lang.instrument.Instrumentation inst) {
      premain(agentArgs);
   }

   private static void printUsage() {
      System.out.println("PerfCake Agent -- monitor memory usage of the hosting JVM");
      System.out.println("Usage: java -jar perfcake-agent.jar <PID> <agent args>");
      System.out.println("<PID>          JVM process ID to attach to");
      System.out.println("<agent args>   voluntary, available args are hostname=<hostname>,port=<port>");
      System.out.println("                 These arguments specify where to bind the agent.");
   }

   /**
    * Starts the agent from command line allowing it to bind to a running process.
    *
    * @param args Command line arguments.
    */
   public static void main(final String... args) {
      if (args.length < 1) {
         printUsage();
         System.exit(1);
      }

      try {
         final int pid = Integer.parseInt(args[0]);
      } catch (NumberFormatException nfe) {
         printUsage();
         System.out.println("First argument is not a number.");
         System.exit(2);
      }

      final StringBuilder agentArgs = new StringBuilder();
      for (int i = 1; i < args.length; i++) {
         agentArgs.append(args[i]);
         agentArgs.append(" ");
      }

      try {
         final VirtualMachine vm = VirtualMachine.attach(args[0]);
         final String path = PerfCakeAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
         vm.loadAgent(path, agentArgs.toString());
         vm.detach();
      } catch (Exception e) {
         System.err.println("Unable to attach to the PID " + args[0]);
         e.printStackTrace(System.err);
         System.exit(3);
      }

      System.out.println("PerfCake Agent successfully attached.");
   }
}
