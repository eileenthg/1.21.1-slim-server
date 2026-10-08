package eteil.csextras.sand;

import eteil.csextras.CSExtras;
import eteil.csextras.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Scorchful-style sandstorms on top of Particle Rain's dust visuals: slowed mobs, breezes, sand piles, sand cauldrons. */
public final class Sandstorms {
    public enum Type { NONE, SAND, RED }

    public static final TagKey<Biome> SAND_BIOMES = TagKey.create(Registries.BIOME, CSExtras.id("has_sandstorms"));
    public static final TagKey<Biome> RED_BIOMES = TagKey.create(Registries.BIOME, CSExtras.id("has_red_sandstorms"));
    public static final TagKey<EntityType<?>> IGNORES = TagKey.create(Registries.ENTITY_TYPE, CSExtras.id("ignores_sandstorms"));

    private static final ResourceLocation SLOW_ID = CSExtras.id("sandstorm_slow");
    private static final ResourceLocation RANGE_ID = CSExtras.id("sandstorm_blind");

    /** Sandstorm type of the biome at pos, regardless of cover. */
    public static Type typeAt(Level level, BlockPos pos) {
        if (!Config.SANDSTORMS.get() || !level.isRaining()) return Type.NONE;
        Holder<Biome> biome = level.getBiome(pos);
        if (biome.value().hasPrecipitation()) return Type.NONE;
        if (biome.is(RED_BIOMES)) return Type.RED;
        if (biome.is(SAND_BIOMES)) return Type.SAND;
        return Type.NONE;
    }

    /** Sandstorm type at pos if pos is open to the sky. */
    public static Type exposedTypeAt(Level level, BlockPos pos) {
        if (!Config.SANDSTORMS.get() || !level.isRaining()) return Type.NONE;
        // Same open-sky test vanilla rain uses (light-independent).
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY()) return Type.NONE;
        return typeAt(level, pos);
    }

    // ---- accumulation: called from ServerLevel.tickPrecipitation (the vanilla snow/ice tick) ----

    public static void onPrecipitationTick(ServerLevel level, BlockPos column) {
        if (!Config.SANDSTORMS.get() || !level.isRaining()) return;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        Type type = typeAt(level, top);
        if (type == Type.NONE) return;
        if (level.random.nextDouble() >= Config.PILE_CHANCE.get()) return;
        accumulate(level, top, type);
    }

    /** One layer of sand (or one cauldron conversion) at the top of a column. Returns true if something changed. */
    public static boolean accumulate(ServerLevel level, BlockPos top, Type type) {
        boolean red = type == Type.RED;
        BlockPos below = top.below();
        BlockState belowState = level.getBlockState(below);

        if (belowState.is(Blocks.CAULDRON)) {
            SandCauldronBlock cauldron = red ? SandBlocks.RED_SAND_CAULDRON.get() : SandBlocks.SAND_CAULDRON.get();
            level.setBlockAndUpdate(below, cauldron.defaultBlockState());
            return true;
        }
        if (belowState.getBlock() instanceof SandCauldronBlock cauldron) {
            return cauldron.fill(belowState, level, below);
        }

        int max = Config.PILE_MAX_LAYERS.get();
        if (max <= 0) return false;
        SandPileBlock pile = red ? SandBlocks.RED_SAND_PILE.get() : SandBlocks.SAND_PILE.get();
        // Thick piles are motion-blocking, so the heightmap can point just above them.
        BlockPos at = belowState.is(pile) ? below : top;
        BlockState current = level.getBlockState(at);
        if (current.is(pile)) {
            int layers = current.getValue(SandPileBlock.LAYERS);
            if (layers >= Math.min(max, SandPileBlock.MAX_LAYERS)) return false;
            level.setBlockAndUpdate(at, current.setValue(SandPileBlock.LAYERS, layers + 1));
            return true;
        }
        if (current.isAir() && pile.defaultBlockState().canSurvive(level, at)) {
            level.setBlockAndUpdate(at, pile.defaultBlockState());
            return true;
        }
        return false;
    }

    // ---- slowed mobs ----

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
        if ((entity.tickCount + entity.getId()) % 10 != 0) return;
        updateSlow(entity);
    }

    /** Returns true if the entity is now slowed. */
    public static boolean updateSlow(LivingEntity entity) {
        boolean exposed = !entity.getType().is(IGNORES)
                && !(entity instanceof Player p && (!Config.SANDSTORM_SLOWS_PLAYERS.get() || p.isCreative() || p.isSpectator()))
                && exposedTypeAt(entity.level(), BlockPos.containing(entity.getEyePosition())) != Type.NONE;
        apply(entity.getAttribute(Attributes.MOVEMENT_SPEED), SLOW_ID, exposed ? Config.SANDSTORM_SLOW.get() : 0);
        apply(entity.getAttribute(Attributes.FOLLOW_RANGE), RANGE_ID, exposed ? Config.SANDSTORM_FOLLOW_RANGE.get() : 0);
        return exposed;
    }

    private static void apply(AttributeInstance attr, ResourceLocation id, double value) {
        if (attr == null) return;
        boolean has = attr.hasModifier(id);
        if (value == 0) {
            if (has) attr.removeModifier(id);
        } else if (!has) {
            attr.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // ---- breezes ----

    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER || !Config.BREEZES.get()) return;
        if (!(event.getLevel() instanceof Level level)) return;
        if (exposedTypeAt(level, event.getPos()) == Type.NONE) return;
        event.addSpawnerData(new MobSpawnSettings.SpawnerData(EntityType.BREEZE, Config.BREEZE_WEIGHT.get(), 1, 1));
    }

    /** Breezes have no natural-spawn rules and fail the monster light check, so decide for them here. */
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof Breeze breeze) || event.getSpawnType() != MobSpawnType.NATURAL) return;
        Level level = event.getLevel().getLevel();
        BlockPos pos = breeze.blockPosition();
        if (exposedTypeAt(level, pos) == Type.NONE) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return;
        }
        boolean grounded = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        int nearby = level.getEntitiesOfClass(Breeze.class, new AABB(pos).inflate(48)).size();
        boolean ok = grounded && nearby < Config.BREEZE_MAX_NEARBY.get() && breeze.checkSpawnObstruction(level);
        event.setResult(ok ? MobSpawnEvent.PositionCheck.Result.SUCCEED : MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    private Sandstorms() {
    }
}
