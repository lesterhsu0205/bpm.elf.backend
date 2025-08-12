#!/bin/sh
. ./env.sh

# eap6.1+
$JAVA_HOME/bin/java -cp $JBOSS_HOME/bin/client/jboss-client.jar org.jgroups.tests.McastReceiverTest -mcast_addr 230.11.11.11 -port 5555
