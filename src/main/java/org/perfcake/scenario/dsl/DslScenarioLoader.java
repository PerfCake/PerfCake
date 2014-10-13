package org.perfcake.scenario.dsl;

import org.perfcake.scenario.Scenario;

import groovy.lang.Binding;
import org.codehaus.groovy.control.CompilationFailedException;

import java.io.IOException;

public class DslScenarioLoader {
   public static void main(String[] args) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException {

      final Binding binding = new Binding();
      binding.setProperty("dslFileName", "src/main/resources/scenarios/my_scenario.dsl");

      final ScenarioDelegate scenarioDelegate = new ScenarioDelegate();
      scenarioDelegate.setBinding(binding);
      final Scenario dsl = (Scenario) scenarioDelegate.run();

      System.out.println(dsl);
   }
}
