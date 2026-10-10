package net.reimaden.touhouorigins.shikigami;

import com.mojang.brigadier.CommandDispatcher;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * {@code /shikigami status [player]} — prints the three pieces of state the pact actually runs on.
 *
 * <p>Every shikigami bug so far has been one of these three disagreeing with the other two: the
 * boolean says linked, the set says empty, the record says nothing. Reading them side by side turns
 * "the outline stopped working" into a specific answer, without another round of guessing.
 *
 * <p>Available to any player (permission level 0) so it can be run from a second client during a
 * two-player test, not only from the host console.
 */
public final class ShikigamiCommand {

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("shikigami")
            .then(Commands.literal("status")
                .executes(c -> report(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> report(c.getSource(),
                        EntityArgument.getPlayer(c, "player"))))));
    }

    private static int report(CommandSourceStack src, ServerPlayer subject) {
        String name = subject.getGameProfile().getName();
        line(src, ChatFormatting.GOLD, "── shikigami: " + name + " ──");

        PowerContainer container = PowerContainer.of(subject);
        boolean hasBoolean = container != null && container.hasPower(ShikigamiPact.BOOLEAN_POWER);
        OptionalInt bool = PowerResources.read(container, ShikigamiPact.BOOLEAN_POWER);
        line(src, bool.orElse(0) == 1 ? ChatFormatting.GREEN : ChatFormatting.RED,
            "boolean: " + (hasBoolean
                ? (bool.isPresent() ? String.valueOf(bool.getAsInt()) : "granted but unreadable")
                : "power not granted"));

        // The durable record. If this is empty while the boolean says 1, the pact predates the
        // record and the repair pass has not adopted it yet.
        UUID recorded = ShikigamiPact.masterOf(subject);
        line(src, recorded == null ? ChatFormatting.RED : ChatFormatting.GREEN,
            "recorded master: " + describe(src, recorded));

        boolean hasSet = container != null && container.hasPower(ShikigamiPact.SET_POWER);
        List<UUID> members = EntitySetPower.iterationOrder(subject, ShikigamiPact.SET_POWER, false);
        line(src, !hasSet ? ChatFormatting.RED
                : members.isEmpty() ? ChatFormatting.YELLOW : ChatFormatting.GREEN,
            "set (" + (hasSet ? "granted" : "NOT granted") + "): "
                + (members.isEmpty() ? "empty" : String.valueOf(members.size())));
        for (UUID m : members) {
            line(src, ChatFormatting.GRAY, "  · " + describe(src, m));
        }

        // The other half of the pact. A master holds no record of their own — the servant does —
        // so the only way to answer "who serves me" is to ask every servant who they serve.
        List<ServerPlayer> servants = servantsOf(src, subject);
        line(src, servants.isEmpty() ? ChatFormatting.GRAY : ChatFormatting.AQUA,
            "serving this player: " + (servants.isEmpty() ? "nobody" : String.valueOf(servants.size())));
        for (ServerPlayer s : servants) {
            line(src, ChatFormatting.GRAY, "  · " + s.getGameProfile().getName());
        }

        // Reading the set is server-only, so this is the one line a client cannot infer for itself:
        // the glow now runs off the record, not the set.
        line(src, ChatFormatting.DARK_GRAY,
            "glow source: recorded master (synced) · death source: recorded master");
        return 1;
    }

    /**
     * Every online player who names this one as their master.
     *
     * <p>Only finds servants who are online: the record lives on the servant's own entity, so an
     * offline servant is unreachable from here. A master with no online servants is reported as
     * "nobody", which is not the same as having none.
     */
    private static List<ServerPlayer> servantsOf(CommandSourceStack src, ServerPlayer master) {
        List<ServerPlayer> found = new ArrayList<>();
        for (ServerPlayer candidate : src.getServer().getPlayerList().getPlayers()) {
            if (candidate == master) continue;
            UUID theirs = ShikigamiPact.masterOf(candidate);
            if (theirs != null && theirs.equals(master.getUUID())) found.add(candidate);
        }
        return found;
    }

    private static String describe(CommandSourceStack src, UUID uuid) {
        if (uuid == null) return "none";
        ServerPlayer p = src.getServer().getPlayerList().getPlayer(uuid);
        return p != null ? p.getGameProfile().getName() + " (online)" : uuid + " (offline)";
    }

    private static void line(CommandSourceStack src, ChatFormatting colour, String text) {
        src.sendSuccess(() -> Component.literal(text).withStyle(colour), false);
    }
}
