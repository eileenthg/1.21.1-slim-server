package dev.eteil.butcheryam2.init;

import com.mojang.serialization.MapCodec;
import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.event.CarcassLootModifier;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTRY =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ButcheryAlexsMobs2.MODID);

    public static final Supplier<MapCodec<CarcassLootModifier>> CARCASS =
            REGISTRY.register("carcass", () -> CarcassLootModifier.CODEC);

    private ModLootModifiers() {}
}
