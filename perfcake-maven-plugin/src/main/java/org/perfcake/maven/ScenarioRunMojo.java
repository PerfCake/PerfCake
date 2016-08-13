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
package org.perfcake.maven;

import org.perfcake.PerfCakeConst;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

/**
 * Maven plugin enabling execution of PerfCake scenarios.
 *
 * @author vjuranek
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Mojo(name = "scenario-run", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class ScenarioRunMojo extends AbstractMojo {

   private static final String PERFCAKE_MAVEN_COORD = "org.perfcake:perfcake:";
   private static final String PERFCAKE_DIR = "perfcake";
   private static final String DEFAULT_SCENARIOS_DIR = PERFCAKE_DIR + File.separator + "scenarios";
   private static final String DEFAULT_MESSAGES_DIR = PERFCAKE_DIR + File.separator + "messages";
   private static final String DEFAULT_PLUGINS_DIR = PERFCAKE_DIR + File.separator + "plugins";

   @Parameter(required = true)
   private String scenario;

   @Parameter(alias = "scenarios-dir")
   private String scenariosDir;

   @Parameter(alias = "messages-dir")
   private String messagesDir;

   @Parameter(alias = "plugins-dir")
   private String pluginsDir;

   @Parameter(alias = "properties-file")
   private String propertiesFile;

   @Parameter(alias = "log-level")
   private String logLevel = "";

   @Parameter(alias = "use-test-resources")
   private Boolean useTestResources = true;

   @Parameter(defaultValue = "${project}", readonly = true)
   private MavenProject project;

   @Override
   public void execute() throws MojoExecutionException {
      initDefaults();

      final Properties perfCakeProperties = new Properties();

      if (logLevel != null && !"".equals(logLevel)) {
         perfCakeProperties.setProperty(PerfCakeConst.LOGGING_LEVEL_PROPERTY, logLevel);
      }

      perfCakeProperties.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, messagesDir);
      perfCakeProperties.setProperty(PerfCakeConst.PLUGINS_DIR_PROPERTY, pluginsDir);
      perfCakeProperties.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, scenariosDir);

      if (propertiesFile != null && !"".equals(propertiesFile)) {
         perfCakeProperties.setProperty(PerfCakeConst.PROPERTIES_FILE_PROPERTY, propertiesFile);
      }

      try {
         Class<?> se = getClass().getClassLoader().loadClass("org.perfcake.ScenarioExecution");
         Method main = se.getMethod("execute", String[].class);
         Object[] argsArray = { scenario, perfCakeProperties };
         getLog().info("PerfCake Maven Plugin: Running scenario " + scenario);
         main.invoke(null, argsArray);
         getLog().info("PerfCake Maven Plugin: Finished scenario " + scenario);
      } catch (Exception e) {
         throw new MojoExecutionException(e.getMessage(), e);
      }
   }

   private void initDefaults() {
      String resPath = getDefaultResourcePath();

      if (scenariosDir == null || scenariosDir.trim().isEmpty()) {
         File testDir = new File(resPath + File.separator + DEFAULT_SCENARIOS_DIR);
         scenariosDir = testDir.isDirectory() ? testDir.getAbsolutePath() : resPath;
         getLog().debug("Setting PerCake scenarios dir to " + scenariosDir);
      }

      if (messagesDir == null || messagesDir.trim().isEmpty()) {
         File testDir = new File(resPath + File.separator + DEFAULT_MESSAGES_DIR);
         messagesDir = testDir.isDirectory() ? testDir.getAbsolutePath() : resPath;
         getLog().debug("Setting PerCake messages dir to " + messagesDir);
      }

      if (pluginsDir == null || pluginsDir.trim().isEmpty()) {
         File testDir = new File(resPath + File.separator + DEFAULT_PLUGINS_DIR);
         pluginsDir = testDir.isDirectory() ? testDir.getAbsolutePath() : resPath;
         getLog().debug("Setting PerCake plugins dir to " + pluginsDir);
      }
   }

   private String getDefaultResourcePath() {
      String defResPath = null;
      List<Resource> defResList;
      if (useTestResources) {
         defResList = project.getBuild().getTestResources();
      } else {
         defResList = project.getBuild().getResources();
      }
      if (!defResList.isEmpty() && defResList.get(0) != null) {
         defResPath = defResList.get(0).getDirectory();
      }
      return defResPath;
   }
}