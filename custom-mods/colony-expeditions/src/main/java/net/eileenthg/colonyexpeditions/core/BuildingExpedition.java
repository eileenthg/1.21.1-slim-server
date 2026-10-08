package net.eileenthg.colonyexpeditions.core;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.settings.ISettingKey;
import com.minecolonies.core.colony.buildings.modules.settings.SettingKey;
import com.minecolonies.core.colony.buildings.modules.settings.StringSetting;
import java.util.ArrayList;
import java.util.List;
import net.eileenthg.colonyexpeditions.ColonyExpeditions;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import net.eileenthg.colonyexpeditions.Destination;
import net.eileenthg.colonyexpeditions.ExpeditionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;

/**
 * A Netherworker building bound to another destination. The parent keeps its trip counters private and reads
 * the trip limits from static methods, so this class keeps its own counters driven by the destination's config.
 */
public class BuildingExpedition extends BuildingNetherWorker
{
    /** Per-hut override of the gear a worker must carry before setting off; shown in the hut's Settings tab. */
    public static final ISettingKey<StringSetting> MIN_GEAR =
      new SettingKey<>(StringSetting.class, ColonyExpeditions.id("mingear"));

    public static final String GEAR_DEFAULT = "com.colony_expeditions.setting.gear.default";
    public static final String GEAR_NONE = "com.colony_expeditions.setting.gear.none";
    /** Index in this list is the MineColonies equipment level (tools 0 wood..4 netherite, armour 0 leather..4 diamond). */
    public static final List<String> GEAR_TIERS = List.of(
      "com.colony_expeditions.setting.gear.wood",
      "com.colony_expeditions.setting.gear.stone",
      "com.colony_expeditions.setting.gear.iron",
      "com.colony_expeditions.setting.gear.diamond",
      "com.colony_expeditions.setting.gear.netherite");

    public static StringSetting gearSetting()
    {
        final List<String> options = new ArrayList<>();
        options.add(GEAR_DEFAULT);
        options.add(GEAR_NONE);
        options.addAll(GEAR_TIERS);
        return new StringSetting(options, 0);
    }

    private static final String TAG_TRIPS = "expedition_trips";
    private static final String TAG_DAY = "expedition_day";

    private final Destination destination;
    private int periodDay;
    private int trips;
    private long snapTime;

    public BuildingExpedition(@NotNull final IColony colony, final BlockPos pos, final Destination destination)
    {
        super(colony, pos);
        this.destination = destination;
        keepX.put(stack -> stack.is(ExpeditionConfig.key(destination)), new Tuple<>(16, true));
    }

    public Destination destination()
    {
        return destination;
    }

    /** The tier this hut demands: the Settings tab override when set, otherwise the config value. */
    public int requiredGearLevel(final boolean armor)
    {
        final String choice = this.<StringSetting, String>getSetting(MIN_GEAR).getValue();
        if (GEAR_NONE.equals(choice))
        {
            return armor ? -1 : 0;
        }
        final int index = GEAR_TIERS.indexOf(choice);
        if (index >= 0)
        {
            return index;
        }
        return armor ? ExpeditionConfig.trips(destination).minArmorLevel.get() : ExpeditionConfig.trips(destination).minToolLevel.get();
    }

    /** A low-level hut still has to allow the tier its expedition demands, or the worker could never hold it. */
    @Override
    public int getMaxEquipmentLevel()
    {
        return Math.max(super.getMaxEquipmentLevel(), Math.max(requiredGearLevel(false), requiredGearLevel(true)));
    }

    @NotNull
    @Override
    public String getSchematicName()
    {
        return destination.workName();
    }

    @Override
    public void onWakeUp()
    {
        super.onWakeUp();
        snapTime = colony.getWorld().getDayTime();
        if (periodDay < ExpeditionConfig.trips(destination).periodDays.get())
        {
            periodDay++;
        }
        else
        {
            periodDay = 0;
            trips = 0;
        }
    }

    @Override
    public boolean isReadyForTrip()
    {
        if (snapTime == 0L)
        {
            snapTime = colony.getWorld().getDayTime();
        }
        if (Math.abs(colony.getWorld().getDayTime() - snapTime) >= 24000L)
        {
            periodDay++;
        }
        return trips < ExpeditionConfig.trips(destination).tripsPerPeriod.get();
    }

    @Override
    public void recordTrip()
    {
        trips++;
    }

    @Override
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag compound)
    {
        super.deserializeNBT(provider, compound);
        trips = compound.getInt(TAG_TRIPS);
        periodDay = compound.getInt(TAG_DAY);
    }

    @Override
    public CompoundTag serializeNBT(@NotNull final HolderLookup.Provider provider)
    {
        final CompoundTag compound = super.serializeNBT(provider);
        compound.putInt(TAG_TRIPS, trips);
        compound.putInt(TAG_DAY, periodDay);
        return compound;
    }
}
