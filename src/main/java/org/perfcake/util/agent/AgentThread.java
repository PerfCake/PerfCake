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
package org.perfcake.util.agent;

import sun.management.ManagementFactoryHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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

   private static final List<String> memoryCommands;

   static {
      memoryCommands = new ArrayList<>();
      for (final PerfCakeAgent.Command commandType : PerfCakeAgent.Command.values()) {
         memoryCommands.add(commandType.name().toUpperCase());
      }
   }

   /**
    * Creates a new agent thread.
    *
    * @param agentArgs Arguments passed to the agent.
    */
   public AgentThread(final String agentArgs) {
      super();
      this.agentArgs = agentArgs;
   }

   @Override
   public void run() {
      InetAddress host = null;
      int port = PerfCakeAgent.DEFAULT_PORT;
      ServerSocket ssocket = null;
      Socket socket = null;
      InputStream is = null;

      try {
         // parse agent properties
         final Properties props = new Properties();
         if (!"".equals(agentArgs) && agentArgs != null) {
            final String[] args = agentArgs.split(",");
            for (final String arg : args) {
               final String[] keyValuePair = arg.split("=");
               if (keyValuePair.length == 2) {
                  props.put(keyValuePair[0], keyValuePair[1]);
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

         ssocket = new ServerSocket(port, 1, host);
         while (!Thread.currentThread().isInterrupted()) {
            log("Listening at " + ssocket.getInetAddress().getHostAddress() + " on port " + ssocket.getLocalPort());
            socket = ssocket.accept();
            log("Client connected from " + socket.getInetAddress().getHostAddress());
            is = socket.getInputStream();
            String command;
            final BufferedReader br = new BufferedReader(new InputStreamReader(is, PerfCakeAgent.DEFAULT_ENCODING));
            final PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), PerfCakeAgent.DEFAULT_ENCODING), true);
            while ((command = br.readLine()) != null) {
               String response = "Unrecognized command!";
               final Runtime rt = Runtime.getRuntime();
               try {
                  if (PerfCakeAgent.Command.FREE.name().equals(command)) {
                     response = String.valueOf(rt.freeMemory());
                  } else if (PerfCakeAgent.Command.MAX.name().equals(command)) {
                     response = String.valueOf(rt.maxMemory());
                  } else if (PerfCakeAgent.Command.TOTAL.name().equals(command)) {
                     response = String.valueOf(rt.totalMemory());
                  } else if (PerfCakeAgent.Command.USED.name().equals(command)) {
                     response = String.valueOf(rt.totalMemory() - rt.freeMemory());
                  } else if (command.startsWith(PerfCakeAgent.Command.DUMP.name())) {
                     final int colonIndex = command.indexOf(":");
                     String dumpFileName;
                     if (colonIndex >= 0) {
                        dumpFileName = command.substring(colonIndex + 1);
                     } else {
                        dumpFileName = "dump-" + System.currentTimeMillis() + ".bin";
                     }
                     int dumpNameIndex = 0;
                     File dumpFile = new File(dumpFileName);
                     while (dumpFile.exists()) {
                        log("WARNING: File " + dumpFileName + " already exists. Trying another file name.");
                        dumpFile = new File(dumpFileName + "." + (dumpNameIndex++));
                     }
                     log("Saving a heap dump to " + dumpFile.getAbsolutePath());
                     try {
                        ManagementFactoryHelper.getDiagnosticMXBean().dumpHeap(dumpFile.getAbsolutePath(), true);
                        log("Heap dump saved to " + dumpFile.getAbsolutePath());
                        response = "0";
                     } catch (final IOException ioe) {
                        log("Error saving heap dump!");
                        ioe.printStackTrace();
                        response = "-1";
                     }
                  } else if (PerfCakeAgent.Command.GC.name().equals(command)) {
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
