package net.eileenthg.colonyexpeditions.mixin;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.eileenthg.colonyexpeditions.SharedBlueprints;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Serves the virtual expeditions folder of every style pack from the shared pack. See SharedBlueprints. */
@Mixin(value = StructurePacks.class, remap = false)
public abstract class StructurePacksMixin
{
    @Inject(method = "getBlueprint(Ljava/lang/String;Ljava/lang/String;ZLnet/minecraft/core/HolderLookup$Provider;)Lcom/ldtteam/structurize/blueprints/v1/Blueprint;",
      at = @At("HEAD"), cancellable = true)
    private static void colonyExpeditions$getBlueprint(
      final String packId, final String subPath, final boolean suppressError, final HolderLookup.Provider provider, final CallbackInfoReturnable<Blueprint> cir)
    {
        final String shared = SharedBlueprints.redirect(packId, subPath);
        if (shared != null)
        {
            final Blueprint blueprint = StructurePacks.getBlueprint(SharedBlueprints.PACK, shared, suppressError, provider);
            if (blueprint != null)
            {
                blueprint.setPackName(packId);
            }
            cir.setReturnValue(blueprint);
        }
    }

    @Inject(method = "getBlueprintData", at = @At("HEAD"), cancellable = true)
    private static void colonyExpeditions$getBlueprintData(final String packId, final String subPath, final CallbackInfoReturnable<byte[]> cir)
    {
        final String shared = SharedBlueprints.redirect(packId, subPath);
        if (shared != null)
        {
            cir.setReturnValue(StructurePacks.getBlueprintData(SharedBlueprints.PACK, shared));
        }
    }

    @Inject(method = "getBlueprints", at = @At("HEAD"), cancellable = true)
    private static void colonyExpeditions$getBlueprints(
      final String packId, final String subPath, final HolderLookup.Provider provider, final CallbackInfoReturnable<List<Blueprint>> cir)
    {
        final String shared = SharedBlueprints.redirect(packId, subPath);
        if (shared != null)
        {
            final List<Blueprint> blueprints = StructurePacks.getBlueprints(SharedBlueprints.PACK, shared, provider);
            blueprints.forEach(blueprint -> blueprint.setPackName(packId));
            cir.setReturnValue(blueprints);
        }
    }

    @Inject(method = "getCategories", at = @At("HEAD"), cancellable = true)
    private static void colonyExpeditions$sharedCategories(
      final String packId, final String subPath, final CallbackInfoReturnable<List<StructurePacks.Category>> cir)
    {
        final String shared = SharedBlueprints.redirect(packId, subPath);
        final StructurePackMeta style = StructurePacks.getStructurePack(packId);
        if (shared != null && style != null)
        {
            final List<StructurePacks.Category> categories = new ArrayList<>(StructurePacks.getCategories(SharedBlueprints.PACK, shared));
            categories.forEach(category -> category.packMeta = style);
            cir.setReturnValue(categories);
        }
    }

    /** Adds the expeditions folder to the top level of every other pack. */
    @Inject(method = "getCategories", at = @At("RETURN"), cancellable = true)
    private static void colonyExpeditions$addFolder(
      final String packId, final String subPath, final CallbackInfoReturnable<List<StructurePacks.Category>> cir)
    {
        if (subPath == null || !subPath.isEmpty() || SharedBlueprints.PACK.equals(packId) || SharedBlueprints.pack() == null)
        {
            return;
        }
        final StructurePackMeta style = StructurePacks.getStructurePack(packId);
        final List<StructurePacks.Category> original = cir.getReturnValue();
        if (style == null || original.stream().anyMatch(c -> SharedBlueprints.FOLDER.equals(c.subPath)))
        {
            return;
        }
        final StructurePacks.Category folder = new StructurePacks.Category();
        folder.packMeta = style;
        folder.subPath = SharedBlueprints.FOLDER;
        folder.hasIcon = false;
        folder.isTerminal = false;
        folder.isCurrent = false;
        final List<StructurePacks.Category> categories = new ArrayList<>(original);
        categories.add(folder);
        cir.setReturnValue(categories);
    }

    /** Direct hut placement searches the selected style for a blueprint anchored on the hut block. */
    @Inject(method = "findBlueprint(Ljava/lang/String;Ljava/util/function/Predicate;Lnet/minecraft/core/HolderLookup$Provider;)Lcom/ldtteam/structurize/blueprints/v1/Blueprint;",
      at = @At("RETURN"), cancellable = true)
    private static void colonyExpeditions$findShared(
      final String packId, final Predicate<Blueprint> predicate, final HolderLookup.Provider provider, final CallbackInfoReturnable<Blueprint> cir)
    {
        if (cir.getReturnValue() != null || SharedBlueprints.PACK.equals(packId) || SharedBlueprints.pack() == null)
        {
            return;
        }
        final Blueprint blueprint = StructurePacks.findBlueprint(SharedBlueprints.PACK, predicate, provider);
        if (blueprint != null)
        {
            blueprint.setPackName(packId);
            cir.setReturnValue(blueprint);
        }
    }

    @Inject(method = "findBlueprint(Ljava/lang/String;Ljava/lang/String;)Ljava/nio/file/Path;", at = @At("RETURN"), cancellable = true)
    private static void colonyExpeditions$findSharedByName(final String packId, final String name, final CallbackInfoReturnable<Path> cir)
    {
        if (cir.getReturnValue() == null && !SharedBlueprints.PACK.equals(packId) && SharedBlueprints.pack() != null)
        {
            cir.setReturnValue(StructurePacks.findBlueprint(SharedBlueprints.PACK, name));
        }
    }
}
