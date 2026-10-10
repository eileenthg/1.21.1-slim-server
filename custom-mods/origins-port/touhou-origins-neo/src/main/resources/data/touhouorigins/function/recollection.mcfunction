# Add a tag to exclude user from this function
tag @s add satoriSelf

# Play a sound around the target to nearby players
execute at @e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf] run playsound touhouorigins:origin.satori.recollect player @a[distance=..32] ~ ~ ~ 2 2

# Proceed depending on if the target is the Satori (Closed Eye) Origin or not.
# Overgrown's Apoli/Origins expose "has this origin" only as an Apoli-internal EntityCondition, not
# as a vanilla loot_condition_type or entity_sub_predicate, so there is nothing a `predicate=`
# selector can query directly (unlike the Fabric original this replaces). satori_closed_eye now
# self-tags via powers/satori_closed_eye/closed_eye_tag.json instead, so a plain tag= check does the
# same job with no Java and no datapack-load error.
#
# The origin-name line reads command storage rather than the target's NBT. Fabric's
# cardinal_components path does not exist here, and while the id IS reachable from the NeoForge
# attachment ("neoforge:attachments"."origins:origins".origins."origins:origin"), that only yields
# the raw id -- Origin.name() builds its translation key by splicing the id's namespace and path,
# which mcfunction macros cannot do. So recollection.json's bientity_action calls
# touhouorigins:store_origin_name on the target first, writing the display name here as a
# serialised text component; "interpret" turns it back into one, so it localises for the reader.
# This depends on apoli:raycast running bientity_action before command_at_hit, which it does.
execute unless entity @e[type=minecraft:player,limit=1,sort=nearest,tag=touhouorigins.satori_closed_eye] run tellraw @s ["",{"translate":"power.touhouorigins.recollection.player","with":[{"selector":"@e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf]","color":"#D02020"}]},"\n",{"translate":"layer.origins.origin.name","color":"#CE0039"},{"text":": ","color":"#CE0039"},{"nbt":"origin_name","storage":"touhouorigins:recollection","interpret":true},"\n",{"translate":"power.touhouorigins.recollection.level","color":"#C6004F"},{"text":": ","color":"#C6004F"},{"nbt":"XpLevel","entity":"@e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf]"},"\n",{"translate":"power.touhouorigins.recollection.spawnXYZ","color":"#BA0062"},{"text":": ","color":"#BA0062"},{"nbt":"SpawnX","entity":"@e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf]"},{"text":" / "},{"nbt":"SpawnY","entity":"@e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf]"},{"text":" / "},{"nbt":"SpawnZ","entity":"@e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf]"},"\n",{"translate":"power.touhouorigins.recollection.spawnDimension","color":"#A80773"},{"text":": ","color":"#A80773"},{"nbt":"SpawnDimension","entity":"@e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf]"},"\n",{"translate":"power.touhouorigins.recollection.lastDeathLocation","color":"#922081"},{"text":": ","color":"#922081"},{"nbt":"LastDeathLocation","entity":"@e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf]"}]
execute if entity @e[type=minecraft:player,limit=1,sort=nearest,tag=touhouorigins.satori_closed_eye] run tellraw @s {"translate":"power.touhouorigins.recollection.fail","with":[{"selector":"@e[type=minecraft:player,limit=1,sort=nearest,tag=!satoriSelf]","color":"#A0DA81"}]}

# Remove tag as it's no longer needed
tag @s remove satoriSelf