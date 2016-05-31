package org.perfcake.reporting.destinations.c3chart;

import org.perfcake.PerfCakeConst;
import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.destinations.ChartDestination;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Test(groups = { "unit" })
public class C3ChartTest extends TestSetup {

   @Test
   public void basicTest() throws Exception {
      Random r = new Random();
      ChartDestination dst = new ChartDestination();
      dst.setAttributes("Result, Average, warmUp");
      dst.setXAxis("Time");
      dst.setYAxis("Throughput msgs/sec");
      dst.setGroup("speedGroup");
      dst.setName("My throughput");
      dst.setxAxisType(PeriodType.TIME);

      String tempDir = TestSetup.createTempDir("test-chart");
      dst.setOutputDir(tempDir);

      dst.open();

      Measurement m;
      Double d, avg = 0d;
      boolean warmUp = true;

      for (int i = 1, j = 1; i <= 100; i++, j++) {
         if (warmUp && r.nextInt(100 - i) < 10) {
            warmUp = false;
            j = 1;
         }

         m = new Measurement(j * 10, j * 1000, (j * 100) + r.nextInt(100));
         d = r.nextDouble() * 100d;
         avg = avg + d;
         m.set(new Quantity<>(d, "msgs/s"));
         m.set("Average", new Quantity<>(avg / i, "msgs/s"));
         m.set("warmUp", warmUp);
         dst.report(m);
      }

      dst.close();

      final Path dir = Paths.get(tempDir);

      Assert.assertTrue(dir.resolve(Paths.get("data", "speedGroup" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".js")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "speedGroup" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".json")).toFile().exists());
      Assert.assertTrue(dir.resolve(Paths.get("data", "speedGroup" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY) + ".html")).toFile().exists());

      final C3ChartData data1 = new C3ChartData("speedGroup" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY), dir);
      Assert.assertEquals(data1.getData().get(0).size(), 5);

      final C3ChartDataFile desc = new C3ChartDataFile(dir, "speedGroup" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY));
      Assert.assertEquals(desc.getChart().getBaseName(), "speedGroup" + System.getProperty(PerfCakeConst.NICE_TIMESTAMP_PROPERTY));
      Assert.assertEquals(desc.getChart().getName(), dst.getName());
      Assert.assertEquals(desc.getChart().getGroup(), dst.getGroup());
      Assert.assertEquals(desc.getChart().getxAxis(), dst.getxAxis());
      Assert.assertEquals(desc.getChart().getyAxis(), dst.getyAxis());
      Assert.assertEquals(desc.getChart().getxAxisType(), dst.getxAxisType());
   }

   @Test
   public void testDataReadAndMixture() throws PerfCakeException, URISyntaxException {
      final Path dataPath = Paths.get(getClass().getResource("/c3chart").toURI());
      final C3ChartData data1 = new C3ChartData("speedGroup20160501143319", dataPath);
      final C3ChartData data2 = new C3ChartData("speedGroup20160501143748", dataPath);

      C3ChartData data3 = data1.filter(3).combineWith(data2.filter(3));

      Assert.assertEquals(data3.getData().size(), 10);

      Assert.assertEquals(data3.getData().get(0).getLong(0), Long.valueOf(1000));
      Assert.assertTrue(data3.getData().get(0).getDouble(1) > 77d); // just make sure there is some value
      Assert.assertTrue(data3.getData().get(0).getDouble(2) > 20d); // just make sure there is some value

      Assert.assertEquals(data3.getData().get(2).getLong(0), Long.valueOf(3000));
      Assert.assertTrue(data3.getData().get(2).getDouble(1) > 84d); // just make sure there is some value
      Assert.assertTrue(data3.getData().get(2).getDouble(2) > 10d); // just make sure there is some value

      Assert.assertEquals(data3.getData().get(3).getLong(0), Long.valueOf(4000));
      Assert.assertNull(data3.getData().get(3).getValue(1));
      Assert.assertTrue(data3.getData().get(3).getDouble(2) > 10d); // just make sure there is some value

      Assert.assertEquals(data3.getData().get(9).getLong(0), Long.valueOf(10000));
      Assert.assertNull(data3.getData().get(9).getValue(1));
      Assert.assertTrue(data3.getData().get(9).getDouble(2) > 75d); // just make sure there is some value

      data3 = data1.filter(1).combineWith(data2.filter(1));

      Assert.assertEquals(data3.getData().size(), 97);

      Assert.assertEquals(data3.getData().get(0).getLong(0), Long.valueOf(1000));
      Assert.assertTrue(data3.getData().get(0).getDouble(1) > 44d); // just make sure there is some value
      Assert.assertTrue(data3.getData().get(0).getDouble(2) > 33d); // just make sure there is some value

      Assert.assertEquals(data3.getData().get(89).getLong(0), Long.valueOf(90000));
      Assert.assertTrue(data3.getData().get(89).getDouble(1) > 12d); // just make sure there is some value
      Assert.assertTrue(data3.getData().get(89).getDouble(2) > 31d); // just make sure there is some value

      Assert.assertEquals(data3.getData().get(90).getLong(0), Long.valueOf(91000));
      Assert.assertTrue(data3.getData().get(90).getDouble(1) > 89d); // just make sure there is some value
      Assert.assertNull(data3.getData().get(90).getValue(2));

      Assert.assertEquals(data3.getData().get(96).getLong(0), Long.valueOf(97000));
      Assert.assertTrue(data3.getData().get(96).getDouble(1) > 1d); // just make sure there is some value
      Assert.assertNull(data3.getData().get(96).getValue(2));
   }

}