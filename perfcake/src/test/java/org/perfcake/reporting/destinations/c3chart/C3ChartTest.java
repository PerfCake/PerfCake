package org.perfcake.reporting.destinations.c3chart;

import org.perfcake.PerfCakeException;
import org.perfcake.TestSetup;
import org.perfcake.common.PeriodType;
import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.destinations.C3ChartDestination;

import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Random;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3ChartTest extends TestSetup {

   @Test
   public void basicTest() throws Exception {
      Random r = new Random();
      C3ChartDestination dst = new C3ChartDestination();
      dst.setAttributes("Result, Average");
      dst.setXAxis("Time");
      dst.setYAxis("Throughput msgs/sec");
      dst.setGroup("speedGroup");
      dst.setName("My throughput");
      dst.setxAxisType(PeriodType.TIME);

      dst.setOutputDir("/home/mvecera/work/PerfCake/tmp/c3-test");

      dst.open();

      Measurement m;
      Double d, avg = 0d;

      for (int i = 1; i <= 10; i++) {
         m = new Measurement(i * 10, i * 1000, (i * 100) + r.nextInt(100));
         d = r.nextDouble() * 100d;
         avg = avg + d;
         m.set(new Quantity<>(d, "msgs/s"));
         m.set("Average", new Quantity<>(avg / i, "msgs/s"));
         dst.report(m);
      }

      dst.close();
   }

   @Test
   public void testDataRead() throws PerfCakeException {
      C3ChartData data1 = new C3ChartData("speedGroup20160430001257", Paths.get("/home/mvecera/work/PerfCake/tmp/c3-test"));
      C3ChartData data2 = new C3ChartData("speedGroup20160430011440", Paths.get("/home/mvecera/work/PerfCake/tmp/c3-test"));

      C3ChartData data3 = data1.combineWith(data2.filter(2));
      System.out.println(data3);
   }

}