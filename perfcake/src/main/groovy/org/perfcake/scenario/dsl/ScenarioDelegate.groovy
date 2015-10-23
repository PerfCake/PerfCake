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
package org.perfcake.scenario.dsl

import groovy.transform.TupleConstructor
import org.apache.logging.log4j.LogManager
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import org.perfcake.PerfCakeConst
import org.perfcake.PerfCakeException
import org.perfcake.common.Period
import org.perfcake.common.PeriodType
import org.perfcake.message.MessageTemplate
import org.perfcake.message.generator.MessageGenerator
import org.perfcake.scenario.Scenario
import org.perfcake.scenario.ScenarioBuilder
import org.perfcake.scenario.ScenarioFactory
import org.perfcake.util.ObjectFactory
import org.perfcake.util.Utils

import java.nio.file.Files
import java.nio.file.Paths

import static org.codehaus.groovy.syntax.Types.*

/**
 * Implementation of DSL scenario specification support for PerfCake.
 * The scenario in the DSL format is a Groovy script. A set of ugly classes in this file
 * permits the use of various wild language constructs.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */

/**
 *  Any call to a non existing method on this class is stored in a map in the form of methodName -> parameter.
 *  Parameters of Time type are automatically converted to a number in milliseconds (which allows passing in time values for senders and destinations).
 *  Multiple parameters are converted to an array stored in the value.
 */
class PropertiesBacked {
   def properties = [:]

   String toString() {
      "Properties: " + properties
   }

   def methodMissing(String name, args) {
      if (args.length == 1) {
         if (args[0] instanceof Time) {
            args[0] = ((Time) args[0]).ms
         }
         properties[name] = args[0]
      } else {
         properties[name] = args
      }
      this
   }
}

/**
 * Furthermore, the object can have a class name and the automatic properties.
 */
class ObjectWithClassName extends PropertiesBacked {
   def className

   ObjectWithClassName(def className) {
      this.className = className
   }

   String toString() {
      "class: $className, ${super.toString()}"
   }
}

/**
 * Class holding all values needed to construct the real scenario.
 */
class DslScenario extends PropertiesBacked {
   def description
   def generator
   def sender
   def sequences = []
   def reporters = []
   def messages = []
   def validation
   def validators = []
   def runInfo

   String toString() {
      "DslScenario {description: $description, \n" +
            "   $runInfo, \n" +
            "   $generator, \n" +
            "   Sequences: [" +
            (sequences.empty ?
                  "" :
                  "\n      " + sequences.join(',\n      ') + "\n   ") +
            "], \n" +
            "   $sender, \n" +
            "   Reporters: [" +
            (reporters.empty ?
                  "" :
                  "\n      " + reporters.join(',\n      ') + "\n   ") +
            "], \n" +
            "   Messages: [" +
            (messages.empty ?
                  "" :
                  "\n      " + messages.join(',\n      ') + "\n   ") +
            "], \n" +
            "   $validation,\n" +
            "   Validators: [" +
            (validators.empty ?
                  "" :
                  "\n      " + validators.join(',\n      ') + "\n   ") +
            "], \n" +
            "   ${super.toString()}\n" +
            "}"
   }

   def methodMissing(String name, args) {
      // this catches the definition of the scenario description right after the 'scenario' keyword
      if (name == "call") {
         this.description = args[0]
      } else {
         return super.methodMissing(name, args)
      }
      this
   }

   // new generator
   def generator(def className) {
      this.generator = new Generator(className)
      this.generator
   }

   // new sender
   def sender(def className) {
      this.sender = new Sender(className)
      this.sender
   }

   // new sequence
   def sequence(def className) {
      def s = new Sequence(className)
      this.sequences.add(s)
      s
   }

   // new reporter
   def reporter(def className) {
      def r = new Reporter(className)
      this.reporters.add(r)
      r
   }

   // new destination
   def destination(def className) {
      this.reporters.last().destination(className)
   }

   // new message
   def message(def location) {
      def m = new Message()
      this.messages.add(m)

      // the location can be specified as type:"location" where type can be
      // uri,file,from,in to lookup for a file (either local or remote), or
      // content,text,body for direct textual content specification in the scenario file
      if (location instanceof Map) {
         Map map = (Map) location
         if (map.containsKey('uri') || map.containsKey('file') || map.containsKey('from') || map.containsKey('in')) {
            m.uri = map.values().iterator().next()
         } else if (map.containsKey('content') || map.containsKey('text') || map.containsKey('body')) {
            m.content = map.values().iterator().next()
         } else {
            throw new PerfCakeException("Message content not defined.")
         }
      } else if (location instanceof String) {
         // in case of a simple string (i.e. "location"), approach it as a file location
         String s = (String) location
         if (s.indexOf('://') >= 0 || Files.exists(Paths.get(s)) || Files.exists(Paths.get(Utils.DEFAULT_RESOURCES_DIR.getAbsolutePath(), 'scenarios', s))) {
            m.uri = s
         } else {
            m.content = s
         }
      } else {
         throw new PerfCakeException("Unknown message content definition type ${location.getClass().getName()}.")
      }
      m
   }

   // configuration of validation, takes two parameters
   def validation(boolean enabled, boolean fast) {
      this.validation = new Validation()
      this.validation.enabled = enabled
      this.validation.fastForward = fast
      this.validation
   }

   // new validator
   def validator(def className) {
      def v = new Validator(className)
      this.validators.add(v)
      v
   }

   // new run info based on time
   def run(Time time) {
      if (this.runInfo != null) {
         throw new PerfCakeException("The 'run' keyword can be used only once in the scenario.")
      }
      this.runInfo = new RunInfo(time)
      this.runInfo
   }

   // new run info based on interations
   def run(Iterations iterations) {
      this.runInfo = new RunInfo(iterations)
      this.runInfo
   }

   // builds the complete PerfCake scenario
   def Scenario buildScenario() {
      MessageGenerator g = generator.buildMessageGenerator()
      g.setThreads((int) runInfo.getThreads()) // get the number of threads from DSL run info
      ScenarioBuilder builder = new ScenarioBuilder(runInfo.buildRunInfo(), g, sender.messageSenderClassName, sender.messageSenderProperties)

      if (sequences) {
         sequences.each {
            builder.putSequence(it.name, it.buildSequence())
         }
      }

      if (reporters) {
         reporters.each {
            builder.addReporter(it.buildReporter())
         }
      }

      def validatorIds = []
      if (validators) {
         validators.each {
            if (!it.id) {
               throw new PerfCakeException("The following validator is missing its 'id': ${it.toString()}")
            }
            validatorIds.add(it.id) // remember the validator id for later binding with messages
            builder.putMessageValidator(it.id, it.buildValidator())
         }
      }


      if (messages) {
         messages.each {
            builder.addMessage(it.buildMessageTemplate())

            // this must be done after building the message, as array of validators may need to be obtained
            // from properties during the message template creation
            it.validators.each {validatorId ->
               if (!validatorIds.contains(validatorId)) {
                  throw new PerfCakeException("Reference '$validatorId' to non-existing validator.")
               }
            }
         }
      }

      Scenario s = builder.build()

      if (validation) {
         // copy the validator configuration parameters
         s.getValidationManager().setFastForward(validation.fastForward)
         s.getValidationManager().setEnabled(validation.getEnabledValue())
      }

      s
   }
}

class RunInfo extends PropertiesBacked {
   def time
   def iterations
   def threads

   RunInfo(Time time) {
      this.time = time
   }

   RunInfo(Iterations iterations) {
      this.iterations = iterations
   }

   def with(Integer threads) {
      this.threads = threads
      this
   }

   String toString() {
      "RunInfo: {" + (time ? "$time ($time.ms ms)" : "$iterations") + " with $threads threads}"
   }

   org.perfcake.RunInfo buildRunInfo() {
      if (!(time || iterations)) {
         throw new PerfCakeException("Either time or iterations must be specified in the 'run' definition.")
      }
      if (!threads) {
         throw new PerfCakeException("Number of threads is missing in the 'run' definition.")
      }

      new org.perfcake.RunInfo(new Period(time ? PeriodType.TIME : PeriodType.ITERATION, time ? time.ms : iterations.amount))
   }
}

class Generator extends ObjectWithClassName {

   Generator(def className) {
      super(className)
   }

   String toString() {
      "Generator: {${super.toString()}}"
   }

   MessageGenerator buildMessageGenerator() {
      def props = new Properties()
      props.putAll(properties)

      ObjectFactory.summonInstance(className.contains('.') ?: ScenarioFactory.DEFAULT_GENERATOR_PACKAGE + '.' + className, props)
   }
}

class Sequences {

}

class Sequence extends ObjectWithClassName {

   def name

   Sequence(def className) {
      super(className)
   }

   String toString() {
      "Sequence: {name: ${name}, ${super.toString()}}"
   }

   def name(def name) {
      this.name = name
      this
   }

   org.perfcake.message.sequence.Sequence buildSequence() {
      def props = new Properties()
      props.putAll(properties)

      ObjectFactory.summonInstance(className.contains('.') ?: ScenarioFactory.DEFAULT_SEQUENCE_PACKAGE + '.' + className, props)
   }
}

class Sender extends ObjectWithClassName {

   Sender(def className) {
      super(className)
   }

   String toString() {
      "Sender: {${super.toString()}}"
   }

   Properties getMessageSenderProperties() {
      def props = new Properties()
      props.putAll(properties)
      props
   }

   String getMessageSenderClassName() {
      className.contains('.') ?: ScenarioFactory.DEFAULT_SENDER_PACKAGE + '.' + className
   }
}

class Reporter extends ObjectWithClassName {
   def enabled = true
   def destinations = []

   Reporter(def className) {
      super(className)
   }

   String toString() {
      "Reporter: {\n" +
            "         Destinations: [" +
            (destinations.empty ?
                  "" :
                  "\n            " + destinations.join(',\n            ') +
                        "\n         ") +
            "], \n" +
            "         ${super.toString()}, enabled: $enabled\n      }"
   }

   // this provides 'enabled' keyword that could be used at the end of the line
   def getEnabled() {
      enabled = true
      this
   }

   // this provides 'disabled' keyword that could be used at the end of the line
   def getDisabled() {
      enabled = false
      this
   }

   // new destination under this reporter
   def destination(def className) {
      def d = new Destination(className)
      destinations.add(d)
      d
   }

   org.perfcake.reporting.reporters.Reporter buildReporter() {
      def props = new Properties()
      props.putAll(properties)
      def r = ObjectFactory.summonInstance(className.contains('.') ?: ScenarioFactory.DEFAULT_REPORTER_PACKAGE + '.' + className, props)

      if (destinations) {
         destinations.each {
            if (it.getEnabledValue()) {
               // skip disabled destinations completely
               def p = null
               if (it.period instanceof Time) {
                  p = new Period(PeriodType.TIME, it.period.ms)
               } else if (it.period instanceof Iterations) {
                  p = new Period(PeriodType.ITERATION, it.period.amount)
               } else if (it.period instanceof Percents) {
                  p = new Period(PeriodType.PERCENTAGE, it.period.amount)
               }
               if (p == null) {
                  throw new PerfCakeException("No reporting period set for the following reporter and destination. Use 'every' keyword. ${this.toString()}, $it")
               }

               r.registerDestination(it.buildDestination(), p)
            }
         }
      }

      r
   }
}

class Destination extends ObjectWithClassName {

   def enabled = true
   def period

   Destination(def className) {
      super(className)
   }

   String toString() {
      "Destination: {period: $period, ${super.toString()}, enabled: $enabled}"
   }

   def every(def period) {
      this.period = period
      this
   }

   // getEnabled is already used for something else, this method is needed to actually obtain the value of the enabled field
   def getEnabledValue() {
      enabled
   }

   // this provides 'enabled' keyword that could be used at the end of the line
   def getEnabled() {
      enabled = true
      this
   }

   // this provides 'disabled' keyword that could be used at the end of the line
   def getDisabled() {
      enabled = false
      this
   }

   org.perfcake.reporting.destinations.Destination buildDestination() {
      def props = new Properties()
      props.putAll(properties)
      ObjectFactory.summonInstance(className.contains('.') ?: ScenarioFactory.DEFAULT_DESTINATION_PACKAGE + '.' + className, props)
   }
}

class Message extends PropertiesBacked {

   def uri
   def multiplicity = 1
   def content
   def validators = []
   def headers = [:]

   def send(int multiplicity) {
      this.multiplicity = multiplicity
   }

   // validator ids can be specified as a list (handled later) or as a string separated by commas or semicolons
   def validate(String validator) {
      this.validators.addAll(validator.tokenize(" ,;\t\n\r\f"))
      this
   }

   // specify headers as a map
   def headers(def headers) {
      this.headers = headers
      this
   }

   // specify a single header value (actually more values can be set at a time and nothing breaks)
   def header(def header) {
      this.headers.putAll(header)
      this
   }

   String toString() {
      "Message: {uri: $uri, content: '$content', multiplicity: $multiplicity, headers: $headers, validators: $validators, ${super.toString()}}"
   }

   def MessageTemplate buildMessageTemplate() {
      if (uri && content) {
         throw new PerfCakeException("Both 'uri' and 'content' cannot be set at the same time on message ${this.toString()}.")
      }

      // if validator ids were specified as a list, they ended up in the object properties
      if (properties.get('validate')) {
         validators.addAll(properties.get('validate'))
         properties.remove('validate')
      }

      Properties props = new Properties()
      props.putAll(properties)
      Properties head = new Properties()
      if (headers) {
         head.putAll(headers)
      }

      def payload = null

      if (uri) {
         def url = Utils.locationToUrl(uri, PerfCakeConst.MESSAGES_DIR_PROPERTY, Utils.determineDefaultLocation("messages"), "")
         payload = Utils.readFilteredContent(url)
      } else if (content) {
         payload = content
      }

      org.perfcake.message.Message m = new org.perfcake.message.Message(payload)
      m.setProperties(props)
      m.setHeaders(head)

      new MessageTemplate(m, multiplicity, validators)
   }
}

class Validation {
   def enabled = true
   def fastForward = false

   String toString() {
      "Validation: {enabled: $enabled, fastForward: $fastForward}"
   }

   def getFast() {
      fastForward = true
      this
   }

   // getEnabled is already used for something else, this method is needed to actually obtain the value of the enabled field
   def getEnabledValue() {
      enabled
   }

   // this provides 'enabled' keyword that could be used at the end of the line
   def getEnabled() {
      enabled = true
      this
   }

   // this provides 'disabled' keyword that could be used at the end of the line
   def getDisabled() {
      enabled = false
      this
   }
}

class Validator extends ObjectWithClassName {

   def id

   Validator(def className) {
      super(className)
   }

   String toString() {
      "Validator: {id: $id, ${super.toString()}}"
   }

   def id(def id) {
      this.id = id
      this
   }

   org.perfcake.validation.MessageValidator buildValidator() {
      def props = new Properties()
      props.putAll(properties)
      ObjectFactory.summonInstance(className.contains('.') ?: ScenarioFactory.DEFAULT_VALIDATION_PACKAGE + '.' + className, props)
   }
}

/**
 * Parent script for all the DSL scenarios which defines some built-in methods.
 */
abstract class BaseDslScriptClass extends Script {

   // set the metaclasses for both integers and longs (large numbers are likely to be used in the configuration)
   static {
      Integer.metaClass.getMs = {-> new Time((Integer) delegate, TimeUnit.millisecond)}
      Integer.metaClass.getS = {-> new Time((Integer) delegate, TimeUnit.second)}
      Integer.metaClass.getM = {-> new Time((Integer) delegate, TimeUnit.minute)}
      Integer.metaClass.getH = {-> new Time((Integer) delegate, TimeUnit.hour)}
      Integer.metaClass.getD = {-> new Time((Integer) delegate, TimeUnit.day)}
      Integer.metaClass.getIterations = {-> new Iterations((Integer) delegate)}
      Integer.metaClass.getIteration = {-> new Iterations((Integer) delegate)}
      Integer.metaClass.getPercents = {-> new Percents((Integer) delegate)}
      Integer.metaClass.getPercent = {-> new Percents((Integer) delegate)}
      Integer.metaClass.getThreads = {-> delegate}
      Integer.metaClass.getThread = {-> delegate}
      Integer.metaClass.getTimes = {-> delegate}

      Long.metaClass.getMs = {-> new Time((Long) delegate, TimeUnit.millisecond)}
      Long.metaClass.getS = {-> new Time((Long) delegate, TimeUnit.second)}
      Long.metaClass.getM = {-> new Time((Long) delegate, TimeUnit.minute)}
      Long.metaClass.getH = {-> new Time((Long) delegate, TimeUnit.hour)}
      Long.metaClass.getD = {-> new Time((Long) delegate, TimeUnit.day)}
      Long.metaClass.getIterations = {-> new Iterations((Long) delegate)}
      Long.metaClass.getIteration = {-> new Iterations((Long) delegate)}
      Long.metaClass.getThreads = {-> delegate}
      Long.metaClass.getThread = {-> delegate}
      Long.metaClass.getTimes = {-> delegate}
   }

   // all missing method calls are stored as generic scenario properties
   def methodMissing(String name, args) {
      def scenario = this.binding.scenario
      scenario.properties[name] = args[0]

      scenario
   }

   // pass the call to the DSL scenario
   def generator(def className) {
      scenario.generator(className)
   }

   def sequence(def className) {
      scenario.sequence(className)
   }

   // pass the call to the DSL scenario
   def sender(def className) {
      scenario.sender(className)
   }

   // pass the call to the DSL scenario
   def reporter(def className) {
      scenario.reporter(className)
   }

   // pass the call to the DSL scenario
   def destination(def className) {
      scenario.destination(className)
   }

   // pass the call to the DSL scenario
   def message(def location) {
      scenario.message(location)
   }

   // pass the call to the DSL scenario
   def validation(int enabled) {
      scenario.validation(enabled == 101, false)
   }

   // pass the call to the DSL scenario
   def validation(String fast) {
      scenario.validation(true, true)
   }

   // this provides the 'validation' keyword
   def getValidation() {
      scenario.validation(true, false)
   }

   // this provides the 'sequences' keyword
   def getSequences() {
      scenario
   }

   // this provides a generic 'enabled' keyword in the scenario, used for validation configuration,
   // we want to distinguish between regular booleans and method calls and this one, this is why we use the number
   def getEnabled() {
      101
   }

   // this provides a generic 'disabled' keyword in the scenario, used for validation configuration,
   // we want to distinguish between regular booleans and method calls and this one, this is why we use the number
   def getDisabled() {
      100
   }

   // this provides a generic 'fast' keyword in the scenario, used for validation configuration,
   // we want to distinguish between regular booleans and method calls and this one, this is why we use the string value
   def getFast() {
      "fast"
   }

   // pass the call to the DSL scenario
   def validator(def className) {
      scenario.validator(className)
   }

   // pass the call to the DSL scenario
   def run(Time time) {
      scenario.run(time)
   }

   // pass the call to the DSL scenario
   def run(Iterations iterations) {
      scenario.run(iterations)
   }

   // this provides the end keyword which is now voluntary
   def getEnd() {
      scenario
   }
}

/**
 * Specification of a time unit.
 */
enum TimeUnit {
   millisecond('ms', 1L),
   second('s', 1000L),
   minute('m', 1000L * 60L),
   hour('h', 1000L * 60L * 60L),
   day('d', 1000L * 60L * 60L * 24L)

   final String abbreviation
   final Long multiplier

   TimeUnit(String abbreviation, Long multiplier) {
      this.abbreviation = abbreviation
      this.multiplier = multiplier
   }

   String toString() {
      abbreviation
   }
}

/**
 * Binds time unit and a value.
 */
@TupleConstructor
class Time {
   Long amount
   TimeUnit unit

   String toString() {
      "$amount$unit"
   }

   def getMs() {
      amount * unit.multiplier
   }
}

/**
 * Binds value with percents.
 */
@TupleConstructor
class Percents {
   Integer amount

   String toString() {
      "$amount%"
   }
}

/**
 * Binds value with iterations.
 */
@TupleConstructor
class Iterations {
   Long amount

   String toString() {
      "$amount iterations"
   }
}

// creates new scenario upon request
def getNewScenario() {
   scenario = new DslScenario()
   scenario
}

// configuration of the script compiler, automatic imports and security
def configuration = new CompilerConfiguration()

def imports = new ImportCustomizer()
imports.addImport('org.perfcake.scenario.dsl.DslScenario')
imports.addImport('org.perfcake.scenario.dsl.Time')
imports.addImport('org.perfcake.scenario.dsl.TimeUnit')
imports.addStaticStars('java.lang.Math')

def secure = new SecureASTCustomizer()
secure.with {
   closuresAllowed = false
   methodDefinitionAllowed = false
   importsWhitelist = ['org.perfcake.scenario.dsl.DslScenario', 'org.perfcake.scenario.dsl.Time', 'org.perfcake.scenario.dsl.TimeUnit']
   staticImportsWhitelist = []
   staticStarImportsWhitelist = ['java.lang.Math']
   tokensWhitelist = [PLUS, MINUS, MULTIPLY, DIVIDE, MOD, POWER, PLUS_PLUS, MINUS_MINUS,
                      COMPARE_EQUAL, COMPARE_NOT_EQUAL, COMPARE_LESS_THAN, COMPARE_LESS_THAN_EQUAL,
                      COMPARE_GREATER_THAN, COMPARE_GREATER_THAN_EQUAL].asImmutable()
   constantTypesClassesWhiteList = [Integer, Float, Long, Double, BigDecimal, Byte, Boolean, Object, String, Time, TimeUnit,
                                    Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Byte.TYPE, Boolean.TYPE].asImmutable()
   receiversClassesWhiteList = [Math, Integer, Float, Double, Long, BigDecimal, Byte, Boolean, Object, String, Time, TimeUnit,
                                Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Byte.TYPE, Boolean.TYPE].asImmutable()
}

configuration.addCompilationCustomizers(imports, secure)
configuration.scriptBaseClass = BaseDslScriptClass.getName() // set the script parent class

def scenarioBinding = new Binding([
      scenario: getNewScenario(), // 'scenario' keyword is bound to the request for a new DSL scenario, therefore the keyword is mandatory
])

// evaluate the script, the return value is ignored, we have the link to the DSL scenario object anyway
def shell = new GroovyShell(scenarioBinding, configuration)
shell.evaluate(dslScript as String)

// print out the completely parsed scenario
def log = LogManager.getLogger(ScenarioDelegate.class)
if (log.isDebugEnabled()) {
   log.debug(scenario.toString())
}

// return PerfCake scenario made of the DSL one
return scenario.buildScenario()