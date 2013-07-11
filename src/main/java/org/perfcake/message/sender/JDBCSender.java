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
 * TODO: Report individual result lines to result validator
 * 
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 * 
 */
public class JDBCSender extends AbstractSender {
   private static final Logger log = Logger.getLogger(JDBCSender.class);

   private String jdbcURL = "";
   private String driverClass = "";
   private String username = "";
   private String password = "";
   private Connection connection = null;
   private Statement statement;
   private Serializable retval;

   @Override
   public void init() throws Exception {
      this.jdbcURL = address;
      Class.forName(driverClass);
      connection = DriverManager.getConnection(jdbcURL, username, password);
   }

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

   @Override
   public void preSend(Message message, Map<String, String> properties) throws Exception {
      statement = connection.createStatement();
   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties) throws Exception {
      boolean result = statement.execute((String) message.getPayload());
      // log.info(message.getPayload());
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

         retval = resultSet.toString();
      } else {
         retval = statement.getUpdateCount();
      }

      return retval;
   }

   @Override
   public Serializable doSend(Message message) throws Exception {
      return send(message, null);
   }

   @Override
   public void postSend(Message message) throws Exception {
      statement.close();
   }

   public String getDriverClass() {
      return driverClass;
   }

   public void setDriverClass(String driverClass) {
      this.driverClass = driverClass;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

}
