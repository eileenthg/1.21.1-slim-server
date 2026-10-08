package net.eileenthg.colonyexpeditions.core;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.eileenthg.colonyexpeditions.ColonyExpeditions;
import net.eileenthg.colonyexpeditions.Destination;
import net.eileenthg.colonyexpeditions.ModContent;
import net.minecraft.resources.ResourceLocation;

public class BlockHutExpedition extends AbstractBlockHut<BlockHutExpedition>
{
    /** AbstractColonyBlock's constructor already asks for the hut name, before our field is assigned. */
    private static final ThreadLocal<Destination> CONSTRUCTING = new ThreadLocal<>();

    private final Destination destination;

    private BlockHutExpedition(final Destination destination)
    {
        super();
        this.destination = destination;
    }

    public static BlockHutExpedition create(final Destination destination)
    {
        CONSTRUCTING.set(destination);
        try
        {
            return new BlockHutExpedition(destination);
        }
        finally
        {
            CONSTRUCTING.remove();
        }
    }

    public Destination destination()
    {
        return destination != null ? destination : CONSTRUCTING.get();
    }

    @Override
    public String getHutName()
    {
        return destination().hutName();
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return ColonyExpeditions.id(getHutName());
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModContent.BUILDINGS.get(destination()).get();
    }
}
