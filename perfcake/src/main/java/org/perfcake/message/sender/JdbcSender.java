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
import org.perfcake.util.properties.MandatoryProperty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

/**
 * Sends SQL queries via JDBC.
 *
 * TODO: Report individual result lines to result validator
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class JdbcSender extends AbstractSender {
   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(JdbcSender.class);

   /**
    * JDBC URL string.
    */
   private String jdbcUrl = "";

   /**
    * JDBC driver class.
    */
   @MandatoryProperty
   private String driverClass = "";

   /**
    * JDBC username.
    */
   private String username = "";

   /**
    * JDBC password.
    */
   private String password = "";

   /**
    * JDBC connection.
    */
   private Connection connection = null;

   /**
    * SQL statement.
    */
   private Statement statement;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      this.jdbcUrl = safeGetTarget(messageAttributes);
      try {
         Class.forName(driverClass);
         connection = DriverManager.getConnection(jdbcUrl, username, password);
      } catch (Exception e) {
         throw new PerfCakeException("Cannot load JDBC driver or open the JDBC connection: ", e);
      }
   }

   @Override
   public void doClose() {
      try {
         connection.close();
      } catch (final SQLException ex) {
         log.error("Unable to close JDBC connection: " + ex.getMessage(), ex);
      }
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);
      statement = connection.createStatement();
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      final boolean result = statement.execute((String) message.getPayload());
      Serializable retVal;
      if (result) {
         final ResultSet resultSet = statement.getResultSet();

         if (log.isDebugEnabled()) {
            final ResultSetMetaData rsmd = resultSet.getMetaData();
            final int columnCount = rsmd.getColumnCount();

            log.debug("Column count: " + columnCount);

            final StringBuffer sb = new StringBuffer();
            for (int i = 1; i <= columnCount; i++) {
               sb.append(" ");
               sb.append(rsmd.getColumnName(i));
               sb.append(":");
               sb.append(rsmd.getColumnTypeName(i));
            }

            log.debug(sb.toString());

            log.debug("Result set's fetch size: " + resultSet.getFetchSize());
            log.debug("Going throught the result set...");
            int rowCount = 0;
            while (resultSet.next()) {
               rowCount++;
               // nop - go through whole result set
            }
            log.debug("Result set's row count: " + rowCount);
         }

         retVal = resultSet.toString();
      } else {
         retVal = statement.getUpdateCount();
      }

      return retVal;
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);
      statement.close();
   }

   /**
    * Gets the JDBC URL.
    *
    * @return The JDBC URL.
    */
   public String getJdbcUrl() {
      return jdbcUrl;
   }

   /**
    * Sets the JDBC URL.
    *
    * @param jdbcUrl
    *       The JDBC URL.
    * @return Instance of this to support fluent API.
    */
   public JdbcSender setJdbcUrl(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      return this;
   }

   /**
    * Gets the JDBC driver class.
    *
    * @return The JDBC driver class.
    */
   public String getDriverClass() {
      return driverClass;
   }

   /**
    * Sets the JDBC driver class.
    *
    * @param driverClass
    *       The JDBC driver class.
    * @return Instance of this to support fluent API.
    */
   public JdbcSender setDriverClass(final String driverClass) {
      this.driverClass = driverClass;
      return this;
   }

   /**
    * Gets the JDBC username.
    *
    * @return The JDBC username.
    */
   public String getUsername() {
      return username;
   }

   /**
    * Sets the JDBC username.
    *
    * @param username
    *       The JDBC username.
    * @return Instance of this to support fluent API.
    */
   public JdbcSender setUsername(final String username) {
      this.username = username;
      return this;
   }

   /**
    * Gets the JDBC password.
    *
    * @return The password.
    */
   public String getPassword() {
      return password;
   }

   /**
    * Sets the JDBC password.
    *
    * @param password
    *       The JDBC password.
    * @return Instance of this to support fluent API.
    */
   public JdbcSender setPassword(final String password) {
      this.password = password;
      return this;
   }
}
