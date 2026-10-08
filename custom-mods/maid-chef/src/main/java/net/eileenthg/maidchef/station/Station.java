package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A kind of cooking station. Stations are stateless; everything about a job lives in the Chef behaviour.
 *
 * <p>Loading always takes items from the maid's own inventory, where the Chef has already put exactly what the job needs.
 */
public interface Station {
    /** Short English name for status lines. */
    String name();

    boolean matches(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be);

    /** Every recipe this station can make. Called rarely and cached per recipe manager. */
    List<RecipeNeed> recipes(ServerLevel level);

    /** This station can make the recipe as it stands right now (e.g. a keg at the temperature the recipe wants). */
    default boolean accepts(ServerLevel level, BlockPos pos, RecipeNeed need) {
        return true;
    }

    /**
     * Things she must bring when she comes back to a slow station that needs attention between stages (the next stage's
     * ingredient, the lid). Fetched from the ingredient chests before she goes to the station.
     */
    default List<net.minecraft.world.item.crafting.Ingredient> attendNeeds(ServerLevel level, BlockPos pos, RecipeNeed need) {
        return List.of();
    }

    /** Ready for a new load: working (lit/heated), empty, and not in use by someone else. */
    boolean idle(ServerLevel level, BlockPos pos);

    /** Most units one load can take. */
    int loadCap(RecipeNeed need);

    /**
     * Load up to {@code units} from the maid's inventory. Instant stations (crafting table, cutting board) also
     * finish the work here, put the output in her inventory and add it to {@code collected}.
     */
    LoadResult load(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units, Map<Item, Integer> collected);

    /**
     * @param units   units taken from her inventory; 0 means the load failed
     * @param results result items already produced (instant stations only)
     */
    record LoadResult(int units, int results) {
        public static final LoadResult FAILED = new LoadResult(0, 0);
    }

    /**
     * Move finished output into the maid's inventory. Adds everything picked up to {@code collected}
     * and returns how many of the recipe's result item were picked up.
     */
    int collect(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Map<Item, Integer> collected);

    /** True when nothing of the current load is still cooking. */
    boolean finished(ServerLevel level, BlockPos pos);

    /** Furnace-like stations burn fuel; others return false. */
    default boolean usesFuel() {
        return false;
    }

    /** Fuel ticks already sitting in the station (fuel slot contents). */
    default int storedFuelTicks(ServerLevel level, BlockPos pos) {
        return 0;
    }

    /** Put fuel from the maid's inventory into the station. */
    default void addFuel(ServerLevel level, EntityMaid maid, BlockPos pos, Item fuel, int count) {
    }

    /** No inventory of its own (crafting table): missing ingredients come straight from the ingredient chests. */
    default boolean usesPantry() {
        return false;
    }

    /** Inputs can be put in over several trips when they don't all fit in her backpack (cooking pot). */
    default boolean canStage() {
        return false;
    }

    /**
     * Put whatever inputs she carries into the station's empty input slots for {@code units} crafts.
     * Returns true once everything is in (the station then cooks by itself).
     */
    default boolean stage(ServerLevel level, EntityMaid maid, BlockPos pos, RecipeNeed need, Alloc alloc, int units) {
        return false;
    }

    /** What still has to go into the station for {@code units} crafts. */
    default Map<Item, Integer> stillToStage(ServerLevel level, BlockPos pos, RecipeNeed need, Alloc alloc, int units) {
        return Map.of();
    }

    /** Take staged inputs back out (when the job is abandoned). */
    default void unstage(ServerLevel level, EntityMaid maid, BlockPos pos) {
    }

    /** Station needs the recipe's tool in the maid's inventory (cutting board). */
    default boolean needsTool() {
        return false;
    }
}
