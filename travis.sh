#!/bin/bash
# Abort on Error
#set -e

if [ -z $TRAVIS_TAG ]; then
	echo "TRAVIS_TAG variable not set! Skipping..."
	exit 0;
fi

#export PING_SLEEP=30s
export MAVEN_OPTS="-Xmx326m";

# Set up a repeating loop to send some output to Travis.
#bash -c "while true; do echo \$(date) - building ...; sleep $PING_SLEEP; done" &
#export PING_LOOP_PID=$!

# My build is using maven, but you could build anything with this, E.g.

mvn install -DskipTests
mvn -pl perfcake package javadoc:javadoc assembly:single -DskipTests
mvn -pl perfcake-agent package javadoc:javadoc assembly:single -DskipTests

export PCV=${TRAVIS_TAG#*v}

cp -vf ./perfcake/target/perfcake-$PCV-bin.zip perfcake-$TRAVIS_TAG-bin.zip
cp -vf ./perfcake/target/perfcake-$PCV-bin.tar.gz perfcake-$TRAVIS_TAG-bin.tar.gz
cp -vf ./perfcake/target/perfcake-$PCV-bin.tar.bz2 perfcake-$TRAVIS_TAG-bin.tar.bz2
cp -vf ./perfcake/target/perfcake-$PCV-javadoc.jar perfcake-$TRAVIS_TAG-javadoc.jar

cp -vf ./perfcake-agent/target/perfcake-agent-$PCV-bin-agent.zip perfcake-agent-$TRAVIS_TAG-bin.zip
cp -vf ./perfcake-agent/target/perfcake-agent-$PCV-bin-agent.tar.gz perfcake-agent-$TRAVIS_TAG-bin.tar.gz
cp -vf ./perfcake-agent/target/perfcake-agent-$PCV-bin-agent.tar.bz2 perfcake-agent-$TRAVIS_TAG-bin.tar.bz2
cp -vf ./perfcake-agent/target/perfcake-agent-$PCV-javadoc.jar perfcake-agent-$TRAVIS_TAG-javadoc.jar

#maven_exit=$?
#if [[ $maven_exit -ne 0 ]];
#then
#	tail -c 2M maven.log;
#fi

# nicely terminate the ping output loop
#kill $PING_LOOP_PID
#exit $maven_exit