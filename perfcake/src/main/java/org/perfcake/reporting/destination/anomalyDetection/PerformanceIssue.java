package org.perfcake.reporting.destination.anomalyDetection;

/**
 * A performance issue.
 *
 * @author <a href="mailto:kurovamartina@gmail.com">Martina Kůrová</a>
 */
public class PerformanceIssue {

   /**
    * The type of performance issue.
    */
   private PerformanceIssueType type;

   /**
    * The initial value of the interval
    */
   private double from;

   /**
    * The last value of the interval
    */
   private double to;

   /**
    * A flag for degradation issue.
    */
   private boolean degradation = false;

   /**
    * A flag for 'regular spikes' issue.
    */
   private boolean regularSpike = false;

   /**
    * A flag for a traffic spike issue.
    */
   private boolean trafficSpike = false;

   /**
    * A constructor for performance issue with setting the flag for the issue.
    *
    * @param type
    *       Type of the issue.
    */
   public PerformanceIssue(PerformanceIssueType type) {
      this.type = type;
      degradation = PerformanceIssueType.DEGRADATION.equals(type);
      regularSpike = PerformanceIssueType.REGULAR_SPIKES.equals(type);
      trafficSpike = PerformanceIssueType.TRAFIC_SPIKE.equals(type);
   }

   public boolean isDegradation() {
      return degradation;
   }

   public boolean isRegularSpike() {
      return regularSpike;
   }

   public boolean isTrafficSpike() {
      return trafficSpike;
   }

   public PerformanceIssueType getType() {
      return type;
   }

   public void setType(PerformanceIssueType type) {
      this.type = type;
   }

   public double getFrom() {
      return from;
   }

   public void setFrom(double from) {
      this.from = from;
   }

   public double getTo() {
      return to;
   }

   public void setTo(double to) {
      this.to = to;
   }
}
