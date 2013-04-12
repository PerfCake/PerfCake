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

package org.perfcake.message.sender;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;

/**
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class HTTPSender extends AbstractSender {
   private URL url;

   private String method = "POST";

   private static final Logger log = Logger.getLogger(HTTPSender.class);

   private static final int DEFAULT_EXPECTED_CODE = 200;

   private List<Integer> expectedCodes = Arrays.asList(DEFAULT_EXPECTED_CODE);

   private HttpURLConnection rc;

   private String reqStr;

   private int len;

   @Override
   public void setProperty(String prop, String value) {
      if ("method".equals(prop)) {
         method = value;
      } else if ("address".equals(prop)) {
         this.address = value;
      } else if ("expectedResponseCode".equals(prop)) {
         this.expectedCodes = setExpectedCodes(value.split(","));
      } else {
         super.setProperty(prop, value);
      }
   }

   @Override
   public void init() throws Exception {
      url = new URL(address);
   }

   @Override
   public void close() {
   }

   private LinkedList<Integer> setExpectedCodes(String[] codes) {
      LinkedList<Integer> numCodes = new LinkedList<Integer>();
      for (int i = 0; i < codes.length; i++) {
         numCodes.add(Integer.parseInt(codes[i].trim()));
      }
      return numCodes;
   }

   private boolean checkResponseCode(int code) {
      for (int i : expectedCodes) {
         if (i == code) {
            return true;
         }
      }
      return false;
   }

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      rc = (HttpURLConnection) url.openConnection();
      rc.setRequestMethod(method);
      rc.setDoOutput(true);
      rc.setDoInput(true);
      rc.setRequestProperty("Content-Type", "text/xml; charset=utf-8");

      reqStr = message.getPayload().toString();
      len = reqStr.length();
      rc.setRequestProperty("Content-Length", Integer.toString(len));

      Set<String> propertyNameSet = message.getProperties().stringPropertyNames();
      for (String property : propertyNameSet) {
         rc.setRequestProperty(property, message.getProperty(property));
      }
      // set additional properties
      if (log.isDebugEnabled()) {
         log.debug("Setting HTTP headers");
      }
      if (properties != null) {
         propertyNameSet = properties.keySet();
         for (String property : propertyNameSet) {
            String pValue = (properties.get(property));
            rc.setRequestProperty(property, pValue);
            if (log.isDebugEnabled()) {
               log.debug(property + ": " + pValue);
            }
         }
      }

      if (message.getHeaders().size() > 0) {
         Set<String> headerNameSet = message.getHeaders().stringPropertyNames();
         for (String header : headerNameSet) {
            String hValue = message.getHeader(header);
            if (log.isDebugEnabled()) {
               log.debug(header + ": " + hValue);
            }
            rc.setRequestProperty(header, hValue);
         }
      }
   }

   @Override
   public Serializable doSend(Message message) throws Exception {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      int respCode = -1;

      rc.connect();

      OutputStreamWriter out = new OutputStreamWriter(rc.getOutputStream());
      out.write(reqStr, 0, len);
      out.flush();

      rc.getOutputStream().close();

      respCode = rc.getResponseCode();
      if (!checkResponseCode(respCode)) {
         String errorMess = "The server returned an unexpected HTTP response code: " + respCode + " " + "\"" + rc.getResponseMessage() + "\". Expected HTTP codes are ";
         for (int code : expectedCodes) {
            errorMess += Integer.toString(code) + ", ";
         }
         throw new PerfCakeException(errorMess.substring(0, errorMess.length() - 2) + ".");
      }
      InputStream rcis = null;
      if (respCode < 400) {
         rcis = rc.getInputStream();
      } else {
         rcis = rc.getErrorStream();
      }
      char[] cbuf = new char[10 * 1024];
      InputStreamReader read = new InputStreamReader(rcis);
      // note that Content-Length is available at this point
      StringBuilder sb = new StringBuilder();
      int ch = read.read(cbuf);
      while (ch != -1) {
         sb.append(cbuf, 0, ch);
         ch = read.read(cbuf);
      }
      read.close();
      rcis.close();
      String payload = sb.toString();

      return payload;
   }

   @Override
   public void postSend(Message message) throws Exception {
      rc.disconnect();
   }
}
