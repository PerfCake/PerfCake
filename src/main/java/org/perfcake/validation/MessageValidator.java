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
import org.w3c.dom.Node;

/**
 * A contract of a message validator.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Marek Baluch <baluchw@gmail.com>
 */
public interface MessageValidator {

   /**
    * Makes sure the provided message is valid.
    * 
    * @param message
    *           A message to be validated.
    * @return True if the message passes all validations.
    */
   public boolean isValid(Message message);

   /**
    * TODO what needs to be specified in XSD to allow any XML in the place for assertions?
    * Sets the assertions that must be valid for all messages with the given message id. Assertions are passed in as a part of
    * an XML document. This can be any arbitrary configuration depending on the validator.
    * 
    * @param validationNode
    *           A part of the XML configuration file specifying the assertion. It is the responsibility of the validator to
    *           understand this.
    * @param msgId
    *           Messages with this message id should be validated using this validator.
    */
   public void setAssertions(Node validationNode, String msgId);
}
