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
package org.perfcake.message.generator;

/**
 * A definition of contract for all message generators.
 *
 * A message generator controls how many threads are being used to generate the messages, is responsible for creating and submitting
 * {@link org.perfcake.message.generator.SenderTask SenderTasks} and controls the other components involved in the performance test execution.
 *
 * A message generator is the most crucial and complicated component of PerfCake and it is highly recommended to reuse one of existing
 * implementations as they already offer mostly wanted features.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MessageGenerator {
}
