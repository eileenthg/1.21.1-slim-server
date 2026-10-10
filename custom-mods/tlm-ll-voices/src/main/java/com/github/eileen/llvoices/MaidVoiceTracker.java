package com.github.eileen.llvoices;

import com.github.JumDa5he.callresponse.compat.emotion.EmotionBetrayalManager;
import com.github.JumDa5he.callresponse.compat.emotion.EmotionData;
import com.github.JumDa5he.callresponse.compat.emotion.MaidEnvironmentLight;
import com.github.JumDa5he.callresponse.compat.hunger.HungerData;
import com.github.JumDa5he.callresponse.compat.talk.TalkEventManager;
import com.github.eileen.llvoices.logic.ChangeCause;
import com.github.eileen.llvoices.logic.ChangeContext;
import com.github.eileen.llvoices.logic.DriftLevel;
import com.github.eileen.llvoices.logic.DriftZone;
import com.github.eileen.llvoices.logic.HungerStage;
import com.github.eileen.llvoices.voice.VoiceDispatcher;
import com.github.eileen.llvoices.voice.VoiceKind;
import com.github.eileen.llvoices.voice.VoicePrompts;
import com.github.eileen.llvoices.voice.VoiceQueue;
import com.github.eileen.llvoices.voice.VoiceRequest;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches each owned maid near her owner once a second: her Love & Loathe trust, fear and hunger, her health, and
 * what is going on around her. Anything worth mentioning is queued, and when she is free to talk the most important
 * line goes to {@link VoiceDispatcher}.
 */
public final class MaidVoiceTracker {
    static final int SCAN_INTERVAL_TICKS = 20;
    /** "Just happened" window for the hints used to explain a trust/fear change. */
    static final long RECENT_TICKS = 60;
    /** Trust/fear changes this close together are one reaction (L&L often moves both a tick apart). */
    static final long PASSIVE_SETTLE_TICKS = 40;
    /** After this long out of sight a maid is re-baselined silently instead of reacting to everything she missed. */
    static final long STALE_TICKS = 200;
    static final long FORGET_STATE_TICKS = 20 * 60 * 10;
    /** Check-ins only happen when the owner is close enough to plausibly be chatting. */
    static final double STATUS_RADIUS = 10.0;
    static final int HEAD_SLOT = 5;

    public static final MaidVoiceTracker INSTANCE = new MaidVoiceTracker();
    private static final Map<UUID, MaidState> STATES = new ConcurrentHashMap<>();

    private MaidVoiceTracker() {
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!VoiceConfig.loaded() || !VoiceConfig.ENABLED.get()) {
            return;
        }
        if (event.getServer().getTickCount() % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        double radius = VoiceConfig.HEARING_RADIUS.get();
        for (ServerPlayer owner : event.getServer().getPlayerList().getPlayers()) {
            if (owner.isSpectator()) {
                continue;
            }
            for (EntityMaid maid : owner.level().getEntitiesOfClass(EntityMaid.class, owner.getBoundingBox().inflate(radius))) {
                if (maid.isAlive() && maid.isTame() && owner.getUUID().equals(maid.getOwnerUUID())) {
                    this.scan(maid, owner);
                }
            }
        }
        long now = event.getServer().overworld().getGameTime();
        STATES.values().removeIf(state -> now - state.lastSeenTick > FORGET_STATE_TICKS || now < state.lastSeenTick);
    }

    @SubscribeEvent
    public void onMaidHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof EntityMaid maid && !maid.level().isClientSide()) {
            MaidState state = STATES.get(maid.getUUID());
            if (state != null) {
                state.lastHurtTick = maid.level().getGameTime();
                LivingEntity owner = maid.getOwner();
                state.hurtByOwner = owner != null && event.getSource().getEntity() == owner;
            }
        }
    }

    @SubscribeEvent
    public void onMaidDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof EntityMaid dead && !dead.level().isClientSide()) {
            long now = dead.level().getGameTime();
            for (EntityMaid witness : dead.level().getEntitiesOfClass(EntityMaid.class, dead.getBoundingBox().inflate(16.0))) {
                MaidState state = STATES.get(witness.getUUID());
                if (state != null && witness != dead) {
                    state.lastMaidDeathTick = now;
                }
            }
        }
    }

    /** One observation of a maid her owner is near. Public so game tests can drive it with an off-list player. */
    public void scan(EntityMaid maid, ServerPlayer owner) {
        long now = maid.level().getGameTime();
        MaidState state = STATES.computeIfAbsent(maid.getUUID(), id -> new MaidState());
        if (isBusyWithLoveLoathe(maid)) {
            // Betraying, or in one of L&L's group talks: L&L is driving her, so keep out and start fresh afterwards.
            state.initialized = false;
            state.queue.clear();
            state.lastSeenTick = now;
            return;
        }
        boolean stale = now - state.lastSeenTick > STALE_TICKS || now < state.lastSeenTick;
        state.lastSeenTick = now;

        Snapshot snap = Snapshot.of(maid, owner);
        if (!state.initialized || stale) {
            state.baseline(snap, maid, now);
            return;
        }

        this.observeSurroundings(maid, owner, state, snap, now);
        if (VoiceConfig.NEEDS_ENABLED.get()) {
            this.observeNeeds(maid, state, snap, now);
        }
        this.observeEmotion(maid, owner, state, snap, now);
        this.observeStatus(maid, owner, state, snap, now);
        state.remember(snap);

        this.trySpeak(maid, owner, state, now);
    }

    private static boolean isBusyWithLoveLoathe(EntityMaid maid) {
        try {
            return EmotionBetrayalManager.isBetraying(maid) || TalkEventManager.isParticipant(maid);
        } catch (LinkageError error) {
            return false;
        }
    }

    // ------------------------------------------------------------------ surroundings (hints for passive changes)

    private void observeSurroundings(EntityMaid maid, ServerPlayer owner, MaidState state, Snapshot snap, long now) {
        if (snap.sitting && !state.sitting) {
            state.sitEdgeTick = now;
        }
        state.sitting = snap.sitting;

        boolean armorFull = isArmorFull(maid);
        if (armorFull && !state.armorFull) {
            state.armorEdgeTick = now;
        }
        state.armorFull = armorFull;

        ItemStack head = maid.getMaidInv().getStackInSlot(HEAD_SLOT);
        if (!head.isEmpty() && (state.headItem.isEmpty() || !ItemStack.isSameItem(state.headItem, head))) {
            state.headChangeTick = now;
        }
        state.headItem = head.copy();

        if (maid.isPassenger()) {
            state.passengerTick = now;
        }
        if (isOwnerStaringWithItem(owner, maid)) {
            state.ownerStaringTick = now;
        }
        if (snap.hunger > state.hunger + 0.5F) {
            state.lastAteTick = now;
        }
    }

    // ------------------------------------------------------------------ needs

    private void observeNeeds(EntityMaid maid, MaidState state, Snapshot snap, long now) {
        HungerStage stage = snap.hungerStage;
        HungerStage previous = state.hungerStage;

        if (stage == HungerStage.HUNGRY && previous.ordinal() > HungerStage.HUNGRY.ordinal()) {
            state.queue.offer(VoicePrompts.hungerPeckish(now));
        }
        // L&L starts auto-eating below 35; with nothing to eat she can only ask.
        if (stage.isHungry() && snap.hunger < 35.0F && !hasFood(maid)) {
            if (!state.queue.hasPending(VoiceKind.HUNGER_NO_FOOD)) {
                state.queue.offer(VoicePrompts.hungerNoFood(now, stage));
            }
        }
        if (stage.ordinal() <= HungerStage.VERY_HUNGRY.ordinal()) {
            state.wasStarving = true;
        } else if (state.wasStarving && stage.ordinal() >= HungerStage.SATISFIED.ordinal()) {
            state.wasStarving = false;
            state.queue.dropTopicsStartingWith(VoiceKind.HUNGER_NO_FOOD.key());
            state.queue.offer(VoicePrompts.hungerSated(now));
        }
        if (stage == HungerStage.FULL && previous.ordinal() <= HungerStage.SATISFIED.ordinal()) {
            state.queue.offer(VoicePrompts.hungerFull(now));
        }

        int band = healthBand(snap.healthRatio);
        if (band > state.healthBand) {
            state.queue.offer(band == 2 ? VoicePrompts.hurtCritical(now) : VoicePrompts.hurtLow(now));
            state.wounded = true;
        } else if (state.wounded && snap.healthRatio >= 0.9F) {
            state.wounded = false;
            state.queue.dropTopicsStartingWith(VoiceKind.HURT_LOW.key());
            state.queue.dropTopicsStartingWith(VoiceKind.HURT_CRITICAL.key());
            state.queue.offer(VoicePrompts.healed(now));
        }
        state.healthBand = band;
    }

    static int healthBand(float ratio) {
        return ratio < 0.25F ? 2 : ratio < 0.5F ? 1 : 0;
    }

    // ------------------------------------------------------------------ emotion

    private void observeEmotion(EntityMaid maid, ServerPlayer owner, MaidState state, Snapshot snap, long now) {
        int deltaTrust = snap.trust - state.trust;
        int deltaFear = snap.fear - state.fear;
        if ((deltaTrust != 0 || deltaFear != 0) && VoiceConfig.PASSIVE_ENABLED.get()) {
            if (state.passiveContext == null) {
                state.passiveContext = this.describeSurroundings(maid, owner, state, now);
                state.passiveTrust = 0;
                state.passiveFear = 0;
            }
            state.passiveTrust += deltaTrust;
            state.passiveFear += deltaFear;
            state.passiveLastTick = now;
        }
        if (state.passiveContext != null && now - state.passiveLastTick >= PASSIVE_SETTLE_TICKS) {
            this.flushPassive(maid, state, now);
        }

        if (VoiceConfig.MOOD_ENABLED.get() && !snap.tendency.equals(state.tendency)) {
            state.queue.offer(VoicePrompts.moodShift(now, state.tendency, snap.tendency));
        }

        if (VoiceConfig.DRIFT_ENABLED.get()) {
            this.observeDrift(state, snap, now);
        }
    }

    private void flushPassive(EntityMaid maid, MaidState state, long now) {
        ChangeContext context = state.passiveContext.withDeltas(state.passiveTrust, state.passiveFear);
        state.passiveContext = null;
        if (context.deltaTrust() == 0 && context.deltaFear() == 0) {
            return;
        }
        ChangeCause cause = context.classify();
        if (cause.voicedElsewhere()) {
            return;
        }
        double chance = cause.routine() ? VoiceConfig.ROUTINE_PASSIVE_CHANCE.get() : VoiceConfig.PASSIVE_CHANCE.get();
        if (maid.getRandom().nextDouble() < chance) {
            state.queue.offer(VoicePrompts.passive(now, cause, context.deltaTrust(), context.deltaFear()));
        }
    }

    private ChangeContext describeSurroundings(EntityMaid maid, ServerPlayer owner, MaidState state, long now) {
        return ChangeContext.builder(0, 0)
                .hurt(now - state.lastHurtTick <= RECENT_TICKS, state.hurtByOwner)
                .maidDiedNearby(now - state.lastMaidDeathTick <= RECENT_TICKS)
                .ateRecently(now - state.lastAteTick <= RECENT_TICKS)
                // Saddle launches land a few seconds after she leaves the owner's shoulders.
                .ridingOrThrown(now - state.passengerTick <= RECENT_TICKS * 3)
                .leashed(maid.isLeashed() || isHooked(maid))
                .headItemChanged(now - state.headChangeTick <= RECENT_TICKS)
                .ownerStaringWithItem(now - state.ownerStaringTick <= RECENT_TICKS)
                .justSat(now - state.sitEdgeTick <= RECENT_TICKS)
                .sleeping(maid.isSleeping())
                .armorJustCompleted(now - state.armorEdgeTick <= RECENT_TICKS)
                .inDarkness(isDark(maid))
                .inWater(maid.isInWater())
                .fireworksNearby(!maid.level().getEntitiesOfClass(FireworkRocketEntity.class, maid.getBoundingBox().inflate(30.0)).isEmpty())
                .working(maid.getScheduleDetail() == Activity.WORK)
                .companionsNearby(hasCompanions(maid, owner))
                .hasPositiveEffect(hasPositiveEffect(maid))
                .build();
    }

    private void observeDrift(MaidState state, Snapshot snap, long now) {
        int lingerMinutes = VoiceConfig.DRIFT_LINGER_MINUTES.get();
        for (DriftZone zone : DriftZone.values()) {
            DriftLevel before = state.drift.get(zone);
            DriftLevel after = zone.level(snap.trust, snap.fear, before);
            state.drift.put(zone, after);
            String zoneSuffix = "/" + zone.key();
            if (after.ordinal() > before.ordinal()) {
                // Deeper in. Reaching CORE is L&L's own moment (it has its own dialogue), so only NEAR/BRINK speak.
                if (after == DriftLevel.NEAR || after == DriftLevel.BRINK) {
                    state.queue.dropTopicsStartingWith(VoiceKind.DRIFT_EASING.key() + zoneSuffix);
                    state.queue.offer(VoicePrompts.drift(now, zone, after, false));
                    state.driftLineTick.put(zone, now);
                }
            } else if (after.ordinal() < before.ordinal()) {
                state.queue.dropTopicsStartingWith(VoiceKind.DRIFT_NEAR.key() + zoneSuffix);
                state.queue.dropTopicsStartingWith(VoiceKind.DRIFT_BRINK.key() + zoneSuffix);
                state.queue.dropTopicsStartingWith(VoiceKind.DRIFT_LINGER.key() + zoneSuffix);
                state.queue.offer(VoicePrompts.easing(now, zone));
                state.driftLineTick.put(zone, now);
            } else if (lingerMinutes > 0 && (after == DriftLevel.NEAR || after == DriftLevel.BRINK)
                    && now - state.driftLineTick.getOrDefault(zone, now) >= lingerMinutes * 1200L) {
                state.queue.offer(VoicePrompts.drift(now, zone, after, true));
                state.driftLineTick.put(zone, now);
            }
        }
    }

    // ------------------------------------------------------------------ periodic check-in

    private void observeStatus(EntityMaid maid, ServerPlayer owner, MaidState state, Snapshot snap, long now) {
        int minutes = VoiceConfig.STATUS_INTERVAL_MINUTES.get();
        if (minutes <= 0 || now < state.nextStatusTick) {
            return;
        }
        state.nextStatusTick = now + nextStatusDelay(maid, minutes);
        if (state.queue.isEmpty() && maid.distanceTo(owner) <= STATUS_RADIUS && !maid.isSleeping()) {
            state.queue.offer(VoicePrompts.status(now, snap.hungerStage, snap.healthRatio, snap.tendency));
        }
    }

    static long nextStatusDelay(EntityMaid maid, int minutes) {
        // 75%-125% of the interval, so a group of maids does not check in all at once.
        return (long) (minutes * 1200L * (0.75 + 0.5 * maid.getRandom().nextDouble()));
    }

    // ------------------------------------------------------------------ speaking

    private void trySpeak(EntityMaid maid, ServerPlayer owner, MaidState state, long now) {
        if (state.queue.isEmpty()) {
            return;
        }
        // Wait while anything is on screen above her: L&L's own lines, TLM chat, or a thinking bubble.
        if (maid.getChatBubbleManager().getChatBubbleDataCollection().size() > 0) {
            return;
        }
        long gap = VoiceConfig.MIN_SECONDS_BETWEEN_LINES.get() * 20L;
        VoiceRequest request = state.queue.poll(now, gap, maid.isSleeping());
        if (request == null) {
            return;
        }
        VoiceDispatcher.Route route = VoiceDispatcher.speak(maid, owner, request);
        if (VoiceConfig.DEBUG.get()) {
            owner.sendSystemMessage(Component.literal("[llvoices] ").append(maid.getName())
                    .append(" " + request.topic() + " via " + route + ": " + request.situation())
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    // ------------------------------------------------------------------ helpers

    static boolean hasFood(EntityMaid maid) {
        if (isFood(maid, maid.getMainHandItem()) || isFood(maid, maid.getOffhandItem())) {
            return true;
        }
        IItemHandler backpack = maid.getAvailableBackpackInv();
        for (int i = 0; i < backpack.getSlots(); i++) {
            if (isFood(maid, backpack.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFood(EntityMaid maid, ItemStack stack) {
        return !stack.isEmpty() && stack.getFoodProperties(maid) != null;
    }

    private static boolean isArmorFull(EntityMaid maid) {
        return !maid.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && !maid.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && !maid.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                && !maid.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }

    private static boolean isHooked(EntityMaid maid) {
        return maid.level().getEntitiesOfClass(FishingHook.class, maid.getBoundingBox().inflate(2.0)).stream()
                .anyMatch(hook -> hook.getHookedIn() == maid);
    }

    private static boolean isDark(EntityMaid maid) {
        try {
            return MaidEnvironmentLight.getEffectiveLightLevel(maid) < 6;
        } catch (LinkageError error) {
            return maid.level().getMaxLocalRawBrightness(maid.blockPosition()) < 6;
        }
    }

    private static boolean hasCompanions(EntityMaid maid, ServerPlayer owner) {
        return maid.level().getEntitiesOfClass(EntityMaid.class, maid.getBoundingBox().inflate(8.0)).stream()
                .anyMatch(other -> other != maid && owner.getUUID().equals(other.getOwnerUUID()));
    }

    private static boolean hasPositiveEffect(EntityMaid maid) {
        for (MobEffectInstance effect : maid.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                return true;
            }
        }
        return false;
    }

    /** Same test L&L uses before it reacts to the owner staring at a maid with food or a weapon. */
    private static boolean isOwnerStaringWithItem(ServerPlayer owner, EntityMaid maid) {
        Vec3 toMaid = maid.getEyePosition().subtract(owner.getEyePosition());
        if (toMaid.length() > 8.0 || owner.getLookAngle().dot(toMaid.normalize()) <= 0.85) {
            return false;
        }
        ItemStack held = owner.getMainHandItem();
        return held.getFoodProperties(maid) != null || held.is(Items.CAKE)
                || held.getItem() instanceof SwordItem || held.getItem() instanceof AxeItem
                || held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem
                || held.getItem() instanceof TridentItem;
    }

    /** For game tests. */
    public static VoiceQueue queueOf(UUID maidId) {
        MaidState state = STATES.get(maidId);
        return state == null ? null : state.queue;
    }

    // ------------------------------------------------------------------ state

    /** The values read this scan. */
    private record Snapshot(int trust, int fear, String tendency, float hunger, HungerStage hungerStage,
                            float healthRatio, boolean sitting) {
        static Snapshot of(EntityMaid maid, ServerPlayer owner) {
            EmotionData.EmotionValues values = EmotionData.get(maid, owner.getUUID());
            String tendency = EmotionData.getTendency(maid, owner.getUUID()).name();
            float hunger = HungerData.get(maid);
            float health = maid.getMaxHealth() <= 0 ? 1.0F : maid.getHealth() / maid.getMaxHealth();
            return new Snapshot(values.trust(), values.fear(), tendency, hunger, HungerStage.of(hunger), health,
                    maid.isInSittingPose());
        }
    }

    private static final class MaidState {
        final VoiceQueue queue = new VoiceQueue();
        final EnumMap<DriftZone, DriftLevel> drift = new EnumMap<>(DriftZone.class);
        final EnumMap<DriftZone, Long> driftLineTick = new EnumMap<>(DriftZone.class);

        boolean initialized;
        long lastSeenTick = Long.MIN_VALUE / 2;

        int trust;
        int fear;
        String tendency = "";
        float hunger;
        HungerStage hungerStage = HungerStage.SATISFIED;
        boolean wasStarving;
        int healthBand;
        boolean wounded;
        long nextStatusTick;

        boolean sitting;
        boolean armorFull;
        ItemStack headItem = ItemStack.EMPTY;
        long sitEdgeTick = Long.MIN_VALUE / 2;
        long armorEdgeTick = Long.MIN_VALUE / 2;
        long headChangeTick = Long.MIN_VALUE / 2;
        long passengerTick = Long.MIN_VALUE / 2;
        long ownerStaringTick = Long.MIN_VALUE / 2;
        long lastAteTick = Long.MIN_VALUE / 2;
        long lastHurtTick = Long.MIN_VALUE / 2;
        boolean hurtByOwner;
        long lastMaidDeathTick = Long.MIN_VALUE / 2;

        ChangeContext passiveContext;
        int passiveTrust;
        int passiveFear;
        long passiveLastTick;

        void baseline(Snapshot snap, EntityMaid maid, long now) {
            this.initialized = true;
            this.queue.clear();
            this.remember(snap);
            this.wasStarving = snap.hungerStage.ordinal() <= HungerStage.VERY_HUNGRY.ordinal();
            this.healthBand = healthBand(snap.healthRatio);
            this.wounded = this.healthBand > 0;
            this.sitting = snap.sitting;
            this.armorFull = isArmorFull(maid);
            this.headItem = maid.getMaidInv().getStackInSlot(HEAD_SLOT).copy();
            this.passiveContext = null;
            for (DriftZone zone : DriftZone.values()) {
                this.drift.put(zone, zone.level(snap.trust, snap.fear));
                this.driftLineTick.put(zone, now);
            }
            int minutes = VoiceConfig.STATUS_INTERVAL_MINUTES.get();
            // First check-in comes sooner than the full interval after you meet up again.
            this.nextStatusTick = now + (minutes > 0 ? nextStatusDelay(maid, minutes) / 2 : 0);
        }

        void remember(Snapshot snap) {
            this.trust = snap.trust;
            this.fear = snap.fear;
            this.tendency = snap.tendency;
            this.hunger = snap.hunger;
            this.hungerStage = snap.hungerStage;
        }
    }
}
