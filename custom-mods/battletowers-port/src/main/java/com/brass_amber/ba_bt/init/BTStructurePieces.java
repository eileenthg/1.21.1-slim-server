package com.brass_amber.ba_bt.init;

import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.worldGen.structures.customUtil.TowerPieces;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BTStructurePieces {
    public static final DeferredRegister<StructurePieceType> PIECE_TYPE = DeferredRegister.create(Registries.STRUCTURE_PIECE, BABattleTowers.MOD_ID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType.StructureTemplateType> TOWER_PIECE = PIECE_TYPE.register("base_piece", () -> TowerPieces.TowerPiece::new);

    public static void register(IEventBus eventBus) {
        PIECE_TYPE.register(eventBus);
    }

}
