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
public class CaimanHeadBlock extends AbstractCarcassBlock {

    @Override
    @Nullable
    public IntegerProperty stateProperty() {
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
          return switch ((Direction)state.getValue(FACING)) {
             case NORTH -> Shapes.or(
             box(3.99777, 0.01651, 3.99301, 11.99777, 8.01651, 11.99301), box(5.99777, 1.01651, 2.99301, 9.99777, 4.01651, 3.99301)
          );
             case EAST -> Shapes.or(
             box(4.00699, 0.01651, 3.99777, 12.00699, 8.01651, 11.99777), box(12.00699, 1.01651, 5.99777, 13.00699, 4.01651, 9.99777)
          );
             case WEST -> Shapes.or(
             box(3.99301, 0.01651, 4.00223, 11.99301, 8.01651, 12.00223), box(2.99301, 1.01651, 6.00223, 3.99301, 4.01651, 10.00223)
          );
             default -> Shapes.or(
             box(4.00223, 0.01651, 4.00699, 12.00223, 8.01651, 12.00699), box(6.00223, 1.01651, 12.00699, 10.00223, 4.01651, 13.00699)
          );
          };
       }
}
