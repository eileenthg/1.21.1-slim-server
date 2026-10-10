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
public class DrainedEmuCarcassBlock extends AbstractCarcassBlock {
    public static final IntegerProperty BLOCKSTATE =
            IntegerProperty.create("blockstate", 0, 9);

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return BLOCKSTATE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
          if ((Integer)state.getValue(BLOCKSTATE) == 1) {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(2.5, 0.0, 8.0, 13.5, 8.0, 14.0), box(2.5, 8.0, 5.0, 13.5, 16.0, 12.0));
                case EAST -> Shapes.or(box(2.0, 0.0, 2.5, 8.0, 8.0, 13.5), box(4.0, 8.0, 2.5, 11.0, 16.0, 13.5));
                case WEST -> Shapes.or(box(8.0, 0.0, 2.5, 14.0, 8.0, 13.5), box(5.0, 8.0, 2.5, 12.0, 16.0, 13.5));
                default -> Shapes.or(box(2.5, 0.0, 2.0, 13.5, 8.0, 8.0), box(2.5, 8.0, 4.0, 13.5, 16.0, 11.0));
             };
          } else if ((Integer)state.getValue(BLOCKSTATE) == 2) {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(2.5, 0.0, 8.0, 13.5, 8.0, 14.0), box(2.5, 8.0, 5.0, 13.5, 16.0, 12.0));
                case EAST -> Shapes.or(box(2.0, 0.0, 2.5, 8.0, 8.0, 13.5), box(4.0, 8.0, 2.5, 11.0, 16.0, 13.5));
                case WEST -> Shapes.or(box(8.0, 0.0, 2.5, 14.0, 8.0, 13.5), box(5.0, 8.0, 2.5, 12.0, 16.0, 13.5));
                default -> Shapes.or(box(2.5, 0.0, 2.0, 13.5, 8.0, 8.0), box(2.5, 8.0, 4.0, 13.5, 16.0, 11.0));
             };
          } else if ((Integer)state.getValue(BLOCKSTATE) == 3) {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(2.5, 0.0, 8.0, 13.5, 8.0, 14.0), box(2.5, 8.0, 5.0, 13.5, 16.0, 12.0));
                case EAST -> Shapes.or(box(2.0, 0.0, 2.5, 8.0, 8.0, 13.5), box(4.0, 8.0, 2.5, 11.0, 16.0, 13.5));
                case WEST -> Shapes.or(box(8.0, 0.0, 2.5, 14.0, 8.0, 13.5), box(5.0, 8.0, 2.5, 12.0, 16.0, 13.5));
                default -> Shapes.or(box(2.5, 0.0, 2.0, 13.5, 8.0, 8.0), box(2.5, 8.0, 4.0, 13.5, 16.0, 11.0));
             };
          } else if ((Integer)state.getValue(BLOCKSTATE) == 4) {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(2.5, 0.0, 8.0, 13.5, 8.0, 14.0), box(2.5, 8.0, 5.0, 13.5, 16.0, 12.0));
                case EAST -> Shapes.or(box(2.0, 0.0, 2.5, 8.0, 8.0, 13.5), box(4.0, 8.0, 2.5, 11.0, 16.0, 13.5));
                case WEST -> Shapes.or(box(8.0, 0.0, 2.5, 14.0, 8.0, 13.5), box(5.0, 8.0, 2.5, 12.0, 16.0, 13.5));
                default -> Shapes.or(box(2.5, 0.0, 2.0, 13.5, 8.0, 8.0), box(2.5, 8.0, 4.0, 13.5, 16.0, 11.0));
             };
          } else if ((Integer)state.getValue(BLOCKSTATE) == 5) {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(2.5, 0.0, 8.0, 13.5, 8.0, 14.0), box(2.5, 8.0, 5.0, 13.5, 16.0, 12.0));
                case EAST -> Shapes.or(box(2.0, 0.0, 2.5, 8.0, 8.0, 13.5), box(4.0, 8.0, 2.5, 11.0, 16.0, 13.5));
                case WEST -> Shapes.or(box(8.0, 0.0, 2.5, 14.0, 8.0, 13.5), box(5.0, 8.0, 2.5, 12.0, 16.0, 13.5));
                default -> Shapes.or(box(2.5, 0.0, 2.0, 13.5, 8.0, 8.0), box(2.5, 8.0, 4.0, 13.5, 16.0, 11.0));
             };
          } else {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> box(-3.36924, 0.0, 1.63076, 17.63076, 11.0, 13.63076);
                case EAST -> box(2.36924, 0.0, -3.36924, 14.36924, 11.0, 17.63076);
                case WEST -> box(1.63076, 0.0, -1.63076, 13.63076, 11.0, 19.36924);
                default -> box(-1.63076, 0.0, 2.36924, 19.36924, 11.0, 14.36924);
             };
          }
       }
}
