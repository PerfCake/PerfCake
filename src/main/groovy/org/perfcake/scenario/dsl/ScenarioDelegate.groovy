package org.perfcake.scenario.dsl

import static org.codehaus.groovy.syntax.Types.*

import org.perfcake.PerfCakeConst
import org.perfcake.PerfCakeException
import org.perfcake.common.Period
import org.perfcake.common.PeriodType
import org.perfcake.message.MessageTemplate
import org.perfcake.message.generator.AbstractMessageGenerator
import org.perfcake.message.sender.MessageSender
import org.perfcake.scenario.Scenario
import org.perfcake.scenario.ScenarioBuilder
import org.perfcake.scenario.ScenarioFactory
import org.perfcake.util.ObjectFactory
import org.perfcake.util.Utils

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.customizers.SecureASTCustomizer

import groovy.transform.TupleConstructor
import java.nio.file.Files
import java.nio.file.Paths

class PropertiesBacked {
   def properties = [:]

   String toString() {
      "Properties: " + properties
   }

   def methodMissing(String name, args) {
      println "scenario missing method $name $args"
      if (args.length == 1) {
         properties[name] = args[0]
      } else {
         properties[name] = args
      }
      this
   }
}

class DslScenario extends PropertiesBacked {
   def description
   def generator
   def sender
   def reporters = []
   def messages = []
   def validation
   def validators = []
   def runInfo

   String toString() {
      "DslScenario {description: $description, \n" +
            "   $runInfo, \n" +
            "   $generator, \n" +
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
      if (name == "call") {
         this.description = args[0]
      } else {
         return super.methodMissing(name, args)
      }
      this
   }

   def generator(def className) {
      this.generator = new Generator(className)
      this.generator
   }

   def sender(def className) {
      this.sender = new Sender(className)
      this.sender
   }

   def reporter(def className) {
      def r = new Reporter(className)
      this.reporters.add(r)
      r
   }

   def destination(def className) {
      this.reporters.last().destination(className)
   }

   def message(def location) {
      def m = new Message()
      this.messages.add(m)
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

   def validation(boolean enabled, boolean fast) {
      this.validation = new Validation()
      this.validation.enabled = enabled
      this.validation.fastForward = fast
      this.validation
   }

   def validator(def className) {
      def v = new Validator(className)
      this.validators.add(v)
      v
   }

   def run(Time time) {
      this.runInfo = new RunInfo(time)
      this.runInfo
   }

   def run(Iterations iterations) {
      this.runInfo = new RunInfo(iterations)
      this.runInfo
   }

   def Scenario buildScenario() {
      ScenarioBuilder builder = new ScenarioBuilder(runInfo.buildRunInfo(), generator.buildMessageGenerator(), sender.buildMessageSender())
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
            validatorIds.add(it.id)
            builder.putMessageValidator(it.id, it.buildValidator())
         }
      }


      if (messages) {
         messages.each {
            builder.addMessage(it.buildMessageTemplate())

            // this must be done after building the message, as array of validators may need to be obtained
            // from properties during the message template creation
            it.validators.each { validatorId ->
               if (!validatorIds.contains(validatorId)) {
                  throw new PerfCakeException("Reference '$validatorId' to non-existing validator.")
               }
            }
         }
      }

      Scenario s = builder.build()
      s.getValidationManager().setFastForward(validation.fastForward)
      s.getValidationManager().setEnabled(validation.getEnabledValue())

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

   def with(Long threads) {
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

class ObjectWithClassName extends PropertiesBacked {
   def className

   ObjectWithClassName(def className) {
      this.className = className
   }

   String toString() {
      "class: $className, ${super.toString()}"
   }
}

class Generator extends ObjectWithClassName {

   Generator(def className) {
      super(className)
   }

   String toString() {
      "Generator: {${super.toString()}}"
   }

   AbstractMessageGenerator buildMessageGenerator() {
      def props = new Properties()
      props.putAll(properties)

      ObjectFactory.summonInstance(className.contains('.') ?: ScenarioFactory.DEFAULT_GENERATOR_PACKAGE + '.' + className, props)
   }
}

class Sender extends ObjectWithClassName {

   Sender(def className) {
      super(className)
   }

   String toString() {
      "Sender: {${super.toString()}}"
   }

   MessageSender buildMessageSender() {
      def props = new Properties()
      props.putAll(properties)

      ObjectFactory.summonInstance(className.contains('.') ?: ScenarioFactory.DEFAULT_SENDER_PACKAGE + '.' + className, props)
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

   def getEnabled() {
      enabled = true
      this
   }

   def getDisabled() {
      enabled = false
      this
   }

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

   def getEnabledValue() {
      enabled
   }

   def getEnabled() {
      enabled = true
      this
   }

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

   def validate(String validator) {
      this.validators.addAll(validator.tokenize(" ,;\t\n\r\f"))
      this
   }

   def headers(def headers) {
      this.headers = headers
      this
   }

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

   def getEnabledValue() {
      enabled
   }

   def getEnabled() {
      enabled = true
      this
   }

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

abstract class BaseDslScriptClass extends Script {

   static {
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

   def methodMissing(String name, args) {
      println "script missing method $name $args"

      def scenario = this.binding.scenario
      scenario.properties[name] = args[0]

      scenario
   }

   def generator(def className) {
      scenario.generator(className)
   }

   def sender(def className) {
      scenario.sender(className)
   }

   def reporter(def className) {
      scenario.reporter(className)
   }

   def destination(def className) {
      scenario.destination(className)
   }

   def message(def location) {
      scenario.message(location)
   }

   def validation(int enabled) {
      scenario.validation(enabled == 101, false)
   }

   def validation(String fast) {
      scenario.validation(true, true)
   }

   def getValidation() {
      scenario.validation(true, false)
   }

   def getEnabled() {
      101
   }

   def getDisabled() {
      100
   }

   def getFast() {
      "fast"
   }

   def validator(def className) {
      scenario.validator(className)
   }

   def run(Time time) {
      scenario.run(time)
   }

   def run(Iterations iterations) {
      scenario.run(iterations)
   }

   def getEnd() {
      scenario
   }
}

enum TimeUnit {
   millisecond('ms', 1L),
   second('s', 1000L),
   minute('m', 1000 * 60L),
   hour('h', 1000 * 60 * 60L),
   day('d', 1000 * 60 * 60 * 24L)

   String abbreviation
   Long multiplier

   TimeUnit(String abbreviation, Long multiplier) {
      this.abbreviation = abbreviation
      this.multiplier = multiplier
   }

   String toString() {
      abbreviation
   }
}

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

@TupleConstructor
class Percents {
   Integer amount

   String toString() {
      "$amount%"
   }
}

@TupleConstructor
class Iterations {
   Long amount

   String toString() {
      "$amount iterations"
   }
}

//   DslScenario.metaClass.getMetaProperty(name).setProperty(scenario, args)

def getNewScenario() {
   scenario = new DslScenario()
   scenario
}

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
configuration.scriptBaseClass = BaseDslScriptClass.getName()

def scenarioBinding = new Binding([
      scenario: getNewScenario(),
])

def shell = new GroovyShell(scenarioBinding, configuration)
shell.evaluate(dslFileName as File)

println scenario.buildScenario()

return scenario