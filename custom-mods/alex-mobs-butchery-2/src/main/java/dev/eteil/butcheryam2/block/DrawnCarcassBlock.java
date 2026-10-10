package dev.eteil.butcheryam2.block;

import dev.eteil.butcheryam2.Config;
import dev.eteil.butcheryam2.blockentity.DrawnCarcassBlockEntity;
import dev.eteil.butcheryam2.butcher.ButcherUtil;
import dev.eteil.butcheryam2.butcher.CutStage;
import dev.eteil.butcheryam2.butcher.MobDef;
import dev.eteil.butcheryam2.butcher.MobDefs;
import dev.eteil.butcheryam2.gen.StepSpecs;
import dev.eteil.butcheryam2.gen.SegmentedSpecs;
import dev.eteil.butcheryam2.init.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A carcass too big or too long for a block model, built like Butchery's Ender Dragon carcass:
 * the block itself is invisible, a block-entity renderer draws Alex's Mobs' own model, and each
 * cut hides one more part of it.
 *
 * <ul>
 *   <li>Segmented bodies (centipede, anaconda) lose the head, then the tail, then one piece per cut.
 *   <li>Large bodies (whales, giant squid, sunbird, laviathan) follow their step list from {@link StepSpecs}.
 * </ul>
 */
public class DrawnCarcassBlock extends AbstractCarcassBlock {

    private static final VoxelShape SHAPE = box(-6, 0, -7, 26, 6, 24);

    private final String mob;

    public DrawnCarcassBlock(String mob) {
        this.mob = mob;
    }

    public String mob() {
        return mob;
    }

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrawnCarcassBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return tryCut(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /** The pieces of a segmented body still on the block, head first. */
    public static List<String> piecesLeft(String mob, DrawnCarcassBlockEntity be) {
        SegmentedSpecs.Spec spec = SegmentedSpecs.SPECS.get(mob);
        List<String> all = spec.pieces(count(spec, be));
        int removedFromTail = Math.max(0, be.cuts() - 1);
        int from = be.cuts() == 0 ? 0 : 1;
        int to = Math.max(from, all.size() - removedFromTail);
        return all.subList(from, to);
    }

    /**
     * The drop table of the piece the next cut takes: the head first, then pieces from the tail
     * end forward, the piece behind the head (the neck) last.
     */
    public static String segmentTable(String mob, DrawnCarcassBlockEntity be) {
        SegmentedSpecs.Spec spec = SegmentedSpecs.SPECS.get(mob);
        if (be.cuts() == 0) {
            return "head_drop";
        }
        List<String> all = spec.pieces(count(spec, be));
        int index = Math.max(1, all.size() - be.cuts());
        if (index == 1 && spec.neckDrop()) {
            return "neck_drop";
        }
        return all.get(index).equals("tail") ? "tail_drop" : "segment_drop";
    }

    public static int count(SegmentedSpecs.Spec spec, DrawnCarcassBlockEntity be) {
        int n = be.count();
        return n <= 0 ? spec.defaultCount() : Math.min(n, spec.maxCount());
    }

    /** The tool the next cut needs, or null when none. */
    @Nullable
    public static CutStage.Tool nextTool(String mob, DrawnCarcassBlockEntity be) {
        List<StepSpecs.Step> steps = StepSpecs.STEPS.get(mob);
        if (steps == null) {
            return CutStage.Tool.CLEAVER;
        }
        return be.cuts() < steps.size() ? CutStage.Tool.valueOf(steps.get(be.cuts()).tool()) : null;
    }

    public static boolean tryCut(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server)
                || !(level.getBlockState(pos).getBlock() instanceof DrawnCarcassBlock block)
                || !(level.getBlockEntity(pos) instanceof DrawnCarcassBlockEntity be)) {
            return false;
        }
        MobDef def = MobDefs.forBlock(block);
        if (def == null || !def.isEnabled()) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        CutStage.Tool tool = nextTool(block.mob, be);
        boolean ok = tool == CutStage.Tool.CLEAVER ? ButcherUtil.isCleaver(held)
                : tool == CutStage.Tool.KNIFE && ButcherUtil.isSkinningKnife(held);
        if (!ok) {
            return false;
        }

        double x = pos.getX() + 0.5, y = pos.getY() + 0.3, z = pos.getZ() + 0.5;
        int looting = ButcherUtil.lootingLevel(level, player);
        int cuts = be.cuts();
        boolean finished;
        List<StepSpecs.Step> steps = StepSpecs.STEPS.get(block.mob);
        if (steps != null) {
            StepSpecs.Step step = steps.get(cuts);
            if (!step.loot().isEmpty()) {
                ButcherUtil.dropLootWithLooting(server, x, y, z, def.loot(step.loot()), looting);
            }
            if (step.fragile() != null) {
                ButcherUtil.rollFragile(server, pos, player, def, step.fragile(), looting);
            }
            if (Config.ORGANS.get()) {
                for (String table : step.organs()) {
                    ButcherUtil.dropLootWithLooting(server, x, y, z, table, looting);
                }
            }
            finished = cuts + 1 >= steps.size();
        } else {
            ButcherUtil.dropLootWithLooting(server, x, y, z, def.loot(segmentTable(block.mob, be)), looting);
            finished = false;
        }
        server.sendParticles(tool == CutStage.Tool.KNIFE ? ModParticles.FEATHERS.get() : ModParticles.BLOOD.get(),
                x, y, z, 8, 0.6, 0.2, 0.6, 0.02);

        be.setCuts(cuts + 1);
        if (finished || (steps == null && piecesLeft(block.mob, be).isEmpty())) {
            level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        ButcherUtil.playSound(level, pos, tool == CutStage.Tool.KNIFE ? CutStage.SOUND_PLUCK : CutStage.SOUND_CUT,
                0.3F, 1.0F);
        ButcherUtil.damageHeldTool(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }
}
