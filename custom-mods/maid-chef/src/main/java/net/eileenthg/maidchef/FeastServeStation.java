package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.eileenthg.maidchef.station.Alloc;
import net.eileenthg.maidchef.station.Inv;
import net.eileenthg.maidchef.station.RecipeNeed;
import net.eileenthg.maidchef.station.Station;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Not a block in the kitchen: serving a feast. She places the feast block next to herself, takes every serving with
 * the right container (bowl, bowl of rice...) and clears away the leftovers. One "unit" = one feast = all its servings.
 * Covers Farmer's Delight feasts and Extra Delight feasts.
 */
public final class FeastServeStation implements Station {
    public static final FeastServeStation INSTANCE = new FeastServeStation();

    private FeastServeStation() {
    }

    @Override
    public String name() {
        return "feast";
    }

    /** Never found by scanning; the planner adds it when the maid serves feasts. */
    @Override
    public boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        return false;
    }

    @Override
    public List<RecipeNeed> recipes(ServerLevel level) {
        List<RecipeNeed> out = new ArrayList<>();
        if (ModList.get().isLoaded("farmersdelight")) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (!(item instanceof BlockItem bi)) continue;
                FdFeast.Feast f = FdFeast.of(bi.getBlock());
                if (f == null || f.servings() <= 0) continue;
                ItemStack container = f.serving().getDefaultInstance().getCraftingRemainingItem();
                out.add(need(item, f.serving(), container.isEmpty() ? null : container.getItem(), f.servings()));
            }
        }
        if (ModList.get().isLoaded("extradelight")) {
            EdFeast.servings(level.getRecipeManager(), level.registryAccess(),
                    (feast, serving, container, servings) -> out.add(need(feast, serving, container, servings)));
        }
        return out;
    }

    private static RecipeNeed need(Item feast, Item serving, @Nullable Item container, int servings) {
        ResourceLocation f = BuiltInRegistries.ITEM.getKey(feast), s = BuiltInRegistries.ITEM.getKey(serving);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MaidChef.MOD_ID,
                "serve/" + f.getNamespace() + "/" + f.getPath() + "/" + s.getNamespace() + "/" + s.getPath());
        return new RecipeNeed(id, null, List.of(Ingredient.of(feast)), null,
                container == null ? ItemStack.EMPTY : new ItemStack(container), new ItemStack(serving, servings), 0, Set.of());
    }

    @Override
    public boolean idle(ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public int loadCap(RecipeNeed need) {
        return 1;
    }

    @Nullable
    private static BlockPos findSpot(ServerLevel level, LivingEntity maid, Block block) {
        BlockPos base = maid.blockPosition();
        BlockState state = block.defaultBlockState();
        for (int r = 1; r <= 3; r++) {
            for (int dy = 0; dy >= -1; dy--) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        BlockPos p = base.offset(dx, dy, dz);
                        if (!level.isEmptyBlock(p) || !state.canSurvive(level, p)) continue;
                        if (!level.getEntities(null, new AABB(p)).isEmpty()) continue;
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /** Move everything the fake player is holding into the maid's inventory. Returns how many of {@code result} that was. */
    private static int drain(FakePlayer fp, EntityMaid maid, Item result, Map<Item, Integer> collected) {
        int got = 0;
        Inventory inv = fp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            inv.setItem(i, ItemStack.EMPTY);
            collected.merge(s.getItem(), s.getCount(), Integer::sum);
            if (s.is(result)) got += s.getCount();
            Inv.giveOrDrop(maid, s);
        }
        return got;
    }

    @Override
    public LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected) {
        Item feastItem = alloc.perInput().get(0);
        if (!(feastItem instanceof BlockItem bi)) return LoadResult.FAILED;
        Block block = bi.getBlock();
        BlockPos spot = findSpot(level, maid, block);
        if (spot == null) {
            ChefBehavior.trace(maid, "no free spot to set the " + feastItem + " down");
            return LoadResult.FAILED;
        }
        ItemStack feast = Inv.take(Inv.backpack(maid), feastItem, 1);
        if (feast.isEmpty()) return LoadResult.FAILED;
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, maid.getDirection().getOpposite());
        }
        level.setBlock(spot, state, 3);
        maid.swing(InteractionHand.MAIN_HAND);

        FakePlayer fp = FakePlayerFactory.getMinecraft(level);
        fp.getInventory().clearContent();
        fp.moveTo(maid.getX(), maid.getY(), maid.getZ());
        Item container = need.container().isEmpty() ? null : need.container().getItem();
        int got = 0;
        for (int guard = 0; guard < 64 && level.getBlockState(spot).is(block); guard++) {
            ItemStack held = container == null ? ItemStack.EMPTY : Inv.take(Inv.backpack(maid), container, 1);
            BlockState cur = level.getBlockState(spot);
            if (held.isEmpty() && container != null && !servingsLeftZero(cur)) break;
            fp.setItemInHand(InteractionHand.MAIN_HAND, held);
            cur.useItemOn(held, level, fp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(spot), Direction.UP, spot, false));
            got += drain(fp, maid, need.resultItem(), collected);
        }
        fp.getInventory().clearContent();
        // leftovers (bones, plates...) dropped when the empty feast was cleared away
        got += Inv.vacuum(level, maid, spot, it -> true, need.resultItem(), collected);
        if (level.getBlockState(spot).is(block)) {
            ChefBehavior.trace(maid, "feast at " + spot.toShortString() + " not fully served (ran out of containers)");
        }
        ChefBehavior.trace(maid, "served " + got + " " + need.resultItem() + " from " + feastItem);
        return new LoadResult(1, got);
    }

    /** True when the feast block has no servings left (only its leftovers remain), so an empty hand clears it. */
    private static boolean servingsLeftZero(BlockState state) {
        for (var p : state.getProperties()) {
            if (p.getName().equals("servings") && state.getValue(p) instanceof Integer i) return i == 0;
        }
        return false;
    }

    @Override
    public int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected) {
        return 0;
    }

    @Override
    public boolean finished(ServerLevel level, BlockPos pos) {
        return true;
    }
}
