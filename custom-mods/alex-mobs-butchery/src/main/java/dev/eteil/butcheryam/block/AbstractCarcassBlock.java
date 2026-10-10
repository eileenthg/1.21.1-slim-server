package dev.eteil.butcheryam.block;

import dev.eteil.butcheryam.blockentity.CarcassBlockEntity;
import dev.eteil.butcheryam.butcher.Butchering;
import dev.eteil.butcheryam.butcher.CutUp;
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
 * <p>Subclasses supply only the {@code blockstate} property — whose range differs per mob, and
 * which is absent entirely on heads — plus the per-state {@link VoxelShape}s, which are carried
 * over unchanged from the original addon.
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
