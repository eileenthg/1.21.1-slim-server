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
public class FlyingFishCarcassBlock extends AbstractCarcassBlock {

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
          return switch ((Direction)state.getValue(FACING)) {
             case NORTH -> Shapes.or(box(8.5, 0.0, 6.0, 17.5, 4.0, 9.0), box(0.5, 1.0, 6.5, 8.5, 4.0, 8.5));
             case EAST -> Shapes.or(box(7.0, 0.0, 8.5, 10.0, 4.0, 17.5), box(7.5, 1.0, 0.5, 9.5, 4.0, 8.5));
             case WEST -> Shapes.or(box(6.0, 0.0, -1.5, 9.0, 4.0, 7.5), box(6.5, 1.0, 7.5, 8.5, 4.0, 15.5));
             default -> Shapes.or(box(-1.5, 0.0, 7.0, 7.5, 4.0, 10.0), box(7.5, 1.0, 7.5, 15.5, 4.0, 9.5));
          };
       }
}
