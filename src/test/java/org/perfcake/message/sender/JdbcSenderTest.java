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

import static org.mockito.Mockito.*;

import org.perfcake.util.ObjectFactory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * @author Lenka Vašková <vaskova.lenka@gmail.com>
 */
public class JdbcSenderTest {

   private static class DebugLogAppender implements Appender {
      private String lastMessage = null;

      @Override
      public void append(final LogEvent logEvent) {
         lastMessage = logEvent.getMessage().getFormattedMessage();
      }

      @Override
      public String getName() {
         return "DebugAppender";
      }

      @Override
      public Layout getLayout() {
         return null;
      }

      @Override
      public boolean ignoreExceptions() {
         return false;
      }

      @Override
      public ErrorHandler getHandler() {
         return null;
      }

      @Override
      public void setHandler(final ErrorHandler errorHandler) {

      }

      @Override
      public void start() {

      }

      @Override
      public void stop() {

      }

      @Override
      public boolean isStarted() {
         return true;
      }

      @Override
      public boolean isStopped() {
         return false;
      }
   }

   @Test
   public void testSelect() throws Exception {
      String sql = "SELECT * FROM USERS";
      String result = "1;Jennifer Aniston;jennifer@aniston.com\n2;Adam Sandler;adam0001@yahoo.com";

      Connection c = mock(Connection.class);
      Statement s = mock(Statement.class);
      ResultSet rs = mock(ResultSet.class);
      ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

      when(c.createStatement()).thenReturn(s);
      when(s.execute(sql)).thenReturn(true);
      when(s.getResultSet()).thenReturn(rs);
      when(rs.getMetaData()).thenReturn(rsmd);
      when(rsmd.getColumnCount()).thenReturn(3);
      when(rsmd.getColumnName(1)).thenReturn("ID");
      when(rsmd.getColumnName(2)).thenReturn("NAME");
      when(rsmd.getColumnName(3)).thenReturn("EMAIL");
      when(rsmd.getColumnTypeName(1)).thenReturn("Long");
      when(rsmd.getColumnTypeName(2)).thenReturn("String");
      when(rsmd.getColumnTypeName(3)).thenReturn("String");
      when(rs.getFetchSize()).thenReturn(1);
      when(rs.toString()).thenReturn(result);

      when(rs.next()).thenAnswer(new Answer<Boolean>() {
         private int count = 0;

         public Boolean answer(InvocationOnMock invocation) {
            return count++ < 2;
         }
      });

      Properties props = new Properties();
      JdbcSender sender = (JdbcSender) ObjectFactory.summonInstance(JdbcSender.class.getName(), props);

      // Inject connection
      Field connectionField = JdbcSender.class.getDeclaredField("connection");
      connectionField.setAccessible(true);
      connectionField.set(sender, c);

      org.perfcake.message.Message message = new org.perfcake.message.Message();
      message.setPayload(sql);
      sender.preSend(message, null);
      Serializable response = sender.send(message, null);
      sender.postSend(message);
      sender.close();

      Assert.assertEquals(response.toString(), result);

      InOrder order = inOrder(c, s, rs, rsmd);
      order.verify(c).createStatement();
      order.verify(s).execute(sql);
      order.verify(s).getResultSet();
      order.verify(rs).getMetaData();
      order.verify(rsmd).getColumnCount();

      verify(rsmd, never()).getColumnType(0);
      verify(rsmd, never()).getColumnType(4);
      verify(rsmd, never()).getColumnTypeName(0);
      verify(rsmd, never()).getColumnTypeName(4);
      verify(rs, times(1)).getFetchSize();
      verify(rs, times(3)).next();

      verify(rsmd).getColumnName(1);
      verify(rsmd).getColumnName(2);
      verify(rsmd).getColumnName(3);
      verify(rsmd).getColumnTypeName(1);
      verify(rsmd).getColumnTypeName(2);
      verify(rsmd).getColumnTypeName(3);

      verify(s).close();
      verify(c).close();

      verifyNoMoreInteractions(c, s, rs, rsmd);
   }

   @Test
   public void testUpdate() throws Exception {
      String sql = "DROP TABLE USERS";

      Connection c = mock(Connection.class);
      Statement s = mock(Statement.class);

      when(c.createStatement()).thenReturn(s);
      when(s.execute(sql)).thenReturn(false);
      when(s.getUpdateCount()).thenReturn(3);

      Properties props = new Properties();
      JdbcSender sender = (JdbcSender) ObjectFactory.summonInstance(JdbcSender.class.getName(), props);

      // Inject connection
      Field connectionField = JdbcSender.class.getDeclaredField("connection");
      connectionField.setAccessible(true);
      connectionField.set(sender, c);

      org.perfcake.message.Message message = new org.perfcake.message.Message();
      message.setPayload(sql);
      sender.preSend(message, null);
      Serializable response = sender.send(message, null);
      sender.postSend(message);
      sender.close();

      Assert.assertEquals((Integer) response, Integer.valueOf(3));

      InOrder order = inOrder(c, s);
      order.verify(c).createStatement();
      order.verify(s).execute(sql);
      order.verify(s).getUpdateCount();

      verify(s).close();
      verify(c).close();

      verifyNoMoreInteractions(c, s);
   }

   @Test
   public void testProperties() throws Exception {
      String username = "zappa";
      String password = "frank";
      String jdbcUrl = "jdbc:none:test";
      String driverClass = "org.perfcake.None";

      Properties props = new Properties();
      props.setProperty("username", username);
      props.setProperty("password", password);
      props.setProperty("jdbcUrl", jdbcUrl);
      props.setProperty("driverClass", driverClass);

      JdbcSender sender = (JdbcSender) ObjectFactory.summonInstance(JdbcSender.class.getName(), props);

      Assert.assertEquals(sender.getUsername(), username);
      Assert.assertEquals(sender.getPassword(), password);
      Assert.assertEquals(sender.getJdbcUrl(), jdbcUrl);
      Assert.assertEquals(sender.getDriverClass(), driverClass);
   }

   @Test
   public void testNegativeClose() throws Exception {
      String errorMessage = "Huhúúú";
      Connection c = mock(Connection.class);
      doThrow(new SQLException(errorMessage)).when(c).close();

      Properties props = new Properties();
      JdbcSender sender = (JdbcSender) ObjectFactory.summonInstance(JdbcSender.class.getName(), props);

      // Inject connection
      Field connectionField = JdbcSender.class.getDeclaredField("connection");
      connectionField.setAccessible(true);
      connectionField.set(sender, c);

      // Add log appender
      Field logField = JdbcSender.class.getDeclaredField("log");
      logField.setAccessible(true);
      Logger log = (Logger) logField.get(sender);
      DebugLogAppender a = new DebugLogAppender();
      ((org.apache.logging.log4j.core.Logger) log).addAppender(a);

      sender.close();

      Assert.assertEquals(a.lastMessage, errorMessage);

      verify(c).close();
      verifyNoMoreInteractions(c);
   }
}
