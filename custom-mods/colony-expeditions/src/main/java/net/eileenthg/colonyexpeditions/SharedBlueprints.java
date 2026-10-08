package net.eileenthg.colonyexpeditions;

import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Makes the expedition huts show up in every style pack. Structurize keys packs by name, so a mod can't add files
 * to MineColonies' own styles. Instead every pack gets a virtual {@code expeditions} folder, and lookups inside it
 * are served from the "Colony Expeditions" pack while still being labelled with the style the player picked.
 * A hut placed under Medieval Oak stays a Medieval Oak building and upgrades through the same shared files.
 */
public final class SharedBlueprints
{
    public static final String PACK = "Colony Expeditions";
    public static final String FOLDER = "expeditions";

    private SharedBlueprints()
    {
    }

    @Nullable
    public static StructurePackMeta pack()
    {
        return StructurePacks.getStructurePack(PACK);
    }

    /**
     * The path inside the shared pack if this lookup should be redirected there, otherwise null.
     * Accepts sub paths that still carry the shared pack's root (MineColonies' direct placement builds those).
     */
    @Nullable
    public static String redirect(final String packId, final String subPath)
    {
        if (subPath == null || PACK.equals(packId))
        {
            return null;
        }
        final StructurePackMeta shared = pack();
        if (shared == null)
        {
            return null;
        }
        String path = subPath.replace('\\', '/');
        final String root = shared.getPath().toString().replace('\\', '/');
        if (path.startsWith(root + "/"))
        {
            path = path.substring(root.length() + 1);
        }
        while (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        return path.equals(FOLDER) || path.startsWith(FOLDER + "/") ? path : null;
    }

    /** True if the file lives in the shared pack. */
    public static boolean isShared(final Path path)
    {
        final StructurePackMeta shared = pack();
        if (shared == null || path == null || path.getFileSystem() != shared.getPath().getFileSystem())
        {
            return false;
        }
        return path.toAbsolutePath().startsWith(shared.getPath().toAbsolutePath());
    }
}
