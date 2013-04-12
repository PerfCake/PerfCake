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

package org.perfcake.reporting.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Totally in-memory database. Each instance represents new database.
 * 
 * @author Filip Nguyen <nguyen.filip@gmail.com>
 * 
 */
public class DerbyDB {
   private Connection connection = null;

   private String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

   private String DERBY_PROTOCOL = "jdbc:derby:memory:";

   private String name;

   public DerbyDB(String name) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
      Class.forName(DERBY_DRIVER).newInstance();
      this.name = name;
      connection = DriverManager.getConnection(DERBY_PROTOCOL + name + ";create=true");

   }

   public Statement createStatement() throws SQLException {
      return connection.createStatement();
   }

   public void drop() throws SQLException {
      connection.close();
      try {
         DriverManager.getConnection(DERBY_PROTOCOL + name + ";drop=true");
      } catch (Exception e) {
         // e.printStackTrace();
         // DERBY throws exception when db is dropped. Duh.
      }
      try {
         DriverManager.getConnection(DERBY_PROTOCOL + name + ";shutdown=true");
      } catch (Exception e) {
         // e.printStackTrace();
      }
   }

   public String getConnectionString(String string) {
      return DERBY_PROTOCOL + name;
   }
}
