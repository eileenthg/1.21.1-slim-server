package eteil.spawnonboarding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world onboarding progress, stored with the overworld's saved data. */
public final class OnboardingState extends SavedData {
    public enum Stage {
        /** Nobody has finished MCA's destiny screen yet. */
        WAITING,
        /** Destiny finished; placement runs once {@link #readyAtTick} is reached. */
        FINALIZED,
        /** Nothing left to do for this world. */
        DONE
    }

    private static final String NAME = "spawnonboarding";
    private static final Factory<OnboardingState> FACTORY = new Factory<>(OnboardingState::new, OnboardingState::load, null);

    public Stage stage = Stage.WAITING;
    public BlockPos destinyPos;
    public BlockPos townOrigin;
    public long readyAtTick;
    /** Captured at server start: does this world still need Eternal Tales' town? Null until checked. */
    public Boolean townPending;

    public static OnboardingState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static OnboardingState load(CompoundTag tag, HolderLookup.Provider registries) {
        OnboardingState s = new OnboardingState();
        try {
            s.stage = Stage.valueOf(tag.getString("stage"));
        } catch (IllegalArgumentException e) {
            // Worlds saved with the old AnotherScape stage (HUB_STARTING) have nothing left to place.
            s.stage = tag.contains("stage") ? Stage.DONE : Stage.WAITING;
        }
        s.destinyPos = NbtUtils.readBlockPos(tag, "destinyPos").orElse(null);
        s.townOrigin = NbtUtils.readBlockPos(tag, "townOrigin").orElse(null);
        s.readyAtTick = tag.getLong("readyAtTick");
        if (tag.contains("townPending")) s.townPending = tag.getBoolean("townPending");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("stage", stage.name());
        if (destinyPos != null) tag.put("destinyPos", NbtUtils.writeBlockPos(destinyPos));
        if (townOrigin != null) tag.put("townOrigin", NbtUtils.writeBlockPos(townOrigin));
        tag.putLong("readyAtTick", readyAtTick);
        if (townPending != null) tag.putBoolean("townPending", townPending);
        return tag;
    }
}
