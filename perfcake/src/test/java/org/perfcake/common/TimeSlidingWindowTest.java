package org.perfcake.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class TimeSlidingWindowTest {

   @Test
   public void slidingWindowTest() throws InterruptedException {
      final TimeSlidingWindow<Long> tsw = new TimeSlidingWindow<>(500);

      for (int i = 0; i < 10; i++) {
         tsw.add(10L);
      }

      final LongAdder la = new LongAdder();

      tsw.forEach(la::add);
      Assert.assertEquals(la.longValue(), 10 * 10); // all 10s should be there

      Thread.sleep(600);

      la.reset();
      tsw.forEach(la::add);
      Assert.assertEquals(la.longValue(), 0); // everything should be gone

      // this takes 1 second
      for (int i = 0; i < 10; i++) {
         tsw.add(10L);
         Thread.sleep(100);
      }

      la.reset();
      tsw.forEach(la::add);
      long sum = la.longValue();
      Assert.assertTrue(sum > 0 && sum < 100); // there should be just some elements (not all, not empty), ideally 4 * 10
   }

   @Test
   public void artificialSlidingWindowTest() {
      final TimeSlidingWindow<Long> tsw = new TimeSlidingWindow<>(500);

      for (int i = 1; i <= 10; i++) {
         tsw.add((long) i, i * 100);
      }

      Assert.expectThrows(IllegalStateException.class, () -> {
         tsw.add(10L, 100); // this should not work
      });

      final LongAdder la = new LongAdder();

      tsw.forEach(500, la::add);
      Assert.assertEquals(la.longValue(), 1 + 2 + 3 + 4 + 5);

      la.reset();
      tsw.forEach(601, la::add);
      Assert.assertEquals(la.longValue(), 2 + 3 + 4 + 5 + 6);

      la.reset();
      tsw.forEach(500, la::add);
      Assert.assertEquals(la.longValue(), 2 + 3 + 4 + 5); // number 1 was already gc'ed

      la.reset();
      tsw.forEach(1000, la::add);
      Assert.assertEquals(la.longValue(), 5 + 6 + 7 + 8 + 9 + 10);

      la.reset();
      tsw.forEach(1500, la::add);
      Assert.assertEquals(la.longValue(), 10);

      la.reset();
      tsw.forEach(1501, la::add);
      Assert.assertEquals(la.longValue(), 0);
   }
}