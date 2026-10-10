package dev.eteil.butcheryam2.butcher;

import dev.eteil.butcheryam2.Config;
import dev.eteil.butcheryam2.block.AbstractCarcassBlock;
import dev.eteil.butcheryam2.blockentity.CarcassBlockEntity;
import dev.eteil.butcheryam2.init.ModParticles;
import dev.eteil.butcheryam2.util.ServerWorkQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Bleeding, draining and breaking of carcass blocks. */
public final class Butchering {

    public static final String FLAG_BLEEDING = "isBleeding";
    public static final String FLAG_DRAINED = "isDrained";
    public static final String FLAG_PLUCKED = "pluckedFeathers";

    /** Ticks a carcass bleeds before it becomes a drained carcass. */
    private static final int BLEED_TICKS = 900;

    /** Total millibuckets of blood a carcass yields, matching the original's instant fill. */
    private static final int BLOOD_TOTAL = 500;

    /** The bleed is paid into the grate in this many instalments. */
    private static final int BLOOD_INSTALMENTS = 10;

    /** How far below a carcass a blood grate is searched for. */
    private static final int GRATE_SEARCH_DEPTH = 4;

    // ---------------------------------------------------------------- block hooks

    /** Right-clicking a fresh carcass starts it bleeding. */
    public static void onCarcassUsed(Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Only a fresh carcass bleeds — not the drained form, and not a severed head.
        MobDef def = MobDefs.forBlock(level.getBlockState(pos).getBlock());
        if (def == null || !def.bleeds() || !def.isEnabled()
                || level.getBlockState(pos).getBlock() != def.carcass().get()) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof CarcassBlockEntity be)) {
            return;
        }
        if (be.getFlag(FLAG_BLEEDING) || be.getFlag(FLAG_DRAINED)) {
            return;
        }

        be.setFlag(FLAG_BLEEDING, true);
        bleedEffects(serverLevel, pos);

        if (Config.INSTANT_BLEED.get()) {
            fillBloodGrate(serverLevel, pos, BLOOD_TOTAL);
            drain(serverLevel, pos, def);
        } else {
            // Pay the blood into the grate gradually, as the original's dripping did.
            int perInstalment = BLOOD_TOTAL / BLOOD_INSTALMENTS;
            for (int i = 1; i <= BLOOD_INSTALMENTS; i++) {
                ServerWorkQueue.schedule(BLEED_TICKS * i / BLOOD_INSTALMENTS, () -> {
                    if (level.getBlockEntity(pos) instanceof CarcassBlockEntity live
                            && live.getFlag(FLAG_BLEEDING)) {
                        fillBloodGrate(serverLevel, pos, perInstalment);
                        serverLevel.sendParticles(ModParticles.BLOOD.get(),
                                pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.02);
                    }
                });
            }
            ServerWorkQueue.schedule(BLEED_TICKS, () -> drain(serverLevel, pos, def));
        }
    }

    /** Breaking a carcass clears its progress; drops come from the block's own loot table. */
    public static void onCarcassBroken(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (level.isClientSide()) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof CarcassBlockEntity be) {
            be.setFlag(FLAG_BLEEDING, false);
        }
    }

    // ---------------------------------------------------------------- bleeding

    private static void bleedEffects(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.BLOOD.get(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);

        BlockPos below = pos.below();
        for (int depth = 1; depth <= GRATE_SEARCH_DEPTH; depth++) {
            if (ButcherUtil.isBlock(level, pos.below(depth), ButcherUtil.BLOOD_GRATE)) {
                return; // collected by the grate instead of pooling
            }
        }
        if (Config.BLOOD_PUDDLE.get() && level.getBlockState(below).isSolidRender(level, below)
                && level.getBlockState(pos.above()).isAir()) {
            Block puddle = ButcherUtil.block(ButcherUtil.BLOOD_PUDDLE);
            if (puddle != null && level.getBlockState(pos.above()).is(Blocks.AIR)) {
                level.setBlock(pos.above(), puddle.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Pours blood into the nearest Butchery blood grate up to
     * {@value #GRATE_SEARCH_DEPTH} blocks below, through its fluid capability.
     */
    private static void fillBloodGrate(ServerLevel level, BlockPos pos, int amount) {
        Fluid blood = BuiltInRegistries.FLUID.get(ResourceLocation.parse("butchery:blood"));
        if (blood == null) {
            return;
        }
        for (int depth = 1; depth <= GRATE_SEARCH_DEPTH; depth++) {
            BlockPos at = pos.below(depth);
            if (!ButcherUtil.isBlock(level, at, ButcherUtil.BLOOD_GRATE)) {
                continue;
            }
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, at, null);
            if (handler != null) {
                handler.fill(new FluidStack(blood, amount), IFluidHandler.FluidAction.EXECUTE);
            }
            return;
        }
    }

    /** Replaces a bled-out carcass with its drained form at stage 1. */
    private static void drain(ServerLevel level, BlockPos pos, MobDef def) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() != def.carcass().get() || def.drained() == null) {
            return; // block changed while bleeding
        }

        Direction facing = current.hasProperty(AbstractCarcassBlock.FACING)
                ? current.getValue(AbstractCarcassBlock.FACING)
                : Direction.NORTH;

        BlockState next = def.drained().get().defaultBlockState()
                .setValue(AbstractCarcassBlock.FACING, facing);
        // Part 1 always switched to the hanging stage here; keep a carcass bled on the ground lying down.
        next = withStage(next, stage(current) == 0 ? 0 : 1);

        level.setBlock(pos, next, 3);

        if (level.getBlockEntity(pos) instanceof CarcassBlockEntity be) {
            be.setFlag(FLAG_DRAINED, true);
            be.setFlag(FLAG_BLEEDING, false);
        }
        ButcherUtil.playSound(level, pos, "block.honey_block.break", 0.4F, 1.0F);
    }

    // ---------------------------------------------------------------- stage helpers

    /** The {@code blockstate} property of a block, looked up by name, or null if it has none. */
    @Nullable
    public static IntegerProperty stageProperty(BlockState state) {
        return state.getBlock().getStateDefinition()
                .getProperty(AbstractCarcassBlock.STATE_NAME) instanceof IntegerProperty prop ? prop : null;
    }

    public static int stage(BlockState state) {
        IntegerProperty prop = stageProperty(state);
        return prop == null ? -1 : state.getValue(prop);
    }

    /** Returns {@code state} with its stage set, or unchanged if the value is out of range. */
    public static BlockState withStage(BlockState state, int value) {
        IntegerProperty prop = stageProperty(state);
        if (prop == null || !prop.getPossibleValues().contains(value)) {
            return state;
        }
        return state.setValue(prop, value);
    }

    public static void setStage(Level level, BlockPos pos, int value) {
        BlockState state = level.getBlockState(pos);
        BlockState next = withStage(state, value);
        if (next != state) {
            level.setBlock(pos, next, 3);
        }
    }

    private Butchering() {}
}
