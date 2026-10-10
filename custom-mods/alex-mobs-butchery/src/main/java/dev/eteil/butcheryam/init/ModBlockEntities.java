package dev.eteil.butcheryam.init;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import dev.eteil.butcheryam.blockentity.CarcassBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ButcheryAlexsMobs.MODID);

    /**
     * A single block-entity type backs every carcass and head. The original addon generated one
     * type per block, but they were structurally identical.
     */
    public static final Supplier<BlockEntityType<CarcassBlockEntity>> CARCASS =
            REGISTRY.register("carcass", () -> BlockEntityType.Builder
                    .of(CarcassBlockEntity::new,
                            ModBlocks.ALL.stream().map(Supplier::get).toArray(Block[]::new))
                    .build(null));

    private ModBlockEntities() {}
}
