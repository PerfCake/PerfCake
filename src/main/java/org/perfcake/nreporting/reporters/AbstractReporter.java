package org.perfcake.nreporting.reporters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.perfcake.nreporting.MeasurementUnit;
import org.perfcake.nreporting.ReportingException;
import org.perfcake.nreporting.destinations.Destination;
import org.perfcake.util.BoundPeriod;
import org.perfcake.util.Period;
import org.perfcake.util.PeriodType;
import org.perfcake.util.RunInfo;

public abstract class AbstractReporter implements Reporter {

   private static final Logger log = Logger.getLogger(AbstractReporter.class);

   private long lastPercentage = -1;
   protected RunInfo runInfo = null;
   private Thread periodicThread;
   private final Set<BoundPeriod<Destination>> periods = new HashSet<>();

   @Override
   public final void report(final MeasurementUnit mu) throws ReportingException {
      if (runInfo == null)
         throw new ReportingException("RunInfo has not been set for this reporter.");

      doReport(mu);

      reportIterations(mu.getIteration());

      // report each percentage value just once
      final long percentage = Math.round(runInfo.getPercentage());
      if (percentage != lastPercentage) {
         lastPercentage = percentage;
         reportPercentage(percentage);
      }
   }

   private void reportIterations(final long iteration) throws ReportingException {
      for (final BoundPeriod<Destination> bp : periods) {
         if (bp.getPeriodType() == PeriodType.ITERATION && iteration % bp.getPeriod() == 0) {
            doPublishResult(PeriodType.ITERATION, bp.getBinding());
         }
      }
   }

   private void reportPercentage(final long percentage) throws ReportingException {
      for (final BoundPeriod<Destination> bp : periods) {
         if (bp.getPeriodType() == PeriodType.PERCENTAGE && percentage % bp.getPeriod() == 0) {
            doPublishResult(PeriodType.PERCENTAGE, bp.getBinding());
         }
      }
   }

   @Override
   public final void registerDestination(final Destination d, final Period p) {
      if (p.getPeriodType() == PeriodType.TIME && p.getPeriod() < 500) {
         log.error("Periodical reporting with time period smaller than 500ms! Ignoring this reporting configuration.");
      } else {
         periods.add(new BoundPeriod<>(p, d));
      }
   }

   @Override
   public final void registerDestination(final Destination d, final Set<Period> periods) {
      for (final Period p : periods) {
         registerDestination(d, p);
      }
   }

   @Override
   public final void unregisterDestination(final Destination d) {
      final Set<BoundPeriod<Destination>> toBeRemoved = new HashSet<>();
      for (final BoundPeriod<Destination> bp : periods) {
         if (bp.getBinding().equals(d)) {
            toBeRemoved.add(bp);
         }
      }

      periods.removeAll(toBeRemoved);

      // close destinations (only once) if the measurement is running
      final Set<Destination> closed = new HashSet<>();
      if (runInfo.isRunning()) {
         for (final BoundPeriod<Destination> bp : toBeRemoved) {
            if (!closed.contains(bp.getBinding())) {
               bp.getBinding().close();
               closed.add(bp.getBinding());
            }
         }
      }
   }

   abstract protected void doReport(MeasurementUnit mu) throws ReportingException;

   abstract protected void doPublishResult(PeriodType periodType, Destination d) throws ReportingException;

   @Override
   public final Set<Destination> getDestinations() {
      final Set<Destination> result = new HashSet<>();
      for (final BoundPeriod<Destination> bp : periods) {
         result.add(bp.getBinding());
      }

      return Collections.unmodifiableSet(result);
   }

   @Override
   public void start() {
      assert runInfo != null : "RunInfo must be set prior to starting a reporter.";

      if (periods.size() == 0) {
         log.warn("No reporting periods are configured for this reporter. The reporter is disabled. Call start() again after the periods are registered.");
         return;
      }

      for (final Destination d : getDestinations()) {
         d.open();
      }

      periodicThread = new Thread(new Runnable() {
         @Override
         public void run() {
            long lastTime = -1;
            long now;

            try {
               while (runInfo.isRunning() && !periodicThread.isInterrupted()) {
                  for (final BoundPeriod<Destination> p : periods) {
                     now = System.currentTimeMillis();
                     if (p.getPeriodType() == PeriodType.TIME && lastTime + p.getPeriod() < now) {
                        lastTime = now;

                        try {
                           doPublishResult(PeriodType.TIME, p.getBinding());
                        } catch (final ReportingException e) {
                           log.warn("Unable to publish result: ", e);
                        }
                     }
                  }

                  Thread.sleep(500);
               }
            } catch (final InterruptedException e) {
               // this means our job is done
            }
            if (log.isDebugEnabled())
               log.debug("Gratefully terminating the periodic reporting thread.");
         }
      });
      periodicThread.setDaemon(true); // allow the thread to die with JVM termination and do not block it
      periodicThread.start();
   }

   @Override
   public void stop() {
      if (periodicThread != null)
         periodicThread.interrupt();
      periodicThread = null;

      for (final Destination d : getDestinations()) {
         d.close();
      }
   }

   @Override
   public final void setRunInfo(final RunInfo runInfo) {
      this.runInfo = runInfo;
   }

   @Override
   public final Set<BoundPeriod<Destination>> getReportingPeriods() {
      return Collections.unmodifiableSet(periods);
   }

}
