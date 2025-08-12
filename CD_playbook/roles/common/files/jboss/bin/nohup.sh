#!/bin/sh
. ./env.sh
tail -f $LOG_HOME/nohup/$SERVER_NAME.out
