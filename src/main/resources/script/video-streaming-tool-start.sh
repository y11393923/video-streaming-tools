 #!/bin/bash

BINFILE=video-streaming-tool
BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd $BIN_DIR
MONITOR_LOG="$BIN_DIR/monitor/monitor.log"
MONITOR_PIDFILE="$BIN_DIR/monitor/monitor.pid"
MONITOR_PID=0
if [[ -f $MONITOR_PIDFILE ]]; then
  MONITOR_PID=`cat $MONITOR_PIDFILE`
fi
PIDFILE="$BIN_DIR/$(basename $BINFILE).pid"
PID=0
if [[ -f $PIDFILE ]]; then
  PID=`cat $PIDFILE`
fi

CONF_DIR=./config
LIB_DIR=./lib
LIB_JARS=`ls $LIB_DIR|grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`
JAVA_OPTS=" -Djava.net.preferIPv4Stack=true -Dfile.encoding=utf-8"
JAVA_MEM_OPTS=" -server -Xms1g -Xmx1g -XX:PermSize=1g -XX:SurvivorRatio=2 -XX:+UseParallelGC "

#START_CMD=$BIN_DIR/$BINFILE
BINMAIN=com.sensetime.tsc.streaming.VideoStreamingToolApplication
START_CMD="java $JAVA_OPTS $JAVA_MEM_OPTS -classpath $CONF_DIR:$LIB_JARS $BINMAIN"
STOP_CMD="kill $PID"
MONITOR_INTERVAL=5

running() {
  if [[ -z $1 || $1 == 0 ]]; then
    return 1
  fi
  if [[ ! -d /proc/$1 ]]; then
      return 1
  fi
}

start_app() {
  echo "### starting $BINFILE `date '+%Y-%m-%d %H:%M:%S'` ###" >>  /dev/null   2>&1 &
  nohup java $JAVA_OPTS $JAVA_MEM_OPTS -classpath $CONF_DIR:$LIB_JARS $BINMAIN >>video-streaming-tool.log 2>&1 &
  if ! $(running $!) ; then
    echo "failed to start $BINFILE"
    exit 1
  fi
  PID=$!
  echo $! > $PIDFILE
  echo "new pid VideoStreamingToolApplication-$!"
}

stop_app() {
  if ! $(running $PID) ; then
    return
  fi
  echo "stopping $PID of $BINFILE ..."
  $STOP_CMD
  while $(running $PID) ; do
    sleep 1
  done
}


start_monitor() {
  while [ 1 ]; do
    if ! $(running $PID) ; then
      echo "$(date '+%Y-%m-%d %T') $BINFILE is gone. monitor pid $!" >> $MONITOR_LOG
      start_app
      echo "$(date '+%Y-%m-%d %T') $BINFILE started. monitor pid $!" >> $MONITOR_LOG
    fi
    sleep $MONITOR_INTERVAL
  done &
  MONITOR_PID=$!
  echo "monitor pid VideoStreamingToolApplication-$!"
  echo $! > $MONITOR_PIDFILE
}


stop_monitor() {
  if ! $(running $MONITOR_PID) ; then
    return
  fi
  echo "stopping $MONITOR_PID of $BINFILE monitor ..."
  kill $MONITOR_PID
  while $(running $MONITOR_PID) ; do
    sleep 1
  done
}

start() {
  mkdir "$BIN_DIR/monitor"
  start_app
  start_monitor
}

stop() {
  stop_monitor
  stop_app
}

restart() {
  stop
  start
}

restart
