#!/usr/bin/env bash
# One full expedition trip per listed destination (or "nether" for the stock Netherworker) on a fresh world.
# Assumes the test server is running on a wiped world. Prints each hut's final report: key count, mobs fought, loot.
T="$(dirname "$0")/testserver.sh"
L="$(cd "$(dirname "$0")/../../test-server/X-expeditions" && pwd)/console.out"
DESTS="${*:-end nether}"

setup=("gamerule doDaylightCycle false" "gamerule doWeatherCycle false" "time set 1000" "weather clear 1000000"
       "colony_expeditions test colony" "colony_expeditions test wake")
for c in "${setup[@]}"; do bash "$T" cmd "$c" 4; done
for d in $DESTS; do bash "$T" cmd "colony_expeditions test hut $d" 8; done
tail -200 "$L" | grep -E "\] \[test\] (placed|No |Blueprint)" | cut -c1-220
for d in $DESTS; do bash "$T" cmd "colony_expeditions test hire $d" 4; done
bash "$T" cmd "colony_expeditions test watch" 2
start=$(wc -l < "$L")
count=$(echo $DESTS | wc -w)

echo "== waiting for $count departure(s)"
for i in $(seq 1 60); do [ "$(tail -n +"$start" "$L" | grep -c 'left level: DISCARDED')" -ge "$count" ] && break; sleep 5; done
tail -n +"$start" "$L" | grep -E "left level" | cut -c1-150
sleep 30
bash "$T" cmd "colony_expeditions test recall" 5

echo "== waiting for $count completed log(s)"
for i in $(seq 1 90); do
  [ "$i" = 18 ] && bash "$T" cmd "colony_expeditions test recall" 5
  done_count=$(tail -300 "$L" | grep -E '\[watch\] colony_exp|\[watch\] minecolonies' | tail -"$count" | grep -cE 'log=(COMPLETED|KILLED)')
  [ "$done_count" -ge "$count" ] && break
  sleep 10
done
bash "$T" cmd "colony_expeditions test status" 6
tail -n +"$start" "$L" | grep -E "died:|removeCivilian|playSoundAtCitizenWith" | grep -v "removal trace" | head -5
tail -300 "$L" | grep -E "\] \[test\] (colony_exp|minecolonies)|^  [A-Z]|^    (inv|mobs|loot)" | tail -$((count * 5)) \
  | sed -E 's/tags=\{[^}]*\}[^ ]*//; s/recipeKey=[^ ]* //; s/, 1xminecraft:(stone|leather)_[a-z]+//g'
