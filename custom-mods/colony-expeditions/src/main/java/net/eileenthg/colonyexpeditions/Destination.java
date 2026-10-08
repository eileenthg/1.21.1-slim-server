package net.eileenthg.colonyexpeditions;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * Every expedition destination. Each one gets its own hut, building, job, portal veil, damage type
 * and crafter recipe key ({@code expedition_<id>_custom}).
 */
public enum Destination
{
    END("end", "minecraft:ender_eye", 4, 4),
    OTHERSIDE("otherside", "deeperdarker:heart_of_the_deep", 3, 3),
    DEEPER_DOWN("deeper_down", "spectrum:bottle_of_ruin", 3, 3),
    BUMBLEZONE("bumblezone", "minecraft:ender_pearl", 0, 0),
    COMETS("comets", "eternal_tales:comets", 2, 2),
    GARDENS_OF_EDEN("gardens_of_eden", "eternal_tales:gardens_of_eden", 2, 2),
    KINGDOM_OF_AMBER("kingdom_of_amber", "eternal_tales:kingdom_of_amber", 3, 3),
    LANDS_OF_KARVAT("lands_of_karvat", "eternal_tales:lands_of_karvat", 0, 0),
    PURGATORIUM("purgatorium", "eternal_tales:purgatorium", 3, 3),
    RAYANA("rayana", "eternal_tales:rayana", 1, 1),
    VOLCANECH("volcanech", "eternal_tales:volcanech", 4, 4);

    private final String id;
    private final String defaultKey;
    private final int minToolLevel;
    private final int minArmorLevel;

    Destination(final String id, final String defaultKey, final int minToolLevel, final int minArmorLevel)
    {
        this.id = id;
        this.defaultKey = defaultKey;
        this.minToolLevel = minToolLevel;
        this.minArmorLevel = minArmorLevel;
    }

    /** Default tool tier a worker needs before setting off (0 wood/gold, 1 stone, 2 iron, 3 diamond, 4 netherite). */
    public int minToolLevel()
    {
        return minToolLevel;
    }

    /** Default armour tier (0 leather, 1 gold, 2 chain, 3 iron, 4 diamond), -1 for none. */
    public int minArmorLevel()
    {
        return minArmorLevel;
    }

    public String id()
    {
        return id;
    }

    /** Item that opens this destination's portal, unless the config overrides it. */
    public String defaultKey()
    {
        return defaultKey;
    }

    /** Hut block name, e.g. blockhutexpedition_end. MineColonies derives the research effect from it. */
    public String hutName()
    {
        return "blockhutexpedition_" + id;
    }

    /** Building and job registry path, e.g. expedition_end. Blueprints are named expedition_end1..5. */
    public String workName()
    {
        return "expedition_" + id;
    }

    public ResourceLocation workId()
    {
        return ColonyExpeditions.id(workName());
    }

    public String portalName()
    {
        return "portal_" + id;
    }

    public ResourceKey<DamageType> damageType()
    {
        return ResourceKey.create(Registries.DAMAGE_TYPE, workId());
    }
}
