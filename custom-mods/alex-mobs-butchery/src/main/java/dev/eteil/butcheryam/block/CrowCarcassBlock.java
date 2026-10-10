package dev.eteil.butcheryam.block;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Generated from the original addon; shapes carried over unchanged. */
public class CrowCarcassBlock extends AbstractCarcassBlock {
    public static final IntegerProperty BLOCKSTATE =
            IntegerProperty.create("blockstate", 0, 2);

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return BLOCKSTATE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
          return switch ((Direction)state.getValue(FACING)) {
             case NORTH -> box(6.5, 0.01213, 6.57782, 9.5, 3.01213, 11.57782);
             case EAST -> box(4.42218, 0.01213, 6.5, 9.42218, 3.01213, 9.5);
             case WEST -> box(6.57782, 0.01213, 6.5, 11.57782, 3.01213, 9.5);
             default -> box(6.5, 0.01213, 4.42218, 9.5, 3.01213, 9.42218);
          };
       }
}
