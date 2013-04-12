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

package org.perfcake;

/**
 * 
 * @author Pavel Macík <pavel.macik@gmail.com>
 * @author Martin Večeřa <marvec@gmail.com>
 * 
 */
public class PerfCakeException extends RuntimeException {

   private static final long serialVersionUID = 7819381443289919857L;

   public PerfCakeException(String message, Throwable cause) {
      super(message, cause);
   }

   public PerfCakeException(Throwable cause) {
      super(cause);
   }

   public PerfCakeException(String message) {
      super(message);
   }
}
//