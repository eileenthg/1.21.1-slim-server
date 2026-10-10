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
public class CaimanCarcassBlock extends AbstractCarcassBlock {
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
                case NORTH -> Shapes.or(
                box(4.83629, 13.81644, 5.69164, 11.83629, 18.81644, 9.69164),
                new VoxelShape[]{
                   box(4.83629, 6.81644, 9.69164, 11.83629, 9.81644, 13.69164), box(4.83629, 9.81644, 7.69164, 11.83629, 13.81644, 11.69164)
                }
             );
                case EAST -> Shapes.or(
                box(6.30836, 13.81644, 4.83629, 10.30836, 18.81644, 11.83629),
                new VoxelShape[]{box(2.30836, 6.81644, 4.83629, 6.30836, 9.81644, 11.83629), box(4.30836, 9.81644, 4.83629, 8.30836, 13.81644, 11.83629)}
             );
                case WEST -> Shapes.or(
                box(5.69164, 13.81644, 4.16371, 9.69164, 18.81644, 11.16371),
                new VoxelShape[]{
                   box(9.69164, 6.81644, 4.16371, 13.69164, 9.81644, 11.16371), box(7.69164, 9.81644, 4.16371, 11.69164, 13.81644, 11.16371)
                }
             );
                default -> Shapes.or(
                box(4.16371, 13.81644, 6.30836, 11.16371, 18.81644, 10.30836),
                new VoxelShape[]{box(4.16371, 6.81644, 2.30836, 11.16371, 9.81644, 6.30836), box(4.16371, 9.81644, 4.30836, 11.16371, 13.81644, 8.30836)}
             );
             };
          } else {
             return switch ((Direction)state.getValue(FACING)) {
                case NORTH -> box(-0.75, 0.1, 6.25, 15.25, 6.1, 14.25);
                case EAST -> box(1.75, 0.1, -0.75, 9.75, 6.1, 15.25);
                case WEST -> box(6.25, 0.1, 0.75, 14.25, 6.1, 16.75);
                default -> box(0.75, 0.1, 1.75, 16.75, 6.1, 9.75);
             };
          }
       }
}
