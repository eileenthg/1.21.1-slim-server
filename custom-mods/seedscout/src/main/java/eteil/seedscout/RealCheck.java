package eteil.seedscout;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Debug: evaluates one structure set at one chunk with the live overworld's own generator state. */
final class RealCheck {
    static String run(MinecraftServer server, ResourceLocation setId, int cx, int cz) {
        ServerLevel level = server.overworld();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        Holder<StructureSet> set = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, setId));
        StringBuilder sb = new StringBuilder();
        sb.append("set=").append(setId).append(" chunk=").append(cx).append(',').append(cz)
                .append(" inPossibleSets=").append(state.possibleStructureSets().contains(set))
                .append(" isStructureChunk=").append(set.value().placement().isStructureChunk(state, cx, cz))
                .append(" placementClass=").append(set.value().placement().getClass().getName());
        for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
            Structure structure = entry.structure().value();
            StructureStart start = structure.generate(server.registryAccess(), generator, generator.getBiomeSource(), state.randomState(),
                    server.getStructureManager(), state.getLevelSeed(), new ChunkPos(cx, cz), 0, level, structure.biomes()::contains);
            sb.append("\n  ").append(entry.structure().unwrapKey().map(k -> k.location().toString()).orElse("?"))
                    .append(" w=").append(entry.weight()).append(" valid=").append(start.isValid());
            if (start.isValid()) sb.append(" box=").append(start.getBoundingBox());
        }
        var existing = level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS).getAllStarts();
        sb.append("\n  stored starts in chunk: ");
        existing.forEach((s, st) -> sb.append(server.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(s)).append(' '));
        return sb.toString();
    }

    private RealCheck() {}
}
