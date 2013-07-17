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

/**
 * 
 * The sender that is able to execute an external Groovy script and pass the message
 * payload via a stream it to input. It extends the {@link CommandSender} and
 * executes the groovy script via groovy command with <code>groovy {@link #target}</code> passed as an argument.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class GroovySender extends CommandSender {

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.CommandSender#init()
    */
   @Override
   public void init() throws Exception {
      super.setCommand("groovy " + this.target);// groovy groovy-file
   }
}
