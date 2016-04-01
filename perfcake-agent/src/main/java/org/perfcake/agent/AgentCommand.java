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
package org.perfcake.agent;

/**
 * PerfCake Agent command type.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public enum AgentCommand {
   /**
    * Requests the amount of free memory in the Java Virtual Machine.
    */
   FREE,

   /**
    * Requests the amount of used memory in the Java Virtual Machine.
    */
   USED,

   /**
    * Requests the amount of total memory in the Java Virtual Machine.
    */
   TOTAL,

   /**
    * Requests the maximal amount of memory the Java Virtual Machine will attempt to use.
    */
   MAX,

   /**
    * Initiates a heap dump in the Java Virtual Machine.
    */
   DUMP,

   /**
    * Calls {@link System#gc()} to perform a garbage collection.
    */
   GC
}
