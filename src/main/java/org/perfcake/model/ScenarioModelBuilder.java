package org.perfcake.model;

import org.apache.log4j.Logger;

/**
 * 
 * @author Jiri Sedlacek <jiri@sedlackovi.cz>
 * 
 */
public class ScenarioModelBuilder {

   public static final Logger log = Logger.getLogger(ScenarioModelBuilder.class);
   
   private ScenarioModel model;

   public ScenarioModelBuilder() {
      model = new ScenarioModel();
   }

   public ScenarioModelBuilder addProperties(ScenarioModel.Properties properties) {
      model.setProperties(properties);
      return this;
   }

   public ScenarioModelBuilder addGenerator(ScenarioModel.Generator g) {
      model.setGenerator(g);
      return this;
   }

   public ScenarioModelBuilder addSender(ScenarioModel.Sender s) {
      model.setSender(s);
      return this;
   }

   public ScenarioModelBuilder addReporting(ScenarioModel.Reporting r) {
      model.setReporting(r);
      return this;
   }

   public ScenarioModelBuilder addMessages(ScenarioModel.Messages m) {
      model.setMessages(m);
      return this;
   }

   public ScenarioModelBuilder addValidation(ScenarioModel.Validation v) {
      model.setValidation(v);
      return this;
   }
   
   public ScenarioModel build() {
      return model;
   }
   
   public ScenarioModelBuilder enabled(boolean enabled) {
      model.setEnabled(enabled);
      return this;
   }
}
