package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.eileenthg.maidchef.station.Inv;
import net.eileenthg.maidchef.station.Station;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Chef's whole work loop as one long-running behaviour: plan → fetch → cook → deliver → return leftovers → plan again.
 * Job state lives in {@link #STATES} keyed by maid, so closing the config screen (which rebuilds her brain) doesn't lose it.
 */
public final class ChefBehavior extends Behavior<EntityMaid> {
    private static final double REACH = 2.6;
    private static final int STUCK_TICKS = 200;
    private static final int STATION_BUSY_TIMEOUT = 600;
    private static final Map<EntityMaid, State> STATES = new WeakHashMap<>();

    enum Phase { PLAN, FETCH, COOK, DELIVER, RETURN, PICKUP }

    static final class State {
        Phase phase = Phase.PLAN;
        long nextPlan;
        @Nullable Planner.Job job;
        Planner.Kitchen kitchen;
        /** Items taken out of the ingredient chests for this job (returned if unused). */
        final Map<Item, Integer> withdrawn = new LinkedHashMap<>();
        /** Everything the stations handed her during this job. */
        final Map<Item, Integer> collected = new LinkedHashMap<>();
        Map<Item, Integer> stillNeeded = new LinkedHashMap<>();
        /** Which step of the job she's on, and how many units of it are still to load. */
        int stepIndex;
        int remaining;
        /** An intermediate step failed, so the dish can't be finished; deliver/return what she has. */
        boolean chainBroken;
        /** Filling a pot over several trips: units being staged, trips made, trips without progress. */
        boolean staging;
        int stageUnits;
        int stageTrips;
        int stageStalls;
        int produced;
        /** Result items already in her backpack before the job, so pickups by TLM itself still count. */
        int resultBefore;
        boolean loadActive;
        long loadStart;
        int loadUnits;
        long waitStart;
        boolean fuelAdded;
        // walking
        @Nullable BlockPos walkingTo;
        /** Where she last arrived (or gave up walking and works from where she stands). Stays valid until the target changes. */
        @Nullable BlockPos arrivedAt;
        long walkStart;
        double bestDist;
        long bestDistAt;
        String lastBubble = "";
        // a player walked off with something from her station
        final Map<Item, Integer> owed = new LinkedHashMap<>();
        final Map<Item, Integer> owedBaseline = new LinkedHashMap<>();
        String owedFrom = "";
        long owedSince;
        long owedBubbleAt;
        boolean aborted;
        // asking the owner for ingredients
        long nextRequest;
        String lastRequestKey = "";
        long lastRequestAt = Long.MIN_VALUE / 2;
        int requestCount;
        @Nullable Planner.Request lastRequest;
        // slow stations: she started a load and left it to work
        boolean leftBrewing;
        @Nullable Pickup pickup;
    }

    /** Coming back for a load she left at a slow station. */
    static final class Pickup {
        final String entry;
        final BlockPos pos;
        final Station station;
        final net.eileenthg.maidchef.station.RecipeNeed need;
        final int units;
        /** Containers to pour or serve into, fetched before going to the station. */
        final Map<Item, Integer> bring = new LinkedHashMap<>();
        /** What the station needs for its next stage (a vat's next ingredient, a lid). */
        final List<net.minecraft.world.item.crafting.Ingredient> attend = new java.util.ArrayList<>();
        /** The station took over again after she tended it: keep it on the list. */
        boolean keep;
        int got;
        boolean delivered;

        Pickup(String entry, BlockPos pos, Station station, net.eileenthg.maidchef.station.RecipeNeed need, int units) {
            this.entry = entry;
            this.pos = pos;
            this.station = station;
            this.need = need;
            this.units = units;
        }
    }

    public ChefBehavior() {
        super(Map.of(), 1, Integer.MAX_VALUE);
    }

    static Planner.Step step(State s) {
        return s.job.steps().get(Math.min(s.stepIndex, s.job.steps().size() - 1));
    }

    static boolean finalStep(State s) {
        return s.stepIndex >= s.job.steps().size() - 1;
    }

    public static State state(EntityMaid maid) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(maid, m -> new State());
        }
    }

    /** For the self-test: forget everything about this maid. */
    public static void reset(EntityMaid maid) {
        synchronized (STATES) {
            STATES.remove(maid);
        }
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return maid.getTask() == TaskChef.INSTANCE && !maid.isMaidInSittingPose() && !maid.isSleeping();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long time) {
        return checkExtraStartConditions(level, maid);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long time) {
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        state(maid).walkingTo = null;
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long time) {
        State s = state(maid);
        if (!s.owed.isEmpty() && s.job != null) {
            waitForReturn(level, maid, s, time);
            return;
        }
        switch (s.phase) {
            case PLAN -> plan(level, maid, s, time);
            case FETCH -> fetch(level, maid, s, time);
            case COOK -> cook(level, maid, s, time);
            case DELIVER -> deliver(level, maid, s, time);
            case RETURN -> giveBack(level, maid, s, time);
            case PICKUP -> pickup(level, maid, s, time);
        }
    }

    // ------------------------------------------------------------------ plan

    private void plan(ServerLevel level, EntityMaid maid, State s, long time) {
        if (time < s.nextPlan) return;
        ChefData data = TaskChef.data(maid);
        if (!maid.isHomeModeEnable()) {
            setStatus(maid, "Waiting: turn on home mode so I know where the kitchen is.");
            s.nextPlan = time + ChefConfig.IDLE_REPLAN_TICKS.get();
            return;
        }
        int radius = Math.min(data.radius(), ChefConfig.MAX_RADIUS.get());
        s.kitchen = Planner.scan(level, maid, radius);
        if (startPickup(level, maid, s, data)) return;
        Map<Item, Integer> pool = Planner.pool(level, maid, s.kitchen);
        Planner.Choice choice = Planner.choose(level, maid, s.kitchen, pool, data);
        if (!choice.dropFromRecent().isEmpty()) {
            for (String id : choice.dropFromRecent()) data = data.withoutRecent(id);
            maid.setAndSyncData(ChefPlugin.CHEF_DATA, data);
        }
        Planner.Job job = choice.job();
        maybeRequest(level, maid, s, pool, data, job != null ? job.value() : 0, time);
        if (job == null) {
            String why;
            if (s.kitchen.stations().isEmpty()) why = "no cooking stations within " + radius + " blocks";
            else if (!s.kitchen.hasHub() && pool.isEmpty()) why = "no ingredients (give me a Culinary Hub bound to an ingredient chest)";
            else if (pool.isEmpty()) why = "the ingredient chests are empty";
            else if (data.targetItem() != null) why = "can't make " + data.targetItem().getDefaultInstance().getHoverName().getString()
                    + (choice.why() != null ? ": " + choice.why() : "") + " (" + stationSummary(level, s.kitchen) + ")";
            else why = "nothing worth cooking with these ingredients at a free station (" + stationSummary(level, s.kitchen) + ")";
            setStatus(maid, "Idle: " + why + ".");
            bubble(maid, s, "Nothing to cook right now...");
            s.nextPlan = time + ChefConfig.IDLE_REPLAN_TICKS.get();
            return;
        }
        s.job = job;
        s.withdrawn.clear();
        s.collected.clear();
        s.stepIndex = 0;
        s.chainBroken = false;
        s.remaining = job.steps().get(0).units();
        s.produced = 0;
        s.loadActive = false;
        s.fuelAdded = false;
        s.waitStart = 0;
        s.aborted = false;
        s.owed.clear();
        s.owedBaseline.clear();
        IItemHandler bag = Inv.backpack(maid);
        s.resultBefore = Inv.count(bag, job.dish());
        s.staging = false;
        s.stillNeeded = shortfall(maid, job.steps().get(0).fetch());
        int expect = job.amount();
        setStatus(maid, "Making " + expect + "x " + job.dishName() + (job.steps().size() > 1 ? ": " + job.describe() : " at the " + job.last().at().station().name()) + ".");
        bubble(maid, s, "I'll make " + expect + " " + job.dishName() + "!");
        s.phase = s.stillNeeded.isEmpty() ? Phase.COOK : Phase.FETCH;
        trace(maid, "plan " + expect + "x " + job.dish() + ": " + job.describe() + " fetch " + job.fetch()
                + " still to fetch " + s.stillNeeded + " bag " + bagSummary(maid));
    }

    // ------------------------------------------------------------------ requests

    /** Now and then: is there a much better dish she could make if her owner brought one or two things? */
    private static void maybeRequest(ServerLevel level, EntityMaid maid, State s, Map<Item, Integer> pool, ChefData data, double currentBest, long time) {
        if (time < s.nextRequest || ChefConfig.REQUEST_MAX_MISSING.get() <= 0) return;
        s.nextRequest = time + ChefConfig.REQUEST_INTERVAL_TICKS.get();
        java.util.UUID owner = maid.getOwnerUUID();
        if (owner == null) return;
        ChefRequests requests = ChefRequests.get(level.getServer());
        Planner.Request r = Planner.findRequest(level, maid, s.kitchen, pool, data, requests.blocked(owner), currentBest);
        if (r == null) {
            trace(maid, "nothing worth asking for (best now " + currentBest + ")");
            return;
        }
        String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(r.dish()) + " " + r.items().keySet();
        if (key.equals(s.lastRequestKey) && time - s.lastRequestAt < ChefConfig.REQUEST_REPEAT_TICKS.get()) return;
        s.lastRequestKey = key;
        s.lastRequestAt = time;
        s.lastRequest = r;
        s.requestCount++;
        for (Item item : r.items().keySet()) requests.asked(owner, item, r.dish(), level.getGameTime());
        trace(maid, "asking for " + r.items() + " to make " + r.amount() + "x " + r.dish() + " (" + r.value() + " vs " + currentBest + "): " + r.chain());
        s.lastBubble = "";
        bubble(maid, s, "Master, could you get me " + r.itemsText() + "? Then I can make " + r.dishName() + "!");
        if (!ChefConfig.REQUEST_CHAT.get()) return;
        net.minecraft.server.level.ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return;
        net.minecraft.network.chat.MutableComponent msg = Component.literal("[" + maid.getName().getString() + "] ")
                .withStyle(net.minecraft.ChatFormatting.GOLD)
                .append(Component.literal(String.format("I could make %dx %s (worth %.0f, vs %.0f now) if I had %s. ",
                        r.amount(), r.dishName(), r.value(), currentBest, r.itemsText())).withStyle(net.minecraft.ChatFormatting.WHITE));
        for (Item item : r.items().keySet()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
            String name = item.getDefaultInstance().getHoverName().getString();
            msg.append(Component.literal("[Never ask for " + name + "] ").withStyle(st -> st
                    .withColor(net.minecraft.ChatFormatting.RED)
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/maidchef block " + id))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                            Component.literal("None of your maids will ask for " + name + " again.\nUndo it in the Chef task's Requests tab.")))));
        }
        player.sendSystemMessage(msg);
    }

    // ------------------------------------------------------------------ fetch

    private void fetch(ServerLevel level, EntityMaid maid, State s, long time) {
        Planner.Job job = s.job;
        BlockPos chest = nextChestWith(level, s.kitchen.ingredientChests(), s.stillNeeded);
        if (chest == null) {
            if (step(s).at().station().canStage() && s.staging) {
                // let the pot step notice nothing more is coming
                s.stillNeeded.clear();
                s.phase = Phase.COOK;
                return;
            }
            // the chests no longer have everything: cook as many as she can with what she carries
            shrinkToCarried(maid, s);
            s.phase = s.remaining > 0 ? Phase.COOK : Phase.RETURN;
            return;
        }
        if (!approach(maid, s, chest, time)) return;
        IItemHandler h = Planner.handler(level, chest);
        IItemHandler bag = Inv.backpack(maid);
        boolean movedAny = false;
        if (h != null) {
            for (Iterator<Map.Entry<Item, Integer>> it = s.stillNeeded.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Item, Integer> e = it.next();
                int moved = Inv.move(h, bag, e.getKey(), e.getValue(), false);
                if (moved > 0) movedAny = true;
                if (moved > 0) s.withdrawn.merge(e.getKey(), moved, Integer::sum);
                trace(maid, "fetch " + e.getKey() + " wanted " + e.getValue() + " moved " + moved);
                if (moved >= e.getValue()) it.remove();
                else e.setValue(e.getValue() - moved);
            }
            maid.swing(maid.getUsedItemHand());
        }
        if (s.stillNeeded.isEmpty()) {
            s.phase = Phase.COOK;
        } else if (step(s).at().station().canStage() && (movedAny || s.staging)) {
            // backpack full (or the chest ran dry): take this load to the pot and come back for the rest
            s.stillNeeded.clear();
            s.phase = Phase.COOK;
        } else if (nextChestWith(level, s.kitchen.ingredientChests(), s.stillNeeded) == null) {
            shrinkToCarried(maid, s);
            s.phase = s.remaining > 0 ? Phase.COOK : Phase.RETURN;
        }
        s.walkingTo = null;
    }

    /** What she still has to bring for this list (minus what's already in her backpack). */
    private static Map<Item, Integer> shortfall(EntityMaid maid, Map<Item, Integer> wanted) {
        IItemHandler bag = Inv.backpack(maid);
        Map<Item, Integer> out = new LinkedHashMap<>();
        wanted.forEach((item, n) -> {
            int missing = n - Inv.count(bag, item);
            if (missing > 0) out.put(item, missing);
        });
        return out;
    }

    private static String stationSummary(ServerLevel level, Planner.Kitchen kitchen) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (Planner.Found f : kitchen.stations()) {
            int[] c = counts.computeIfAbsent(f.station().name(), k -> new int[2]);
            c[0]++;
            if (f.station().idle(level, f.pos())) c[1]++;
        }
        StringBuilder b = new StringBuilder();
        counts.forEach((name, c) -> {
            if (b.length() > 0) b.append(", ");
            b.append(name).append(c[0] > 1 ? " x" + c[0] : "").append(c[1] == 0 ? ": not ready" : c[1] < c[0] ? ": " + c[1] + " ready" : "");
        });
        return b.toString();
    }

    /** The chests changed since planning. A one-step job shrinks to what she carries; a chain is abandoned. */
    private static void shrinkToCarried(EntityMaid maid, State s) {
        IItemHandler bag = Inv.backpack(maid);
        int units = 0;
        if (s.job.steps().size() == 1) {
            Planner.Step st = s.job.steps().get(0);
            units = s.remaining;
            for (Map.Entry<Item, Integer> e : st.alloc().perUnit().entrySet()) {
                units = Math.min(units, Inv.count(bag, e.getKey()) / e.getValue());
            }
            if (st.need().tool() != null && Inv.findLive(maid, st.need().tool()::test).isEmpty()) units = 0;
        }
        s.remaining = Math.max(0, units);
        if (s.remaining == 0) s.chainBroken = true;
        s.stillNeeded.clear();
    }

    @Nullable
    private static BlockPos nextChestWith(ServerLevel level, List<BlockPos> chests, Map<Item, Integer> needed) {
        for (BlockPos p : chests) {
            IItemHandler h = Planner.handler(level, p);
            if (h == null) continue;
            for (Item item : needed.keySet()) {
                if (Inv.count(h, item) > 0) return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ cook

    private void cook(ServerLevel level, EntityMaid maid, State s, long time) {
        Planner.Step step = step(s);
        boolean last = finalStep(s);
        Station st = step.at().station();
        BlockPos pos = step.at().pos();
        if (!approach(maid, s, pos, time)) return;
        maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
        Item result = step.need().resultItem();
        ChefClaims.activate(maid, pos, it -> it == result || step.need().extras().contains(it));

        if (s.loadActive) {
            if (time % 10 != 0) return;
            int got = st.collect(level, maid, pos, step.need(), s.collected);
            if (got > 0) trace(maid, "collected " + got + " " + result + " bag " + bagSummary(maid));
            if (last) s.produced += got;
            if (st.finished(level, pos)) {
                got = st.collect(level, maid, pos, step.need(), s.collected);
                if (got > 0) trace(maid, "collected " + got + " " + result + " (final) bag " + bagSummary(maid));
                if (last) s.produced += got;
                s.loadActive = false;
                trace(maid, "load finished (step " + (s.stepIndex + 1) + "/" + s.job.steps().size() + "); remaining units " + s.remaining);
            } else if (time - s.loadStart > (long) s.loadUnits * Math.max(20, step.need().cookTicks()) * 2 + 600) {
                // something is wrong with the station (lost heat, blocked output...): stop here
                got = st.collect(level, maid, pos, step.need(), s.collected);
                if (last) s.produced += got;
                s.loadActive = false;
                s.remaining = 0;
                if (!last) s.chainBroken = true;
                trace(maid, "load timed out at the " + st.name());
            }
            return;
        }

        if (s.remaining <= 0) {
            ChefClaims.deactivate(maid, pos);
            if (!last && !s.chainBroken) {
                // on to the next step; intermediates wait in her backpack
                s.stepIndex++;
                s.remaining = step(s).units();
                s.fuelAdded = false;
                s.waitStart = 0;
                s.staging = false;
                s.stillNeeded = shortfall(maid, step(s).fetch());
                trace(maid, "next step " + (s.stepIndex + 1) + "/" + s.job.steps().size() + ": " + step(s).label() + " fetch " + s.stillNeeded
                        + " bag " + bagSummary(maid));
                if (!s.stillNeeded.isEmpty()) {
                    s.phase = Phase.FETCH;
                    s.walkingTo = null;
                }
                return;
            }
            s.phase = Phase.DELIVER;
            s.walkingTo = null;
            return;
        }
        if (st.canStage()) {
            stageStep(level, maid, s, st, step, pos, last, time);
            return;
        }
        if (!st.idle(level, pos)) {
            if (s.waitStart == 0) s.waitStart = time;
            if (time - s.waitStart > STATION_BUSY_TIMEOUT) {
                trace(maid, "station stayed busy; giving up the rest (" + s.remaining + " units)");
                s.remaining = 0;
                if (!last) s.chainBroken = true;
            }
            return;
        }
        s.waitStart = 0;
        if (st.usesFuel() && !s.fuelAdded && step.fuel() != null) {
            st.addFuel(level, maid, pos, step.fuel(), step.fuelCount());
            s.fuelAdded = true;
        }
        int want = Math.min(s.remaining, st.loadCap(step.need()));
        Station.LoadResult r = st.load(level, maid, pos, step.need(), step.alloc(), want, s.collected);
        trace(maid, "load " + st.name() + " wanted " + want + " units, loaded " + r.units() + ", instant results " + r.results() + " bag " + bagSummary(maid));
        if (r.units() <= 0) {
            s.remaining = 0;
            if (!last) s.chainBroken = true;
            return;
        }
        maid.swing(maid.getUsedItemHand());
        s.remaining -= r.units();
        if (last) s.produced += r.results();
        if (step.need().cookTicks() > 0) {
            s.loadActive = true;
            s.loadStart = time;
            s.loadUnits = r.units();
            int leave = ChefConfig.LEAVE_AFTER_TICKS.get();
            if (last && s.remaining <= 0 && leave > 0 && (long) step.need().cookTicks() * Math.max(1, r.units()) > leave) {
                leaveToBrew(maid, s, st, step, pos, r.units());
            }
        }
    }

    // ------------------------------------------------------------------ slow stations: start, leave, come back

    private static String posKey(BlockPos p) {
        return p.getX() + "," + p.getY() + "," + p.getZ();
    }

    /** The load will take a while: note it, put the leftovers away and go on with other cooking. */
    private void leaveToBrew(EntityMaid maid, State s, Station st, Planner.Step step, BlockPos pos, int units) {
        ChefData data = TaskChef.data(maid);
        String entry = posKey(pos) + "|" + step.need().id() + "|" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(step.need().resultItem())
                + "|" + units;
        List<String> list = new java.util.ArrayList<>(data.brewing());
        list.removeIf(e -> e.startsWith(posKey(pos) + "|"));
        list.add(entry);
        maid.setAndSyncData(ChefPlugin.CHEF_DATA, data.withBrewing(list));
        s.loadActive = false;
        s.leftBrewing = true;
        ChefClaims.deactivate(maid, pos);
        trace(maid, "left " + units + "x " + step.need().resultItem() + " working at the " + st.name() + " (" + step.need().cookTicks() + " ticks a unit)");
        s.phase = Phase.RETURN;
        s.walkingTo = null;
    }

    /** Is something she left at a slow station ready? Then go and get it before planning anything new. */
    private boolean startPickup(ServerLevel level, EntityMaid maid, State s, ChefData data) {
        if (data.brewing().isEmpty()) return false;
        List<String> keep = new java.util.ArrayList<>();
        Pickup ready = null;
        for (String e : data.brewing()) {
            String[] f = e.split("\\|");
            if (f.length < 4) continue;
            String[] xyz = f[0].split(",");
            BlockPos pos;
            try {
                pos = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
            } catch (RuntimeException ex) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                keep.add(e);
                continue;
            }
            Station st = null;
            var state = level.getBlockState(pos);
            var be = level.getBlockEntity(pos);
            for (Station c : net.eileenthg.maidchef.station.Stations.all()) {
                if (c.matches(level, pos, state, be)) {
                    st = c;
                    break;
                }
            }
            net.eileenthg.maidchef.station.RecipeNeed need = null;
            if (st != null) {
                for (var n : net.eileenthg.maidchef.station.Stations.recipes(level, st)) {
                    if (n.id().toString().equals(f[1])) need = n;
                }
            }
            if (need == null) {
                // the block changed into something else (a pot of soup is what the pot became): find the recipe's station
                for (Station c : net.eileenthg.maidchef.station.Stations.all()) {
                    for (var n : net.eileenthg.maidchef.station.Stations.recipes(level, c)) {
                        if (n.id().toString().equals(f[1])) {
                            st = c;
                            need = n;
                        }
                    }
                }
            }
            if (st == null || need == null) {
                trace(maid, "forgetting " + e + ": the station or recipe is gone");
                continue;
            }
            keep.add(e);
            if (ready == null && st.finished(level, pos)) {
                int units = 1;
                try {
                    units = Integer.parseInt(f[3]);
                } catch (NumberFormatException ignored) {
                }
                ready = new Pickup(e, pos, st, need, units);
            }
        }
        if (!keep.equals(data.brewing())) maid.setAndSyncData(ChefPlugin.CHEF_DATA, data.withBrewing(keep));
        if (ready == null) return false;
        if (!ready.need.container().isEmpty()) {
            ready.bring.put(ready.need.container().getItem(), ready.units * ready.need.resultCount() * Math.max(1, ready.need.container().getCount()));
        }
        Map<Item, Integer> missing = shortfall(maid, ready.bring);
        ready.bring.clear();
        ready.bring.putAll(missing);
        for (var ing : ready.station.attendNeeds(level, ready.pos, ready.need)) {
            if (Inv.findLive(maid, ing::test).isEmpty()) ready.attend.add(ing);
        }
        s.pickup = ready;
        s.job = null;
        s.withdrawn.clear();
        s.collected.clear();
        s.resultBefore = Inv.count(Inv.backpack(maid), ready.need.resultItem());
        s.walkingTo = null;
        s.phase = Phase.PICKUP;
        setStatus(maid, "Collecting " + ready.need.result().getHoverName().getString() + " from the " + ready.station.name() + ".");
        trace(maid, "pickup " + ready.entry + " bring " + ready.bring);
        return true;
    }

    private void pickup(ServerLevel level, EntityMaid maid, State s, long time) {
        Pickup p = s.pickup;
        if (p == null) {
            s.phase = Phase.PLAN;
            return;
        }
        Item dish = p.need.resultItem();
        // 1. containers to pour or serve into, and whatever the station's next stage needs
        if (!p.attend.isEmpty()) {
            var ing = p.attend.get(0);
            BlockPos chest = null;
            Item found = null;
            for (BlockPos c : s.kitchen.ingredientChests()) {
                IItemHandler h = Planner.handler(level, c);
                if (h == null) continue;
                for (int i = 0; i < h.getSlots() && found == null; i++) {
                    if (ing.test(h.getStackInSlot(i))) found = h.getStackInSlot(i).getItem();
                }
                if (found != null) {
                    chest = c;
                    break;
                }
            }
            if (chest == null) {
                trace(maid, "pickup: nothing in the chests for " + java.util.Arrays.toString(ing.getItems()));
                p.attend.remove(0);
                return;
            }
            if (!approach(maid, s, chest, time)) return;
            int moved = Inv.move(Planner.handler(level, chest), Inv.backpack(maid), found, 1, false);
            if (moved > 0) s.withdrawn.merge(found, moved, Integer::sum);
            maid.swing(maid.getUsedItemHand());
            p.attend.remove(0);
            s.walkingTo = null;
            return;
        }
        if (!p.bring.isEmpty()) {
            BlockPos chest = nextChestWith(level, s.kitchen.ingredientChests(), p.bring);
            if (chest != null) {
                if (!approach(maid, s, chest, time)) return;
                IItemHandler h = Planner.handler(level, chest);
                if (h != null) {
                    for (Map.Entry<Item, Integer> e : p.bring.entrySet()) {
                        int moved = Inv.move(h, Inv.backpack(maid), e.getKey(), e.getValue(), false);
                        if (moved > 0) s.withdrawn.merge(e.getKey(), moved, Integer::sum);
                    }
                }
                maid.swing(maid.getUsedItemHand());
            }
            p.bring.clear();
            s.walkingTo = null;
            return;
        }
        // 2. take it out of the station
        if (!p.delivered) {
            if (!approach(maid, s, p.pos, time)) return;
            maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(p.pos));
            if (time % 10 != 0) return;
            p.got += p.station.collect(level, maid, p.pos, p.need, s.collected);
            if (!p.station.finished(level, p.pos)) {
                // she tended it and it's working again (a vat's next stage): leave it on the list and come back later
                p.keep = true;
            } else {
                p.got += p.station.collect(level, maid, p.pos, p.need, s.collected);
            }
            if (!p.keep || !p.station.finished(level, p.pos)) {
                ChefData data = TaskChef.data(maid);
                List<String> list = new java.util.ArrayList<>(data.brewing());
                if (!p.keep) list.remove(p.entry);
                maid.setAndSyncData(ChefPlugin.CHEF_DATA, data.withBrewing(list));
            }
            p.delivered = true;
            s.walkingTo = null;
            trace(maid, "picked up " + p.got + " " + dish + " from the " + p.station.name() + " bag " + bagSummary(maid));
            return;
        }
        // 3. the dish to the output chest, everything else back to the ingredient chest
        int made = Math.max(0, Inv.count(Inv.backpack(maid), dish) - s.resultBefore);
        List<BlockPos> outs = s.kitchen.outputChests();
        if (made > 0 && !outs.isEmpty()) {
            if (!approach(maid, s, outs.get(0), time)) return;
            putAway(level, maid, outs, Map.of(dish, made));
            s.walkingTo = null;
            s.resultBefore = Inv.count(Inv.backpack(maid), dish);
            setStatus(maid, "Collected " + made + "x " + p.need.result().getHoverName().getString() + " from the " + p.station.name() + ".");
            return;
        }
        Map<Item, Integer> back = new LinkedHashMap<>(s.withdrawn);
        s.collected.forEach((item, n) -> {
            if (item != dish) back.merge(item, n, Integer::sum);
        });
        List<BlockPos> ins = s.kitchen.ingredientChests();
        if (!back.isEmpty() && !ins.isEmpty()) {
            if (!approach(maid, s, ins.get(0), time)) return;
            putAway(level, maid, ins, back);
        }
        s.pickup = null;
        s.withdrawn.clear();
        s.collected.clear();
        s.walkingTo = null;
        s.phase = Phase.PLAN;
    }


    /** Fill a pot, over several trips if her backpack can't hold everything at once. */
    private void stageStep(ServerLevel level, EntityMaid maid, State s, Station st, Planner.Step step, BlockPos pos, boolean last, long time) {
        if (!s.staging) {
            if (!st.idle(level, pos)) {
                if (s.waitStart == 0) s.waitStart = time;
                if (time - s.waitStart > STATION_BUSY_TIMEOUT) {
                    trace(maid, "station stayed busy; giving up the rest (" + s.remaining + " units)");
                    s.remaining = 0;
                    if (!last) s.chainBroken = true;
                }
                return;
            }
            s.waitStart = 0;
            s.staging = true;
            s.stageUnits = Math.min(s.remaining, st.loadCap(step.need()));
            s.stageTrips = 0;
            s.stageStalls = 0;
        }
        Map<Item, Integer> before = st.stillToStage(level, pos, step.need(), step.alloc(), s.stageUnits);
        boolean done = st.stage(level, maid, pos, step.need(), step.alloc(), s.stageUnits);
        maid.swing(maid.getUsedItemHand());
        if (done) {
            trace(maid, "pot filled for " + s.stageUnits + " units after " + s.stageTrips + " extra trips; cooking");
            s.staging = false;
            s.remaining -= s.stageUnits;
            s.loadActive = true;
            s.loadStart = time;
            s.loadUnits = s.stageUnits;
            return;
        }
        Map<Item, Integer> missing = st.stillToStage(level, pos, step.need(), step.alloc(), s.stageUnits);
        Map<Item, Integer> toFetch = shortfall(maid, missing);
        boolean progress = !missing.equals(before);
        s.stageStalls = progress ? 0 : s.stageStalls + 1;
        boolean chestsHaveIt = toFetch.isEmpty() || nextChestWith(level, s.kitchen.ingredientChests(), toFetch) != null;
        if (!chestsHaveIt || s.stageStalls > 3 || s.stageTrips > 24) {
            trace(maid, "can't finish filling the " + st.name() + " (missing " + missing + "); taking it back out");
            st.unstage(level, maid, pos);
            s.staging = false;
            s.remaining = 0;
            if (!last) s.chainBroken = true;
            return;
        }
        if (toFetch.isEmpty()) return; // items held back to avoid a wrong recipe; try again next tick
        s.stageTrips++;
        s.stillNeeded = toFetch;
        trace(maid, "pot needs " + missing + "; trip " + s.stageTrips + " for " + toFetch);
        s.phase = Phase.FETCH;
        s.walkingTo = null;
    }

    // ------------------------------------------------------------------ deliver / return

    private void deliver(ServerLevel level, EntityMaid maid, State s, long time) {
        Item result = s.job.dish();
        int made = Math.max(0, Inv.count(Inv.backpack(maid), result) - s.resultBefore);
        s.produced = Math.max(s.produced, made);
        Map<Item, Integer> dishes = new LinkedHashMap<>();
        if (s.produced > 0) dishes.put(result, s.produced);
        List<BlockPos> outs = s.kitchen.outputChests();
        if (dishes.isEmpty() || outs.isEmpty()) {
            s.phase = Phase.RETURN;
            s.walkingTo = null;
            return;
        }
        BlockPos target = outs.get(0);
        if (!approach(maid, s, target, time)) return;
        trace(maid, "deliver " + dishes + " (produced " + s.produced + ", made delta " + made + ") bag " + bagSummary(maid));
        putAway(level, maid, outs, dishes);
        s.phase = Phase.RETURN;
        s.walkingTo = null;
    }

    private void giveBack(ServerLevel level, EntityMaid maid, State s, long time) {
        // everything except the dish goes back: unused ingredients, spare intermediates, containers, leftovers
        Map<Item, Integer> back = new LinkedHashMap<>(s.withdrawn);
        s.collected.forEach((item, n) -> {
            if (item != s.job.dish()) back.merge(item, n, Integer::sum);
        });
        List<BlockPos> ins = s.kitchen.ingredientChests();
        if (!back.isEmpty() && !ins.isEmpty()) {
            if (!approach(maid, s, ins.get(0), time)) return;
            trace(maid, "return " + back + " bag " + bagSummary(maid));
            putAway(level, maid, ins, back);
        }
        finishJob(maid, s, time);
    }

    /** Move up to the given amounts from her backpack into the first chests with room. */
    private static void putAway(ServerLevel level, EntityMaid maid, List<BlockPos> chests, Map<Item, Integer> amounts) {
        IItemHandler bag = Inv.backpack(maid);
        for (Map.Entry<Item, Integer> e : amounts.entrySet()) {
            int left = Math.min(e.getValue(), Inv.count(bag, e.getKey()));
            for (BlockPos p : chests) {
                if (left <= 0) break;
                IItemHandler h = Planner.handler(level, p);
                if (h != null) left -= Inv.move(bag, h, e.getKey(), left, true);
            }
        }
        maid.swing(maid.getUsedItemHand());
    }

    private void finishJob(EntityMaid maid, State s, long time) {
        Planner.Job job = s.job;
        ChefData data = TaskChef.data(maid);
        ChefClaims.deactivate(maid, step(s).at().pos());
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(job.dish()).toString();
        if (s.leftBrewing) {
            s.leftBrewing = false;
            data = data.withRecent(id);
            data = data.withStatus("Started " + job.amount() + "x " + job.dishName() + " at the " + job.last().at().station().name()
                    + "; I'll come back for it when it's ready.");
        } else if (s.aborted) {
            data = data.withRecent(id);
            data = data.withStatus("Gave up on " + job.dishName() + ": " + s.owedFrom + " never gave back what they took. Made " + s.produced + ".");
        } else if (s.produced > 0) {
            data = data.withRecent(id);
            String where = s.kitchen.outputChests().isEmpty() ? " (kept in my backpack: no output chest)" : "";
            data = data.withStatus("Made " + s.produced + "x " + job.dishName() + where + ".");
        } else {
            // a failed attempt at a recent dish (the fallback) removes it from the list
            if (job.fallback()) data = data.withoutRecent(id);
            data = data.withStatus("Couldn't make " + job.dishName() + "; trying something else.");
        }
        maid.setAndSyncData(ChefPlugin.CHEF_DATA, data);
        s.job = null;
        s.phase = Phase.PLAN;
        s.walkingTo = null;
        s.nextPlan = time + 40;
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    // ------------------------------------------------------------------ taken items

    /** A player picked up something that popped out of her station. */
    static void owed(EntityMaid maid, net.minecraft.world.entity.player.Player player, Item item, int count) {
        State s = state(maid);
        if (s.job == null) return;
        long now = maid.level().getGameTime();
        if (s.owed.isEmpty()) {
            s.owedSince = now;
            s.owedBubbleAt = 0;
        }
        s.owedBaseline.putIfAbsent(item, Inv.count(Inv.backpack(maid), item));
        s.owed.merge(item, count, Integer::sum);
        s.owedFrom = maid.isOwnedBy(player) ? "Master" : player.getName().getString();
    }

    private void waitForReturn(ServerLevel level, EntityMaid maid, State s, long time) {
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (time % 10 != 0) return;
        Planner.Step step = step(s);
        Item result = step.need().resultItem();
        IItemHandler bag = Inv.backpack(maid);
        // the station keeps cooking meanwhile: collect that output first and don't count it as given back
        if (s.loadActive) {
            Map<Item, Integer> before = new LinkedHashMap<>();
            for (Item item : s.owed.keySet()) before.put(item, Inv.count(bag, item));
            Station st = step.at().station();
            int got = st.collect(level, maid, step.at().pos(), step.need(), s.collected);
            if (finalStep(s)) s.produced += got;
            if (st.finished(level, step.at().pos())) s.loadActive = false;
            before.forEach((item, n) -> s.owedBaseline.merge(item, Inv.count(bag, item) - n, Integer::sum));
        }
        // dropped at her feet, or put straight into her backpack
        Inv.vacuum(level, maid, maid.blockPosition(), s.owed::containsKey, result, s.collected);
        s.owed.entrySet().removeIf(e -> Inv.count(bag, e.getKey()) - s.owedBaseline.getOrDefault(e.getKey(), 0) >= e.getValue());
        if (s.owed.isEmpty()) {
            s.owedBaseline.clear();
            s.lastBubble = "";
            bubble(maid, s, "Thank you, " + s.owedFrom + "!");
            setStatus(maid, "Got it back. Carrying on with " + s.job.dishName() + ".");
            return;
        }
        String what = describeOwed(s);
        if (time - s.owedBubbleAt >= 400) {
            s.owedBubbleAt = time;
            s.lastBubble = "";
            bubble(maid, s, s.owedFrom + ", please give back " + what + " (´；ω；`)");
            setStatus(maid, "Paused: " + s.owedFrom + " picked up " + what + ". Drop it near me or put it in my backpack.");
        }
        if (time - s.owedSince > ChefConfig.RETURN_WAIT_SECONDS.get() * 20L) {
            s.lastBubble = "";
            bubble(maid, s, "Fine... I'll cook something else.");
            s.owed.clear();
            s.owedBaseline.clear();
            s.aborted = true;
            s.loadActive = false;
            s.remaining = 0;
            s.stillNeeded.clear();
            s.chainBroken = true;
            ChefClaims.deactivate(maid, step.at().pos());
            s.phase = Phase.DELIVER;
            s.walkingTo = null;
        }
    }

    private static String describeOwed(State s) {
        StringBuilder b = new StringBuilder();
        s.owed.forEach((item, n) -> {
            if (b.length() > 0) b.append(", ");
            b.append(n).append(' ').append(item.getDefaultInstance().getHoverName().getString());
        });
        return b.toString();
    }

    // ------------------------------------------------------------------ helpers

    /** Walk towards pos; true once she is close enough to use it (or has been stuck long enough to use it anyway). */
    private static boolean approach(EntityMaid maid, State s, BlockPos pos, long time) {
        double d = Math.sqrt(maid.distanceToSqr(Vec3.atCenterOf(pos)));
        if (d <= REACH || (pos.equals(s.arrivedAt) && s.walkingTo == null)) {
            if (d <= REACH) maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            s.walkingTo = null;
            if (!pos.equals(s.arrivedAt)) trace(maid, "arrived at " + pos.toShortString() + String.format(" (%.1f blocks)", d));
            s.arrivedAt = pos;
            return true;
        }
        if (!pos.equals(s.walkingTo)) {
            s.walkingTo = pos;
            s.walkStart = time;
            s.bestDist = d;
            s.bestDistAt = time;
        }
        if (d < s.bestDist - 0.5) {
            s.bestDist = d;
            s.bestDistAt = time;
        }
        if (time - s.bestDistAt > STUCK_TICKS) {
            // can't get closer (fenced counter, odd pathing): work from where she stands
            s.walkingTo = null;
            s.arrivedAt = pos;
            trace(maid, "stuck; working from " + String.format("%.1f", d) + " blocks away from " + pos.toShortString());
            return true;
        }
        maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, 0.5f, 1));
        maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
        return false;
    }

    /** Step-by-step log while the self-test runs (or with debugLog on). */
    public static void trace(EntityMaid maid, String msg) {
        if (MaidChef.selfTestActive || ChefConfig.DEBUG_LOG.get()) {
            com.mojang.logging.LogUtils.getLogger().info("[maidchef] maid {} @{}: {}", maid.getId(), maid.level().getGameTime(), msg);
        }
    }

    private static String bagSummary(EntityMaid maid) {
        Map<Item, Integer> m = new LinkedHashMap<>();
        Inv.addCounts(maid.getAvailableInv(true), m, false);
        StringBuilder b = new StringBuilder("[");
        m.forEach((k, v) -> b.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(k).getPath()).append('x').append(v).append(' '));
        return b.append(']').toString();
    }

    private static void setStatus(EntityMaid maid, String status) {
        ChefData data = TaskChef.data(maid);
        if (!status.equals(data.status())) {
            maid.setAndSyncData(ChefPlugin.CHEF_DATA, data.withStatus(status));
        }
    }

    private static void bubble(EntityMaid maid, State s, String text) {
        if (!ChefConfig.ANNOUNCE.get() || text.equals(s.lastBubble)) return;
        s.lastBubble = text;
        maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type2(Component.literal(text)));
    }

    /** Debug/self-test view of the current state. */
    public static String describe(EntityMaid maid) {
        State s = state(maid);
        StringBuilder b = new StringBuilder(s.phase.name());
        if (s.job != null) {
            Planner.Step st = step(s);
            b.append(" dish=").append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.job.dish()))
                    .append(" step ").append(s.stepIndex + 1).append('/').append(s.job.steps().size()).append(' ').append(st.need().id())
                    .append(" at ").append(st.at().station().name()).append(' ').append(st.at().pos().toShortString())
                    .append(" units=").append(st.units()).append(" remaining=").append(s.remaining).append(" produced=").append(s.produced);
        }
        return b.toString();
    }
}
