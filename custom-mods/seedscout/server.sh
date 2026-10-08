#!/usr/bin/env bash
# Helper for test-server/W-seedscout (port 25611). Only ever stops the PID recorded at launch.
#   server.sh restart   stop (if running), install the built jar, start, wait for "Done"
#   server.sh stop
#   server.sh cmd "<command>"
set -u
SERVER=/c/Users/eteil.000/Claude/modpack-audit/test-server/W-seedscout
JAR=/c/Users/eteil.000/Claude/modpack-audit/seedscout/build/libs/seedscout-0.1.0.jar
PIDFILE=$SERVER/server.pid
JAVA="/c/Program Files/Java/jdk-21.0.7/bin/java"

stop() {
  [ -f "$PIDFILE" ] || { echo "no pid file"; return; }
  pid=$(cat "$PIDFILE")
  if tasklist //FI "PID eq $pid" | grep -q java; then
    echo stop >> "$SERVER/console.in"
    for i in $(seq 1 100); do tasklist //FI "PID eq $pid" | grep -q java || break; sleep 3; done
  fi
  echo "stopped $pid"; rm -f "$PIDFILE"
}

start() {
  cp "$JAR" "$SERVER/mods/"
  : > "$SERVER/console.in"; rm -rf "$SERVER/logs"
  (cd "$SERVER" && tail -f console.in | "$JAVA" @user_jvm_args.txt @seedscout_launch_args.txt nogui > console.out 2>&1) </dev/null >/dev/null 2>&1 &
  sleep 12
  powershell -NoProfile -Command "(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*seedscout_launch_args*' }).ProcessId" | tr -dc '0-9' > "$PIDFILE"
  echo "started pid $(cat "$PIDFILE")"
  for i in $(seq 1 150); do grep -qE "Done \([0-9.]+s\)|Crash report saved" "$SERVER/console.out" 2>/dev/null && break; sleep 4; done
  grep -E "Done \(|Crash report saved" "$SERVER/console.out"
}

case "$1" in
  restart) stop; start ;;
  stop) stop ;;
  cmd) echo "$2" >> "$SERVER/console.in" ;;
esac
