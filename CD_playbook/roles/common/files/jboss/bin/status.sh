#!/bin/sh
. ./env.sh
ps -ef | grep java | grep --color "SERVER=$SERVER_NAME "
