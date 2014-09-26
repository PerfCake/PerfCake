package org.perfcake.scenario.dsl

import groovy.transform.TupleConstructor
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.customizers.SecureASTCustomizer

import static org.codehaus.groovy.syntax.Types.*

class PropertiesBacked {
   def properties = [:]

   String toString() {
      return "Properties: " + properties
   }

   def methodMissing(String name, args) {
      println "scenario missing method $name $args"
      properties[name] = args[0]
      return this
   }
}

class DslScenario extends PropertiesBacked {
   def description
   def generator
   def runInfo

   String toString() {
      return "DslScenario {description: $description, RunInfo: $runInfo, Generator: [$generator], ${super.toString()}}"
   }

   def methodMissing(String name, args) {
      if (name == "call") {
         this.description = args[0]
      } else {
         return super.methodMissing(name, args)
      }
      return this
   }

/*   def propertyMissing(String name, value) {
      println "scenario missing prop $name $value"
      properties[name] = value
      return this
   }*/
//   def propertyMissing(String name) { properties[name] }

   def generator(def className) {
      this.generator = new Generator(className)
      return this.generator
   }

   def run(Time time) {
      this.runInfo = new RunInfo(time)
      return this.runInfo
   }

   def run(Long iterations) {
      this.runInfo = new RunInfo(iterations)
      return this.runInfo
   }
}

class RunInfo extends PropertiesBacked {
   def time
   def iterations
   def threads

   RunInfo(Time time) {
      this.time = time.amount * time.unit.multiplier
   }

   RunInfo(Long iterations) {
      this.iterations = iterations
   }

   def with(Long threads) {
      this.threads = threads
      return this
   }

   String toString() {
      return "RunInfo: {" + (time ? "$time ms" : "$iterations iterations") + " with $threads threads}"
   }
}

class Generator extends PropertiesBacked {
   def className

   Generator(className) {
      this.className = className
   }

   String toString() {
      return "Generator: {class: $className, ${super.toString()}}"
   }
}

abstract class BaseDslScriptClass extends Script {

   static {
      Integer.metaClass.getS = { -> new Time((Long) delegate, TimeUnit.second) }
      Integer.metaClass.getM = { -> new Time((Long) delegate, TimeUnit.minute) }
      Integer.metaClass.getH = { -> new Time((Long) delegate, TimeUnit.hour) }
      Integer.metaClass.getD = { -> new Time((Long) delegate, TimeUnit.day) }
      Integer.metaClass.getIterations = { -> delegate }
      Integer.metaClass.getPercents = { -> delegate }
      Integer.metaClass.getPercent = { -> delegate }
      Integer.metaClass.getThreads = { -> delegate }
      Integer.metaClass.getThread = { -> delegate }
   }

   def methodMissing(String name, args) {
      println "script missing method $name $args"

      def scenario = this.binding.scenario
      scenario.properties[name] = args[0]

      return scenario
   }

   def generator(def className) {
      return scenario.generator(className)
   }

   def run(Time time) {
      return scenario.run(time)
   }

   def run(Long iterations) {
      return scenario.run(iterations)
   }

   def getEnd() {
      return scenario
   }
}

enum TimeUnit {
   millisecond ('ms', 1L),
   second ('s', 1000L),
   minute ('m', 1000 * 60L),
   hour ('h', 1000 * 60 * 60L),
   day ('d', 1000 * 60 * 60 * 24L)

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
}

//   DslScenario.metaClass.getMetaProperty(name).setProperty(scenario, args)

def getNewScenario() {
   scenario = new DslScenario()
   scenario
}

Integer.metaClass.getShares = { -> delegate }
Integer.metaClass.of = { instrument -> [instrument, delegate] }

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
return shell.evaluate(dslFileName as File)