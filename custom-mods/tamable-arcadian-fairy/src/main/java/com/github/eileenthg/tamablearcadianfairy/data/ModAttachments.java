package com.github.eileenthg.tamablearcadianfairy.data;

import com.github.eileenthg.tamablearcadianfairy.TamableArcadianFairy;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TamableArcadianFairy.MOD_ID);

    /** Both attachments are synced so the client can render owner info and open the settings screen. */
    public static final Supplier<AttachmentType<TameData>> TAME_DATA = ATTACHMENTS.register("tame_data",
            () -> AttachmentType.builder(() -> TameData.EMPTY)
                    .serialize(TameData.CODEC)
                    .sync(TameData.STREAM_CODEC)
                    .build());

    public static final Supplier<AttachmentType<ActState>> ACT_STATE = ATTACHMENTS.register("act_state",
            () -> AttachmentType.builder(() -> ActState.DEFAULT)
                    .serialize(ActState.CODEC)
                    .sync(ActState.STREAM_CODEC)
                    .build());

    private ModAttachments() {}

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
