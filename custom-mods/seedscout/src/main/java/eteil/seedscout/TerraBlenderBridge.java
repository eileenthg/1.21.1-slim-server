package eteil.seedscout;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * TerraBlender keeps a seeded "uniqueness" noise on the overworld parameter list that picks which region
 * (vanilla or a mod's) owns each area. It is built once from the world seed at server start, so to predict
 * biomes for another seed we clone the biome source and give the clone that seed's uniqueness noise.
 * Everything is reflective so the mod also runs without TerraBlender.
 */
final class TerraBlenderBridge {
    private static boolean available;
    private static Method uniqueness;
    private static Object overworldRegion;
    private static Method cloneSource;
    private static Method cloneList;
    private static Method paramsMethod;
    private static Field areaField;
    private static Field paramsField;

    static void init(Logger log) {
        try {
            Class<?> noiseUtil = Class.forName("terrablender.worldgen.noise.LayeredNoiseUtil");
            Class<?> regionType = Class.forName("terrablender.api.RegionType");
            Class<?> area = Class.forName("terrablender.worldgen.noise.Area");
            uniqueness = noiseUtil.getMethod("uniqueness", RegistryAccess.class, regionType, long.class);
            for (Object c : regionType.getEnumConstants()) if (((Enum<?>) c).name().equals("OVERWORLD")) overworldRegion = c;
            cloneSource = Class.forName("terrablender.worldgen.IExtendedMultiNoiseBiomeSource").getMethod("clone");
            cloneList = Class.forName("terrablender.worldgen.IExtendedParameterList").getMethod("clone");
            for (Field f : Climate.ParameterList.class.getDeclaredFields()) {
                if (f.getType() == area) { areaField = f; f.setAccessible(true); }
            }
            paramsMethod = MultiNoiseBiomeSource.class.getDeclaredMethod("parameters");
            paramsMethod.setAccessible(true);
            paramsField = MultiNoiseBiomeSource.class.getDeclaredField("parameters");
            paramsField.setAccessible(true);
            available = areaField != null && overworldRegion != null;
            log.info("[seedscout] TerraBlender bridge {}", available ? "active" : "incomplete (area field or region type missing)");
        } catch (ClassNotFoundException e) {
            log.info("[seedscout] TerraBlender not present; using the biome source as-is");
        } catch (Throwable t) {
            log.warn("[seedscout] TerraBlender bridge failed; biome predictions ignore TerraBlender regions", t);
        }
    }

    static boolean active() {
        return available;
    }

    /** Returns a biome source that samples like {@code base} would in a world with {@code seed}. */
    static BiomeSource forSeed(BiomeSource base, RegistryAccess access, long seed) {
        if (!available || !(base instanceof MultiNoiseBiomeSource)) return base;
        try {
            Object list = paramsMethod.invoke(base);
            Object listCopy = cloneList.invoke(list);
            areaField.set(listCopy, uniqueness.invoke(null, access, overworldRegion, seed));
            Object sourceCopy = cloneSource.invoke(base);
            paramsField.set(sourceCopy, Either.left(listCopy));
            return (BiomeSource) sourceCopy;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("TerraBlender reseed failed", e);
        }
    }

    private TerraBlenderBridge() {}
}
