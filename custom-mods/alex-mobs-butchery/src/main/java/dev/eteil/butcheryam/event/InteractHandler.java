package dev.eteil.butcheryam.event;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import dev.eteil.butcheryam.block.AbstractCarcassBlock;
import dev.eteil.butcheryam.butcher.ButcherUtil;
import dev.eteil.butcheryam.butcher.Butchering;
import dev.eteil.butcheryam.butcher.MobDef;
import dev.eteil.butcheryam.butcher.MobDefs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

/** Hangs carcasses on hooks and ropes, and drives the butchering progression. */
@EventBusSubscriber(modid = ButcheryAlexsMobs.MODID)
public final class InteractHandler {

    /** The stage a carcass takes when hung; the ground progression starts at 0 instead. */
    private static final int HANGING_STAGE = 1;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();

        // Butchering itself is handled by the carcass block's own use hook; the hook and rope
        // belong to Butchery, so hanging has to be caught here.
        if (tryHang(level, pos, player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Right-clicking a Butchery hook or rope with a carcass in hand hangs it in the empty block
     * below, as Butchery hangs its own carcasses; the hook or rope stays where it is.
     */
    public static boolean tryHang(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return false;
        }
        if (!ButcherUtil.isBlock(level, pos, ButcherUtil.HOOK)
                && !ButcherUtil.isBlock(level, pos, ButcherUtil.ROPE)) {
            return false;
        }

        ItemStack held = player.getMainHandItem();
        Block carcass = carcassFor(held);
        if (carcass == null) {
            return false;
        }

        BlockPos below = pos.below();
        if (!level.getBlockState(below).isAir()) {
            return false;
        }

        BlockState state = Butchering.withStage(carcass.defaultBlockState(), HANGING_STAGE)
                .setValue(AbstractCarcassBlock.FACING, player.getDirection().getOpposite());
        level.setBlock(below, state, 3);

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        ButcherUtil.playSound(level, below, "block.honey_block.place", 0.6F, 1.0F);
        ButcherUtil.playSound(level, pos, "block.chain.hit", 0.6F, 1.0F);
        return true;
    }

    /** The carcass block an item corresponds to — fresh or drained — or null. */
    @Nullable
    private static Block carcassFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (MobDef def : MobDefs.ALL) {
            if (!def.isEnabled()) {
                continue;
            }
            if (stack.getItem() == def.carcass().get().asItem()) {
                return def.carcass().get();
            }
            if (def.drained() != null && stack.getItem() == def.drained().get().asItem()) {
                return def.drained().get();
            }
        }
        return null;
    }

    private InteractHandler() {}
}
