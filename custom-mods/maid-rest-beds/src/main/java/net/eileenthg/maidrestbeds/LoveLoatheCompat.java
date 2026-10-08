package net.eileenthg.maidrestbeds;

import com.github.JumDa5he.callresponse.compat.facility.FacilityCapacityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Love &amp; Loathe lets one maid bed hold several maids, so "occupied" does not always mean "full".
 * Only loaded when that mod is present.
 */
final class LoveLoatheCompat {
    private LoveLoatheCompat() {}

    static boolean hasVacancy(ServerLevel level, BlockPos pos) {
        return FacilityCapacityManager.hasVacancy(level, pos);
    }
}
