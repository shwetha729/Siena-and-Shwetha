#!/bin/sh

# cd to application directory
cd `dirname $0`

# check the script for being a symbolik link we can follow
SCRIPT=`basename $0`
while [ -h "$SCRIPT" ]; do
 SCRIPT=`ls -l $SCRIPT | grep -o '[-_/.[:alnum:]]*$'`
 cd `dirname $SCRIPT`
 SCRIPT=`basename $SCRIPT`
done

# find main archive
if [ ! -f "./run.jar"  ]; then
 echo "*** ERROR: Missing GenJ resource(s) in "`pwd`
 exit 1
fi

# find java
JAVA=$JAVA_HOME/bin/java
if [ ! -x "$JAVA" ]; then
 JAVA=`which java`
 if [ $? -eq 1 ]; then
  echo "*** ERROR: Can't find java executable"
  exit 1
 fi
fi

# run it 
$JAVA -Xmx512m -Xms32m -Djava.net.preferIPv4Stack=true -jar run.jar $@
