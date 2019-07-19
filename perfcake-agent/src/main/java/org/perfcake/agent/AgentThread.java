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
package org.perfcake.agent;

import com.sun.management.HotSpotDiagnosticMXBean;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * Implements the PerfCake agent.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class AgentThread implements Runnable {
   /**
    * Agent's arguments.
    */
   private final String agentArgs;

   /**
    * Creates a new agent thread.
    *
    * @param agentArgs
    *       Arguments passed to the agent.
    */
   public AgentThread(final String agentArgs) {
      super();
      this.agentArgs = agentArgs;
   }

   @Override
   public void run() {
      InetAddress host;
      int port = PerfCakeAgent.DEFAULT_PORT;
      ServerSocket serverSocket = null;
      Socket socket;
      InputStream is = null;

      try {
         // parse agent properties
         final Properties props = new Properties();
         if (!"".equals(agentArgs) && agentArgs != null) {
            final String[] args = agentArgs.split(",");
            for (final String arg : args) {
               final String[] keyValuePair = arg.split("=");
               if (keyValuePair.length == 2) {
                  props.put(keyValuePair[0], keyValuePair[1].trim());
               } else {
                  err("Invalid agent argument \"" + arg + "\" - ignoring");
               }
            }
         }

         if (props.get("port") != null) {
            port = Integer.parseInt((String) props.get("port"));
         }

         if (props.get("hostname") != null) {
            host = InetAddress.getByName(props.getProperty("hostname"));
         } else {
            host = InetAddress.getLocalHost();
         }

         serverSocket = new ServerSocket(port, 1, host);
         while (!Thread.currentThread().isInterrupted()) {
            log("Listening at " + serverSocket.getInetAddress().getHostAddress() + " on port " + serverSocket.getLocalPort());
            socket = serverSocket.accept();
            log("Client connected from " + socket.getInetAddress().getHostAddress());
            is = socket.getInputStream();
            String command;
            final BufferedReader br = new BufferedReader(new InputStreamReader(is, PerfCakeAgent.DEFAULT_ENCODING));
            final PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), PerfCakeAgent.DEFAULT_ENCODING), true);
            while ((command = br.readLine()) != null) {
               String response = "Unrecognized command!";
               final Runtime rt = Runtime.getRuntime();
               try {
                  if (AgentCommand.FREE.name().equals(command)) {
                     response = String.valueOf(rt.freeMemory());
                  } else if (AgentCommand.MAX.name().equals(command)) {
                     response = String.valueOf(rt.maxMemory());
                  } else if (AgentCommand.TOTAL.name().equals(command)) {
                     response = String.valueOf(rt.totalMemory());
                  } else if (AgentCommand.USED.name().equals(command)) {
                     response = String.valueOf(rt.totalMemory() - rt.freeMemory());
                  } else if (command.startsWith(AgentCommand.DUMP.name())) {
                     final int colonIndex = command.indexOf(":");
                     String dumpFileNamePrefix = "dump-" + System.currentTimeMillis();
                     String dumpFileName = dumpFileNamePrefix + ".hprof";;
                     if (colonIndex >= 0) {
                        dumpFileName = command.substring(colonIndex + 1);
                        if (!dumpFileName.endsWith(".hprof")) {
                           dumpFileName = dumpFileName + ".hprof";
                           log("WARNING: Heap dump file must end with .hprof extension. Renaming to '" + dumpFileName + "'");
                        }
                        dumpFileNamePrefix = dumpFileName.substring(0, dumpFileName.lastIndexOf(".hprof"));
                     }

                     int dumpNameIndex = 0;
                     File dumpFile = new File(dumpFileName);
                     while (dumpFile.exists()) {
                        log("WARNING: File " + dumpFileName + " already exists. Trying another file name.");
                        dumpFile = new File(dumpFileNamePrefix + "." + (dumpNameIndex++) + ".hprof");
                     }
                     log("Saving a heap dump to " + dumpFile.getAbsolutePath());
                     try {
                        ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).dumpHeap(dumpFile.getAbsolutePath(), true);
                        log("Heap dump saved to " + dumpFile.getAbsolutePath());
                        response = "0";
                     } catch (final IOException ioe) {
                        log("Error saving heap dump!");
                        ioe.printStackTrace();
                        response = "-1";
                     }
                  } else if (AgentCommand.GC.name().equals(command)) {
                     System.gc();
                     response = "0";
                  }
               } catch (final IllegalArgumentException iae) {
                  err(iae.getLocalizedMessage());
                  response = "-1";
               }
               pw.println(response);
            }
            log("Client disconnected");
         }

      } catch (final Throwable e) {
         e.printStackTrace();
      } finally {
         if (is != null) {
            try {
               is.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }

         if (serverSocket != null) {
            try {
               serverSocket.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   /**
    * Logs a message to standard output.
    *
    * @param message
    *       Message to be logged.
    */

   private static void log(final String message) {
      System.out.println(PerfCakeAgent.class.getSimpleName() + " > " + message);
   }

   /**
    * Logs a message to error output.
    *
    * @param message
    *       Message to be logged.
    */
   private static void err(final String message) {
      System.err.println(PerfCakeAgent.class.getSimpleName() + " > ERROR: " + message);
   }
}
