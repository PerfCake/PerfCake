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

import org.perfcake.PerfCakeException;

/**
 * Indicates problems in message validation.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ValidationException extends PerfCakeException {

   private static final long serialVersionUID = -1980381518783639733L;

   /**
    * Defaults to {@link org.perfcake.PerfCakeException#PerfCakeException(String, Throwable)}.
    *
    * @param message
    *       The detailed message. The detailed message is saved for
    *       later retrieval by the {@link #getMessage()} method.
    * @param cause
    *       The cause (which is saved for later retrieval by the
    *       {@link #getCause()} method). (A <tt>null</tt> value is
    *       permitted, and indicates that the cause is nonexistent or
    *       unknown.)
    * @see org.perfcake.PerfCakeException#PerfCakeException(String, Throwable)
    */
   public ValidationException(final String message, final Throwable cause) {
      super(message, cause);
   }

   /**
    * Defaults to {@link org.perfcake.PerfCakeException#PerfCakeException(Throwable)}.
    *
    * @param cause
    *       The cause (which is saved for later retrieval by the
    *       {@link #getCause()} method). (A <tt>null</tt> value is
    *       permitted, and indicates that the cause is nonexistent or
    *       unknown.)
    * @see org.perfcake.PerfCakeException#PerfCakeException(Throwable)
    */
   public ValidationException(final Throwable cause) {
      super(cause);
   }

   /**
    * Defaults to {@link org.perfcake.PerfCakeException#PerfCakeException(String)}.
    *
    * @param message
    *       The detailed message. The detailed message is saved for
    *       later retrieval by the {@link #getMessage()} method.
    * @see org.perfcake.PerfCakeException#PerfCakeException(String)
    */
   public ValidationException(final String message) {
      super(message);
   }
}
