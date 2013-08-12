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

import java.io.Serializable;
import java.util.Map;

import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

/**
 * TODO: Provide implementation. This is to cover jMeter functionality. Can be based on HTTP sender???
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * 
 */
public class SOAPSender extends AbstractSender {

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#init()
    */
   @Override
   public void init() throws Exception {
      // TODO Auto-generated method stub

   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#close()
    */
   @Override
   public void close() {
      // TODO Auto-generated method stub

   }

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.AbstractSender#doSend(org.perfcake.message.Message, java.util.Map, org.perfcake.reporting.MeasurementUnit)
    */
   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      // TODO Auto-generated method stub
      return null;
   }

}
