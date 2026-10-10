package dev.eteil.butcheryam2.block;

import dev.eteil.butcheryam2.blockentity.CarcassBlockEntity;
import dev.eteil.butcheryam2.butcher.Butchering;
import dev.eteil.butcheryam2.butcher.CutUp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Shared behaviour for every carcass and head block.
 *
 * <p>Subclasses supply only the {@code blockstate} property: its range differs between fresh,
 * drained and staged carcasses, and heads have none. Outline shapes come from
 * {@link ModelShapes}, computed from the same converted models the block renders.
 *
 * <p>{@link #stateProperty()} must return a {@code static final} constant rather than an instance
 * field: {@link #createBlockStateDefinition} is invoked from {@link Block}'s constructor, before
 * any subclass instance field has been assigned.
 */
public abstract class AbstractCarcassBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Property name the butchering engine looks up by name, as the original addon did. */
    public static final String STATE_NAME = "blockstate";

    protected AbstractCarcassBlock() {
        super(Properties.of()
                .sound(SoundType.HONEY_BLOCK)
                .strength(1.0F, 10.0F)
                .noOcclusion()
                .isRedstoneConductor((bs, br, bp) -> false)
                .dynamicShape());

        BlockState base = stateDefinition.any().setValue(FACING, Direction.NORTH);
        IntegerProperty prop = stateProperty();
        registerDefaultState(prop == null ? base : base.setValue(prop, 0));
    }

    /** The butchering-progression property, or null for blocks that have none (heads). */
    @Nullable
    public abstract IntegerProperty stateProperty();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        IntegerProperty prop = stateProperty();
        if (prop != null) {
            builder.add(prop);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, this::buildShape);
    }

    private VoxelShape buildShape(BlockState state) {
        IntegerProperty prop = stateProperty();
        int stage = prop == null ? 0 : state.getValue(prop);
        double[][] boxes = ModelShapes.boxes(BuiltInRegistries.BLOCK.getKey(this).getPath(), stage);
        if (boxes == null) {
            return Shapes.block();
        }
        VoxelShape shape = Shapes.empty();
        for (double[] b : boxes) {
            shape = Shapes.or(shape, rotated(b, state.getValue(FACING)));
        }
        return shape.optimize();
    }

    /** A north-facing box (pixels) turned the way the blockstate turns the model. */
    private static VoxelShape rotated(double[] b, Direction facing) {
        double x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];
        return switch (facing) {
            // blockstate y=90: (x, z) -> (16 - z, x)
            case EAST -> box(16 - z2, y1, x1, 16 - z1, y2, x2);
            case SOUTH -> box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case WEST -> box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            default -> box(x1, y1, z1, x2, y2, z2);
        };
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // A held cleaver or knife butchers; an empty hand starts the carcass bleeding.
        if (CutUp.tryCut(level, pos, player)) {
            return InteractionResult.SUCCESS;
        }
        Butchering.onCarcassUsed(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        Butchering.onCarcassBroken(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CarcassBlockEntity(pos, state);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }
}
