#!/usr/bin/env bash

# Either return greeting message concatenated with
# cmdline argument or with input from STDIN.
if [ $# -ge 1 ] ;
then
   if [[ $TEST_VARIABLE != "" ]] ;
   then 
      echo "Greetings $1! From ARG #1. TEST_VARIABLE=$TEST_VARIABLE."
   else
      echo "Greetings $1! From ARG #1."
   fi
else
   read name
   if [[ $TEST_VARIABLE != "" ]] ;
   then
      echo "Greetings $name! From STDIN. TEST_VARIABLE=$TEST_VARIABLE."
   else
      echo "Greetings $name! From STDIN."
   fi
fi

true

