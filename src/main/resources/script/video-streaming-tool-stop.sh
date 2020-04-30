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

STOP_CMD="kill $PID"
MONITOR_INTERVAL=5

running() {
  if [[ -z $1 || $1 == 0 ]]; then
    echo "running1 $1" >> $MONITOR_LOG
    return 1
  fi
  if [[ ! -d /proc/$1 ]]; then
      echo "running2 $1" >> $MONITOR_LOG
      return 1
  fi
}

stop_app() {
  if ! $(running $PID) ; then
    echo "stop_app1 $PID of $BINFILE ..." >> $MONITOR_LOG
    return
  fi
  echo "stopping $PID of $BINFILE ..." >> $MONITOR_LOG
  $STOP_CMD
  while $(running $PID) ; do
    sleep 1
  done
}

stop_monitor() {
  if ! $(running $MONITOR_PID) ; then
    echo "stop_monitor1 $(running $MONITOR_PID)" >> $MONITOR_LOG
    echo "stop_monitor1 $MONITOR_PID of $BINFILE ..." >> $MONITOR_LOG
    return
  fi
  echo "stopping $MONITOR_PID of $BINFILE monitor ..." >> $MONITOR_LOG
  kill $MONITOR_PID
  while $(running $MONITOR_PID) ; do
    sleep 1
  done
}

stop() {
  stop_monitor
  stop_app
}

stop
