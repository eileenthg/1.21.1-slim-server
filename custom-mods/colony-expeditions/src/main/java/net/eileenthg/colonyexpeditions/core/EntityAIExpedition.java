package net.eileenthg.colonyexpeditions.core;

import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.workers.util.GuardGearBuilder;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import net.minecraft.util.Tuple;
import com.minecolonies.api.util.constant.GuardConstants;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.eileenthg.colonyexpeditions.Destination;
import net.eileenthg.colonyexpeditions.ExpeditionConfig;
import net.eileenthg.colonyexpeditions.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The Netherworker AI with the nether swapped out. EntityAIWorkNetherMixin calls back into this class wherever the
 * Netherworker would touch a nether portal, flint and steel or the nether damage type.
 */
public class EntityAIExpedition extends EntityAIWorkNether
{
    /** Same limit as a vanilla portal's inner size. */
    private static final int MAX_SPAN = 21;

    public EntityAIExpedition(@NotNull final JobExpedition job)
    {
        super(job);
        // The Netherworker only lets a hut equip armour matching its own level. An expedition worker may wear
        // anything the colony can supply, so the configured tier is actually worn.
        itemsNeeded.clear();
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(0, Integer.MAX_VALUE, GuardConstants.LEATHER_BUILDING_LEVEL_RANGE, new Tuple<>(0, 99)));
    }

    /** Tool of at least the configured tier; requests one when missing, so the worker waits instead of setting off. */
    public boolean isToolMissing(final EquipmentTypeEntry type)
    {
        return checkForToolOrWeapon(type, ((BuildingExpedition) building).requiredGearLevel(false));
    }

    /** Full armour set of at least the configured tier. */
    public boolean isArmorMissing()
    {
        final int min = ((BuildingExpedition) building).requiredGearLevel(true);
        if (min < 0)
        {
            return false;
        }
        boolean missing = false;
        for (final var type : List.of(ModEquipmentTypes.helmet, ModEquipmentTypes.chestplate, ModEquipmentTypes.leggings, ModEquipmentTypes.boots))
        {
            missing |= checkForToolOrWeapon(type.get(), min);
        }
        return missing;
    }

    public Destination destination()
    {
        return ((BuildingExpedition) building).destination();
    }

    public Block portalBlock()
    {
        return ModContent.PORTALS.get(destination()).get();
    }

    public boolean isKeyMissing()
    {
        return PortalKeys.missing(this, ExpeditionConfig.key(destination()), ExpeditionConfig.keysPerTrip(destination()));
    }

    /** Replaces the Netherworker's openPortal: fill the frame's opening with the veil, using up the key. */
    public IAIState openExpeditionPortal()
    {
        final BlockPos portal = building.getPortalLocation();
        if (portal == null || currentRecipeStorage == null)
        {
            return AIWorkerState.START_WORKING;
        }
        if (!walkToWorkPos(portal))
        {
            return getState();
        }
        if (world.getBlockState(portal).is(portalBlock()))
        {
            return AIWorkerState.NETHER_LEAVE;
        }

        final Set<BlockPos> opening = findOpening(portal);
        if (opening.isEmpty())
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            return AIWorkerState.IDLE;
        }
        if (!PortalKeys.consume(worker, ExpeditionConfig.key(destination()), ExpeditionConfig.keysPerTrip(destination())))
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            return AIWorkerState.IDLE;
        }

        final BlockState veil = portalBlock().defaultBlockState().setValue(ExpeditionPortalBlock.AXIS, axisOf(portal));
        for (final BlockPos pos : opening)
        {
            // Clients only: neighbours are not updated while the opening is half filled.
            world.setBlock(pos, veil, Block.UPDATE_CLIENTS);
        }
        world.playSound(null, portal, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.4F, 1.2F);
        return AIWorkerState.NETHER_LEAVE;
    }

    /** Removes every veil block connected to the given position. */
    public void clearPortal(final BlockPos start)
    {
        final Deque<BlockPos> queue = new ArrayDeque<>();
        final Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_SPAN * MAX_SPAN)
        {
            final BlockPos pos = queue.poll();
            if (!seen.add(pos) || !world.getBlockState(pos).is(portalBlock()))
            {
                continue;
            }
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (final Direction dir : Direction.values())
            {
                queue.add(pos.relative(dir));
            }
        }
    }

    /** The frame runs along whichever horizontal axis has blocks closer on both sides. */
    private Direction.Axis axisOf(final BlockPos portal)
    {
        return span(portal, Direction.Axis.X) <= span(portal, Direction.Axis.Z) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private int span(final BlockPos portal, final Direction.Axis axis)
    {
        final Direction positive = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        int width = 1;
        for (final Direction dir : new Direction[] {positive, positive.getOpposite()})
        {
            BlockPos pos = portal.relative(dir);
            while (world.isEmptyBlock(pos) && width <= MAX_SPAN)
            {
                width++;
                pos = pos.relative(dir);
            }
        }
        return width;
    }

    /** The empty rectangle at the tagged portal position, bounded by blocks on every side. Empty if it is not enclosed. */
    private Set<BlockPos> findOpening(final BlockPos portal)
    {
        final Set<BlockPos> result = new HashSet<>();
        if (!world.isEmptyBlock(portal))
        {
            return result;
        }
        final Direction positive = Direction.fromAxisAndDirection(axisOf(portal), Direction.AxisDirection.POSITIVE);

        BlockPos corner = portal;
        while (world.isEmptyBlock(corner.relative(positive.getOpposite())))
        {
            corner = corner.relative(positive.getOpposite());
            if (corner.distManhattan(portal) > MAX_SPAN)
            {
                return result;
            }
        }
        while (world.isEmptyBlock(corner.below()))
        {
            corner = corner.below();
            if (corner.distManhattan(portal) > MAX_SPAN * 2)
            {
                return result;
            }
        }

        int width = 0;
        while (world.isEmptyBlock(corner.relative(positive, width)))
        {
            width++;
            if (width > MAX_SPAN)
            {
                return result;
            }
        }

        int height = 0;
        while (height <= MAX_SPAN && rowEmpty(corner.above(height), positive, width))
        {
            height++;
        }
        if (height < 2 || height > MAX_SPAN)
        {
            return result;
        }

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                result.add(corner.above(y).relative(positive, x));
            }
        }
        return result;
    }

    private boolean rowEmpty(final BlockPos start, final Direction dir, final int width)
    {
        for (int x = 0; x < width; x++)
        {
            if (!world.isEmptyBlock(start.relative(dir, x)))
            {
                return false;
            }
        }
        return true;
    }
}
