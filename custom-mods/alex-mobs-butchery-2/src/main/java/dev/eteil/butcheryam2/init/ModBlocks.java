package dev.eteil.butcheryam2.init;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.block.CarcassBlocks;
import dev.eteil.butcheryam2.block.DrawnCarcassBlock;
import dev.eteil.butcheryam2.gen.GeneratedMobs;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Registers every creature's blocks from the generated table ({@link GeneratedMobs}):
 * {@code <name>_carcass}, plus {@code drained_<name>_carcass} for creatures that bleed and
 * {@code <name>_head} for those with a head block.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks REGISTRY =
            DeferredRegister.createBlocks(ButcheryAlexsMobs2.MODID);

    /** Every block this mod registers, in creative-tab order. */
    public static final List<DeferredBlock<Block>> ALL = new ArrayList<>();

    private static final Map<String, DeferredBlock<Block>> CARCASS = new LinkedHashMap<>();
    private static final Map<String, DeferredBlock<Block>> DRAINED = new LinkedHashMap<>();
    private static final Map<String, DeferredBlock<Block>> HEAD = new LinkedHashMap<>();

    /** Blocks drawn by the block-entity renderer (segmented and large carcasses). */
    public static final List<DeferredBlock<Block>> DRAWN = new ArrayList<>();

    private static DeferredBlock<Block> register(String name, Supplier<Block> factory) {
        DeferredBlock<Block> block = REGISTRY.register(name, factory);
        ALL.add(block);
        return block;
    }

    static {
        for (GeneratedMobs.Entry e : GeneratedMobs.ALL) {
            String n = e.name();
            switch (e.kind()) {
                case "FULL" -> {
                    CARCASS.put(n, register(n + "_carcass", CarcassBlocks.Fresh::new));
                    DRAINED.put(n, register("drained_" + n + "_carcass", CarcassBlocks.Staged::new));
                }
                case "STAGED" -> CARCASS.put(n, register(n + "_carcass", CarcassBlocks.Staged::new));
                case "STEPS" -> CARCASS.put(n, register(n + "_carcass", CarcassBlocks.Long::new));
                case "SINGLE" -> CARCASS.put(n, register(n + "_carcass", CarcassBlocks.Fresh::new));
                case "SEGMENTED", "LARGE" -> {
                    DeferredBlock<Block> block = register(n + "_carcass", () -> new DrawnCarcassBlock(n));
                    CARCASS.put(n, block);
                    DRAWN.add(block);
                }
                default -> throw new IllegalStateException("unknown kind " + e.kind());
            }
            if (e.head()) {
                HEAD.put(n, register(n + "_head", CarcassBlocks.Head::new));
            }
        }
    }

    public static DeferredBlock<Block> carcass(String mob) {
        return CARCASS.get(mob);
    }

    @Nullable
    public static DeferredBlock<Block> drained(String mob) {
        return DRAINED.get(mob);
    }

    @Nullable
    public static DeferredBlock<Block> head(String mob) {
        return HEAD.get(mob);
    }

    private ModBlocks() {}
}
