#!/usr/bin/env bash
# Test-server helper for test-server/X-expeditions (port 25571). Never touches the Prism instance.
#   testserver.sh stop            stop the server (only the jdk-21.0.7 server process, never a game client)
#   testserver.sh install         put the freshly built jar in extra-mods/ and relink mods/
#   testserver.sh wait            wait until the server is up (or crashed)
#   testserver.sh cmd "<command>" [seconds]   send a console command, then wait
set -u
SERVER="$(cd "$(dirname "$0")/../../test-server/X-expeditions" && pwd)"
JAR="$(cd "$(dirname "$0")/.." && pwd)/build/libs/colony_expeditions-0.1.0.jar"

server_pid() {
  powershell -NoProfile -Command "(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*win_args.txt*' -and \$_.CommandLine -like '*jdk-21.0.7*' }).ProcessId"
}

case "$1" in
  stop)
    pids=$(server_pid | tr -d '\r')
    [ -z "$pids" ] && { echo "not running"; exit 0; }
    echo stop >> "$SERVER/console.in"
    for pid in $pids; do
      for i in $(seq 1 60); do tasklist //FI "PID eq $pid" | grep -q java || break; sleep 3; done
      tasklist //FI "PID eq $pid" | grep -q java && taskkill //PID "$pid" //F > /dev/null
    done
    pkill -f "tail -f console.in" 2> /dev/null
    echo "stopped $pids" ;;
  install)
    # mods/ holds hard links to the live pack: never cp into it (that writes through to the Prism jar).
    # Put the build in extra-mods/ and relink; sync-mods.ps1 lets extra-mods/ override a live jar of the same name.
    cp "$JAR" "$SERVER/extra-mods/" && powershell -NoProfile -File "$(cygpath -w "$SERVER/sync-mods.ps1")" && : > "$SERVER/console.in" && echo installed ;;
  wait)
    sleep 15
    for i in $(seq 1 150); do
      grep -qE "Done \([0-9.]+s\)|Crash report saved" "$SERVER/console.out" 2>/dev/null && break
      sleep 4
    done
    grep -E "Done \(|Crash report saved" "$SERVER/console.out" || { echo "SERVER DID NOT START (10 min)"; exit 1; } ;;
  cmd)
    echo "$2" >> "$SERVER/console.in"; sleep "${3:-4}" ;;
esac
