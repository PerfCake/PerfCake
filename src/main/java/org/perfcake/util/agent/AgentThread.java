package org.perfcake.util.agent;

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

import org.perfcake.util.Utils;
import org.perfcake.util.agent.PerfCakeAgent.Memory;

public class AgentThread implements Runnable {

   private String agentArgs;

   public AgentThread(String agentArgs) {
      super();
      this.agentArgs = agentArgs;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Runnable#run()
    */
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
         if ("".equals(agentArgs) && agentArgs != null) {
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
         while (true) {
            log("Listening at " + ssocket.getInetAddress().getHostAddress() + " on port " + ssocket.getLocalPort());
            socket = ssocket.accept();
            log("Client connected from " + socket.getInetAddress().getHostAddress());
            is = socket.getInputStream();
            String input = null;
            BufferedReader br = new BufferedReader(new InputStreamReader(is, Utils.getDefaultEncoding()));
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Utils.getDefaultEncoding()), true);
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

   private static void log(String msg) {
      System.out.println(PerfCakeAgent.class.getSimpleName() + " > " + msg);
   }

   private static void err(String msg) {
      System.err.println(PerfCakeAgent.class.getSimpleName() + " > ERROR: " + msg);
   }
}
