/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package org.perfcake.validation;

import org.perfcake.message.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class PrintingValidator implements MessageValidator {

   private static final Logger log = LogManager.getLogger(PrintingValidator.class);

   @Override
   public boolean isValid(final Message originalMessage, final Message response, final Properties messageAttributes) {
      log.info("Original message: {}", originalMessage.toString());
      log.info("Message attributes: {}", messageAttributes.toString());
      log.info("Response: {}", response.toString());

      return true;
   }
}
