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
package org.perfcake.message.sender;

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.util.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.Properties;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Passes messages to a script compatible with Java Script Engine compliant with JSR-223.
 * The script gets three bounded variables - message, measurementUnit and log. None of them are suitable to be changed.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScriptSender extends AbstractSender {

   /**
    * Logger.
    */
   private static final Logger log = LogManager.getLogger(ScriptSender.class);

   /**
    * The name of the Java Script Engine.
    */
   private String engine;

   /**
    * Compiled representation of the script.
    */
   private CompiledScript compiledScript = null;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      compiledScript = getCompiledScript(safeGetTarget(messageAttributes));
   }

   @Override
   public void doClose() throws PerfCakeException {
      // nop
   }

   @Override
   public Serializable doSend(final Message message, final MeasurementUnit measurementUnit) throws Exception {
      final Bindings b = compiledScript.getEngine().createBindings();
      b.put("message", message);
      b.put("measurementUnit", measurementUnit);
      b.put("log", log);

      return (Serializable) compiledScript.eval(b);
   }

   /**
    * Compiles the script.
    *
    * @param scriptFile
    *       The script location.
    * @return The compiled script.
    * @throws PerfCakeException
    *       When it was not possible to compile the script.
    */
   private CompiledScript getCompiledScript(final String scriptFile) throws PerfCakeException {
      final ScriptEngineManager manager = new ScriptEngineManager();
      final ScriptEngine engine = manager.getEngineByName(this.engine);

      try (Reader fr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(scriptFile)), Utils.getDefaultEncoding()))) {
         return ((Compilable) engine).compile(fr);
      } catch (final ScriptException | IOException e) {
         throw new PerfCakeException("Error loading script file: ", e);
      }
   }

   /**
    * Gets the Java Script Engine.
    *
    * @return The Java Script Engine.
    */
   public String getEngine() {
      return engine;
   }

   /**
    * Sets the Java Script Engine.
    *
    * @param engine
    *       The Java Script Engine.
    * @return This instance for fluent API.
    */
   public ScriptSender setEngine(final String engine) {
      this.compiledScript = null;
      this.engine = engine;
      return this;
   }
}
