package com.brass_amber.ba_bt.init;

import com.mojang.serialization.MapCodec;
import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.worldGen.TowerStructurePlacement;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BTStructurePlacements {

    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPE_REGISTRY = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, BABattleTowers.MOD_ID);

    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<TowerStructurePlacement>> TOWER_STRUCTURE_PLACEMENT = STRUCTURE_PLACEMENT_TYPE_REGISTRY.register("tower_structure_placement", () -> explicitStructureTypeTyping(TowerStructurePlacement.CODEC));

    private static <T extends StructurePlacement> StructurePlacementType<T> explicitStructureTypeTyping(MapCodec<T> structurePlacementTypeCodec) {
        return () -> structurePlacementTypeCodec;
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_PLACEMENT_TYPE_REGISTRY.register(eventBus);
    }

}
