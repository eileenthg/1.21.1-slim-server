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
public class CapuchinHeadBlock extends AbstractCarcassBlock {

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
          return switch ((Direction)state.getValue(FACING)) {
             case NORTH -> box(6.00989, 0.03223, 7.04212, 10.00989, 4.03223, 10.04212);
             case EAST -> box(5.95788, 0.03223, 6.00989, 8.95788, 4.03223, 10.00989);
             case WEST -> box(7.04212, 0.03223, 5.99011, 10.04212, 4.03223, 9.99011);
             default -> box(5.99011, 0.03223, 5.95788, 9.99011, 4.03223, 8.95788);
          };
       }
}
