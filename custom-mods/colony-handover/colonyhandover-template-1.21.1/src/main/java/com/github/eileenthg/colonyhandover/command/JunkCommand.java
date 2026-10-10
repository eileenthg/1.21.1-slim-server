package com.github.eileenthg.colonyhandover.command;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * {@code /colonyhandover junk [stacks]} - stuff every colonist's pockets, for testing.
 *
 * <h2>Why this exists</h2>
 *
 * The coffer only has anything in it after a handover, and a colony's citizens are
 * usually carrying little. Testing paging, merging and the no-gaps-until-reopen rule
 * needs a coffer with several pages of mixed stacks, and filling inventories by hand
 * is not a realistic way to get one.
 *
 * <h2>Why operator-only</h2>
 *
 * It conjures items from nothing. Permission level 2 is the same bar as {@code /give},
 * which is exactly what this is a bulk version of.
 */
public final class JunkCommand {

    private static final int DEFAULT_STACKS = 12;

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("junk")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> run(ctx.getSource(), DEFAULT_STACKS))
                .then(Commands.argument("stacks", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> run(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "stacks"))));
    }

    private static int run(CommandSourceStack source, int stacksEach) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Run this in game, standing in the colony."));
            return 0;
        }
        final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                .getIColony(player.serverLevel(), player.blockPosition());
        if (colony == null) {
            source.sendFailure(Component.literal("You are not standing in a colony."));
            return 0;
        }

        // Every real item, so merging and paging get tested against genuine variety
        // rather than a handful of materials that all merge into a few stacks.
        final List<Holder.Reference<Item>> pool = BuiltInRegistries.ITEM.holders()
                .filter(h -> h.value() != Items.AIR)
                .toList();
        final RandomSource random = player.getRandom();

        int citizens = 0;
        int placed = 0;
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
            final InventoryCitizen inventory = citizen.getInventory();
            if (inventory == null) {
                continue;
            }
            citizens++;
            int given = 0;
            for (int slot = 0; slot < inventory.getSlots() && given < stacksEach; slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) {
                    continue;
                }
                final Item item = pool.get(random.nextInt(pool.size())).value();
                final ItemStack stack = new ItemStack(item,
                        1 + random.nextInt(Math.max(1, item.getDefaultMaxStackSize())));
                inventory.setStackInSlot(slot, stack);
                given++;
            }
            placed += given;
        }

        final int total = placed;
        final int people = citizens;
        source.sendSuccess(() -> Component.literal("Gave " + total + " stack(s) of junk to "
                + people + " colonist(s).").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private JunkCommand() {}
}
