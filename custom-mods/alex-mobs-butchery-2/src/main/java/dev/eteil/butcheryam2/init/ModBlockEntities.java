package dev.eteil.butcheryam2.init;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.blockentity.CarcassBlockEntity;
import dev.eteil.butcheryam2.blockentity.DrawnCarcassBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ButcheryAlexsMobs2.MODID);

    /** One type backs every block-model carcass and head, as in Part 1. */
    public static final Supplier<BlockEntityType<CarcassBlockEntity>> CARCASS =
            REGISTRY.register("carcass", () -> BlockEntityType.Builder
                    .of(CarcassBlockEntity::new, ModBlocks.ALL.stream()
                            .filter(b -> !ModBlocks.DRAWN.contains(b))
                            .map(Supplier::get).toArray(Block[]::new))
                    .build(null));

    /** Carcasses drawn by a block-entity renderer have their own type. */
    public static final Supplier<BlockEntityType<DrawnCarcassBlockEntity>> DRAWN =
            REGISTRY.register("drawn_carcass", () -> BlockEntityType.Builder
                    .of(DrawnCarcassBlockEntity::new, ModBlocks.DRAWN.stream().map(Supplier::get).toArray(Block[]::new))
                    .build(null));

    private ModBlockEntities() {}
}
