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

import org.perfcake.message.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Validates messages using Java Script Engine and the provided script.
 * The script engine must be installed in the extensions directory. The original message is passed
 * to the script in the 'originalMessage' property and the response is inserted as 'message', both using
 * script bindings. Script return value is evaluated for validation success (true = passed, false = failed).
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ScriptValidator implements MessageValidator {

   private String engine;
   private String script;
   private String scriptFile;
   private CompiledScript compiledScript = null;

   private static final Logger log = LogManager.getLogger(ScriptValidator.class);

   private CompiledScript getCompiledScript() throws ScriptException, ValidationException {
      if (compiledScript == null) {
         final ScriptEngineManager manager = new ScriptEngineManager();
         final ScriptEngine engine = manager.getEngineByName(this.engine);

         if (script != null) {
            compiledScript = ((Compilable) engine).compile(script);
         } else if (scriptFile != null) {
            try (Reader fr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(scriptFile)), StandardCharsets.UTF_8))) {
               compiledScript = ((Compilable) engine).compile(fr);
            } catch (final IOException e) {
               throw new ValidationException("Error loading script file: ", e);
            }
         } else {
            throw new ValidationException("No script was set.");
         }
      }

      return compiledScript;
   }

   @Override
   public boolean isValid(final Message originalMessage, final Message response, final Properties messageAttributes) {
      boolean result = false;

      try {
         final CompiledScript script = getCompiledScript();
         final Bindings b = script.getEngine().createBindings();
         b.put("originalMessage", originalMessage);
         b.put("message", response);
         b.put("attributes", messageAttributes);
         b.put("log", log);
         final Object ret = script.eval(b);
         if (ret instanceof Boolean) {
            result = (Boolean) ret;
         }
      } catch (ScriptException | ValidationException e) {
         log.warn("Error validating a message: ", e);
         result = false;
      }

      if (!result) {
         log.info(String.format("Script validating failed with the message '%s' using script validator.", response.toString()));
      }
      return result;
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
   public ScriptValidator setEngine(final String engine) {
      this.compiledScript = null;
      this.engine = engine;
      return this;
   }

   /**
    * Gets the Java script.
    *
    * @return The Java script.
    */
   public String getScript() {
      return script;
   }

   /**
    * Sets the Java script.
    *
    * @param script
    *       The Java script.
    * @return This instance for fluent API.
    */
   public ScriptValidator setScript(final String script) {
      this.scriptFile = null;
      this.compiledScript = null;
      this.script = script;

      return this;
   }

   /**
    * Sets the Java script taken from {@link org.w3c.dom.Element Element}'s text content.
    *
    * @param script
    *       The DOM element from whose content the Java script is taken.
    */
   public void setScriptAsElement(final Element script) {
      this.scriptFile = null;
      this.compiledScript = null;
      this.script = script.getTextContent();
   }

   /**
    * Gets the script file.
    *
    * @return The script file.
    */
   public String getScriptFile() {
      return scriptFile;
   }

   /**
    * Sets the file from which the Java script is taken.
    *
    * @param scriptFile
    *       The script file.
    * @return This instance for fluent API.
    */
   public ScriptValidator setScriptFile(final String scriptFile) {
      this.script = null;
      this.compiledScript = null;
      this.scriptFile = scriptFile;

      return this;
   }
}
