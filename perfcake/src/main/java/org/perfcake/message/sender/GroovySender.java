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

import java.io.File;
import java.util.Properties;

/**
 * Executes an external Groovy script and pass the message
 * payload via the standard input or as a command argument. It extends the {@link CommandSender} and
 * executes the groovy script via groovy command with <code>groovy {@link #target}</code> passed as an argument.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class GroovySender extends CommandSender {

   /**
    * Classpath for groovy executable - specifies where to find the class files.
    */
   private String classpath = null;

   /**
    * The groovy executable prefix.
    */
   private String groovyExecutable = null;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      if (classpath != null) {
         setCommandPrefix(getGroovyExecutable() + " -classpath " + classpath);
      } else {
         setCommandPrefix(getGroovyExecutable());
      }
   }

   /**
    * Gets the value of groovyExecutable property.
    *
    * @return The groovyExecutable.
    */
   public String getGroovyExecutable() {
      if (groovyExecutable == null) {
         groovyExecutable = System.getenv("GROOVY_HOME") + File.separator + "bin" + File.separator + "groovy";
      }
      return groovyExecutable;
   }

   /**
    * Sets the value of groovyExecutable property.
    *
    * @param groovyExecutable
    *       The groovyExecutable to set.
    * @return Instance of this to support fluent API.
    */
   public GroovySender setGroovyExecutable(final String groovyExecutable) {
      this.groovyExecutable = groovyExecutable;
      return this;
   }

   /**
    * Gets a classpath used by the groovy command.
    *
    * @return Classpath for groovy command.
    */
   public String getClasspath() {
      return classpath;
   }

   /**
    * Sets a classpath used by the groovy command.
    *
    * @param classpath
    *       Classpath for groovy command.
    * @return Instance of this to support fluent API.
    */
   public GroovySender setClasspath(final String classpath) {
      this.classpath = classpath;
      return this;
   }
}
