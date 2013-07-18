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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfcake.message.Message;

/**
 * The sender that is able to send SQL queries via JDBC.
 * 
 * TODO: Report individual result lines to result validator
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class JDBCSender extends AbstractSender {
   /**
    * The sender's logger.
    */
   private static final Logger log = Logger.getLogger(JDBCSender.class);

   /**
    * JDBC URL string.
    */
   private String jdbcURL = "";

   /**
    * JDBC driver class.
    */
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

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#init()
    */
   @Override
   public void init() throws Exception {
      this.jdbcURL = target;
      Class.forName(driverClass);
      connection = DriverManager.getConnection(jdbcURL, username, password);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#close()
    */
   @Override
   public void close() {
      try {
         connection.close();
      } catch (SQLException ex) {
         if (log.isEnabledFor(Level.ERROR)) {
            log.error(ex.getMessage());
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#preSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      statement = connection.createStatement();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message, java.util.Map)
    */
   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      boolean result = statement.execute((String) message.getPayload());
      Serializable retVal;
      if (result) {
         ResultSet resultSet = statement.getResultSet();

         if (log.isDebugEnabled()) {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnCount = rsmd.getColumnCount();

            log.debug("Column count: " + columnCount);

            StringBuffer sb = new StringBuffer();
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

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message)
    */
   @Override
   public Serializable doSend(Message message) throws Exception {
      return send(message, null);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#postSend(org.perfcake.message.Message)
    */
   @Override
   public void postSend(Message message) throws Exception {
      statement.close();
   }

   /**
    * Used to read the value of jdbcURL.
    * 
    * @return The jdbcURL.
    */
   public String getJdbcURL() {
      return jdbcURL;
   }

   /**
    * Sets the value of jdbcURL.
    * 
    * @param jdbcURL
    *           The jdbcURL to set.
    */
   public void setJdbcURL(String jdbcURL) {
      this.jdbcURL = jdbcURL;
   }

   /**
    * Used to read the value of driverClass.
    * 
    * @return The driverClass.
    */
   public String getDriverClass() {
      return driverClass;
   }

   /**
    * Sets the value of driverClass.
    * 
    * @param driverClass
    *           The driverClass to set.
    */
   public void setDriverClass(String driverClass) {
      this.driverClass = driverClass;
   }

   /**
    * Used to read the value of username.
    * 
    * @return The username.
    */
   public String getUsername() {
      return username;
   }

   /**
    * Sets the value of username.
    * 
    * @param username
    *           The username to set.
    */
   public void setUsername(String username) {
      this.username = username;
   }

   /**
    * Used to read the value of password.
    * 
    * @return The password.
    */
   public String getPassword() {
      return password;
   }

   /**
    * Sets the value of password.
    * 
    * @param password
    *           The password to set.
    */
   public void setPassword(String password) {
      this.password = password;
   }

}
