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

package org.perfcake.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerfCakeAgent {

   public static void premain(String agentArgs, Instrumentation inst) {
      ExecutorService es = Executors.newSingleThreadExecutor();
      es.submit(new AgentThread(agentArgs, inst));
   }

   public static class AgentThread implements Runnable {
      private String agentArgs;

      private Instrumentation inst;

      public AgentThread(String agentArgs, Instrumentation inst) {
         super();
         this.agentArgs = agentArgs;
         this.inst = inst;
      }

      @Override
      public void run() {
         InetAddress host = null;
         int port = 8849; // default
         ServerSocket ssocket = null;
         Socket socket = null;
         InputStream is = null;

         try {
            // parse agent properties
            Properties props = new Properties();
            if (agentArgs != "" && agentArgs != null) {
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

            // ThreadMXBean tBean = ManagementFactory.getThreadMXBean();

            host = InetAddress.getLocalHost();
            ssocket = new ServerSocket(port, 1, host);
            while (true) {
               log("Listening at " + ssocket.getInetAddress().getHostAddress() + " on port " + ssocket.getLocalPort());
               socket = ssocket.accept();
               log("Client connected from " + socket.getInetAddress().getHostAddress());
               is = socket.getInputStream();
               String input = null;
               BufferedReader br = new BufferedReader(new InputStreamReader(is));
               PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
               while ((input = br.readLine()) != null) {
                  String response = "Unrecognized command!";
                  Runtime rt = Runtime.getRuntime();
                  switch (input) {
                     case "getFreeMemory":
                        response = String.valueOf(rt.freeMemory());
                        break;
                     case "getMaxMemory":
                        response = String.valueOf(rt.maxMemory());
                        break;
                     case "getTotalMemory":
                        response = String.valueOf(rt.totalMemory());
                        break;
                     case "getUsedMemory":
                        response = String.valueOf(rt.totalMemory() - rt.freeMemory());
                        break;
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
   }

   private static void log(String msg) {
      System.out.println(PerfCakeAgent.class.getSimpleName() + " > " + msg);
   }

   private static void err(String msg) {
      System.err.println(PerfCakeAgent.class.getSimpleName() + " > ERROR: " + msg);
   }
}
