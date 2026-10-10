package net.reimaden.touhouorigins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.action.ActionType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Teleports the acting entity to another entity matching a bi-entity condition, anywhere on the
 * server — used by {@code global/by_masters_side} so a shikigami can snap to their master, across
 * dimensions if need be.
 *
 * <p>This is the one Voile type that could not be expressed by composing Apoli's own actions.
 * {@code apoli:teleport_to} is a <i>bi-entity</i> action: it already has a target and simply moves
 * the actor to it. The whole point here is the <i>search</i> — find who satisfies the condition,
 * with a distinct failure path when nobody does. That search is what needs Java.
 *
 * <p>Voile's semantics preserved deliberately, including the odd ones:
 * <ul>
 *   <li>every loaded level is searched, not just the actor's — the master may be in the Nether;</li>
 *   <li>a match is chosen <b>at random</b> rather than by proximity, which matters if a master ever
 *       has several bonded servants;</li>
 *   <li>the actor's own yaw and pitch survive the trip, so you arrive facing the way you were;</li>
 *   <li>the sound plays twice on purpose — once where you left, once where you arrived.</li>
 * </ul>
 */
public final class TeleportToEntityAction implements ActionType<EntityCtx, TeleportToEntityAction.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("teleport_to_entity");

    public record Cfg(Optional<BiEntityAction> biEntityAction,
                      Optional<BiEntityCondition> biEntityCondition,
                      Optional<EntityAction> failAction,
                      Optional<ResourceLocation> sound,
                      float volume,
                      float pitch) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("bientity_action", BiEntityAction.CODEC).forGetter(Cfg::biEntityAction),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::biEntityCondition),
            LoggedOptionalField.strict("fail_action", EntityAction.CODEC).forGetter(Cfg::failAction),
            IdCodecs.ID.optionalFieldOf("sound").forGetter(Cfg::sound),
            Codec.FLOAT.optionalFieldOf("volume", 1.0F).forGetter(Cfg::volume),
            Codec.FLOAT.optionalFieldOf("pitch", 1.0F).forGetter(Cfg::pitch)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.entity();
        if (!(actor.level() instanceof ServerLevel actorLevel)) return;
        MinecraftServer server = actorLevel.getServer();

        List<Entity> candidates = findCandidates(cfg, actor, server);
        if (candidates.isEmpty()) {
            cfg.failAction().ifPresent(a -> a.run(ctx));
            return;
        }

        Entity target = candidates.get(actorLevel.random.nextInt(candidates.size()));
        if (!(target.level() instanceof ServerLevel targetLevel)) return;

        // Captured before the move so the departure sound plays where the actor actually left.
        double fromX = actor.getX();
        double fromY = actor.getY();
        double fromZ = actor.getZ();

        boolean teleported = actor.teleportTo(targetLevel, target.getX(), target.getY(), target.getZ(),
            Set.of(), actor.getYRot(), actor.getXRot());
        if (!teleported) return;

        playSounds(cfg, actor, actorLevel, fromX, fromY, fromZ);
        cfg.biEntityAction().ifPresent(a ->
            a.run(BiEntityCtx.of(actor, target, actor.level())));
    }

    /** Every entity on the server, in any dimension, that satisfies the condition — minus the actor. */
    private static List<Entity> findCandidates(Cfg cfg, Entity actor, MinecraftServer server) {
        List<Entity> candidates = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            candidates.addAll(level.getEntities(EntityTypeTest.forClass(Entity.class), candidate -> {
                if (candidate == actor) return false;
                if (cfg.biEntityCondition().isEmpty()) return true;
                return cfg.biEntityCondition().get()
                    .test(BiEntityCtx.of(actor, candidate, actor.level()));
            }));
        }
        return candidates;
    }

    private static void playSounds(Cfg cfg, Entity actor, ServerLevel origin,
                                   double fromX, double fromY, double fromZ) {
        actor.gameEvent(GameEvent.TELEPORT);
        if (cfg.sound().isEmpty() || actor.isSilent()) return;

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(cfg.sound().get());
        if (sound == null) sound = SoundEvent.createVariableRangeEvent(cfg.sound().get());

        // Where they vanished from — for anyone still standing there.
        origin.playSound(null, fromX, fromY, fromZ, sound, actor.getSoundSource(),
            cfg.volume(), cfg.pitch());
        // And where they arrived, which for a player is heard locally on their own client.
        actor.playSound(sound, cfg.volume(), cfg.pitch());
    }
}
