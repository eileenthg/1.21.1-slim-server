# usage: dev-abtest.sh <interval>
cd /c/Users/eteil.000/Claude/modpack-audit/test-server/mini-thinair
rm -rf world logs/latest.log
printf 'checkIntervalTicks = %s\nrecheckDistance = 4.0\n' "$1" > config/thinairthrottle-common.toml
{
  until grep -qE 'Done [(]|Stopping server|Exception in' dev-$1.out 2>/dev/null || grep -qE 'Done [(]' logs/latest.log 2>/dev/null; do sleep 2; done
  sleep 5
  echo "forceload add -16 -16 16 16"
  sleep 3
  echo "fill -2 199 -2 2 199 2 stone"
  echo "fill 6 99 6 10 99 10 stone"
  echo "summon minecraft:pillager 0 200 0 {Tags:[\"hi\"],NoAI:1b,PersistenceRequired:1b}"
  echo "summon minecraft:pillager 8 100 8 {Tags:[\"lo\"],NoAI:1b,PersistenceRequired:1b}"
  echo "summon minecraft:pillager 4 5 4 {Tags:[\"uw\"],NoAI:1b,PersistenceRequired:1b}"
  echo "fill 3 4 3 5 7 5 water"
  sleep 2
  echo "time query gametime"
  echo "data get entity @e[tag=hi,limit=1] Air"
  echo "data get entity @e[tag=lo,limit=1] Air"
  echo "data get entity @e[tag=uw,limit=1] Air"
  sleep 20
  echo "time query gametime"
  echo "data get entity @e[tag=hi,limit=1] Air"
  echo "data get entity @e[tag=lo,limit=1] Air"
  echo "data get entity @e[tag=uw,limit=1] Air"
  echo "fill 3 4 3 5 7 5 air"
  sleep 5
  echo "time query gametime"
  echo "data get entity @e[tag=uw,limit=1] Air"
  echo "stop"
} | "/c/Program Files/Java/jdk-21.0.7/bin/java" @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.249/win_args.txt nogui > dev-$1.out 2>&1
grep -E 'The time is|entity data|No entity|Mixin|thinairthrottle.*(ERROR|WARN)|InvalidInjection' logs/latest.log | sed 's/^.*MinecraftServer\/\]: //' | cut -c1-200
