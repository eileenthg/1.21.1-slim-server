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
public class CrowHeadBlock extends AbstractCarcassBlock {

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
          return switch ((Direction)state.getValue(FACING)) {
             case NORTH -> Shapes.or(box(6.7, 0.01213, 7.27782, 9.3, 3.61213, 9.87782), box(7.5, 1.41213, 4.87782, 8.5, 3.41213, 7.87782));
             case EAST -> Shapes.or(box(6.12218, 0.01213, 6.7, 8.72218, 3.61213, 9.3), box(8.12218, 1.41213, 7.5, 11.12218, 3.41213, 8.5));
             case WEST -> Shapes.or(box(7.27782, 0.01213, 6.7, 9.87782, 3.61213, 9.3), box(4.87782, 1.41213, 7.5, 7.87782, 3.41213, 8.5));
             default -> Shapes.or(box(6.7, 0.01213, 6.12218, 9.3, 3.61213, 8.72218), box(7.5, 1.41213, 8.12218, 8.5, 3.41213, 11.12218));
          };
       }
}
