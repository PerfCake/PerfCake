PerfCake Change Log
===================

Release 3.0 (next release)
==========================

Features
--------
* Scenario schema namespace changed to "**urn:perfcake:scenario:3.0**".
* Added **content** attribute to **message** element in scenario XML for specifying message content directly from scenario definition.
* *AverageThrouhgputRepoter*, *ResponseTimeReporter* and *WindowResponseTimeReporter* removed and replaced by more sophisticated **ThroughputStatsReporter** and **ResponseTimeStatsReporter**.
* Extended **CommandSender** to set message headers and properties as environmental variables for the executing script.
* Extended **CSVDestination** features - ability to specify line prefix, suffix, end-of-line and to skip the file header.
* Added a PIT (http://pitest.org/) plugin for mutation testing.
* Improved performance.
* Increased test coverage.

Bug Fixes
---------
* Bugs fixed

Release 2.0
===========

Features
--------
* Scenario schema namespace changed to "**urn:perfcake:scenario:2.0**".
* Added **-pf** parameter to CLI for specifying a property file.
* Added a possibility to add extra jars to the classpath (lib/ext directory).
* Added a mechanism for extending PerfCake via plugins (lib/plugins directory).
* Added **appendStrategy** property to CSVDestination.
* Added a new generator - **RampUpDownGenerator**.
* Added a new sender - **WebSocketSender**.
* Implemented a memory leak detection feature into **MemoryUsageReporter**.
* Improved performance.
* Increased test coverage.

Bug Fixes
---------
* Bugs fixed

Release 1.0
===========

Features
--------
* TODO: fill in changes.

Bug Fixes
---------
* Bugs fixed

Release 0.3
===========
* First released version of PerfCake - all has changed.