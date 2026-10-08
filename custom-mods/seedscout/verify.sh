#!/usr/bin/env bash
# Generate a real world for a seed on W-seedscout and compare it with the prediction.
#   verify.sh <seed>
# Prints the predicted spawn/village, the actual spawn, and the structure starts stored around spawn.
set -u
SEED=$1
SERVER=/c/Users/eteil.000/Claude/modpack-audit/test-server/W-seedscout
HERE=/c/Users/eteil.000/Claude/modpack-audit/seedscout
WORLD="verify-$(echo "$SEED" | tr -d '-')"

sed -i -e "s/^level-name=.*/level-name=$WORLD/" -e "s/^level-seed=.*/level-seed=$SEED/" "$SERVER/server.properties"
"$HERE/server.sh" restart | tail -1
"$HERE/server.sh" cmd "seedscout check"
until grep -q "\[seedscout\] check" "$SERVER/console.out"; do sleep 3; done
"$HERE/server.sh" cmd "save-all flush"
until grep -q "Saved the game" "$SERVER/console.out"; do sleep 2; done
grep -A4 "\[seedscout\] check" "$SERVER/console.out" | head -5 | sed 's/^.*\[seedscout\] check /PREDICTED /'
spawn=$(grep -o "ACTUAL spawn=[-0-9]*,[-0-9]*,[-0-9]*" "$SERVER/console.out" | head -1 | cut -d= -f2)
sx=${spawn%%,*}; sz=${spawn##*,}
cd "$SERVER" && python "$HERE/region_starts.py" "$WORLD" $((sx >> 4)) $((sz >> 4)) 12 | grep -iE "village|statuses"
