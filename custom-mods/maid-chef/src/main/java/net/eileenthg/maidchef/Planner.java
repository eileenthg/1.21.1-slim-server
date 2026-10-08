package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.eileenthg.maidchef.station.Alloc;
import net.eileenthg.maidchef.station.Inv;
import net.eileenthg.maidchef.station.RecipeNeed;
import net.eileenthg.maidchef.station.Station;
import net.eileenthg.maidchef.station.Stations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Looks at the kitchen and decides what to cook next, including multi-step chains: to make a dish she may first make
 * its ingredients (and theirs), up to chainDepth steps deep, and she may serve feasts. Nothing is moved here.
 */
public final class Planner {
    public static final int VERTICAL = 3;

    public record Found(Station station, BlockPos pos) {
    }

    public record Kitchen(BlockPos center, List<Found> stations, List<BlockPos> ingredientChests, List<BlockPos> outputChests,
                          boolean hasHub) {
    }

    /**
     * One cooking step.
     *
     * @param units     how many crafts
     * @param fuel      fuel to put in (furnace/smoker), or null
     * @param fuelCount how much of it
     * @param tool      tool to fetch from the chest (cutting board) when she doesn't carry one, or null
     * @param fetch     raw items to take from the ingredient chests right before this step (intermediates from earlier
     *                  steps are already in her backpack; a crafting table takes its own straight from the chests)
     */
    public record Step(Found at, RecipeNeed need, Alloc alloc, int units, @Nullable Item fuel, int fuelCount, @Nullable Item tool,
                       Map<Item, Integer> fetch) {
        Step withFetch(Map<Item, Integer> f) {
            return new Step(at, need, alloc, units, fuel, fuelCount, tool, Map.copyOf(f));
        }

        public int output() {
            return units * need.resultCount();
        }

        public String label() {
            return output() + "x " + need.result().getHoverName().getString() + " (" + at.station().name() + ")";
        }
    }

    /**
     * A whole plan: steps in order (the last one makes the dish), and what she has to take out of storage for it.
     *
     * @param fetch    items taken from the ingredient chests/her backpack (raw ingredients, containers, fuel, tools)
     * @param fallback true when this is a recently made dish tried because nothing else was possible
     */
    public record Job(List<Step> steps, Item dish, int amount, double value, Map<Item, Integer> fetch, boolean fallback) {
        public Step last() {
            return steps.get(steps.size() - 1);
        }

        public String dishName() {
            return dish.getDefaultInstance().getHoverName().getString();
        }

        public String describe() {
            return steps.stream().map(Step::label).collect(Collectors.joining(" -> "));
        }
    }

    /**
     * @param job            what to cook, or null
     * @param dropFromRecent recent dishes whose fallback attempt failed (can't be made now); remove them from the list
     */
    public record Choice(@Nullable Job job, List<String> dropFromRecent, @Nullable String why) {
        public Choice(@Nullable Job job, List<String> dropFromRecent) {
            this(job, dropFromRecent, null);
        }
    }

    private Planner() {
    }

    // ------------------------------------------------------------------ kitchen

    public static BlockPos center(EntityMaid maid) {
        return maid.isHomeModeEnable() && maid.hasRestriction() ? maid.getRestrictCenter() : maid.blockPosition();
    }

    public static Kitchen scan(ServerLevel level, EntityMaid maid, int radius) {
        BlockPos c = center(maid);
        List<Station> kinds = Stations.all().stream().filter(Stations::enabled).toList();
        List<Found> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-radius, -VERTICAL, -radius), c.offset(radius, VERTICAL, radius))) {
            BlockState state = level.getBlockState(p);
            if (state.isAir()) continue;
            BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(p) : null;
            for (Station s : kinds) {
                if (s.matches(level, p, state, be)) {
                    found.add(new Found(s, p.immutable()));
                    break;
                }
            }
        }
        found.sort(Comparator.comparingDouble(f -> f.pos().distSqr(maid.blockPosition())));
        HubChests.Bound hub = HubChests.of(maid);
        return new Kitchen(c, found, withinReach(hub.ingredient(), c, radius), withinReach(hub.output(), c, radius), hub.hasHub());
    }

    private static List<BlockPos> withinReach(List<BlockPos> chests, BlockPos c, int radius) {
        int r = radius + 4;
        return chests.stream().filter(p -> Math.abs(p.getX() - c.getX()) <= r && Math.abs(p.getZ() - c.getZ()) <= r
                && Math.abs(p.getY() - c.getY()) <= r).toList();
    }

    @Nullable
    public static IItemHandler handler(ServerLevel level, BlockPos pos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    /** Everything she can cook with: the ingredient chests plus her own backpack. */
    public static Map<Item, Integer> pool(ServerLevel level, EntityMaid maid, Kitchen kitchen) {
        Map<Item, Integer> pool = new HashMap<>();
        for (BlockPos p : kitchen.ingredientChests()) {
            IItemHandler h = handler(level, p);
            if (h != null) Inv.addCounts(h, pool, false);
        }
        Inv.addCounts(Inv.backpack(maid), pool, true);
        return pool;
    }

    /** Does this dish pass the global exclusions and the maid's own filters (blacklist, minimum nutrition/saturation/effects)? */
    public static boolean allowed(Item result, ChefData data) {
        if (FoodValue.matchesAny(result, ChefConfig.EXCLUDED_RESULTS.get())) return false;
        if (FoodValue.matchesAny(result, data.blacklist())) return false;
        if (data.minNutrition() <= 0 && data.minSaturation() <= 0 && data.minEffects() <= 0) return true;
        FoodValue.Stats st = FoodValue.stats(result);
        return st != null && st.nutrition() >= data.minNutrition() && st.saturation() + 1e-4 >= data.minSaturation()
                && st.effects() >= data.minEffects();
    }

    // ------------------------------------------------------------------ planning context

    /** A way to make an item: a recipe at one of the kitchen's ready stations (or serving a feast). */
    private record Producer(Found at, RecipeNeed need) {
    }

    /** Stock during one planning attempt: copied before trying something, committed if it works. */
    private static final class Sim {
        final Map<Item, Integer> stock;
        final Map<BlockPos, Integer> fuelTicks;
        /** Items she doesn't have but imagined having (only when looking for something to ask for). */
        final Map<Item, Integer> wishes;

        Sim(Map<Item, Integer> stock, Map<BlockPos, Integer> fuelTicks) {
            this(stock, fuelTicks, new LinkedHashMap<>());
        }

        Sim(Map<Item, Integer> stock, Map<BlockPos, Integer> fuelTicks, Map<Item, Integer> wishes) {
            this.stock = stock;
            this.fuelTicks = fuelTicks;
            this.wishes = wishes;
        }

        Sim copy() {
            return new Sim(new HashMap<>(stock), new HashMap<>(fuelTicks), new LinkedHashMap<>(wishes));
        }

        void commit(Sim other) {
            stock.clear();
            stock.putAll(other.stock);
            fuelTicks.clear();
            fuelTicks.putAll(other.fuelTicks);
            wishes.clear();
            wishes.putAll(other.wishes);
        }

        /** Imagine having n of item (allowed while the wish budget lasts). */
        boolean wish(Ctx ctx, Item item, int n) {
            if (!ctx.wishing() || ctx.blocked.contains(item)) return false;
            if (!wishes.containsKey(item) && wishes.size() >= ctx.wishBudget) return false;
            wishes.merge(item, n, Integer::sum);
            return true;
        }

        int get(Item i) {
            return stock.getOrDefault(i, 0);
        }

        void add(Item i, int n) {
            stock.merge(i, n, Integer::sum);
        }

        /** Use up whatever is in stock of i (the rest was wished for). Always true, for use in conditions. */
        boolean add0(Item i) {
            stock.put(i, Math.min(0, get(i)));
            return true;
        }
    }

    private static final class Ctx {
        final ServerLevel level;
        final EntityMaid maid;
        final ChefData data;
        final Kitchen kitchen;
        final Map<Item, Integer> pool;
        final int maxDepth;
        final Map<Item, List<Producer>> producers = new HashMap<>();
        final Map<Ingredient, List<Item>> matches = new IdentityHashMap<>();
        final Map<Item, byte[]> canMemo = new HashMap<>();
        final Map<Item, byte[]> canMemoWish = new HashMap<>();
        /** While wishing: temporarily plan as if she couldn't ask for anything. */
        boolean wishSuspended;

        boolean wishing() {
            return wishMode && !wishSuspended;
        }
        final Set<Item> feastItems = new HashSet<>();
        String why;
        /** "Prepare X" mode: the owner chose the dish, so the "worth it" check doesn't apply. */
        boolean targetMode;
        // looking for something to ask the owner for
        boolean wishMode;
        int wishBudget;
        Set<Item> blocked = Set.of();
        Map<Item, Integer> lastWishes = Map.of();

        Ctx(ServerLevel level, EntityMaid maid, Kitchen kitchen, Map<Item, Integer> pool, ChefData data) {
            this.level = level;
            this.maid = maid;
            this.data = data;
            this.kitchen = kitchen;
            this.pool = pool;
            this.maxDepth = ChefConfig.CHAIN_DEPTH.get();
        }
    }

    private static Ctx context(ServerLevel level, EntityMaid maid, Kitchen kitchen, Map<Item, Integer> pool, ChefData data) {
        FoodValue.update(level);
        Ctx ctx = new Ctx(level, maid, kitchen, pool, data);
        List<? extends String> excluded = ChefConfig.EXCLUDED_RECIPES.get();
        Map<Station, Found> idle = new LinkedHashMap<>();
        for (Found f : kitchen.stations()) {
            if (!idle.containsKey(f.station()) && f.station().idle(level, f.pos())) idle.put(f.station(), f);
        }
        idle.forEach((s, f) -> {
            for (RecipeNeed need : Stations.recipes(level, s)) {
                if (excluded.contains(need.id().toString()) || !s.accepts(level, f.pos(), need)) continue;
                ctx.producers.computeIfAbsent(need.resultItem(), k -> new ArrayList<>()).add(new Producer(f, need));
            }
        });
        // "Prepare X" for a potion: only recipes that make that potion
        Item wanted = data.targetItem();
        String variant = data.targetVariant();
        if (wanted != null && !variant.isEmpty() && ctx.producers.containsKey(wanted)) {
            List<Producer> keep = new ArrayList<>();
            for (Producer p : ctx.producers.get(wanted)) if (Variant.matches(variant, p.need().result())) keep.add(p);
            if (keep.isEmpty()) ctx.producers.remove(wanted);
            else ctx.producers.put(wanted, keep);
        }
        List<RecipeNeed> serving = Stations.recipes(level, FeastServeStation.INSTANCE);
        for (RecipeNeed need : serving) ctx.feastItems.add(need.inputs().get(0).getItems()[0].getItem());
        if (data.serveFeasts()) {
            Found at = new Found(FeastServeStation.INSTANCE, kitchen.center());
            for (RecipeNeed need : serving) {
                ctx.producers.computeIfAbsent(need.resultItem(), k -> new ArrayList<>()).add(new Producer(at, need));
            }
        }
        return ctx;
    }

    /** Items that could fill this ingredient: ones she has, or ones a ready station could make. */
    private static List<Item> candidates(Ctx ctx, Ingredient ing) {
        return ctx.matches.computeIfAbsent(ing, i -> {
            Set<Item> out = new LinkedHashSet<>();
            for (ItemStack s : i.getItems()) {
                Item it = s.getItem();
                if (ctx.pool.containsKey(it) || ctx.producers.containsKey(it)) out.add(it);
            }
            for (Item it : ctx.pool.keySet()) {
                if (!out.contains(it) && i.test(new ItemStack(it))) out.add(it);
            }
            return new ArrayList<>(out);
        });
    }

    private static boolean canMake(Ctx ctx, Item item, int depth) {
        return ctx.pool.getOrDefault(item, 0) > 0 || canProduce(ctx, item, depth);
    }

    /** Could a ready station make this, with ingredients that are in stock or makeable in the remaining depth? (quantities ignored) */
    private static boolean canProduce(Ctx ctx, Item item, int depth) {
        if (depth > ctx.maxDepth) return false;
        byte[] memo = (ctx.wishing() ? ctx.canMemoWish : ctx.canMemo).computeIfAbsent(item, k -> new byte[ctx.maxDepth + 2]);
        if (memo[depth] != 0) return memo[depth] == 1;
        memo[depth] = 2; // guards against cycles while computing
        boolean ok = false;
        for (Producer p : ctx.producers.getOrDefault(item, List.of())) {
            if (inputsPossible(ctx, p, item, depth)) {
                ok = true;
                break;
            }
        }
        memo[depth] = (byte) (ok ? 1 : 2);
        return ok;
    }

    private static boolean inputsPossible(Ctx ctx, Producer p, Item result, int depth) {
        for (Ingredient ing : p.need().inputs()) {
            boolean any = false;
            for (Item m : candidates(ctx, ing)) {
                if (m != result && canMake(ctx, m, depth + 1)) {
                    any = true;
                    break;
                }
            }
            if (!any && !(ctx.wishing() && wishable(ctx, ing) != null)) return false;
        }
        if (!p.need().container().isEmpty() && !canMake(ctx, p.need().container().getItem(), depth + 1)
                && !(ctx.wishing() && !ctx.blocked.contains(p.need().container().getItem()))) return false;
        // a tool she must have but doesn't use up: the cutting board's knife, the oven's baking dish
        if (p.at().station().needsTool() || p.need().tool() != null) {
            if (p.need().tool() == null) return false;
            if (Inv.findLive(ctx.maid, p.need().tool()::test).isEmpty()
                    && ctx.pool.keySet().stream().noneMatch(i -> p.need().tool().test(new ItemStack(i)))
                    && !(ctx.wishing() && wishable(ctx, p.need().tool()) != null)) return false;
        }
        return true;
    }

    /** The item she'd ask for to fill this ingredient (the first one that isn't blocked), or null. */
    @Nullable
    private static Item wishable(Ctx ctx, Ingredient ing) {
        for (Item it : preferred(ing)) {
            if (!ctx.blocked.contains(it)) return it;
        }
        return null;
    }

    /** The items an ingredient accepts, plainest first: vanilla before modded, then shorter names. */
    private static List<Item> preferred(Ingredient ing) {
        List<Item> out = new ArrayList<>();
        for (ItemStack st : ing.getItems()) {
            if (!st.isEmpty() && !out.contains(st.getItem())) out.add(st.getItem());
        }
        out.sort(Comparator.<Item>comparingInt(it -> BuiltInRegistries.ITEM.getKey(it).getNamespace().equals("minecraft") ? 0 : 1)
                .thenComparingInt(it -> name(it).length()));
        return out;
    }

    private static String firstMissing(Ctx ctx, Producer p, Item result, int depth) {
        for (Ingredient ing : p.need().inputs()) {
            if (candidates(ctx, ing).stream().noneMatch(m -> m != result && canMake(ctx, m, depth + 1))) return "missing " + describe(ing);
        }
        if (!p.need().container().isEmpty() && !canMake(ctx, p.need().container().getItem(), depth + 1)) {
            return "missing " + name(p.need().container().getItem());
        }
        if (p.need().tool() != null) return "no tool: needs " + describe(p.need().tool());
        return "missing something for the " + p.at().station().name();
    }

    /** Make q of item X (on top of what's in stock), adding the steps. Changes sim only if it succeeds. */
    private static boolean produce(Ctx ctx, Item x, int q, Sim sim, int depth, Set<Item> visiting, List<Step> steps) {
        if (q <= 0) return true;
        if (depth > ctx.maxDepth || visiting.contains(x)) return false;
        for (Producer p : ctx.producers.getOrDefault(x, List.of())) {
            if (!inputsPossible(ctx, p, x, depth)) {
                if (ctx.why == null) ctx.why = firstMissing(ctx, p, x, depth);
                continue;
            }
            Sim s2 = sim.copy();
            List<Step> st2 = new ArrayList<>();
            Set<Item> vis2 = new HashSet<>(visiting);
            vis2.add(x);
            int u = (q + p.need().resultCount() - 1) / p.need().resultCount();
            Step step = makeStep(ctx, p, x, u, s2, depth, vis2, st2);
            if (step == null) continue;
            s2.add(x, step.output());
            st2.add(step);
            sim.commit(s2);
            steps.addAll(st2);
            return true;
        }
        return false;
    }

    @Nullable
    private static Step makeStep(Ctx ctx, Producer p, Item x, int u, Sim s, int depth, Set<Item> vis, List<Step> steps) {
        RecipeNeed need = p.need();
        List<Item> perInput = new ArrayList<>();
        Map<Item, Integer> perUnit = new LinkedHashMap<>();
        for (Ingredient ing : need.inputs()) {
            Item pick;
            if (ctx.wishing()) {
                boolean was = ctx.wishSuspended;
                ctx.wishSuspended = true;
                pick = obtain(ctx, candidates(ctx, ing), x, u, s, depth, vis, steps);
                ctx.wishSuspended = was;
                if (pick == null) {
                    // simplest request: the ingredient itself
                    Item w = wishable(ctx, ing);
                    if (w != null && s.wish(ctx, w, Math.max(0, u - Math.max(0, s.get(w))))) {
                        s.add(w, -Math.max(0, s.get(w)));
                        pick = w;
                    }
                }
                if (pick == null) pick = obtain(ctx, candidates(ctx, ing), x, u, s, depth, vis, steps);
            } else {
                pick = obtain(ctx, candidates(ctx, ing), x, u, s, depth, vis, steps);
            }
            if (pick == null) {
                ctx.why = "missing " + describe(ing);
                return null;
            }
            perInput.add(pick);
            perUnit.merge(pick, 1, Integer::sum);
        }
        if (!need.container().isEmpty()) {
            Item c = need.container().getItem();
            int per = need.container().getCount() * need.resultCount();
            if (obtain(ctx, List.of(c), x, per * u, s, depth, vis, steps) == null
                    && !(ctx.wishing() && s.wish(ctx, c, Math.max(0, per * u - Math.max(0, s.get(c)))) && s.add0(c))) {
                ctx.why = "missing " + per * u + " " + c.getDefaultInstance().getHoverName().getString();
                return null;
            }
            perUnit.merge(c, per, Integer::sum);
        }
        Item tool = null;
        if (p.at().station().needsTool() || need.tool() != null) {
            if (need.tool() == null) return null;
            if (Inv.findLive(ctx.maid, need.tool()::test).isEmpty()) {
                tool = s.stock.entrySet().stream().filter(e -> e.getValue() > 0 && need.tool().test(new ItemStack(e.getKey())))
                        .map(Map.Entry::getKey).findFirst().orElse(null);
                if (tool == null && ctx.wishing()) {
                    Item w = wishable(ctx, need.tool());
                    if (w != null && s.wish(ctx, w, 1)) tool = w;
                }
                if (tool == null) {
                    ctx.why = "no tool: needs " + describe(need.tool());
                    return null;
                }
            }
        }
        Item fuel = null;
        int fuelCount = 0;
        if (p.at().station().usesFuel()) {
            BlockPos pos = p.at().pos();
            int have = s.fuelTicks.computeIfAbsent(pos, k -> p.at().station().storedFuelTicks(ctx.level, pos));
            int needTicks = u * Math.max(1, need.cookTicks());
            if (have < needTicks) {
                int bestBurn = 0;
                for (Map.Entry<Item, Integer> e : s.stock.entrySet()) {
                    if (e.getValue() <= 0 || perUnit.containsKey(e.getKey()) || !FoodValue.matchesAny(e.getKey(), ChefConfig.FUELS.get())) continue;
                    int burn = new ItemStack(e.getKey()).getBurnTime(null);
                    if (burn > bestBurn) {
                        bestBurn = burn;
                        fuel = e.getKey();
                    }
                }
                if (fuel == null && ctx.wishing()) {
                    // ask for plain coal
                    Item coal = net.minecraft.world.item.Items.COAL;
                    int burn = new ItemStack(coal).getBurnTime(null);
                    int n = (needTicks - have + burn - 1) / burn;
                    if (s.wish(ctx, coal, n)) {
                        s.add(coal, n);
                        fuel = coal;
                        bestBurn = burn;
                    }
                }
                if (fuel == null) {
                    ctx.why = "no fuel for the " + p.at().station().name();
                    return null;
                }
                fuelCount = (needTicks - have + bestBurn - 1) / bestBurn;
                if (s.get(fuel) < fuelCount || fuelCount > fuel.getDefaultMaxStackSize()) {
                    ctx.why = "not enough fuel for the " + p.at().station().name();
                    return null;
                }
                s.add(fuel, -fuelCount);
                have += fuelCount * bestBurn;
            }
            s.fuelTicks.put(pos, have - needTicks);
        }
        return new Step(p.at(), need, new Alloc(List.copyOf(perInput), Map.copyOf(perUnit)), u, fuel, fuelCount, tool, Map.of());
    }

    /** Take n of one of the candidate items from stock, making the shortfall if needed. Returns the item used. */
    @Nullable
    private static Item obtain(Ctx ctx, List<Item> cands, Item making, int n, Sim s, int depth, Set<Item> vis, List<Step> steps) {
        List<Item> sorted = new ArrayList<>(cands);
        sorted.remove(making);
        sorted.sort(Comparator.comparingInt(s::get).reversed());
        for (Item m : sorted) {
            if (s.get(m) >= n) {
                s.add(m, -n);
                return m;
            }
        }
        for (Item m : sorted) {
            if (!canMake(ctx, m, depth + 1)) continue;
            int have = Math.max(0, s.get(m));
            if (produce(ctx, m, n - have, s, depth + 1, vis, steps)) {
                s.add(m, -n);
                return m;
            }
        }
        return null;
    }

    /** A complete plan for q of the dish, or null (ctx.why says why). */
    @Nullable
    private static Job attempt(Ctx ctx, Item dish, int q, double value, boolean fallback) {
        Sim s = new Sim(new HashMap<>(ctx.pool), new HashMap<>());
        List<Step> steps = new ArrayList<>();
        ctx.why = null;
        if (!produce(ctx, dish, q, s, 0, Set.of(), steps) || steps.isEmpty()) {
            if (ctx.why == null) ctx.why = "can't be made at a ready station";
            return null;
        }
        Map<Item, Integer> fetch = new LinkedHashMap<>();
        double usedValue = 0;
        for (Map.Entry<Item, Integer> e : ctx.pool.entrySet()) {
            int used = e.getValue() - s.get(e.getKey());
            if (used > 0) {
                fetch.put(e.getKey(), used);
                // serving a feast only turns it into edible portions, so the feast itself isn't food "used up"
                boolean feast = ctx.data.serveFeasts() && ctx.feastItems.contains(e.getKey());
                if (!feast) usedValue += FoodValue.of(e.getKey()) * used;
            }
        }
        for (Step st : steps) {
            if (st.tool() != null) fetch.putIfAbsent(st.tool(), 1);
        }
        for (Map.Entry<Item, Integer> w : s.wishes.entrySet()) usedValue += FoodValue.of(w.getKey()) * w.getValue();
        ctx.lastWishes = Map.copyOf(s.wishes);
        steps = perStepFetch(steps);
        int made = steps.get(steps.size() - 1).output();
        if (!ctx.targetMode && value * made <= usedValue + 1e-6) {
            ctx.why = String.format("not worth it: uses up food worth %.0f to make %.0f (%s)", usedValue, value * made, fetch);
            return null;
        }
        if (!ctx.wishMode && !fits(ctx.maid, steps)) {
            ctx.why = "doesn't fit in her backpack (give her a bigger one)";
            return null;
        }
        return new Job(List.copyOf(steps), dish, made, value, fetch, fallback);
    }

    /** The most of the dish (up to her batch size) she can plan for, or null. */
    @Nullable
    private static Job best(Ctx ctx, Item dish, double value, boolean fallback) {
        int target = Math.max(1, ctx.data.target());
        Job job = attempt(ctx, dish, target, value, fallback);
        if (job != null || target == 1) return job;
        String why = ctx.why;
        Job one = attempt(ctx, dish, 1, value, fallback);
        if (one == null) {
            ctx.why = why;
            return null;
        }
        int lo = 1, hi = target - 1;
        Job bestJob = one;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            Job j = attempt(ctx, dish, mid, value, fallback);
            if (j != null) {
                lo = mid;
                bestJob = j;
            } else {
                hi = mid - 1;
            }
        }
        return bestJob;
    }

    /** Food she could aim for, most valuable first. With "serve feasts" on, a feast is only a step (its servings are the dish). */
    private static List<Item> dishes(Ctx ctx) {
        List<Item> out = new ArrayList<>();
        for (Item it : ctx.producers.keySet()) {
            if (FoodValue.of(it) <= 0) continue;
            if (ctx.data.serveFeasts() && ctx.feastItems.contains(it)) continue;
            out.add(it);
        }
        out.sort(Comparator.comparingDouble(FoodValue::of).reversed());
        return out;
    }

    // ------------------------------------------------------------------ choosing

    /**
     * Best dish she isn't tired of. If only recently made dishes are possible, the oldest one she can still make
     * (recent dishes she can't make any more are reported in {@link Choice#dropFromRecent()}).
     */
    public static Choice choose(ServerLevel level, EntityMaid maid, Kitchen kitchen, Map<Item, Integer> pool, ChefData data) {
        if (kitchen.stations().isEmpty() || pool.isEmpty()) return new Choice(null, List.of());
        Ctx ctx = context(level, maid, kitchen, pool, data);
        Item wanted = data.targetItem();
        if (wanted != null) {
            // "Prepare X": only X, whatever its value, ignoring rotation and the no-cook list
            ctx.targetMode = true;
            if (!ctx.producers.containsKey(wanted)) {
                // is there a station that could make it, just not right now?
                for (Found f : kitchen.stations()) {
                    for (RecipeNeed need : Stations.recipes(level, f.station())) {
                        if (need.resultItem() == wanted && Variant.matches(data.targetVariant(), need.result())) {
                            return new Choice(null, List.of(), "the " + f.station().name() + " that makes it is busy (or unlit, unheated or covered) right now;"
                                    + " she'll make it once it's free");
                        }
                    }
                }
                return new Choice(null, List.of(), "no station in this kitchen can make it");
            }
            Job job = best(ctx, wanted, FoodValue.of(wanted), false);
            return new Choice(job, List.of(), job == null ? ctx.why : null);
        }
        Set<Item> recent = new LinkedHashSet<>();
        for (String id : data.recent()) BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id)).ifPresent(recent::add);

        List<Job> ties = new ArrayList<>();
        double best = -1;
        for (Item dish : dishes(ctx)) {
            double v = FoodValue.of(dish);
            if (best >= 0 && v < best - 1e-6) break;
            if (recent.contains(dish) || !allowed(dish, data) || !canProduce(ctx, dish, 0)) continue;
            Job job = best(ctx, dish, v, false);
            if (job != null) {
                best = v;
                ties.add(job);
            }
        }
        if (!ties.isEmpty()) return new Choice(ties.get(maid.getRandom().nextInt(ties.size())), List.of());

        // only recent dishes are possible: oldest first
        List<String> drop = new ArrayList<>();
        List<String> order = new ArrayList<>(data.recent());
        Collections.reverse(order);
        for (String id : order) {
            Item dish = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id)).orElse(null);
            Job job = dish != null && allowed(dish, data) && canProduce(ctx, dish, 0) ? best(ctx, dish, FoodValue.of(dish), true) : null;
            if (job != null) return new Choice(job, drop);
            drop.add(id);
        }
        return new Choice(null, List.of());
    }

    private static String describe(Ingredient ing) {
        List<Item> items = preferred(ing);
        if (items.isEmpty()) return "(empty ingredient)";
        String first = name(items.get(0));
        return items.size() == 1 ? first : first + " (or " + (items.size() - 1) + " alternatives)";
    }

    private static String name(Item item) {
        return item.getDefaultInstance().getHoverName().getString();
    }

    // ------------------------------------------------------------------ requests

    /**
     * Something worth asking the owner for: a dish worth at least (1 + minGain) x what she can make now that needs at most
     * maxMissing kinds of items she doesn't have (and that aren't blocked).
     */
    public record Request(Item dish, int amount, double value, double currentBest, Map<Item, Integer> items, String chain) {
        public String dishName() {
            return name(dish);
        }

        public String itemsText() {
            return items.entrySet().stream().map(e -> e.getValue() + " " + name(e.getKey())).collect(Collectors.joining(", "));
        }
    }

    /**
     * What she won't ask for: the owner's blocked items, the neverRequest config, rare/epic items, and prepared food
     * (she'd make that herself from its ingredients). Looked up lazily, item by item.
     */
    private static final class Unwishable extends java.util.AbstractSet<Item> {
        private final Set<Item> blocked;
        private final Set<Item> prepared;
        private final java.util.function.Predicate<Item> known;
        private final Map<Item, Boolean> cache = new HashMap<>();

        Unwishable(ServerLevel level, Set<Item> blocked, java.util.function.Predicate<Item> known) {
            this.blocked = blocked;
            this.prepared = Stations.preparedFoods(level);
            this.known = known;
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Item item)) return false;
            return cache.computeIfAbsent(item, it -> blocked.contains(it) || prepared.contains(it) || !known.test(it)
                    || FoodValue.matchesAny(it, ChefConfig.NEVER_REQUEST.get())
                    || it.getDefaultInstance().getRarity().ordinal() >= net.minecraft.world.item.Rarity.RARE.ordinal());
        }

        @Override
        public java.util.Iterator<Item> iterator() {
            return blocked.iterator();
        }

        @Override
        public int size() {
            return blocked.size();
        }
    }

    /** Set by the self-test, whose owner is not a real player. */
    static volatile boolean ignoreKnownItems;

    /**
     * Items her owner knows (has picked up or crafted), or everything when that check is off. Null when the check is on
     * but the owner is offline (then she doesn't ask at all).
     */
    @Nullable
    static java.util.function.Predicate<Item> knownItems(ServerLevel level, EntityMaid maid) {
        if (!ChefConfig.ONLY_KNOWN.get() || ignoreKnownItems) return it -> true;
        java.util.UUID owner = maid.getOwnerUUID();
        net.minecraft.server.level.ServerPlayer p = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (p == null) return null;
        var stats = p.getStats();
        return it -> stats.getValue(net.minecraft.stats.Stats.ITEM_PICKED_UP.get(it)) > 0
                || stats.getValue(net.minecraft.stats.Stats.ITEM_CRAFTED.get(it)) > 0
                || p.getInventory().contains(new ItemStack(it));
    }

    @Nullable
    public static Request findRequest(ServerLevel level, EntityMaid maid, Kitchen kitchen, Map<Item, Integer> pool, ChefData data,
                                      Set<Item> blocked, double currentBest) {
        java.util.function.Predicate<Item> known = knownItems(level, maid);
        if (known == null) return null;
        Item wanted = data.targetItem();
        int budget = wanted != null ? ChefConfig.TARGET_MAX_MISSING.get() : ChefConfig.REQUEST_MAX_MISSING.get();
        if (budget <= 0 || kitchen.stations().isEmpty()) return null;
        Ctx normal = context(level, maid, kitchen, pool, data);
        Ctx ctx = context(level, maid, kitchen, pool, data);
        ctx.wishMode = true;
        ctx.wishBudget = budget;
        ctx.blocked = new Unwishable(level, blocked, known);
        if (wanted != null) {
            ctx.targetMode = true;
            normal.targetMode = true;
            if (canProduce(normal, wanted, 0) || !canProduce(ctx, wanted, 0)) return null;
            Job job = attempt(ctx, wanted, Math.max(1, data.target()), FoodValue.of(wanted), false);
            return job != null && !ctx.lastWishes.isEmpty()
                    ? new Request(wanted, job.amount(), FoodValue.of(wanted), currentBest, ctx.lastWishes, job.describe()) : null;
        }
        double threshold = currentBest * (1 + ChefConfig.REQUEST_MIN_GAIN_PERCENT.get() / 100.0);
        int tried = 0;
        for (Item dish : dishes(ctx)) {
            double v = FoodValue.of(dish);
            if (v <= threshold + 1e-6) break;
            if (!allowed(dish, data) || canProduce(normal, dish, 0)) continue;
            // quick filter: a direct recipe for it is missing at most `budget` ingredients
            boolean close = false;
            for (Producer p : ctx.producers.getOrDefault(dish, List.of())) {
                int missing = 0;
                for (Ingredient ing : p.need().inputs()) {
                    if (candidates(normal, ing).stream().noneMatch(m -> canMake(normal, m, 1))) missing++;
                }
                if (!p.need().container().isEmpty() && !canMake(normal, p.need().container().getItem(), 1)) missing++;
                if (missing <= budget) {
                    close = true;
                    break;
                }
            }
            if (!close || !canProduce(ctx, dish, 0)) continue;
            if (++tried > 40) break;
            Job job = attempt(ctx, dish, Math.max(1, data.target()), v, false);
            if (job != null && !ctx.lastWishes.isEmpty()) {
                return new Request(dish, job.amount(), v, currentBest, ctx.lastWishes, job.describe());
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ explain

    /**
     * Human-readable view of her kitchen and choices: stations, the next plan, dishes she can make (with how),
     * dishes that are blocked and why, and the ones she's closest to. Used by the Status tab and /maidchef explain.
     */
    /** Separates a line's text from the item it's about (the Status tab makes such lines clickable). */
    public static final char ITEM_SEP = '\u0000';

    private static String withItem(String line, Item item) {
        return line + ITEM_SEP + BuiltInRegistries.ITEM.getKey(item);
    }

    public static List<String> explain(ServerLevel level, EntityMaid maid, ChefData data, int limit) {
        List<String> out = new ArrayList<>();
        int radius = Math.min(data.radius(), ChefConfig.MAX_RADIUS.get());
        Kitchen kitchen = scan(level, maid, radius);
        Map<Item, Integer> pool = pool(level, maid, kitchen);
        out.add("Kitchen: " + kitchen.stations().size() + " stations, " + kitchen.ingredientChests().size() + " ingredient / "
                + kitchen.outputChests().size() + " output chests, " + pool.size() + " kinds of ingredients. Feasts: "
                + (data.serveFeasts() ? "served" : "kept whole")
                + (data.targetItem() != null ? ". Mode: prepare " + name(data.targetItem()) : ". Mode: best food"));
        for (Found f : kitchen.stations()) {
            boolean ready = f.station().idle(level, f.pos());
            out.add("  " + f.station().name() + " " + f.pos().toShortString() + (ready ? ": ready" : ": NOT ready (unheated, unlit, in use or covered)"));
        }
        if (kitchen.stations().isEmpty()) {
            out.add("  No stations within " + radius + " blocks of her home point.");
            return out;
        }
        Ctx ctx = context(level, maid, kitchen, pool, data);
        ChefBehavior.State st = ChefBehavior.state(maid);
        if (st.job != null) {
            out.add("Now: " + st.job.amount() + "x " + st.job.dishName() + " (step " + (st.stepIndex + 1) + "/" + st.job.steps().size() + "): "
                    + st.job.describe());
        }
        Choice next = choose(level, maid, kitchen, pool, data);
        Request req = findRequest(level, maid, kitchen, pool, data,
                maid.getOwnerUUID() == null ? Set.of() : ChefRequests.get(level.getServer()).blocked(maid.getOwnerUUID()),
                next.job() != null ? next.job().value() : st.job != null ? st.job.value() : 0);
        if (req != null) out.add(String.format("Would ask for: %s -> %dx %s (%.0f vs %.0f now)", req.itemsText(), req.amount(), req.dishName(),
                req.value(), req.currentBest()));
        out.add((st.job != null ? "Next (after this): " : "Next: ") + (next.job() == null ? "nothing" + (next.why() != null ? " (" + next.why() + ")" : "") : next.job().amount() + "x " + next.job().dishName() + ": " + next.job().describe()));

        List<String> ok = new ArrayList<>(), blocked = new ArrayList<>();
        int tried = 0;
        for (Item dish : dishes(ctx)) {
            if (ok.size() >= limit && blocked.size() >= limit) break;
            String head = String.format("  %.0f %s: ", FoodValue.of(dish), name(dish));
            if (!allowed(dish, data)) {
                if (blocked.size() < limit) blocked.add(head + "filtered (blacklist or minimums)");
                continue;
            }
            if (!canProduce(ctx, dish, 0) || tried++ > 60) continue;
            Job job = best(ctx, dish, FoodValue.of(dish), false);
            if (job != null) {
                if (ok.size() < limit) ok.add(withItem(head + job.amount() + " via " + job.describe()
                        + (data.recent().contains(BuiltInRegistries.ITEM.getKey(dish).toString()) ? " (made recently)" : ""), dish));
            } else if (blocked.size() < limit) {
                blocked.add(head + ctx.why);
            }
        }
        out.add("Can make:" + (ok.isEmpty() ? " nothing" : ""));
        out.addAll(ok);
        if (!blocked.isEmpty()) {
            out.add("Blocked:");
            out.addAll(blocked);
        }

        // closest misses: direct recipes where most inputs are in stock or makeable
        record Miss(double closeness, double value, String line) {
        }
        List<Miss> misses = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        for (Item dish : dishes(ctx)) {
            if (canProduce(ctx, dish, 0) || !seen.add(dish) || !allowed(dish, data)) continue;
            for (Producer p : ctx.producers.get(dish)) {
                int have = 0;
                String missing = null;
                for (Ingredient ing : p.need().inputs()) {
                    boolean any = candidates(ctx, ing).stream().anyMatch(m -> canMake(ctx, m, 1));
                    if (any) have++;
                    else if (missing == null) missing = describe(ing);
                }
                int n = p.need().inputs().size();
                if (have > 0 && missing != null) {
                    misses.add(new Miss((double) have / n, FoodValue.of(dish), withItem(
                            String.format("  %.0f %s: missing %s (has %d of %d, %s)", FoodValue.of(dish), name(dish), missing, have, n, p.at().station().name()), dish)));
                    break;
                }
            }
        }
        misses.sort(Comparator.comparingDouble(Miss::closeness).thenComparingDouble(Miss::value).reversed());
        if (!misses.isEmpty()) {
            out.add("Closest (missing an ingredient):");
            misses.stream().limit(limit).forEach(m -> out.add(m.line()));
        }
        return out;
    }

    // ------------------------------------------------------------------ backpack

    /**
     * Split what she takes from storage over the steps: each step brings only the raw items it uses itself
     * (intermediates made by earlier steps are already in her backpack).
     */
    private static List<Step> perStepFetch(List<Step> steps) {
        Map<Item, Integer> made = new HashMap<>();
        Set<Item> toolsFetched = new HashSet<>();
        List<Step> out = new ArrayList<>();
        for (Step st : steps) {
            Map<Item, Integer> use = new LinkedHashMap<>(st.alloc().times(st.units()));
            if (st.fuel() != null && st.fuelCount() > 0) use.merge(st.fuel(), st.fuelCount(), Integer::sum);
            Map<Item, Integer> f = new LinkedHashMap<>();
            use.forEach((item, n) -> {
                int fromMade = Math.min(n, made.getOrDefault(item, 0));
                if (fromMade > 0) made.merge(item, -fromMade, Integer::sum);
                if (n - fromMade > 0) f.merge(item, n - fromMade, Integer::sum);
            });
            if (st.tool() != null && toolsFetched.add(st.tool())) f.merge(st.tool(), 1, Integer::sum);
            // a crafting table pulls its own ingredients from the chests
            if (st.at().station().usesPantry()) f.clear();
            made.merge(st.need().resultItem(), st.output(), Integer::sum);
            out.add(st.withFetch(f));
        }
        return out;
    }

    /**
     * Can she carry what each step needs? A crafting table takes from the chests and a pot can be filled over several
     * trips, so those always fit; other steps must fit at once (plus a slot or two for intermediates she's carrying).
     */
    private static boolean fits(EntityMaid maid, List<Step> steps) {
        IItemHandler bag = Inv.backpack(maid);
        int free = 0;
        for (int i = 0; i < bag.getSlots(); i++) {
            if (i != Inv.HUB_SLOT && bag.getStackInSlot(i).isEmpty()) free++;
        }
        for (int k = 0; k < steps.size(); k++) {
            Step st = steps.get(k);
            if (st.at().station().usesPantry() || st.at().station().canStage()) continue;
            int slots = Math.min(k, 2);
            for (Map.Entry<Item, Integer> e : st.fetch().entrySet()) {
                int missing = e.getValue() - (k == 0 ? Inv.count(bag, e.getKey()) : 0);
                if (missing > 0) slots += (missing + e.getKey().getDefaultMaxStackSize() - 1) / e.getKey().getDefaultMaxStackSize();
            }
            if (slots > free) return false;
        }
        return true;
    }
}
