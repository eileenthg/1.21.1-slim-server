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
public class BaldEagleHeadBlock extends AbstractCarcassBlock {

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
          return switch ((Direction)state.getValue(FACING)) {
             case NORTH -> box(5.0, 0.0, 4.0, 11.0, 7.0, 11.0);
             case EAST -> box(5.0, 0.0, 5.0, 12.0, 7.0, 11.0);
             case WEST -> box(4.0, 0.0, 5.0, 11.0, 7.0, 11.0);
             default -> box(5.0, 0.0, 5.0, 11.0, 7.0, 12.0);
          };
       }
}
