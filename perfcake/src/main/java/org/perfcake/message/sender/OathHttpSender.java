/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2017 the original author or authors.
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

import org.perfcake.message.Message;
import org.perfcake.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Allows token authentication to an HTTP based service.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class OathHttpSender extends HttpSender {

   /**
    * URL of the server granting us a token. Default value works with Keycloak, supposing your realm name is demo.
    */
   private String tokenServerUrl = "http://127.0.0.1:8180/auth/realms/demo/protocol/openid-connect/token";

   /**
    * Data to send to the server to request a token. Default value works with Keycloak.
    */
   private String tokenServerData = "grant_type=password&client_id=jboss-javaee-webapp&username=marvec&password=abc123";

   /**
    * RegEx to create a capture group around the token value in the server's response.
    */
   private String responseParser = ".*\"access_token\":\"([^\"]*)\".*";

   /**
    * Header where the token is passed to the target service. The default value works with Keycloak.
    */
   private String oathHeader = "Authorization";

   /**
    * String formatting the value of authorization header. The token is placed instead of %s. The default value works with Keycloak.
    */
   private String oathHeaderFormat = "Bearer %s";

   /**
    * How long is a token valid before a new one is needed. Defaults to 60s.
    */
   private long tokenTimeout = 60000L;

   /**
    * The cached token value.
    */
   private String token = "";

   /**
    * Time when the token was last updated.
    */
   private long tokenUpdated = 0;

   void obtainToken() throws IOException {
      final HttpURLConnection http = (HttpURLConnection) new URL(tokenServerUrl).openConnection();
      final byte[] data = tokenServerData.getBytes(Utils.getDefaultEncoding());
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.setDoInput(true);
      http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      http.setRequestProperty("Content-Length", Integer.toString(data.length));
      http.connect();
      http.getOutputStream().write(data);
      http.getOutputStream().flush();
      http.getOutputStream().close();

      String response = "";
      try (BufferedReader buffer = new BufferedReader(new InputStreamReader(http.getInputStream()))) {
         response = buffer.lines().collect(Collectors.joining("\n"));
      }

      http.disconnect();

      Pattern p = Pattern.compile(responseParser);
      Matcher m = p.matcher(response);
      m.find();

      token = m.group(1);

      tokenUpdated = System.currentTimeMillis();
   }

   private void setRequestHeader(final HttpURLConnection http) {
      http.setRequestProperty(oathHeader, String.format(oathHeaderFormat, token));
   }

   private void refreshToken() throws IOException {
      if (token == null || token.isEmpty() || System.currentTimeMillis() > tokenUpdated + tokenTimeout) {
         obtainToken();
      }
   }

   @Override
   public void preSend(final Message message, final Properties messageAttributes) throws Exception {
      super.preSend(message, messageAttributes);

      refreshToken();
      setRequestHeader(requestConnection);
   }

   public String getTokenServerUrl() {
      return tokenServerUrl;
   }

   public void setTokenServerUrl(final String tokenServerUrl) {
      this.tokenServerUrl = tokenServerUrl;
   }

   public String getTokenServerData() {
      return tokenServerData;
   }

   public void setTokenServerData(final String tokenServerData) {
      this.tokenServerData = tokenServerData;
   }

   public String getResponseParser() {
      return responseParser;
   }

   public void setResponseParser(final String responseParser) {
      this.responseParser = responseParser;
   }

   public String getOathHeader() {
      return oathHeader;
   }

   public void setOathHeader(final String oathHeader) {
      this.oathHeader = oathHeader;
   }

   public String getOathHeaderFormat() {
      return oathHeaderFormat;
   }

   public void setOathHeaderFormat(final String oathHeaderFormat) {
      this.oathHeaderFormat = oathHeaderFormat;
   }

   public long getTokenTimeout() {
      return tokenTimeout;
   }

   public void setTokenTimeout(final long tokenTimeout) {
      this.tokenTimeout = tokenTimeout;
   }
}
