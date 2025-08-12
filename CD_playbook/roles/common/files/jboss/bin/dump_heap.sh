#!/bin/sh
. ./env.sh
PID=`ps -ef | grep java | grep "=$SERVER_NAME " | awk '{print $2}'`
DUMP_PATH=$LOG_HOME/gclog
# check if it is a valid pid
#check=`ps efo pid | sed 's/^ *//g'|grep "^$PID"`
check=`ps -ef |grep java|grep $PID| grep -v grep`
if [ "$check" = "" ]
then
   echo "ERROR: no such process/해당 PID가 존재하지 않습니다."
   exit 1
fi
# check DUMP PATH
if [ "$DUMP_PATH" != "" ]; then
    #check valiud path
    if [ ! -d "$DUMP_PATH" ];then
    echo "Folder not exist!! [$DUMP_PATH]"
    exit 1
    fi
fi
if [ "$DUMP_PATH" ==  "" ]; then
    DUMP_PATH=$PWD
fi
echo "Target Process Number is : [$PID]"
echo "Dump file create PATH is : [$DUMP_PATH]"
echo "Can tack longer time../시간이 오래 걸릴 수 있으며 그동안 해당 Process는 서비스가 중지됩니다"
echo -n "Do you really want to generate Heap Dump? (y/n): "
read i
case $i in
        [Yy]*)
                echo "JBoss instance Heap Memory dump start......../메모리 덤프 시작"
                $JAVA_HOME/bin/jmap -dump:format=b,file=$DUMP_PATH/heap-$SERVER_NAME-$PID-$DATE.hprof $PID
        ;;
        [Nn]*)
                echo "cancelled / 취소하셨습니다......."
        ;;
        *)
                echo "retry please / 다시선택하세요...."
        ;;
esac
exit
