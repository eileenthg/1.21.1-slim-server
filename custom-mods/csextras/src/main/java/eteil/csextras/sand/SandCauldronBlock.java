package eteil.csextras.sand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eteil.csextras.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;

/** A cauldron filling with sand during sandstorms. Scoop it out with a shovel or bare hand. */
public class SandCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<SandCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("red").forGetter(b -> b.red),
            propertiesCodec()
    ).apply(i, SandCauldronBlock::new));

    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_CAULDRON;
    private static final CauldronInteraction.InteractionMap NO_INTERACTIONS = CauldronInteraction.newInteractionMap("csextras_sand");

    private final boolean red;

    public SandCauldronBlock(boolean red, Properties properties) {
        super(properties, NO_INTERACTIONS);
        this.red = red;
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 1));
    }

    public boolean isRed() {
        return red;
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == 3;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0 + state.getValue(LEVEL) * 3.0) / 16.0;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !isFull(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextDouble() < Config.CAULDRON_FILL_CHANCE.get()) fill(state, level, pos);
    }

    /** One level of sand, if a matching sandstorm is blowing over the cauldron. */
    public boolean fill(BlockState state, Level level, BlockPos pos) {
        Sandstorms.Type type = Sandstorms.exposedTypeAt(level, pos.above());
        if (isFull(state) || type == Sandstorms.Type.NONE || (type == Sandstorms.Type.RED) != red) return false;
        level.setBlockAndUpdate(pos, state.setValue(LEVEL, state.getValue(LEVEL) + 1));
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.canPerformAction(ItemAbilities.SHOVEL_DIG)) {
            empty(state, level, pos, player, hand, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        empty(state, level, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** A full cauldron gives a sand block; a partly filled one gives a sand pile per level. */
    private void empty(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack tool) {
        if (level.isClientSide) return;
        int lvl = state.getValue(LEVEL);
        ItemStack loot = isFull(state)
                ? new ItemStack(red ? Items.RED_SAND : Items.SAND)
                : new ItemStack(red ? SandBlocks.RED_SAND_PILE_ITEM.get() : SandBlocks.SAND_PILE_ITEM.get(), lvl);
        if (!player.getInventory().add(loot)) player.drop(loot, false);
        if (!tool.isEmpty() && tool.isDamageableItem()) tool.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1f, 1f);
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        player.awardStat(Stats.USE_CAULDRON);
    }
}
