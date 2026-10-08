package net.eileenthg.maidchef;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ChefCommand {
    private ChefCommand() {
    }

    /** Why is (or isn't) this Chef cooking? Without an argument: the nearest Chef maid within 16 blocks. */
    private static int explain(CommandSourceStack src, @javax.annotation.Nullable net.minecraft.world.entity.Entity target) {
        com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid = null;
        if (target instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid m) {
            maid = m;
        } else if (target == null) {
            maid = src.getLevel().getEntitiesOfClass(com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid.class,
                            new net.minecraft.world.phys.AABB(net.minecraft.core.BlockPos.containing(src.getPosition())).inflate(16),
                            m -> m.getTask() == TaskChef.INSTANCE)
                    .stream().min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(src.getPosition()))).orElse(null);
        }
        if (maid == null || maid.getTask() != TaskChef.INSTANCE) {
            src.sendFailure(Component.literal("No Chef maid there (look within 16 blocks, or name one)."));
            return 0;
        }
        if (!(maid.level() instanceof net.minecraft.server.level.ServerLevel level)) return 0;
        ChefData data = TaskChef.data(maid);
        src.sendSuccess(() -> Component.literal("Chef status: " + data.status()), false);
        for (String raw : Planner.explain(level, maid, data, 10)) {
            String line = raw.indexOf(Planner.ITEM_SEP) >= 0 ? raw.substring(0, raw.indexOf(Planner.ITEM_SEP)) : raw;
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    /** Block or unblock an ingredient on your own request list (the chat buttons run this). */
    private static int block(CommandSourceStack src, net.minecraft.resources.ResourceLocation id, boolean blocked) {
        if (src.getPlayer() == null) {
            src.sendFailure(Component.literal("Only players have request lists."));
            return 0;
        }
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)) {
            src.sendFailure(Component.literal("Unknown item " + id));
            return 0;
        }
        ChefRequests.get(src.getServer()).setBlocked(src.getPlayer().getUUID(), id.toString(), blocked);
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id).getDefaultInstance().getHoverName().getString();
        src.sendSuccess(() -> Component.literal(blocked
                ? "Your maids won't ask for " + name + " any more. (Undo in the Chef task's Requests tab, or /maidchef unblock " + id + ")"
                : "Your maids may ask for " + name + " again."), false);
        net.eileenthg.maidchef.net.RequestPayloads.sendTo(src.getPlayer());
        return 1;
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal(MaidChef.MOD_ID).requires(s -> s.hasPermission(0))
                .then(Commands.literal("explain")
                        .executes(ctx -> explain(ctx.getSource(), null))
                        .then(Commands.argument("maid", net.minecraft.commands.arguments.EntityArgument.entity())
                                .executes(ctx -> explain(ctx.getSource(), net.minecraft.commands.arguments.EntityArgument.getEntity(ctx, "maid")))))
                .then(Commands.literal("block").then(Commands.argument("item", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                        .executes(ctx -> block(ctx.getSource(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "item"), true))))
                .then(Commands.literal("unblock").then(Commands.argument("item", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                        .executes(ctx -> block(ctx.getSource(), net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "item"), false)))));
    }
}
