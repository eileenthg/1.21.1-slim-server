package com.github.eileenthg.colonyhandover.command;

import com.github.eileenthg.colonyhandover.ColonyHandover;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Hooks our commands onto the server command dispatcher.
 *
 * <p>{@link RegisterCommandsEvent} lives on the NeoForge game bus, not the mod
 * bus, which is why this uses a bare {@code @EventBusSubscriber} (the game bus
 * is its default) rather than the {@code Bus.MOD} form used for registry and
 * lifecycle events.
 */
@EventBusSubscriber(modid = ColonyHandover.MODID)
public final class ModCommands {

    /**
     * Bumped whenever the command surface changes, so a log line identifies
     * exactly which jar is loaded. Two builds can differ by nine bytes.
     */
    private static final String BUILD_STAMP = "s7.4-fixes";

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(PreviewCommand.build()
                .then(PlaceCommand.place())
                .then(PlaceCommand.undo())
                .then(DiagnoseCommand.build())
                .then(ReadinessCommand.build())
                .then(MarkCommand.build())
                .then(HandoverCommand.build())
                .then(CofferCommand.build())
                .then(ParentsCommand.build())
                .then(ChecklistCommand.build())
                .then(JunkCommand.build()));
        ColonyHandover.LOGGER.info("Registered /colonyhandover commands "
                + "[build {} | stages 1-3: preview, place, place dry, place force, undo, diagnose, readiness, marks, handover, handover dry, handover confirm, handover debug step/abort, coffer, parents, checklist, junk]", BUILD_STAMP);
    }

    private ModCommands() {}
}
