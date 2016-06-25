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
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class ElasticsearchDestinationTest extends TestSetup {

   private static final String[][] CORRECT_RESULTS = {
         { "http://localhost:9292/perfcake", "POST", "{ \"mappings\": { \"results\": {\"properties\" : { \"time\" : {\"type\" : \"date\", \"format\" : \"epoch_millis\"}, \"rt\" : {\"type\" : \"date\", \"format\" : \"epoch_millis\"} } } } }" },
         { "http://localhost:9292/perfcake/results", "POST" },
         { "http://localhost:9292/perfcake/results", "POST" },
         { "http://localhost:9292/perfcake/results", "POST" },
   };

   @Test
   public void elasticsearchTest() throws PerfCakeException {
      final Vertx vertx = Vertx.vertx();
      final HttpServer server = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      final List<List<String>> results = new ArrayList<>();
      router.route("/*").handler(BodyHandler.create());
      router.post("/*").handler((context) -> {
         results.add(Arrays.asList(context.request().absoluteURI(), context.request().method().toString(), context.getBodyAsString()));
         context.response().setStatusCode(200).end();
      });
      new Thread(() -> server.requestHandler(router::accept).listen(9292)).start();

      final Scenario scenario = ScenarioLoader.load("test-elastic");

      ScenarioRetractor sr = new ScenarioRetractor(scenario);
      sr.getReportManager();

      scenario.init();
      scenario.run();
      scenario.close();

      server.close();

      Assert.assertEquals(results.size(), CORRECT_RESULTS.length);
      for (int i = 0; i < 2; i++) {
         for (int j = 0; j < CORRECT_RESULTS.length; j++) {
            Assert.assertEquals(results.get(j).get(i), CORRECT_RESULTS[j][i]);
         }
      }

      ResultData res = Json.decodeValue(results.get(1).get(2).replace("Result", "result"), ResultData.class);
      Assert.assertEquals(res.iteration, 0);
      Assert.assertEquals(res.percentage, 10);
      Assert.assertEquals(res.tags, new String[] { "tag1", "tag2" });
      Assert.assertEquals(res.warmUp, false);
      Assert.assertTrue(res.time > 0);
      Assert.assertTrue(res.rt > 0);
      Assert.assertTrue(res.result > 0);

      res = Json.decodeValue(results.get(2).get(2).replace("Result", "result"), ResultData.class);
      Assert.assertEquals(res.iteration, 4);
      Assert.assertEquals(res.percentage, 50);
      Assert.assertEquals(res.tags, new String[] { "tag1", "tag2" });
      Assert.assertEquals(res.warmUp, false);
      Assert.assertTrue(res.time > 0);
      Assert.assertTrue(res.rt > 0);
      Assert.assertTrue(res.result > 0);

      res = Json.decodeValue(results.get(3).get(2).replace("Result", "result"), ResultData.class);
      Assert.assertEquals(res.iteration, 9);
      Assert.assertEquals(res.percentage, 100);
      Assert.assertEquals(res.tags, new String[] { "tag1", "tag2" });
      Assert.assertEquals(res.warmUp, false);
      Assert.assertTrue(res.time > 0);
      Assert.assertTrue(res.rt > 0);
      Assert.assertTrue(res.result > 0);
   }

   private static class ResultData {
      private long iteration;
      private long time;
      private int percentage;
      private long rt;
      private String[] tags;
      private boolean warmUp;
      private double result;

      public ResultData() {
      }

      public ResultData(final long iteration, final long time, final int percentage, final long rt, final String[] tags, final boolean warmUp, final double result) {
         this.iteration = iteration;
         this.time = time;
         this.percentage = percentage;
         this.rt = rt;
         this.tags = tags;
         this.warmUp = warmUp;
         this.result = result;
      }

      public long getIteration() {
         return iteration;
      }

      public void setIteration(final long iteration) {
         this.iteration = iteration;
      }

      public long getTime() {
         return time;
      }

      public void setTime(final long time) {
         this.time = time;
      }

      public int getPercentage() {
         return percentage;
      }

      public void setPercentage(final int percentage) {
         this.percentage = percentage;
      }

      public long getRt() {
         return rt;
      }

      public void setRt(final long rt) {
         this.rt = rt;
      }

      public String[] getTags() {
         return tags;
      }

      public void setTags(final String[] tags) {
         this.tags = tags;
      }

      public boolean isWarmUp() {
         return warmUp;
      }

      public void setWarmUp(final boolean warmUp) {
         this.warmUp = warmUp;
      }

      public double getResult() {
         return result;
      }

      public void setResult(final double result) {
         this.result = result;
      }

      public String toString() {
         return Json.encodePrettily(this);
      }
   }

}