package net.eileenthg.colonyexpeditions;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.ldtteam.structurize.api.RotationMirror;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.IAssignsJob;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.colony.buildings.modules.ExpeditionLogModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Server-only test harness, so trips can be tested with nobody online:
 * <pre>
 * /colonyexpeditions test colony              found a colony at world spawn (+64 east)
 * /colonyexpeditions test hut &lt;dest|nether&gt; [style]  paste the level 1 hut through the given style pack
 * /colonyexpeditions test hire &lt;dest|nether&gt;         hire and equip a worker
 * /colonyexpeditions test status              report every expedition worker
 * </pre>
 */
public final class ExpeditionTestCommand
{
    private static final GameProfile TESTER = new GameProfile(UUID.fromString("5e1f7a3c-0b1d-4e6a-9c3e-7e57e7e57e57"), "ExpeditionTester");

    /** Set by "test watch": the status report is logged every 200 ticks. */
    private static ServerLevel watching;
    private static int ticks;

    private ExpeditionTestCommand()
    {
    }

    public static boolean isWatching()
    {
        return watching != null;
    }

    public static void onServerTick(final net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)
    {
        if (watching != null && ++ticks % 200 == 0)
        {
            report(watching, msg -> ColonyExpeditions.LOG.info("[watch] {}", msg));
        }
    }

    /** Logs how an expedition worker or Netherworker died. */
    public static void onDeath(final net.neoforged.neoforge.event.entity.living.LivingDeathEvent event)
    {
        if (watching != null && event.getEntity() instanceof com.minecolonies.api.entity.citizen.AbstractEntityCitizen citizen)
        {
            ColonyExpeditions.LOG.warn("[test] citizen {} died: {} ({}) at {} invisible={}",
              citizen.getCitizenData() == null ? "?" : citizen.getCitizenData().getName(),
              event.getSource().getMsgId(), event.getSource().getEntity(), citizen.blockPosition().toShortString(), citizen.isInvisible());
        }
    }

    public static void onLeave(final net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event)
    {
        if (watching != null && event.getEntity() instanceof com.minecolonies.api.entity.citizen.AbstractEntityCitizen citizen)
        {
            ColonyExpeditions.LOG.warn("[test] citizen {} left level: {} at {}",
              citizen.getCitizenData() == null ? "?" : citizen.getCitizenData().getName(), citizen.getRemovalReason(), citizen.blockPosition().toShortString());
        }
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal(ColonyExpeditions.MOD_ID)
          .requires(src -> src.hasPermission(2))
          .then(Commands.literal("test")
            .then(Commands.literal("colony").executes(ExpeditionTestCommand::colony))
            .then(Commands.literal("hut")
              .then(Commands.argument("dest", StringArgumentType.word())
                .executes(ctx -> hut(ctx, "Minecolonies Original"))
                .then(Commands.argument("style", StringArgumentType.greedyString())
                  .executes(ctx -> hut(ctx, StringArgumentType.getString(ctx, "style"))))))
            .then(Commands.literal("hire")
              .then(Commands.argument("dest", StringArgumentType.word()).executes(ExpeditionTestCommand::hire)))
            .then(Commands.literal("wake").executes(ExpeditionTestCommand::wake))
            .then(Commands.literal("recall").executes(ExpeditionTestCommand::recall))
            .then(Commands.literal("kit")
              .then(Commands.argument("dest", StringArgumentType.word())
                .executes(ctx -> rekit(ctx, -1))
                .then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 4))
                  .executes(ctx -> rekit(ctx, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier"))))))
            .then(Commands.literal("watch").executes(ctx -> {
                watching = ctx.getSource().getLevel();
                return reply(ctx, "Logging status every 10 seconds");
            }))
            .then(Commands.literal("simulate")
              .then(Commands.argument("trials", com.mojang.brigadier.arguments.IntegerArgumentType.integer(10, 100000))
                .executes(ExpeditionTestCommand::simulate)))
            .then(Commands.literal("status").executes(ExpeditionTestCommand::status))));
    }

    private static BlockPos origin(final ServerLevel level)
    {
        return level.getSharedSpawnPos().offset(64, 0, 0).atY(level.getMaxBuildHeight() - 1);
    }

    private static FakePlayer tester(final ServerLevel level)
    {
        return FakePlayerFactory.get(level, TESTER);
    }

    private static void forceLoad(final ServerLevel level, final BlockPos center)
    {
        final ChunkPos c = new ChunkPos(center);
        for (int x = -3; x <= 3; x++)
        {
            for (int z = -3; z <= 3; z++)
            {
                level.setChunkForced(c.x + x, c.z + z, true);
            }
        }
    }

    private static BlockPos ground(final ServerLevel level, final BlockPos pos)
    {
        // Generate the chunk first; an unloaded chunk reports the world floor.
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
    }

    private static int colony(final CommandContext<CommandSourceStack> ctx)
    {
        final ServerLevel level = ctx.getSource().getLevel();
        final BlockPos pos = ground(level, origin(level));
        forceLoad(level, pos);
        final IColony existing = IColonyManager.getInstance().getColonyByPosFromWorld(level, pos);
        if (existing != null)
        {
            return reply(ctx, "Colony " + existing.getID() + " already exists at " + pos.toShortString());
        }
        final IColony colony = IColonyManager.getInstance().createColony(level, pos, tester(level), "Expedition Test", "Minecolonies Original");
        return reply(ctx, colony == null ? "createColony returned null" : "Created colony " + colony.getID() + " at " + pos.toShortString());
    }

    private static String blueprintPath(final String dest)
    {
        return "nether".equals(dest)
                 ? "mystic/netherworker1.blueprint"
                 : SharedBlueprints.FOLDER + "/" + dest + "/expedition_" + dest + "1.blueprint";
    }

    private static int hut(final CommandContext<CommandSourceStack> ctx, final String style)
    {
        final ServerLevel level = ctx.getSource().getLevel();
        final String dest = StringArgumentType.getString(ctx, "dest");
        final int offset = "nether".equals(dest) ? -1 : Destination.valueOf(dest.toUpperCase()).ordinal();
        // 4x3 grid south-east of the colony centre, 24 apart so the 17x14 huts never overlap; the Netherworker sits north-west.
        final BlockPos pos = ground(level, offset < 0 ? origin(level).offset(-24, 0, -24)
                                                      : origin(level).offset(-36 + 24 * (offset % 4), 0, 24 * (offset / 4)));
        forceLoad(level, pos);

        final String path = blueprintPath(dest);
        final Blueprint blueprint = StructurePacks.getBlueprint(style, path, level.registryAccess());
        if (blueprint == null)
        {
            return reply(ctx, "No blueprint " + path + " in style " + style);
        }
        final Block anchor = blueprint.getBlockState(blueprint.getPrimaryBlockOffset()).getBlock();
        if (!(anchor instanceof AbstractBlockHut<?> hut))
        {
            return reply(ctx, "Blueprint anchor is " + anchor + ", not a hut");
        }
        final FakePlayer player = tester(level);
        // Ownership may have been handed to a real player, which leaves the tester without place-hut rights.
        final IColony owner = IColonyManager.getInstance().getColonyByPosFromWorld(level, ground(level, origin(level)));
        if (owner != null)
        {
            owner.getPermissions().addPlayer(TESTER.getId(), TESTER.getName(), owner.getPermissions().getRankOfficer());
        }
        final boolean ok = hut.setup(player, level, pos, blueprint, RotationMirror.NONE, false, style, path);
        final IBuilding building = IColonyManager.getInstance().getBuilding(level, pos);
        // Paste the rest immediately instead of waiting on Structurize's tick queue.
        final BlockPos origin = pos.subtract(blueprint.getPrimaryBlockOffset());
        int placed = 0;
        for (final var info : blueprint.getBlockInfoAsList())
        {
            final BlockPos at = origin.offset(info.getPos());
            if (!at.equals(pos) && info.getState() != null && !info.getState().isAir())
            {
                level.setBlock(at, info.getState(), 2);
                placed++;
            }
        }
        if (building != null && building.getTileEntity() != null)
        {
            final var teData = blueprint.getTileEntityData(pos, blueprint.getPrimaryBlockOffset());
            if (teData != null)
            {
                building.getTileEntity().readSchematicDataFromNBT(teData);
                building.getTileEntity().setChanged();
                building.markDirty();
            }
        }
        return reply(ctx, "placed=" + placed + " setup=" + ok + " blueprint pack=" + blueprint.getPackName() + " at " + pos.toShortString()
                            + " building=" + (building == null ? "none" : building.getBuildingDisplayName() + " pack=" + building.getStructurePack()
                                                                          + " path=" + building.getBlueprintPath()));
    }

    private static int hire(final CommandContext<CommandSourceStack> ctx)
    {
        final ServerLevel level = ctx.getSource().getLevel();
        final String dest = StringArgumentType.getString(ctx, "dest");
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, ground(level, origin(level)));
        if (colony == null)
        {
            return reply(ctx, "No test colony");
        }
        final IBuilding building = findBuilding(colony, dest);
        if (building == null)
        {
            return reply(ctx, "No " + dest + " building in the test colony");
        }
        final ICitizenData citizen = colony.getCitizenManager().spawnOrCreateCivilian(null, level, new ArrayList<>(List.of(building.getPosition())), true);
        final IAssignsJob work = building.getFirstModuleOccurance(IAssignsJob.class);
        final boolean assigned = work != null && work.assignCitizen(citizen);

        final var inv = citizen.getInventory();
        final List<ItemStack> kit = kit(dest);
        kit.forEach(stack -> InventoryUtils.addItemStackToItemHandler(inv, stack));
        return reply(ctx, "Hired " + citizen.getName() + " assigned=" + assigned + " kit=" + kit.size() + " stacks");
    }

    /**
     * Colonies only tick while a member player is close by or online. Registers the stand-in player again,
     * which the colony forgets on every restart.
     */
    private static int wake(final CommandContext<CommandSourceStack> ctx)
    {
        final ServerLevel level = ctx.getSource().getLevel();
        final BlockPos pos = ground(level, origin(level));
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, pos);
        if (colony == null)
        {
            return reply(ctx, "No test colony");
        }
        forceLoad(level, pos);
        colony.getServerBuildingManager().getBuildings().keySet().forEach(p -> forceLoad(level, p));
        final FakePlayer player = tester(level);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        // MineColonies refuses fake players through addImportantColonyPlayer, but the returned set is live.
        colony.getPackageManager().getImportantColonyPlayers().add(player);
        return reply(ctx, "Woke colony " + colony.getID() + " state=" + colony.getState() + " loadedChunks=" + colony.getLoadedChunkCount());
    }

    /**
     * Travellers respawn from the colony tick, which needs a town hall and only runs every ~5 minutes.
     * Runs the same per-citizen check immediately.
     */
    private static int recall(final CommandContext<CommandSourceStack> ctx)
    {
        final ServerLevel level = ctx.getSource().getLevel();
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, ground(level, origin(level)));
        if (colony == null)
        {
            return reply(ctx, "No test colony");
        }
        int back = 0;
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            final boolean away = citizen.getEntity().isEmpty();
            citizen.updateEntityIfNecessary();
            if (away && citizen.getEntity().isPresent())
            {
                back++;
            }
        }
        return reply(ctx, "Respawned " + back + " citizen(s); still travelling: "
                            + colony.getCitizenManager().getCitizens().stream().filter(c -> colony.getTravellingManager().isTravelling(c)).count());
    }

    /** Clears and re-equips the worker of an existing hut. */
    private static int rekit(final CommandContext<CommandSourceStack> ctx, final int tierOverride)
    {
        final ServerLevel level = ctx.getSource().getLevel();
        final String dest = StringArgumentType.getString(ctx, "dest");
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, ground(level, origin(level)));
        final IBuilding building = colony == null ? null : findBuilding(colony, dest);
        if (building == null)
        {
            return reply(ctx, "No " + dest + " building");
        }
        for (final ICitizenData citizen : building.getAllAssignedCitizen())
        {
            final var inv = citizen.getInventory();
            for (int i = 0; i < inv.getSlots(); i++)
            {
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
            kit(dest, tierOverride).forEach(stack -> InventoryUtils.addItemStackToItemHandler(inv, stack));
        }
        return reply(ctx, "Re-equipped " + building.getAllAssignedCitizen().size() + " worker(s)");
    }

    /** Gear at the tier the destination demands, so a test worker is allowed to set off. */
    private static List<ItemStack> kit(final String dest)
    {
        return kit(dest, -1);
    }

    /** tierOverride < 0 means "whatever the destination currently demands". */
    private static List<ItemStack> kit(final String dest, final int tierOverride)
    {
        final int tier = tierOverride >= 0 ? tierOverride
                           : "nether".equals(dest) ? ExpeditionConfig.NETHER.minToolLevel.get()
                               : ExpeditionConfig.trips(Destination.valueOf(dest.toUpperCase())).minToolLevel.get();
        final Item[][] tools = {
          {Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL},
          {Items.STONE_SWORD, Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL},
          {Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL},
          {Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL},
          {Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL}};
        final Item[][] armor = {
          {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
          {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
          {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
          {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
          {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}};
        final int t = Math.min(Math.max(tier, 0), 4);

        final List<ItemStack> kit = new ArrayList<>();
        for (final Item item : tools[t])
        {
            kit.add(new ItemStack(item));
        }
        for (final Item item : armor[t])
        {
            kit.add(new ItemStack(item));
        }
        kit.add(new ItemStack(Items.BREAD, 32));
        kit.add(new ItemStack(Items.COBBLESTONE, 64));
        kit.add(new ItemStack(Items.TORCH, 32));
        kit.add(new ItemStack(Items.LADDER, 16));
        kit.add("nether".equals(dest) ? new ItemStack(ExpeditionConfig.netherOpener(), 4)
                                      : new ItemStack(ExpeditionConfig.key(Destination.valueOf(dest.toUpperCase())), 4));
        return kit;
    }

    private static IBuilding findBuilding(final IColony colony, final String dest)
    {
        final String id = "nether".equals(dest) ? "minecolonies:netherworker" : ColonyExpeditions.MOD_ID + ":expedition_" + dest;
        return colony.getServerBuildingManager().getBuildings().values().stream()
                 .filter(b -> b.getBuildingType().getRegistryName().toString().equals(id))
                 .findFirst().orElse(null);
    }

    /** Prints the survival rate per gear tier for every destination, so the required tiers can be set from data. */
    private static int simulate(final CommandContext<CommandSourceStack> ctx)
    {
        final ServerLevel level = ctx.getSource().getLevel();
        final int trials = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "trials");
        reply(ctx, "survival % by gear tier (wood/leather, stone/gold, iron/chain, diamond/iron, netherite/diamond)");
        for (final Destination dest : Destination.values())
        {
            final StringBuilder line = new StringBuilder(String.format("%-18s", dest.id()));
            for (int tier = 0; tier <= 4; tier++)
            {
                line.append(String.format(" t%d=%3.0f%%", tier, ExpeditionSimulator.survivalRate(level, tester(level), dest, tier, trials, 1, 20.0F) * 100));
            }
            reply(ctx, line.toString());
        }
        return 1;
    }

    private static int status(final CommandContext<CommandSourceStack> ctx)
    {
        report(ctx.getSource().getLevel(), msg -> reply(ctx, msg));
        return 1;
    }

    private static void report(final ServerLevel level, final java.util.function.Consumer<String> sink)
    {
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, ground(level, origin(level)));
        if (colony == null)
        {
            sink.accept("No test colony");
            return;
        }
        sink.accept("colony " + colony.getID() + " " + colony.getState() + " citizens=" + colony.getCitizenManager().getCurrentCitizenCount());
        for (final IBuilding b : colony.getServerBuildingManager().getBuildings().values())
        {
            if (!(b instanceof BuildingNetherWorker))
            {
                continue;
            }
            final BuildingNetherWorker nb = (BuildingNetherWorker) b;
            final StringBuilder line = new StringBuilder(b.getBuildingType().getRegistryName() + " lvl=" + b.getBuildingLevel()
                                                          + " portal=" + nb.getPortalLocation() + " ready=" + nb.isReadyForTrip()
                                                          + " netherMaxPerPeriod=" + BuildingNetherWorker.getMaxPerPeriod()
                                                          + " trips=" + tripCount(nb));
            final ExpeditionLogModule log = b.getFirstModuleOccurance(ExpeditionLogModule.class);
            if (log != null)
            {
                line.append(" log=").append(log.getLog().getStatus())
                  .append("\n    mobs=").append(log.getLog().getMobs().stream()
                                              .map(t -> t.getB() + "x" + net.minecraft.world.entity.EntityType.getKey(t.getA())).collect(Collectors.joining(", ")))
                  .append("\n    loot=").append(log.getLog().getLoot().stream()
                                              .map(i -> i.getAmount() + "x" + i.getItem()).collect(Collectors.joining(", ")));
            }
            final BuildingNetherWorker.CraftingModule craft = b.getFirstModuleOccurance(BuildingNetherWorker.CraftingModule.class);
            if (craft != null)
            {
                line.append(" colony=").append(colony.getState())
                  .append(" recipeKey=").append(craft.getCustomRecipeKey())
                  .append(" recipes=").append(craft.getRecipes().size())
                  .append(" fulfillable=").append(craft.getFirstFulfillableRecipe(ItemStack::isEmpty, 1, false) != null)
                  .append(" built=").append(b.isBuilt());
            }
            if (b.getTileEntity() != null)
            {
                line.append(" tags=").append(b.getTileEntity().getPositionedTags());
            }
            if (nb.getPortalLocation() != null)
            {
                line.append(" portalBlock=").append(level.getBlockState(nb.getPortalLocation()).getBlock());
            }
            for (final ICitizenData citizen : b.getAllAssignedCitizen())
            {
                line.append("\n  ").append(citizen.getName()).append(" status=").append(citizen.getJobStatus());
                if (citizen.getJob() != null && citizen.getJob().getWorkerAI() instanceof EntityAIWorkNether ai)
                {
                    line.append(" state=").append(ai.getState());
                }
                citizen.getEntity().ifPresent(e -> line.append(" hp=").append(e.getHealth()).append(" at=").append(e.blockPosition().toShortString()));
                final var inv = citizen.getInventory();
                final List<String> items = new ArrayList<>();
                for (int i = 0; i < inv.getSlots(); i++)
                {
                    final ItemStack s = inv.getStackInSlot(i);
                    if (!s.isEmpty())
                    {
                        items.add(s.getCount() + "x" + s.getItem());
                    }
                }
                line.append("\n    inv=").append(items.stream().collect(Collectors.joining(", ")));
                line.append("\n    requests=").append(b.getOpenRequests(citizen.getId()).stream()
                                                    .map(r -> r.getShortDisplayString().getString()).collect(Collectors.joining("; ")));
            }
            sink.accept(line.toString());
        }
    }

    /** Reads the private trip counter of a Netherworker building (or BuildingExpedition's own). */
    private static String tripCount(final BuildingNetherWorker building)
    {
        try
        {
            final Class<?> owner = building instanceof net.eileenthg.colonyexpeditions.core.BuildingExpedition
                                     ? net.eileenthg.colonyexpeditions.core.BuildingExpedition.class : BuildingNetherWorker.class;
            final var field = owner.getDeclaredField(owner == BuildingNetherWorker.class ? "currentTrips" : "trips");
            field.setAccessible(true);
            return String.valueOf(field.get(building));
        }
        catch (final ReflectiveOperationException e)
        {
            return "?";
        }
    }

    private static int reply(final CommandContext<CommandSourceStack> ctx, final String msg)
    {
        ColonyExpeditions.LOG.info("[test] {}", msg);
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}
