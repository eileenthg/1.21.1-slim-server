package com.github.eileenthg.colonyhandover.coffer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The chest the colony leaves behind, standing where its town hall did.
 *
 * <h2>Why a block and not just a command</h2>
 *
 * A command is invisible. Someone returning to a converted village weeks later has
 * no reason to think there is anything to collect, and no way to find out except by
 * remembering a command they typed once. A block in the middle of the village where
 * the town hall used to be says what happened without being told.
 *
 * <h2>Why this position specifically</h2>
 *
 * Deletion removes every {@code AbstractBlockHut} unconditionally (finding #2), so
 * the town hall marker is gone and its position is a hole in the middle of the
 * building. That is both the most findable spot in the village and the one that
 * needs filling anyway - the coffer stands exactly where the colony was
 * administered from.
 *
 * <h2>The command stays regardless</h2>
 *
 * Blocks can be broken, buried, or lost to a creeper. The rescued goods live in
 * level {@link CofferData}, not in this block, so {@code /colonyhandover coffer}
 * still reaches them if the block ever goes. This is the signpost, not the vault.
 *
 * <h2>It always drops</h2>
 *
 * Its loot table deliberately omits the {@code minecraft:survives_explosion}
 * condition that {@code BlockLootSubProvider} applies to almost every block
 * ({@code BlockLootSubProvider:128}). That condition is what makes an exploded
 * block drop only {@code 1/radius} of the time, so leaving it out means a creeper
 * relocates the coffer rather than deleting it. Paired with
 * {@link CofferItemProtection}, which keeps the resulting item entity alive
 * through the same blast, the coffer survives its own destruction.
 */
public final class CofferBlock extends Block {

    public CofferBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        final CofferData data = CofferData.get((ServerLevel) level);
        if (data.isEmpty()) {
            // Opening an empty screen tells you nothing. Saying so tells you the
            // coffer worked and is finished with.
            player.displayClientMessage(Component
                    .literal("The coffer is empty - everything has been collected.")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new CofferMenu(containerId, inventory, data),
                Component.literal("Colony Coffer")));
        return InteractionResult.CONSUME;
    }
}
