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

package org.perfcake.message.generator;

import java.util.List;

import org.perfcake.ObjectWithProperties;
import org.perfcake.PerfCakeException;
import org.perfcake.message.MessageToSend;
import org.perfcake.message.sender.MessageSenderManager;
import org.perfcake.reporting.ReportManager;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public abstract class AbstractMessageGenerator implements ObjectWithProperties {

   protected MessageSenderManager messageSenderManager;
   protected ReportManager reportManager;
   protected List<MessageToSend> messageStore;
   protected int threads = 1;
   protected long start = -1;
   protected long stop = -1;
   protected boolean warmUpEnabled = false;
   protected long minimalWarmUpDuration = 15000; // default 15s
   protected long minimalWarmUpCount = 10000; // by JIT
   protected boolean isMeasuring = false;

   @Override
   public void setProperty(String property, String value) {
      if ("threads".equals(property)) {
         threads = Integer.valueOf(value);
      } else if ("minimalWarmUpDuration".equals(property)) {
         minimalWarmUpDuration = Long.valueOf(value);
      } else if ("minimalWarmUpCount".equals(property)) {
         minimalWarmUpCount = Long.valueOf(value);
      } else if ("warmUpEnabled".equals(property)) {
         warmUpEnabled = Boolean.valueOf(value);
      }
   }

   public String getProperty(String property) {
      if ("threads".equals(property)) {
         return String.valueOf(threads);
      } else if ("minimalWarmUpDuration".equals(property)) {
         return String.valueOf(minimalWarmUpDuration);
      } else if ("minimalWarmUpCount".equals(property)) {
         return String.valueOf(minimalWarmUpCount);
      } else {
         return null;
      }
   }

   public void init(MessageSenderManager messageSenderManager, List<MessageToSend> messageStore) throws Exception {
      this.messageStore = messageStore;
      this.messageSenderManager = messageSenderManager;
      this.messageSenderManager.init();
   }

   public void setReportManager(ReportManager reportManager) {
      this.reportManager = reportManager;
   }

   public void close() throws PerfCakeException {
      messageSenderManager.close();
   }

   protected void setStartTime() {
      start = System.currentTimeMillis();
      if (isMeasuring) {
         reportManager.reportTestStarted();
      }
   }

   protected long getDurationInMillis() {
      return System.currentTimeMillis() - start;
   }

   protected void setStopTime() {
      if (stop == -1) {
         stop = System.currentTimeMillis();
         reportManager.reportTestFinished();
      }
   }

   public abstract void generate() throws Exception;

   protected abstract void postWarmUp() throws Exception;

   public int getThreads() {
      return threads;
   }

   public void setThreads(int threads) {
      this.threads = threads;
   }

   public boolean isWarmUpEnabled() {
      return warmUpEnabled;
   }

   public void setWarmUpEnabled(boolean warmUpEnabled) {
      this.warmUpEnabled = warmUpEnabled;
   }

   public long getMinimalWarmUpDuration() {
      return minimalWarmUpDuration;
   }

   public void setMinimalWarmUpDuration(long minimalWarmUpDuration) {
      this.minimalWarmUpDuration = minimalWarmUpDuration;
   }

   public long getMinimalWarmUpCount() {
      return minimalWarmUpCount;
   }

   public void setMinimalWarmUpCount(long minimalWarmUpCount) {
      this.minimalWarmUpCount = minimalWarmUpCount;
   }
}
