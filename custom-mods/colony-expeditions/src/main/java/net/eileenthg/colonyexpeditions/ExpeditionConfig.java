package net.eileenthg.colonyexpeditions;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Balance settings, written to config/colony_expeditions-common.toml. */
public final class ExpeditionConfig
{
    public static final ModConfigSpec SPEC;

    public static final Trips NETHER;
    public static final ModConfigSpec.ConfigValue<String> NETHER_OPENER;
    public static final ModConfigSpec.IntValue NETHER_OPENER_PER_TRIP;

    private static final Map<Destination, Trips> TRIPS = new EnumMap<>(Destination.class);
    private static final Map<Destination, ModConfigSpec.ConfigValue<String>> KEYS = new EnumMap<>(Destination.class);
    private static final Map<Destination, ModConfigSpec.IntValue> KEYS_PER_TRIP = new EnumMap<>(Destination.class);

    static
    {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("The stock MineColonies Netherworker.").push("netherworker");
        NETHER_OPENER = b.comment("Item the Netherworker uses up to light the nether portal (replaces flint and steel).")
                          .define("portal_opener", "dimensional_tears:dimensional_tears_bottle");
        NETHER_OPENER_PER_TRIP = b.comment("Opener items used per portal opening. Durability items lose this much durability instead.")
                                   .defineInRange("opener_per_trip", 1, 0, 64);
        NETHER = new Trips(b, 1, 3, 1, 0);
        b.pop();

        b.push("expeditions");
        for (final Destination dest : Destination.values())
        {
            b.push(dest.id());
            KEYS.put(dest, b.comment("Item that opens this portal. Set it to dimensional_tears:dimensional_tears_bottle to use tears instead.")
                             .define("portal_key", dest.defaultKey()));
            KEYS_PER_TRIP.put(dest, b.comment("Keys used per portal opening. Durability items (the Eternal Tales crystals) lose this much durability instead.")
                                      .defineInRange("keys_per_trip", 1, 0, 64));
            TRIPS.put(dest, new Trips(b, 1, 3, dest.minToolLevel(), dest.minArmorLevel()));
            b.pop();
        }
        b.pop();

        SPEC = b.build();
    }

    private ExpeditionConfig()
    {
    }

    public static Trips trips(final Destination dest)
    {
        return TRIPS.get(dest);
    }

    public static Item key(final Destination dest)
    {
        return item(KEYS.get(dest).get());
    }

    public static int keysPerTrip(final Destination dest)
    {
        return KEYS_PER_TRIP.get(dest).get();
    }

    public static Item netherOpener()
    {
        return item(NETHER_OPENER.get());
    }

    private static Item item(final String id)
    {
        final ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Items.AIR : Optional.ofNullable(BuiltInRegistries.ITEM.get(rl)).orElse(Items.AIR);
    }

    /** Settings shared by the Netherworker and every expedition. */
    public static final class Trips
    {
        public final ModConfigSpec.IntValue tripsPerPeriod;
        public final ModConfigSpec.IntValue periodDays;
        public final ModConfigSpec.DoubleValue tripTimeMultiplier;
        public final ModConfigSpec.DoubleValue damageMultiplier;
        public final ModConfigSpec.IntValue minToolLevel;
        public final ModConfigSpec.IntValue minArmorLevel;

        Trips(final ModConfigSpec.Builder b, final int trips, final int days, final int tools, final int armor)
        {
            tripsPerPeriod = b.comment("Trips allowed per period.").defineInRange("trips_per_period", trips, 0, 100);
            periodDays = b.comment("Length of the trip period in days.").defineInRange("period_days", days, 1, 100);
            tripTimeMultiplier = b.comment("Multiplier on how long the worker stays away.").defineInRange("trip_time_multiplier", 1.0, 0.05, 20.0);
            damageMultiplier = b.comment("Multiplier on damage taken from each encounter.").defineInRange("damage_multiplier", 1.0, 0.0, 20.0);
            minToolLevel = b.comment("Tool tier the worker must hold before leaving: 0 wood or gold, 1 stone, 2 iron, 3 diamond, 4 netherite.",
                                     "The worker waits and asks the colony for that tier instead of setting off under-equipped.")
                             .defineInRange("min_tool_level", tools, 0, 5);
            minArmorLevel = b.comment("Armour tier the worker must wear before leaving: 0 leather, 1 gold, 2 chain, 3 iron, 4 diamond.",
                                      "-1 requires no armour at all.")
                              .defineInRange("min_armor_level", armor, -1, 5);
        }
    }
}
