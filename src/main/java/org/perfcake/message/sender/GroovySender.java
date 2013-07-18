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

import java.io.File;

/**
 * 
 * The sender that is able to execute an external Groovy script and pass the message
 * payload via the standard input or as a command argument. It extends the {@link CommandSender} and
 * executes the groovy script via groovy command with <code>groovy {@link #target}</code> passed as an argument.
 * 
 * @author Martin Večeřa <marvenec@gmail.com>
 * @author Pavel Macík <pavel.macik@gmail.com>
 */
public class GroovySender extends CommandSender {

   /**
    * The groovy executable prefix.
    */
   private String groovyExecutable = null;

   /*
    * (non-Javadoc)
    * 
    * @see org.perfcake.message.sender.CommandSender#init()
    */
   @Override
   public void init() throws Exception {
      setCommandPrefix(getGroovyExecutable());
   }

   /**
    * Used to read the value of groovyExecutable property.
    * 
    * @return The groovyExecutable.
    */
   public String getGroovyExecutable() {
      if (groovyExecutable == null) {
         groovyExecutable = System.getenv("GROOVY_HOME") + File.separator + "groovy";
      }
      return groovyExecutable;
   }

   /**
    * Sets the value of groovyExecutable property.
    * 
    * @param groovyExecutable
    *           The groovyExecutable to set.
    */
   public void setGroovyExecutable(String groovyExecutable) {
      this.groovyExecutable = groovyExecutable;
   }

}
