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

import org.perfcake.util.agent.PerfCakeAgent.Memory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * The actuall implementation of the PerfCake agent.
 *
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class AgentThread implements Runnable {
   /**
    * Agent's arguments.
    */
   private final String agentArgs;

   /**
    * @param agentArgs
    */
   public AgentThread(String agentArgs) {
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
         Properties props = new Properties();
         if (!"".equals(agentArgs) && agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
               String[] keyValuePair = arg.split("=");
               if (keyValuePair.length == 2) {
                  props.put(keyValuePair[0], keyValuePair[1]);
               } else {
                  err("Invalid agent argument \"" + arg + "\" - ignoring");
               }
            }
         }

         if (props.get("port") != null) {
            port = Integer.valueOf((String) props.get("port"));
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
            String input = null;
            BufferedReader br = new BufferedReader(new InputStreamReader(is, PerfCakeAgent.DEFAULT_ENCODING));
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), PerfCakeAgent.DEFAULT_ENCODING), true);
            while ((input = br.readLine()) != null) {
               String response = "Unrecognized command!";
               Runtime rt = Runtime.getRuntime();
               try {
                  switch (Memory.valueOf(input.toUpperCase())) {
                     case FREE:
                        response = String.valueOf(rt.freeMemory());
                        break;
                     case MAX:
                        response = String.valueOf(rt.maxMemory());
                        break;
                     case TOTAL:
                        response = String.valueOf(rt.totalMemory());
                        break;
                     case USED:
                        response = String.valueOf(rt.totalMemory() - rt.freeMemory());
                        break;
                  }
               } catch (IllegalArgumentException iae) {
                  err(iae.getLocalizedMessage());
               }
               pw.println(response);
            }
            log("Client disconnected");
         }

      } catch (Throwable e) {
         e.printStackTrace();
      } finally {
         if (is != null) {
            try {
               is.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   /**
    * Logs a message.
    *
    * @param msg
    *       Message to be logged.
    */
   private static void log(String msg) {
      System.out.println(PerfCakeAgent.class.getSimpleName() + " > " + msg);
   }

   /**
    * Logs an error message.
    *
    * @param msg
    *       Message to be logged.
    */
   private static void err(String msg) {
      System.err.println(PerfCakeAgent.class.getSimpleName() + " > ERROR: " + msg);
   }
}
