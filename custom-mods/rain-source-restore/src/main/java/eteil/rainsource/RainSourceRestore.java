package eteil.rainsource;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;

/**
 * Lets rain repair water bodies that have been broken up by bucketing.
 *
 * <p>NeoForge patches {@code FlowingFluid#getNewLiquid} so that, for every horizontal neighbour
 * that is already a source of the same fluid, it posts {@link CreateFluidSourceEvent} for THAT
 * NEIGHBOUR. Each neighbour the event allows increments a counter; vanilla then still requires the
 * counter to reach 2 and the block below to be solid or a source before a new source forms.
 *
 * <p>So this handler answers "may this neighbouring source count?" with "yes, if rain is falling on
 * it". A gap therefore heals only when it is flanked by at least two rained-on water sources with
 * support underneath - it closes holes in existing surface water and cannot create new water.
 */
@Mod(RainSourceRestore.MODID)
public class RainSourceRestore {
    public static final String MODID = "rainsource";

    public RainSourceRestore(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(RainSourceRestore::onCreateFluidSource);
    }

    private static void onCreateFluidSource(CreateFluidSourceEvent event) {
        // Gamerule (or another mod) already allows it - nothing to add.
        if (event.canConvert()) {
            return;
        }
        if (!Config.ENABLED.get()) {
            return;
        }
        // The event's fluid is the neighbouring source. Water only; lava must never be affected.
        if (!event.getFluidState().is(FluidTags.WATER)) {
            return;
        }
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (pos.getY() < Config.MIN_Y.get()) {
            return;
        }
        if (level.isRainingAt(pos) || (Config.INCLUDE_SNOWY_BIOMES.get() && isSnowingAt(level, pos))) {
            event.setCanConvert(true);
        }
    }

    /** Mirrors {@link Level#isRainingAt(BlockPos)} but accepts SNOW instead of RAIN. */
    private static boolean isSnowingAt(Level level, BlockPos pos) {
        if (!level.isRaining() || !level.canSeeSky(pos)) {
            return false;
        }
        if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        }
        return level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
    }
}
