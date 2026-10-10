package dev.eteil.butcheryam.init;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import dev.eteil.butcheryam.block.*;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks REGISTRY =
            DeferredRegister.createBlocks(ButcheryAlexsMobs.MODID);

    public static final DeferredBlock<Block> BALD_EAGLE_CARCASS =
            REGISTRY.register("bald_eagle_carcass", BaldEagleCarcassBlock::new);
    public static final DeferredBlock<Block> DRAINED_BALD_EAGLE_CARCASS =
            REGISTRY.register("drained_bald_eagle_carcass", DrainedBaldEagleCarcassBlock::new);
    public static final DeferredBlock<Block> BALD_EAGLE_HEAD =
            REGISTRY.register("baldeaglehead", BaldEagleHeadBlock::new);

    public static final DeferredBlock<Block> BLOBFISH_CARCASS =
            REGISTRY.register("blobfish_carcass", BlobfishCarcassBlock::new);

    public static final DeferredBlock<Block> CAIMAN_CARCASS =
            REGISTRY.register("caiman_carcass", CaimanCarcassBlock::new);
    public static final DeferredBlock<Block> DRAINED_CAIMAN_CARCASS =
            REGISTRY.register("drained_caiman_carcass", DrainedCaimanCarcassBlock::new);
    public static final DeferredBlock<Block> CAIMAN_HEAD =
            REGISTRY.register("caiman_head", CaimanHeadBlock::new);

    public static final DeferredBlock<Block> CAPUCHIN_CARCASS =
            REGISTRY.register("capuchin_carcass", CapuchinCarcassBlock::new);
    public static final DeferredBlock<Block> DRAINED_CAPUCHIN_CARCASS =
            REGISTRY.register("drained_capuchin_carcass", DrainedCapuchinCarcassBlock::new);
    public static final DeferredBlock<Block> CAPUCHIN_HEAD =
            REGISTRY.register("capuchin_head", CapuchinHeadBlock::new);

    public static final DeferredBlock<Block> CROW_CARCASS =
            REGISTRY.register("crow_carcass", CrowCarcassBlock::new);
    public static final DeferredBlock<Block> CROW_HEAD =
            REGISTRY.register("crow_head", CrowHeadBlock::new);

    public static final DeferredBlock<Block> EMU_CARCASS =
            REGISTRY.register("emu_carcass", EmuCarcassBlock::new);
    public static final DeferredBlock<Block> DRAINED_EMU_CARCASS =
            REGISTRY.register("drained_emu_carcass", DrainedEmuCarcassBlock::new);
    public static final DeferredBlock<Block> EMU_HEAD =
            REGISTRY.register("emu_head", EmuHeadBlock::new);

    public static final DeferredBlock<Block> FLYING_FISH_CARCASS =
            REGISTRY.register("flying_fish_carcass", FlyingFishCarcassBlock::new);

    /** Every block this mod registers, in creative-tab order. */
    public static final List<DeferredBlock<Block>> ALL = List.of(
            BALD_EAGLE_CARCASS, DRAINED_BALD_EAGLE_CARCASS, BALD_EAGLE_HEAD,
            BLOBFISH_CARCASS,
            DRAINED_CAIMAN_CARCASS, CAIMAN_CARCASS, CAIMAN_HEAD,
            CAPUCHIN_CARCASS, DRAINED_CAPUCHIN_CARCASS, CAPUCHIN_HEAD,
            EMU_CARCASS, DRAINED_EMU_CARCASS, EMU_HEAD,
            CROW_CARCASS, CROW_HEAD,
            FLYING_FISH_CARCASS
    );

    private ModBlocks() {}
}
