package net.reimaden.touhouorigins.shikigami;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Remembers who a shikigami's master is, so the pact can outlive the entity set that carries it.
 *
 * <p>Apoli's {@code apoli:entity_set} keeps its membership in <b>static in-memory maps</b> that are
 * never serialised, and strips a UUID whenever that entity is removed with a reason that destroys
 * — which includes {@code KILLED} and {@code DISCARDED}, and a respawning player discards their old
 * entity. So the set loses the master the moment the master dies, and loses everything on a server
 * restart.
 *
 * <p>The {@code boundless_servitude} boolean, by contrast, is an {@code apoli:resource} inside a
 * container attachment declared {@code copyOnDeath} and serialised. It survives both. That mismatch
 * is the bug Eileen hit: the servant still reads as linked while the set is empty, so the glow, the
 * teleport and the shared death all fail quietly while the pact appears intact.
 *
 * <p>This attachment stores the master's UUID alongside the boolean, and
 * {@link ShikigamiPactHandler} re-seeds the set from it.
 *
 * <p>It is also <b>synced to clients</b>, which the set is not, and that is what makes the glow
 * work. {@code apoli:entity_glow} is evaluated entirely client-side (see Apoli's
 * {@code EntityGlowMixin}), so a condition reading the server-only set can only ever pass on a
 * client sharing the integrated server's JVM — the host of a LAN game, and nobody else.
 */
public final class ShikigamiPact {

    /** The set power whose membership we mirror. */
    public static final ResourceLocation SET_POWER =
        TouhouOrigins.id("global/boundless_servitude_set");

    /** The resource that says a pact exists at all. */
    public static final ResourceLocation BOOLEAN_POWER =
        TouhouOrigins.id("global/boundless_servitude_boolean");

    public record Data(Optional<UUID> master) {
        public static final Data EMPTY = new Data(Optional.empty());

        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("master").forGetter(Data::master)
        ).apply(i, Data::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Data::master, Data::new);
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TouhouOrigins.MOD_ID);

    public static final Supplier<AttachmentType<Data>> PACT =
        ATTACHMENTS.register("shikigami_pact", () -> AttachmentType.builder(() -> Data.EMPTY)
            .serialize(Data.CODEC)
            .sync(Data.STREAM_CODEC)
            .copyOnDeath()
            .build());

    private ShikigamiPact() {}

    /**
     * The master this entity has sworn to, or {@code null}.
     *
     * <p>Reads without writing. {@code getData} installs the default when the attachment is absent,
     * which is harmless on the server but happens on the render thread for every entity the glow
     * check walks past, so the presence check comes first.
     */
    public static @Nullable UUID masterOf(Entity entity) {
        if (entity == null || !entity.hasData(PACT.get())) return null;
        return entity.getData(PACT.get()).master().orElse(null);
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
