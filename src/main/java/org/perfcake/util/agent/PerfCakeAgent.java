/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2014 the original author or authors.
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

package org.perfcake.util.agent;

/**
 * PerfCake agent that can be used to monitor tested system's JVM.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class PerfCakeAgent {

   /**
    * Default encoding of the input and output streams.
    */
   public static final String DEFAULT_ENCODING = "UTF-8";

   /**
    * Default agent port.
    */
   public static final int DEFAULT_PORT = 8850;

   /**
    * The command type.
    *
    * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
    */
   public enum Command {
      FREE, USED, TOTAL, MAX, DUMP, GC
   }

   /**
    * {@link PerfCakeAgent}'s pre-main method.
    *
    * @param agentArgs
    *       Agent arguments.
    */
   public static void premain(String agentArgs) {
      Thread agentThread = new Thread(new AgentThread(agentArgs));
      agentThread.setDaemon(true);
      agentThread.start();
   }
}
