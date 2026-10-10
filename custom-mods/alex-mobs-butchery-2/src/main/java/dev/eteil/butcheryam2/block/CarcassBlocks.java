package dev.eteil.butcheryam2.block;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The carcass block kinds, which differ only in the range of their {@code blockstate} property.
 * Each property is a static constant because it is read from Block's constructor.
 */
public final class CarcassBlocks {

    /** A fresh carcass or a fish: 0 on the ground, 1 hanging. */
    public static class Fresh extends AbstractCarcassBlock {
        public static final IntegerProperty BLOCKSTATE = IntegerProperty.create(STATE_NAME, 0, 1);

        @Override
        @Nullable
        public IntegerProperty stateProperty() {
            return BLOCKSTATE;
        }
    }

    /** A carcass being cut up: ground stages 0,6,7,8,9 and hanging stages 1..5. */
    public static class Staged extends AbstractCarcassBlock {
        public static final IntegerProperty BLOCKSTATE = IntegerProperty.create(STATE_NAME, 0, 9);

        @Override
        @Nullable
        public IntegerProperty stateProperty() {
            return BLOCKSTATE;
        }
    }

    /** A step-list carcass: step i is state 2i on the ground and 2i+1 hanging, up to 12 steps. */
    public static class Long extends AbstractCarcassBlock {
        public static final IntegerProperty BLOCKSTATE = IntegerProperty.create(STATE_NAME, 0, 23);

        @Override
        @Nullable
        public IntegerProperty stateProperty() {
            return BLOCKSTATE;
        }
    }

    /** A severed head. */
    public static class Head extends AbstractCarcassBlock {
        @Override
        @Nullable
        public IntegerProperty stateProperty() {
            return null;
        }
    }

    private CarcassBlocks() {}
}
