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
package org.perfcake;

/**
 * Indicates generic problems in PerfCake.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class PerfCakeException extends Exception {

   private static final long serialVersionUID = 7819381443289919857L;

   /**
    * Defaults to {@link java.lang.Exception#Exception(String, Throwable)}.
    *
    * @param message
    *       The detailed message. The detailed message is saved for
    *       later retrieval by the {@link #getMessage()} method.
    * @param cause
    *       The cause (which is saved for later retrieval by the
    *       {@link #getCause()} method). (A <tt>null</tt> value is
    *       permitted, and indicates that the cause is nonexistent or
    *       unknown.)
    * @see java.lang.Exception#Exception(String, Throwable)
    */
   public PerfCakeException(final String message, final Throwable cause) {
      super(message, cause);
   }

   /**
    * Defaults to {@link java.lang.Exception#Exception(Throwable)}.
    *
    * @param cause
    *       The cause (which is saved for later retrieval by the
    *       {@link #getCause()} method). (A <tt>null</tt> value is
    *       permitted, and indicates that the cause is nonexistent or
    *       unknown.)
    * @see java.lang.Exception#Exception(Throwable)
    */
   public PerfCakeException(final Throwable cause) {
      super(cause);
   }

   /**
    * Defaults to {@link java.lang.Exception#Exception(String)}.
    *
    * @param message
    *       The detailed message. The detailed message is saved for
    *       later retrieval by the {@link #getMessage()} method.
    * @see java.lang.Exception#Exception(String)
    */
   public PerfCakeException(final String message) {
      super(message);
   }
}
