#!/bin/sh
. ./env.sh
PID=`ps -ef | grep java | grep "SERVER=$SERVER_NAME " | awk '{print $2}'`
if [ "$PID" = "" ]
then
  echo "ERROR: no such process : PID does not exist."
  exit 1
fi
   
echo "Target Process Number is : [$PID]"
for count in 1 2 3 4 5; do
  echo "Thread Dump count : $count"
  echo "Sending signal...and wait 3 sec $PID"
  kill -3 $PID
  echo "Logging CPU Utilization per Thread..."
  TOP_CPU_LOG_FILE=$LOG_HOME/top_cpu_`date "+%Y-%m-%d_%H%M%S"`.log
  top -H -c -b -n1 > $TOP_CPU_LOG_FILE
  sleep 3
done
echo "See the following files..."
echo "-- $LOG_HOME/nohup/$SERVER_NAME.out -- for Thread Dump"
echo "-- $LOG_HOME/top_cpu_[yyyy-mm-dd_HHMMSS].log -- for Top CPU Utilization Thread."
