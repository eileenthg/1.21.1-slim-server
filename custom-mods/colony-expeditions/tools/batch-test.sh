#!/usr/bin/env bash
# Runs trip-test.sh on a fresh world for each batch of destinations. Summaries go to tools/batch-results.txt.
T="$(dirname "$0")/testserver.sh"
SERVER="$(cd "$(dirname "$0")/../../test-server/X-expeditions" && pwd)"
OUT="$(dirname "$0")/batch-results.txt"
: > "$OUT"
bash "$T" install > /dev/null
for batch in "$@"; do
  bash "$T" stop > /dev/null
  rm -rf "$SERVER/expeditions"
  : > "$SERVER/console.in"
  (cd "$SERVER" && tail -f console.in | "/c/Program Files/Java/jdk-21.0.7/bin/java" @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.249/win_args.txt nogui > console.out 2>&1) &
  echo "===== batch: $batch" >> "$OUT"
  bash "$T" wait >> "$OUT" || { tail -40 "$SERVER/console.out" | grep -E "ERROR|Exception" >> "$OUT"; break; }
  bash "$(dirname "$0")/trip-test.sh" $batch 2>&1 | grep -vE "^\s+at |java.lang.Exception: null" | cut -c1-700 >> "$OUT"
done
bash "$T" stop >> "$OUT"
