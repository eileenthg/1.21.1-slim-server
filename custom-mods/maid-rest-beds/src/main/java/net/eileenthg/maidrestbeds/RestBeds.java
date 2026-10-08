package net.eileenthg.maidrestbeds;

import com.github.tartaricacid.touhoulittlemaid.block.BlockMaidBed;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.init.InitPoi;
import java.util.Comparator;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.fml.ModList;

/** Which blocks a maid may sleep in, and how to pick one. */
public final class RestBeds {

    public enum Kind { NONE, MAID_BED, BED, HAMMOCK }

    private static final boolean LOVE_LOATHE = ModList.get().isLoaded("callresponse");

    private RestBeds() {}

    public static boolean isHammock(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id.getNamespace().equals("comforts") && id.getPath().startsWith("hammock_");
    }

    public static boolean isHammockHead(BlockState state) {
        return isHammock(state.getBlock()) && state.getValue(BedBlock.PART) == BedPart.HEAD;
    }

    public static Kind kindOf(BlockState state) {
        if (state.is(InitBlocks.MAID_BED.get())) return Kind.MAID_BED;
        if (state.getBlock() instanceof BedBlock) return isHammock(state.getBlock()) ? Kind.HAMMOCK : Kind.BED;
        return Kind.NONE;
    }

    public static Kind kindAtSleepPos(EntityMaid maid) {
        return maid.getSleepingPos().map(pos -> kindOf(maid.level().getBlockState(pos))).orElse(Kind.NONE);
    }

    /** A maid bed head with room left: unoccupied, or under Love &amp; Loathe, below its capacity. */
    public static boolean isUsableMaidBed(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.is(InitBlocks.MAID_BED.get()) || state.getValue(BlockMaidBed.PART) != BedPart.HEAD) return false;
        return LOVE_LOATHE ? LoveLoatheCompat.hasVacancy(level, pos) : !state.getValue(BlockMaidBed.OCCUPIED);
    }

    /** A loaded bed or hammock head, of a kind the config allows, that nobody is sleeping in. */
    public static boolean isFreeFallback(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        Kind kind = kindOf(state);
        if (kind == Kind.BED && !RestConfig.ALLOW_BEDS.get()) return false;
        if (kind == Kind.HAMMOCK && !RestConfig.ALLOW_HAMMOCKS.get()) return false;
        if (kind != Kind.BED && kind != Kind.HAMMOCK) return false;
        return state.getValue(BedBlock.PART) == BedPart.HEAD && !state.getValue(BedBlock.OCCUPIED);
    }

    /**
     * Called when the maid-bed search came back empty or with a bed that is already full.
     * Tries the other maid beds in range first, then ordinary beds and hammocks.
     */
    @Nullable
    public static BlockPos findFallback(ServerLevel level, EntityMaid maid) {
        BlockPos center = maid.getBrainSearchPos();
        int range = (int) maid.getRestrictRadius();
        BlockPos from = maid.blockPosition();
        Comparator<BlockPos> nearest = Comparator.comparingDouble(pos -> pos.distSqr(from));
        PoiManager poi = level.getPoiManager();

        Optional<BlockPos> maidBed = poi.getInRange(t -> t.value() == InitPoi.MAID_BED.get(), center, range, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(pos -> maid.isWithinRestriction(pos) && isUsableMaidBed(level, pos))
                .min(nearest);
        if (maidBed.isPresent()) return maidBed.get();

        boolean beds = RestConfig.ALLOW_BEDS.get();
        boolean hammocks = RestConfig.ALLOW_HAMMOCKS.get();
        if (!beds && !hammocks) return null;
        boolean skipClaimed = RestConfig.SKIP_CLAIMED.get();
        return poi.getInRange(t -> (beds && t.is(PoiTypes.HOME)) || (hammocks && t.value() == MaidRestBeds.HAMMOCK_POI.get()),
                        center, range, PoiManager.Occupancy.ANY)
                .filter(rec -> !(skipClaimed && rec.getPoiType().is(PoiTypes.HOME) && !rec.hasSpace()))
                .map(PoiRecord::getPos)
                .filter(pos -> maid.isWithinRestriction(pos) && isFreeFallback(level, pos))
                .min(nearest)
                .orElse(null);
    }
}
