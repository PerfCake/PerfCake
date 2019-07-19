#!/bin/bash
export MAVEN_OPTS="-Xmx326m";
set -x
mvn -ntp clean package -DskipTests
mvn test -P travis-tests
