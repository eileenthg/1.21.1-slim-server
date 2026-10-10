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
public class BaldEagleCarcassBlock extends AbstractCarcassBlock {
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
                case NORTH -> box(6.0, 4.51175, 6.66835, 10.0, 12.51175, 11.66835);
                case EAST -> box(4.33165, 4.51175, 6.0, 9.33165, 12.51175, 10.0);
                case WEST -> box(6.66835, 4.51175, 6.0, 11.66835, 12.51175, 10.0);
                default -> box(6.0, 4.51175, 4.33165, 10.0, 12.51175, 9.33165);
             };
          } else {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> box(6.0, 0.38189, 2.65529, 10.0, 5.38189, 10.65529);
                case EAST -> box(5.34471, 0.38189, 6.0, 13.34471, 5.38189, 10.0);
                case WEST -> box(2.65529, 0.38189, 6.0, 10.65529, 5.38189, 10.0);
                default -> box(6.0, 0.38189, 5.34471, 10.0, 5.38189, 13.34471);
             };
          }
       }
}
