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
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.util.Utils;

/**
 * The sender that is able to send the messages via HTTP protocol.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 * 
 */
public class HTTPSender extends AbstractSender {

   /**
    * Default expected response code.
    */
   protected static final int DEFAULT_EXPECTED_CODE = 200;

   /**
    * The sender's logger.
    */
   private static final Logger log = Logger.getLogger(HTTPSender.class);

   /**
    * The URL where the HTTP request is send.
    */
   private URL url;

   /**
    * The HTTP method that will be used.
    */
   private Method method = Method.POST;

   /**
    * Enumeration on available HTTP methods.
    */
   public static enum Method {
      GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE
   }

   /**
    * The list of response codes that are expected to be returned by HTTP response.
    */
   private List<Integer> expectedResponseCodeList = Arrays.asList(DEFAULT_EXPECTED_CODE);

   /**
    * The property for expected response codes.
    */
   private String expectedResponseCodes = String.valueOf(DEFAULT_EXPECTED_CODE);

   /**
    * The HTTP request connection.
    */
   protected HttpURLConnection requestConnection;

   /**
    * The request payload.
    */
   private String payload;

   /**
    * The request payload lenght.
    */
   private int payloadLenght;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#init()
    */
   @Override
   public void init() throws Exception {
      url = new URL(target);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#close()
    */
   @Override
   public void close() {
      // nop
   }

   /**
    * Sets the value of expectedResponseCodes property.
    * 
    * @param expectedResponseCodes
    *           The expectedResponseCodes property to set.
    */
   public void setExpectedResponseCodes(final String expectedResponseCodes) {
      this.expectedResponseCodes = expectedResponseCodes;
      setExpectedResponseCodesList(expectedResponseCodes.split(","));
   }

   /**
    * Used to read the list of expected response codes.
    * 
    * @return The list of expected response codes.
    */
   public List<Integer> getExpectedResponseCodeList() {
      return expectedResponseCodeList;
   }

   /**
    * Used to read the value of expectedResponseCodes property.
    * 
    * @return The expectedResponseCodes.
    */
   public String getExpectedResponseCodes() {
      return expectedResponseCodes;
   }

   /**
    * Sets a list of expected response codes.
    * 
    * @param codes
    *           The array of codes.
    */
   protected void setExpectedResponseCodesList(final String[] codes) {
      LinkedList<Integer> numCodes = new LinkedList<Integer>();
      for (String code : codes) {
         numCodes.add(Integer.parseInt(code.trim()));
      }
      expectedResponseCodeList = numCodes;
   }

   /**
    * Checks if the code is expected.
    * 
    * @param code
    *           Checked response code.
    * @return
    *         true/false according to if the code is expected or not.
    */
   private boolean checkResponseCode(final int code) {
      for (int i : expectedResponseCodeList) {
         if (i == code) {
            return true;
         }
      }
      return false;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#preSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public void preSend(final Message message, final Map<String, String> properties) throws Exception {
      super.preSend(message, properties);

      if (message.getPayload() != null) {
         payload = message.getPayload().toString();
         payloadLenght = payload.length();
      } else {
         payloadLenght = 0;
      }

      requestConnection = (HttpURLConnection) url.openConnection();
      requestConnection.setRequestMethod(method.name());
      requestConnection.setDoInput(true);
      if (method == Method.POST || method == Method.PUT) {
         requestConnection.setDoOutput(true);
      }
      requestConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
      if (payloadLenght > 0) {
         requestConnection.setRequestProperty("Content-Length", Integer.toString(payloadLenght));
      }
      for (Entry<Object, Object> property : message.getProperties().entrySet()) {
         requestConnection.setRequestProperty(property.getKey().toString(), property.getValue().toString());
      }
      // set additional properties
      if (log.isDebugEnabled()) {
         log.debug("Setting HTTP headers");
      }
      if (properties != null) {
         for (Entry<String, String> property : properties.entrySet()) {
            String pKey = property.getKey();
            String pValue = property.getValue();
            requestConnection.setRequestProperty(pKey, pValue);
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
            requestConnection.setRequestProperty(header, hValue);
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message, java.util.Map, org.perfcake.reporting.MeasurementUnit)
    */
   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      int respCode = -1;
      requestConnection.connect();
      if (payload != null && (method == Method.POST || method == Method.PUT)) {
         OutputStreamWriter out = new OutputStreamWriter(requestConnection.getOutputStream(), Utils.getDefaultEncoding());
         out.write(payload, 0, payloadLenght);
         out.flush();
         out.close();
         requestConnection.getOutputStream().close();
      }

      respCode = requestConnection.getResponseCode();
      if (!checkResponseCode(respCode)) {
         StringBuffer errorMess = new StringBuffer();
         errorMess.append("The server returned an unexpected HTTP response code: ").append(respCode).append(" ").append("\"").append(requestConnection.getResponseMessage()).append("\". Expected HTTP codes are ");
         for (int code : expectedResponseCodeList) {
            errorMess.append(Integer.toString(code)).append(", ");
         }
         throw new PerfCakeException(errorMess.substring(0, errorMess.length() - 2) + ".");
      }
      InputStream rcis = null;
      if (respCode < 400) {
         rcis = requestConnection.getInputStream();
      } else {
         rcis = requestConnection.getErrorStream();
      }
      char[] cbuf = new char[10 * 1024];
      InputStreamReader read = new InputStreamReader(rcis, Utils.getDefaultEncoding());
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

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#postSend(org.perfcake.message.Message)
    */
   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
      requestConnection.disconnect();
   }

   /**
    * Used to read the value of HTTP method.
    * 
    * @return The HTTP method.
    */
   public Method getMethod() {
      return method;
   }

   /**
    * Sets the value of HTTP method.
    * 
    * @param method
    *           The HTTP method to set.
    */
   public void setMethod(final Method method) {
      this.method = method;
   }

}
