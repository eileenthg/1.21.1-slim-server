package dev.eteil.butcheryam2.blockentity;

import dev.eteil.butcheryam2.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A carcass drawn by a block-entity renderer, the way Butchery draws its Ender Dragon: how many
 * cuts have been made, and for segmented bodies how long the body is and which variant it was.
 */
public class DrawnCarcassBlockEntity extends CarcassBlockEntity {

    public static final String COUNT = "segments";
    public static final String VARIANT = "variant";
    public static final String CUTS = "cuts";

    public DrawnCarcassBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAWN.get(), pos, state);
    }

    /** The repeat count of a segmented body, or 0 if it never learned one. */
    public int count() {
        return getPersistentData().getInt(COUNT);
    }

    public boolean variant() {
        return getPersistentData().getBoolean(VARIANT);
    }

    public int cuts() {
        return getPersistentData().getInt(CUTS);
    }

    public void setCuts(int cuts) {
        getPersistentData().putInt(CUTS, cuts);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
