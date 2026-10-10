package dev.eteil.butcheryam2.butcher;

import dev.eteil.butcheryam2.Config;
import dev.eteil.butcheryam2.blockentity.CarcassBlockEntity;
import dev.eteil.butcheryam2.init.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Advances a carcass through its butchering stages. */
public final class CutUp {

    /**
     * Attempts one butchering step at {@code pos}.
     *
     * @return true if a step was performed, so the caller can cancel the interaction
     */
    public static boolean tryCut(Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        MobDef def = MobDefs.forBlock(block);
        if (def == null || !def.isEnabled()) {
            return false;
        }

        // Heads are not butcherable; only the carcass forms are.
        if (def.head() != null && def.head().get() == block) {
            return false;
        }

        CutStage.Tool tool = toolOf(player.getMainHandItem());
        if (tool == null) {
            return false;
        }

        // A mob that bleeds must be drained first. Being the drained block *is* that condition:
        // the original also checked an isDrained flag, but that flag lives on the block entity and
        // is lost if the player picks the carcass up and puts it down again.
        if (def.bleeds() && (def.drained() == null || def.drained().get() != block)) {
            return false;
        }

        int stage = Butchering.stage(state);
        CutStage step = find(def, tool, stage);
        if (step == null) {
            return false;
        }

        // A pluck/skin step only happens once.
        if (step.flag() != null && level.getBlockEntity(pos) instanceof CarcassBlockEntity be
                && be.getFlag(step.flag())) {
            return false;
        }

        apply(serverLevel, pos, player, def, step);
        return true;
    }

    /** The tool that would advance the carcass at {@code pos}, or null if none applies. */
    @Nullable
    public static CutStage.Tool nextTool(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        MobDef def = MobDefs.forBlock(block);
        if (def == null || !def.isEnabled()) {
            return null;
        }
        if (def.head() != null && def.head().get() == block) {
            return null;
        }
        if (block instanceof dev.eteil.butcheryam2.block.DrawnCarcassBlock drawn) {
            return level.getBlockEntity(pos) instanceof dev.eteil.butcheryam2.blockentity.DrawnCarcassBlockEntity be
                    ? dev.eteil.butcheryam2.block.DrawnCarcassBlock.nextTool(drawn.mob(), be) : null;
        }
        if (def.bleeds() && (def.drained() == null || def.drained().get() != block)) {
            return null;
        }
        int stage = Butchering.stage(state);
        for (CutStage step : CutTables.forMob(def)) {
            if (step.from() == CutStage.ANY_STAGE || step.from() == stage) {
                if (step.flag() != null && level.getBlockEntity(pos) instanceof CarcassBlockEntity be
                        && be.getFlag(step.flag())) {
                    continue;
                }
                return step.tool();
            }
        }
        return null;
    }

    @Nullable
    private static CutStage find(MobDef def, CutStage.Tool tool, int stage) {
        for (CutStage step : CutTables.forMob(def)) {
            if (step.matches(tool, stage)) {
                return step;
            }
        }
        return null;
    }

    @Nullable
    private static CutStage.Tool toolOf(ItemStack stack) {
        if (ButcherUtil.isCleaver(stack)) {
            return CutStage.Tool.CLEAVER;
        }
        if (ButcherUtil.isSkinningKnife(stack)) {
            return CutStage.Tool.KNIFE;
        }
        return null;
    }

    private static void apply(ServerLevel level, BlockPos pos, Player player, MobDef def, CutStage step) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        int looting = ButcherUtil.lootingLevel(level, player);

        for (String table : step.loot()) {
            ButcherUtil.dropLootWithLooting(level, x, y, z, table, looting);
        }
        if (step.fragile() != null) {
            ButcherUtil.rollFragile(level, pos, player, def, step.fragile(), looting);
        }
        if (Config.ORGANS.get()) {
            for (String table : step.organs()) {
                ButcherUtil.dropLootWithLooting(level, x, y, z, table, looting);
            }
        }

        if (step.flag() != null && level.getBlockEntity(pos) instanceof CarcassBlockEntity be) {
            be.setFlag(step.flag(), true);
        }

        if (step.tool() == CutStage.Tool.KNIFE) {
            level.sendParticles(ModParticles.FEATHERS.get(), x, y, z, 16, 0.3, 0.3, 0.3, 0.2);
        } else {
            level.sendParticles(ModParticles.BLOOD.get(), x, y, z, 8, 0.25, 0.25, 0.25, 0.05);
        }

        BlockState state = level.getBlockState(pos);
        if (step.to() == CutStage.DESTROY) {
            level.levelEvent(2001, pos, Block.getId(state));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            Butchering.setStage(level, pos, step.to());
        }

        ButcherUtil.playSound(level, pos, step.sound(), 0.3F, 1.0F);
        ButcherUtil.damageHeldTool(player);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    private CutUp() {}
}
