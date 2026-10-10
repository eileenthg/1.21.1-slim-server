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
public class CapuchinCarcassBlock extends AbstractCarcassBlock {
    public static final IntegerProperty BLOCKSTATE =
            IntegerProperty.create("blockstate", 0, 1);

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return BLOCKSTATE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
          if ((Integer)state.getValue(BLOCKSTATE) == 1) {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(5.5, 14.0, 6.0, 10.5, 18.0, 9.0), box(5.5, 10.0, 8.0, 10.5, 14.0, 11.0));
                case EAST -> Shapes.or(box(7.0, 14.0, 5.5, 10.0, 18.0, 10.5), box(5.0, 10.0, 5.5, 8.0, 14.0, 10.5));
                case WEST -> Shapes.or(box(6.0, 14.0, 5.5, 9.0, 18.0, 10.5), box(8.0, 10.0, 5.5, 11.0, 14.0, 10.5));
                default -> Shapes.or(box(5.5, 14.0, 7.0, 10.5, 18.0, 10.0), box(5.5, 10.0, 5.0, 10.5, 14.0, 8.0));
             };
          } else {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> box(0.74212, 0.5, 5.74212, 11.74212, 5.5, 11.74212);
                case EAST -> box(4.25788, 0.5, 0.74212, 10.25788, 5.5, 11.74212);
                case WEST -> box(5.74212, 0.5, 4.25788, 11.74212, 5.5, 15.25788);
                default -> box(4.25788, 0.5, 4.25788, 15.25788, 5.5, 10.25788);
             };
          }
       }
}
