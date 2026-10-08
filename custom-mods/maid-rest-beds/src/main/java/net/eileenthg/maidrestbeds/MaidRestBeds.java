package net.eileenthg.maidrestbeds;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MaidRestBeds.MOD_ID)
public final class MaidRestBeds {
    public static final String MOD_ID = "maidrestbeds";

    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MOD_ID);

    /**
     * Vanilla beds are already "home" POIs, which is how maids find them. Comforts hammocks are not
     * POIs at all, so this gives them one. Blocks are registered before POI types, so they can be listed here.
     */
    public static final DeferredHolder<PoiType, PoiType> HAMMOCK_POI = POI_TYPES.register("hammock", () -> {
        ImmutableSet.Builder<BlockState> states = ImmutableSet.builder();
        BuiltInRegistries.BLOCK.stream()
                .filter(RestBeds::isHammock)
                .forEach(block -> block.getStateDefinition().getPossibleStates().stream()
                        .filter(RestBeds::isHammockHead)
                        .forEach(states::add));
        Set<BlockState> set = states.build();
        return new PoiType(set, 0, 1);
    });

    public MaidRestBeds(IEventBus modBus, ModContainer container) {
        POI_TYPES.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, RestConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(MaidRestBeds::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, MaidRestBeds::onBedClick);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> SelfTest.register(e.getDispatcher()));
    }

    /**
     * POI data for a chunk section is only built once, so hammocks placed before this mod was
     * installed have no POI. This adds the missing ones as chunks load. The palette check skips
     * nearly every section without looking at a single block.
     */
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)) return;
        List<BlockPos> hammocks = findHammocks(event.getChunk());
        if (!hammocks.isEmpty()) level.getServer().execute(() -> addMissingPoi(level, hammocks));
    }

    static List<BlockPos> findHammocks(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        List<BlockPos> found = new ArrayList<>();
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(RestBeds::isHammockHead)) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (RestBeds.isHammockHead(section.getBlockState(x, y, z))) {
                    found.add(new BlockPos(cp.getMinBlockX() + x, baseY + y, cp.getMinBlockZ() + z));
                }
            }
        }
        return found;
    }

    static int addMissingPoi(ServerLevel level, List<BlockPos> hammocks) {
        PoiManager poi = level.getPoiManager();
        int added = 0;
        for (BlockPos pos : hammocks) {
            if (level.isLoaded(pos) && RestBeds.isHammockHead(level.getBlockState(pos)) && poi.getType(pos).isEmpty()) {
                poi.add(pos, HAMMOCK_POI);
                added++;
            }
        }
        return added;
    }

    /**
     * Vanilla beds and Comforts hammocks wake a sleeping villager when a player clicks them, but not
     * a maid, so the player would just see "this bed is occupied". Wake her and use the bed as normal.
     */
    private static void onBedClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide || event.getHand() != InteractionHand.MAIN_HAND || !RestConfig.WAKE_ON_CLICK.get()) return;
        BlockState state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BedBlock) || !state.getValue(BedBlock.OCCUPIED)) return;
        Player player = event.getEntity();
        if (player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())) return;

        BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD
                ? event.getPos()
                : event.getPos().relative(state.getValue(BedBlock.FACING));
        List<EntityMaid> sleepers = level.getEntitiesOfClass(EntityMaid.class, new AABB(head).inflate(0.5),
                maid -> maid.isSleeping() && maid.getSleepingPos().map(head::equals).orElse(false));
        if (sleepers.isEmpty()) return;
        sleepers.forEach(LivingEntity::stopSleeping);

        // The interaction already read the old "occupied" state, so run the bed's use again ourselves.
        BlockState now = level.getBlockState(event.getPos());
        InteractionResult result = now.useWithoutItem(level, player, event.getHitVec());
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
