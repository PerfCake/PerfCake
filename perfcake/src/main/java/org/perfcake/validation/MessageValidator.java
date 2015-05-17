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
package org.perfcake.validation;

import org.perfcake.message.Message;

import java.util.Properties;

/**
 * A contract of a message validator.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface MessageValidator {

   /**
    * Makes sure the provided message is valid.
    *
    * @param originalMessage
    *       The message that has been sent.
    * @param response
    *       A response for the original message.
    * @param messageAttributes
    *       A snapshot of sequences' values and possible other attributes used for sending a message. These attributes can be used by a validator to replace placeholders.
    * @return <code>true</code> if the message passes all validations.
    */
   boolean isValid(final Message originalMessage, final Message response, final Properties messageAttributes);
}
