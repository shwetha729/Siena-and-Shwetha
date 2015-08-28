#!/bin/sh
#

if [ ! -d "$JAVA_HOME" ]; then
  JAVA=`which java`
  BIN=`dirname $JAVA`
  JAVA_HOME=${BIN%/*}
fi 

"$JAVA_HOME/bin/java" -cp ./contrib/ant/ant.jar org.apache.tools.ant.Main $1 $2 $3

