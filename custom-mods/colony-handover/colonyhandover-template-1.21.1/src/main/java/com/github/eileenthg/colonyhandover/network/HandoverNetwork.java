package com.github.eileenthg.colonyhandover.network;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import com.github.eileenthg.colonyhandover.command.HandoverCommand;
import com.github.eileenthg.colonyhandover.handover.ColonySnapshot;
import com.github.eileenthg.colonyhandover.handover.Handover;
import com.github.eileenthg.colonyhandover.handover.ParentSlots;
import com.github.eileenthg.colonyhandover.scroll.ColonyIssues;
import com.github.eileenthg.colonyhandover.scroll.IssueSurvey;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Registers the town hall window's payloads and answers them on the server.
 *
 * <h2>Nothing from the client is trusted</h2>
 *
 * A request names a colony id, and a modified client can name any id it likes. So
 * every request looks the colony up again in the sender's own level and checks colony
 * manager rank - the same gate every command here uses - before doing anything. The
 * window being open proves nothing.
 *
 * <h2>The client handler never loads on a server</h2>
 *
 * The reply handler calls into client-only code. It is registered as a lambda whose
 * body does that, rather than a method reference, because a lambda body is only linked
 * when it runs - and on a dedicated server it never runs.
 */
@EventBusSubscriber(modid = ColonyHandover.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class HandoverNetwork {

    private static final String VERSION = "1";

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION)
                .playToServer(HandoverPayloads.Request.TYPE, HandoverPayloads.Request.CODEC,
                        HandoverNetwork::onRequest)
                .playToClient(HandoverPayloads.Summary.TYPE, HandoverPayloads.Summary.CODEC,
                        (summary, context) -> context.enqueueWork(
                                () -> com.github.eileenthg.colonyhandover.client.HandoverWindow
                                        .receive(summary)));
    }

    private static void onRequest(HandoverPayloads.Request request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            final ServerLevel level = player.serverLevel();
            final IColony colony = IMinecoloniesAPI.getInstance().getColonyManager()
                    .getColonyByWorld(request.colonyId(), level);
            if (colony == null) {
                say(player, ChatFormatting.RED, "That colony no longer exists.");
                return;
            }
            final Rank rank = colony.getPermissions().getRank(player);
            if (rank == null || !rank.isColonyManager()) {
                say(player, ChatFormatting.RED, "You need colony manager rank to do that.");
                return;
            }

            switch (request.action()) {
                case SUMMARY -> PacketDistributor.sendToPlayer(player, summarise(level, colony));
                case CHECKLIST -> giveChecklist(player, level, colony);
                case GRANT -> grant(player, level, colony);
            }
        });
    }

    /** The numbers the window shows, built from the same survey and snapshot the commands use. */
    private static HandoverPayloads.Summary summarise(ServerLevel level, IColony colony) {
        final ColonyIssues issues = IssueSurvey.of(level, colony);
        final ColonySnapshot snap = ColonySnapshot.of(colony);

        int children = 0;
        int partnered = 0;
        int known = 0;
        int placed = 0;
        int sameGender = 0;
        for (final ColonySnapshot.Citizen c : snap.citizens()) {
            if (c.child()) {
                children++;
            }
            if (c.partnerId() >= 0) {
                partnered++;
            }
            for (final ColonySnapshot.Parent p : List.of(c.parentA(), c.parentB())) {
                if (p != null && p.known()) {
                    known++;
                }
            }
            final ParentSlots.Slots slots = ParentSlots.assign(c.parentA(), c.parentB());
            placed += (slots.father() != null ? 1 : 0) + (slots.mother() != null ? 1 : 0);
            if (slots.misfiled()) {
                sameGender++;
            }
        }

        return new HandoverPayloads.Summary(colony.getID(), colony.getName(),
                issues.buildings().size(), issues.outstanding().size(),
                (int) issues.inaccessibleCount(),
                snap.citizens().size(), children,
                partnered / 2, known, placed, sameGender);
    }

    private static void giveChecklist(ServerPlayer player, ServerLevel level, IColony colony) {
        final ItemStack scroll = new ItemStack(ColonyHandover.CHECKLIST_SCROLL.get());
        scroll.set(ColonyHandover.COLONY_ISSUES.get(), IssueSurvey.of(level, colony));
        if (!player.getInventory().add(scroll)) {
            player.drop(scroll, false);
        }
        say(player, ChatFormatting.GREEN, "Here is a Conversion Checklist for " + colony.getName() + ".");
    }

    /**
     * Runs the handover, exactly as {@code handover confirm} does.
     *
     * <p>The window's "Sign the Charter" page is the confirmation here, standing in for
     * the command's typed token: both exist so one careless click or keystroke cannot
     * end a colony.
     */
    private static void grant(ServerPlayer player, ServerLevel level, IColony colony) {
        final ColonySnapshot snap = ColonySnapshot.of(colony);
        say(player, ChatFormatting.GREEN, "The charter is signed. " + snap.colonyName()
                + " stands on its own.");
        final Handover.Result result = Handover.run(level, snap);
        HandoverCommand.report(player.createCommandSourceStack(), result);
    }

    private static void say(ServerPlayer player, ChatFormatting colour, String text) {
        player.sendSystemMessage(Component.literal(text).withStyle(colour));
    }

    private HandoverNetwork() {}
}
