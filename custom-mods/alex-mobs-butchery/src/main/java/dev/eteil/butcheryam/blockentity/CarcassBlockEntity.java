package dev.eteil.butcheryam.blockentity;

import dev.eteil.butcheryam.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries the butchering flags for a carcass ({@code isDrained}, {@code pluckedFeathers}, ...).
 *
 * <p>The original addon inherited a 9-slot container from MCreator's template, but no menu was
 * ever registered and nothing wrote to the inventory, so only the persistent NBT is kept here.
 */
public class CarcassBlockEntity extends BlockEntity {

    private static final String PERSISTENT_KEY = "butcheryam";

    public CarcassBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARCASS.get(), pos, state);
    }

    public boolean getFlag(String key) {
        return getPersistentData().getBoolean(key);
    }

    public void setFlag(String key, boolean value) {
        getPersistentData().putBoolean(key, value);
        setChanged();
    }

    public double getNumber(String key) {
        return getPersistentData().contains(key) ? getPersistentData().getDouble(key) : -1.0D;
    }

    public void setNumber(String key, double value) {
        getPersistentData().putDouble(key, value);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(PERSISTENT_KEY, getPersistentData());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(PERSISTENT_KEY)) {
            getPersistentData().merge(tag.getCompound(PERSISTENT_KEY));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
