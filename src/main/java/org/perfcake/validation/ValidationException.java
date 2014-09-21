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
 * An exception to indicate problems in message validation.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class ValidationException extends PerfCakeException {

   private static final long serialVersionUID = -1980381518783639733L;

   public ValidationException(final String message, final Throwable cause) {
      super(message, cause);
   }

   public ValidationException(final Throwable cause) {
      super(cause);
   }

   public ValidationException(final String message) {
      super(message);
   }
}
