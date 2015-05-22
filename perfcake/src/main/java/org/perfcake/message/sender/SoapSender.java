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

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

/**
 * TODO: Provide implementation. This is to cover jMeter functionality. Can be based on HTTP sender???
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class SoapSender extends AbstractSender {

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      // TODO Auto-generated method stub

   }

   @Override
   public void doClose() {
      // TODO Auto-generated method stub

   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      // TODO Auto-generated method stub
      return null;
   }
}
