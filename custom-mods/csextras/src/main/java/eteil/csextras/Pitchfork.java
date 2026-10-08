package eteil.csextras;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Convenient Decor's pitchfork is decorative only; this makes it a hay/leaf clearing tool. */
public final class Pitchfork {
    public static final TagKey<Item> PITCHFORKS = TagKey.create(Registries.ITEM, CSExtras.id("pitchforks"));
    public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, CSExtras.id("pitchfork_mineable"));
    public static final TagKey<Block> SWEEPABLE = TagKey.create(Registries.BLOCK, CSExtras.id("pitchfork_sweepable"));

    private static final ThreadLocal<Boolean> SWEEPING = ThreadLocal.withInitial(() -> false);

    static boolean holding(Player player) {
        return Config.PITCHFORK.get() && player.getMainHandItem().is(PITCHFORKS);
    }

    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (holding(event.getEntity()) && (event.getState().is(MINEABLE) || event.getState().is(SWEEPABLE))) {
            event.setNewSpeed(event.getNewSpeed() * Config.PITCHFORK_SPEED.get().floatValue());
        }
    }

    static void onBreak(BlockEvent.BreakEvent event) {
        if (SWEEPING.get() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!holding(player) || player.isShiftKeyDown() || !event.getState().is(SWEEPABLE)) return;
        sweep(player, event.getPos());
    }

    /** Breaks sweepable blocks around the origin through normal player breaking (drops, protection events). */
    public static int sweep(ServerPlayer player, BlockPos origin) {
        int r = Config.PITCHFORK_RADIUS.get();
        int broken = 0;
        SWEEPING.set(true);
        try {
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -1, -r), origin.offset(r, 1, r))) {
                if (pos.equals(origin) || !player.level().getBlockState(pos).is(SWEEPABLE)) continue;
                if (player.gameMode.destroyBlock(pos.immutable())) broken++;
            }
        } finally {
            SWEEPING.set(false);
        }
        return broken;
    }

    private Pitchfork() {
    }
}
