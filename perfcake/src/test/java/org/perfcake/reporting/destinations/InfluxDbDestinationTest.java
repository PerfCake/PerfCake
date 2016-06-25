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
package org.perfcake.reporting.destinations;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;
import org.perfcake.scenario.ScenarioRetractor;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class InfluxDbDestinationTest extends TestSetup {

   @Test
   public void influxDbTest() throws PerfCakeException {
      final Vertx vertx = Vertx.vertx();
      final HttpServer server = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      final List<List<String>> results = new ArrayList<>();
      router.route("/*").handler(BodyHandler.create());
      router.route("/*").handler((context) -> {
         results.add(Arrays.asList(context.request().absoluteURI(), context.request().method().toString(), context.getBodyAsString()));
         context.response().setStatusCode(200).end("{\"results\":[{\"series\":[{\"name\":\"databases\",\"columns\":[\"name\"],\"values\":[[\"mydb\"]]}]}]}");
      });
      new Thread(() -> server.requestHandler(router::accept).listen(8086)).start();

      final Scenario scenario = ScenarioLoader.load("test-influxdb");

      ScenarioRetractor sr = new ScenarioRetractor(scenario);
      sr.getReportManager();

      scenario.init();
      scenario.run();
      scenario.close();

      server.close();

      Assert.assertEquals(results.size(), 2); // batch update, all data are in the second entry

      // create database
      Assert.assertEquals(results.get(0).get(0), "http://localhost:8086/query?u=admin&p=abc123&q=CREATE%20DATABASE%20IF%20NOT%20EXISTS%20%22perfcake%22");
      Assert.assertEquals(results.get(0).get(1), "GET");

      // send measurements
      Assert.assertEquals(results.get(1).get(0), "http://localhost:8086/write?u=admin&p=abc123&db=perfcake&rp=default&precision=n&consistency=one");
      Assert.assertEquals(results.get(1).get(1), "POST");

      final List<String> records = new ArrayList<>(Arrays.asList(results.get(1).get(2).split("results ")));
      records.remove(0); // first string is empty because the results start with "results "

      Assert.assertEquals(records.size(), 3);

      // let's verify the stable parts of the entries
      records.forEach(record -> {
         Assert.assertTrue(record.startsWith("Result="));
         Assert.assertTrue(record.contains(",iteration="));
         Assert.assertTrue(record.contains("i,percentage="));
         Assert.assertTrue(record.contains("i,tags=\"[\\\"tag1\\\",\\\"tag2\\\"]\",time="));
         Assert.assertTrue(record.contains("i,warmUp=\"false\" "));
         Assert.assertTrue(record.endsWith("000000\n"));
      });
   }
}