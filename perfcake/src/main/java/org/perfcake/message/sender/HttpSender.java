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

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.util.StringTemplate;
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Sends messages via HTTP protocol.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class HttpSender extends AbstractSender {

   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(HttpSender.class);

   /**
    * The URL where the HTTP request is sent.
    */
   private URL url;

   /**
    * The HTTP method that will be used.
    */
   private Method method = Method.POST;

   /**
    * A string template determining the HTTP method to be used dynamically for each request.
    * If not configured (set to null), static configuration in {@link #method} is used instead.
    */
   private StringTemplate dynamicMethod = null;

   /**
    * HTTP method that should be used for the current send operation, pre-calculated in {@link #preSend(Message, Map, Properties)}.
    */
   private Method currentMethod;

   /**
    * Enumeration on available HTTP methods.
    *
    * @see java.net.HttpURLConnection#setRequestMethod(String)
    */
   public static enum Method {
      GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE
   }

   /**
    * The list of response codes that are expected to be returned by HTTP response.
    */
   private List<Integer> expectedResponseCodeList = new ArrayList<>();

   /**
    * The property for expected response codes.
    */
   private String expectedResponseCodes = null;

   /**
    * The HTTP request connection.
    */
   protected HttpURLConnection requestConnection;

   /**
    * The request payload.
    */
   private String payload;

   /**
    * The request payload length.
    */
   private int payloadLength;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      final String targetUrl = safeGetTarget(messageAttributes);
      try {
         url = new URL(targetUrl);
      } catch (MalformedURLException e) {
         throw new PerfCakeException(String.format("Cannot initialize HTTP connection, invalid URL %s: ", targetUrl), e);
      }
   }

   @Override
   public void doClose() {
      // nop
   }

   /**
    * Sets the value of expectedResponseCodes property.
    *
    * @param expectedResponseCodes
    *       The expectedResponseCodes property to set.
    * @return Instance of this to support fluent API.
    */
   public HttpSender setExpectedResponseCodes(final String expectedResponseCodes) {
      this.expectedResponseCodes = expectedResponseCodes;
      setExpectedResponseCodesList(expectedResponseCodes.split(","));
      return this;
   }

   /**
    * Gets the list of expected response codes.
    *
    * @return The list of expected response codes.
    */
   public List<Integer> getExpectedResponseCodeList() {
      return expectedResponseCodeList;
   }

   /**
    * Gets the value of expectedResponseCodes property.
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
    *       The array of codes.
    * @return Instance of this to support fluent API.
    */
   protected HttpSender setExpectedResponseCodesList(final String[] codes) {
      final LinkedList<Integer> numCodes = new LinkedList<Integer>();
      for (final String code : codes) {
         numCodes.add(Integer.parseInt(code.trim()));
      }
      expectedResponseCodeList = numCodes;

      return this;
   }

   /**
    * Checks if the code is expected.
    *
    * @param code
    *       Checked response code.
    * @return true/false according to if the code is expected or not.
    */
   private boolean checkResponseCode(final int code) {
      if (expectedResponseCodeList.isEmpty()) {
         return true;
      }
      for (final int i : expectedResponseCodeList) {
         if (i == code) {
            return true;
         }
      }
      return false;
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);

      currentMethod = getDynamicMethod(messageAttributes);

      payloadLength = 0;
      if (message == null) {
         payload = null;
      } else if (message.getPayload() != null) {
         payload = message.getPayload().toString();
         payloadLength = payload.length();
      }

      requestConnection = (HttpURLConnection) url.openConnection();
      requestConnection.setRequestMethod(currentMethod.name());
      requestConnection.setDoInput(true);
      if (currentMethod == Method.POST || currentMethod == Method.PUT) {
         requestConnection.setDoOutput(true);
      }
      requestConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
      if (payloadLength > 0) {
         requestConnection.setRequestProperty("Content-Length", Integer.toString(payloadLength));
      }

      if (log.isDebugEnabled()) {
         log.debug("Setting HTTP headers: ");
      }

      // set message properties as HTTP headers
      if (message != null) {
         for (final Entry<Object, Object> property : message.getProperties().entrySet()) {
            final String pKey = property.getKey().toString();
            final String pValue = property.getValue().toString();
            requestConnection.setRequestProperty(pKey, pValue);
            if (log.isDebugEnabled()) {
               log.debug(pKey + ": " + pValue);
            }
         }
      }

      // set message headers as HTTP headers
      if (message != null) {
         if (message.getHeaders().size() > 0) {
            for (final Entry<Object, Object> property : message.getHeaders().entrySet()) {
               final String pKey = property.getKey().toString();
               final String pValue = property.getValue().toString();
               requestConnection.setRequestProperty(pKey, pValue);
               if (log.isDebugEnabled()) {
                  log.debug(pKey + ": " + pValue);
               }
            }
         }
      }

      // set additional properties as HTTP headers
      if (properties != null) {
         for (final Entry<String, String> property : properties.entrySet()) {
            final String pKey = property.getKey();
            final String pValue = property.getValue();
            requestConnection.setRequestProperty(pKey, pValue);
            if (log.isDebugEnabled()) {
               log.debug(pKey + ": " + pValue);
            }
         }
      }

      if (log.isDebugEnabled()) {
         log.debug("End of HTTP headers.");
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      int respCode = -1;
      requestConnection.connect();
      if (payload != null && (currentMethod == Method.POST || currentMethod == Method.PUT)) {
         final OutputStreamWriter out = new OutputStreamWriter(requestConnection.getOutputStream(), Utils.getDefaultEncoding());
         out.write(payload, 0, payloadLength);
         out.flush();
         out.close();
         requestConnection.getOutputStream().close();
      }

      respCode = requestConnection.getResponseCode();
      if (!checkResponseCode(respCode)) {
         final StringBuilder errorMess = new StringBuilder();
         errorMess.append("The server returned an unexpected HTTP response code: ").append(respCode).append(" ").append("\"").append(requestConnection.getResponseMessage()).append("\". Expected HTTP codes are ");
         for (final int code : expectedResponseCodeList) {
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

      String payload = null;
      if (rcis != null) {
         final char[] cbuf = new char[10 * 1024];
         final InputStreamReader read = new InputStreamReader(rcis, Utils.getDefaultEncoding());
         // note that Content-Length is available at this point
         final StringBuilder sb = new StringBuilder();
         int ch = read.read(cbuf);
         while (ch != -1) {
            sb.append(cbuf, 0, ch);
            ch = read.read(cbuf);
         }
         read.close();
         rcis.close();
         payload = sb.toString();
      }

      return payload;
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
      requestConnection.disconnect();
   }

   /**
    * Gets the value of HTTP method.
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
    *       The HTTP method to set.
    * @return Instance of this to support fluent API.
    */
   public HttpSender setMethod(final Method method) {
      this.method = method;
      return this;
   }

   /**
    * Sets the template used to determine HTTP method dynamically.
    *
    * @param dynamicMethod
    *       The string template to dynamically determine HTTP method.
    * @return Instance of this to support fluent API.
    */
   public HttpSender setDynamicMethod(final String dynamicMethod) {
      if (dynamicMethod == null || dynamicMethod.isEmpty()) {
         this.dynamicMethod = null;
      } else {
         this.dynamicMethod = new StringTemplate(dynamicMethod);
      }
      return this;
   }

   /**
    * Gets the template used to determine HTTP method dynamically.
    *
    * @param placeholders
    *       The properties to render the string template.
    * @return The HTTP method.
    */
   public Method getDynamicMethod(final Properties placeholders) {
      if (dynamicMethod == null) {
         return this.method;
      } else {
         return Method.valueOf(dynamicMethod.toString(placeholders));
      }
   }

}
