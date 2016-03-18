PerfCake Change Log
===================

Release 6.0
===========

Features
--------
* We solved coordinated omission in PerfCake in the following issues and by introducing a new reporter (**ResponseTimeHistogramReporter**):
  * https://github.com/PerfCake/PerfCake/issues/258 - Coordinated omission problem
  * https://github.com/PerfCake/PerfCake/issues/267 - Switch to HdrHistogram
* https://github.com/PerfCake/PerfCake/issues/264 - Failed messages should be left out of statistics

Bug Fixes
---------
* https://github.com/PerfCake/PerfCake/issues/261 - Annotation MandatoryProperty is not accessible at runtime
* https://github.com/PerfCake/PerfCake/issues/262 - NPE is thrown in AbstractSender's preSend it logging is on TRACE level
* https://github.com/PerfCake/PerfCake/issues/265 - Make sure slow services are handled properly

Release 5.1
===========

TBD

Release 5.0
===========

TBD

Release 4.0
===========

Features
--------
* Scenario schema namespace changed to "**urn:perfcake:scenario:4.0**".
* https://github.com/PerfCake/PerfCake/issues/68 - Allow to set Logger threshold by parameter or similar way
* https://github.com/PerfCake/PerfCake/issues/116 - Add Google Charts templates
* https://github.com/PerfCake/PerfCake/issues/136 - Implement expression language
* https://github.com/PerfCake/PerfCake/issues/140 - Develop Validators integration test
* https://github.com/PerfCake/PerfCake/issues/141 - Refactor Reporter Contract test
* https://github.com/PerfCake/PerfCake/issues/142 - Review members visibility and usage of final
* https://github.com/PerfCake/PerfCake/issues/143 - Update maven-javadoc-plugin to the latest version
* https://github.com/PerfCake/PerfCake/issues/145 - Archive signatures using website SSL key
* https://github.com/PerfCake/PerfCake/issues/148 - Review Javadoc Maven plugin configuration
* https://github.com/PerfCake/PerfCake/issues/150 - Review JavaDoc enhancement
* https://github.com/PerfCake/PerfCake/issues/152 - Optimize test groups and run time
* https://github.com/PerfCake/PerfCake/issues/153 - Investigate possibility to initiate a heapdump once possible memory leak is detected by **MemoryUsageReporter**.
* https://github.com/PerfCake/PerfCake/issues/158 - Add possibility to show validation statistics when validation is enabled.
* https://github.com/PerfCake/PerfCake/issues/159 - Improve integration with Sonatype enhancement
* https://github.com/PerfCake/PerfCake/issues/160 - Enable possibility to perform a garbage collection for MemoryUsageReporter
* https://github.com/PerfCake/PerfCake/issues/163 - Extend tests to cover message templating
* https://github.com/PerfCake/PerfCake/issues/167 - Extend RegExpValidator to be able to configure Pattern.compile flags
* https://github.com/PerfCake/PerfCake/issues/168 - Publish documentation in HTML format documentation
* https://github.com/PerfCake/PerfCake/issues/172 - Try using Timer from Faban as a precise source of time
* https://github.com/PerfCake/PerfCake/issues/173 - Investigate possibilities of using Goldman Sachs collections
* https://github.com/PerfCake/PerfCake/issues/174 - Extend accumulators tests
* https://github.com/PerfCake/PerfCake/issues/175 - Update embedded AS Arquillian tests to run with Java 8
* https://github.com/PerfCake/PerfCake/issues/176 - Convert **CSVDestination** to use FileChannel
* https://github.com/PerfCake/PerfCake/issues/177 - Document all result names for each reporter
* https://github.com/PerfCake/PerfCake/issues/178 - Log validators separately
* https://github.com/PerfCake/PerfCake/issues/180 - Allow only alphanumeric digits and an underscore in group name for **ChartDestination**

Bug Fixes
---------
* https://github.com/PerfCake/PerfCake/issues/156 - XSLT transformation adds an empty property to sender
* https://github.com/PerfCake/PerfCake/issues/157 - **MemoryUsageReporter** does not reset correctly
* https://github.com/PerfCake/PerfCake/issues/161 - Issue with space in JAVA_HOME
* https://github.com/PerfCake/PerfCake/issues/164 - **RampUpDownGenerator** produces NPE
* https://github.com/PerfCake/PerfCake/issues/165 - **RampUpDownGenerator** does not have implemented **mainThreadCount** getters/setters
* https://github.com/PerfCake/PerfCake/issues/166 - **RampUpDownGenerator** logs current phase too often
* https://github.com/PerfCake/PerfCake/issues/179 - Log4j config is missing in binary distro

Release 3.3
==========================

Features
--------
* https://github.com/PerfCake/PerfCake/issues/94 - First Technical Preview of DSL scenario specification support

Bug Fixes
---------
* https://github.com/PerfCake/PerfCake/issues/146 - The XSLT for scenario conversion from v2 to v3 does not cover RequestResponseJmsSender properly.

Release 3.2
===========

Features
--------

Bug Fixes
---------
* Workaround for https://jira.codehaus.org/browse/MJAVADOC-408
* Scenario conversion XSLT is missing from binary distribution.

Release 3.1
==========================

Features
--------

Bug Fixes
---------
* JBoss Maven repository is missing - needed for JMS based tests.

Release 3.0
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
* Improved validators - renamed some classes, simplified Validator interface, updated RulesValidator (KIE 6.1.0.Beta3), created ScriptValidator (JSR-223), both original and response messages
     are passed to the validator, validation can run fast from the very beginning (fastForward in scenario), validation can be disabled in scenario.
* Compatibility with JDK 8.
* Properties in Scenario can have arbitrary XML elements in it, components can declare setter that accepts org.w3c.dom.Element
* Implemented **LDAPSender**.
* **RequestResponseJMSSender** has the ability to set different connection for request and response queues.
* Created XSLT for scenario XML conversion from v 2.0 to 3.0.

Bug Fixes
---------
* https://github.com/PerfCake/PerfCake/issues/60  - Refactor validators
* https://github.com/PerfCake/PerfCake/issues/104 - Fix CSVDestination synchronization problem.
* https://github.com/PerfCake/PerfCake/issues/111 - PerfCakeAgent hangs when configured to monitor local JVM process.
* https://github.com/PerfCake/PerfCake/issues/123 - Review and update tests to become independent to each other.
* https://github.com/PerfCake/PerfCake/issues/127 - CommandSender throws NullPointerException when message is not used.
* https://github.com/PerfCake/PerfCake/issues/128 - Fix ext.dirs path to also include JRE libraries.
* https://github.com/PerfCake/PerfCake/issues/130 - Early log messages are not shown.
* https://github.com/PerfCake/PerfCake/issues/132 - Rename those classes that do not follow the naming conventions so they do.

Release 2.1
===========

Features
--------
* Added site.xml making use of Maven Fluido Skin.

Bug Fixes
---------
* https://github.com/PerfCake/PerfCake/issues/119 - Message properties and headers are ignored.

Release 2.0
===========

Features
--------
* Scenario schema namespace changed to "**urn:perfcake:scenario:2.0**".
* Added **-pf** parameter to CLI for specifying a property file.
* Added a possibility to add extra jars to the classpath (lib/ext directory).
* Added a mechanism for extending PerfCake via plugins (lib/plugins directory).
* Added **appendStrategy** property to **CSVDestination**.
* Added a new generator - **RampUpDownGenerator**.
* Added a new sender - **WebSocketSender**.
* Implemented a memory leak detection feature into **MemoryUsageReporter**.
* Improved sender pool implementation.
* Improved performance.
* Increased test coverage.

Bug Fixes
---------
* https://github.com/PerfCake/PerfCake/issues/77 - Exceptions thrown by senders are not processed by the logging subsystem.
* https://github.com/PerfCake/PerfCake/issues/84 - CSVDestination appends new records into the output file if the one exists and losts the warmUp column.
* https://github.com/PerfCake/PerfCake/issues/86 - MemoryUsageReporter collects data for memory leak detection analysis incorrectly.
* https://github.com/PerfCake/PerfCake/issues/87 - Bad maxIterations handling after warm up.
* https://github.com/PerfCake/PerfCake/issues/97 - Accumulating the results leads to NPE because of concurrency.

Release 1.0.1
=============

Bug Fixes
---------
* https://github.com/PerfCake/PerfCake/issues/82 - GroovySender resolves default groovy path wrong.
* https://github.com/PerfCake/PerfCake/issues/83 - Default scenarios and messages directories are resolved wrong in binary distribution.

Release 1.0
===========

Features
--------
* Basic performance testing functionality including Generators, Senders, Reporters, Destinations and Validators.

Bug Fixes
---------
* Bugs fixed

Release 0.3
===========
* First released version of PerfCake - all has changed.
