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
public class BlobfishCarcassBlock extends AbstractCarcassBlock {
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
                case NORTH -> box(5.75, 0.65, 4.0, 11.75, 18.65, 12.0);
                case EAST -> box(4.0, 0.65, 5.75, 12.0, 18.65, 11.75);
                case WEST -> box(4.0, 0.65, 4.25, 12.0, 18.65, 10.25);
                default -> box(4.25, 0.65, 4.0, 10.25, 18.65, 12.0);
             };
          } else {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> box(0.0, 0.0, 4.0, 16.0, 6.0, 12.0);
                case EAST -> box(4.0, 0.0, 0.0, 12.0, 6.0, 16.0);
                case WEST -> box(4.0, 0.0, 0.0, 12.0, 6.0, 16.0);
                default -> box(0.0, 0.0, 4.0, 16.0, 6.0, 12.0);
             };
          }
       }
}
