package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.context.AbstractMaidContext;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.context.GameContextRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ITool;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ToolRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.Parameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.StringParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;

/**
 * Lets you talk to a Chef maid about her cooking through TLM's AI chat (e.g. Player2).
 * <ul>
 *   <li>A short "cooking" block in every chat message: her mode, status, current job, recent dishes, what she'd like.</li>
 *   <li>Tool {@code chef_kitchen_report}: the full Status-tab breakdown (stations, what she can make, what's blocked, closest).</li>
 *   <li>Tool {@code chef_prepare}: "could you make fried eggs?" sets Prepare mode; "anything"/"best" switches back.</li>
 * </ul>
 * Tools may be called off the server thread, so they do their work through {@link LLMCallback#runOnServerThread}.
 */
public final class ChefChat {
    private ChefChat() {
    }

    // ------------------------------------------------------------------ prompt context

    public static void registerContexts(GameContextRegister register) {
        register.registerCategory("cooking", "Your cooking as the Chef (what you're making and why)", true);
        register.registerContext("cooking", context("chef_task", "Cooking task", maid -> {
            if (maid.getTask() != TaskChef.INSTANCE) return "not on the Chef task";
            ChefData d = TaskChef.data(maid);
            Item dish = d.targetItem();
            return (dish != null ? "preparing only " + name(dish) : "making the best food she can") + ", " + d.target() + " per round"
                    + (d.serveFeasts() ? ", serves feasts into portions" : "");
        }));
        register.registerContext("cooking", context("chef_now", "Cooking right now", maid -> {
            if (maid.getTask() != TaskChef.INSTANCE) return "-";
            ChefBehavior.State st = ChefBehavior.state(maid);
            String status = TaskChef.data(maid).status();
            if (st.job != null) {
                return st.job.amount() + "x " + st.job.dishName() + " (step " + (st.stepIndex + 1) + "/" + st.job.steps().size() + ": "
                        + st.job.describe() + ")";
            }
            return status.isEmpty() ? "starting up" : status;
        }));
        register.registerContext("cooking", context("chef_recent", "Recently cooked", maid -> {
            if (maid.getTask() != TaskChef.INSTANCE) return "-";
            List<String> names = new ArrayList<>();
            for (String id : TaskChef.data(maid).recent()) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) BuiltInRegistries.ITEM.getOptional(rl).ifPresent(i -> names.add(name(i)));
            }
            return names.isEmpty() ? "nothing yet" : String.join(", ", names);
        }));
        register.registerContext("cooking", context("chef_wish", "Would like the owner to bring", maid -> {
            if (maid.getTask() != TaskChef.INSTANCE) return "-";
            Planner.Request r = ChefBehavior.state(maid).lastRequest;
            return r == null ? "nothing" : r.itemsText() + " (to make " + r.dishName() + ")";
        }));
    }

    private static AbstractMaidContext context(String key, String label, Function<EntityMaid, String> value) {
        return new AbstractMaidContext(key, label) {
            @Override
            public String getValue(EntityMaid maid) {
                try {
                    return value.apply(maid);
                } catch (RuntimeException e) {
                    return "unknown";
                }
            }
        };
    }

    // ------------------------------------------------------------------ tools

    public static void registerTools(ToolRegister register) {
        register.register(new ReportTool());
        register.register(new PrepareTool());
    }

    /** Run on the server thread and answer the LLM with the returned text. */
    private static CompletableFuture<LLMCallback> onServer(String toolId, LLMCallback callback, Function<EntityMaid, String> work) {
        CompletableFuture<LLMCallback> future = new CompletableFuture<>();
        Runnable job = () -> {
            String text;
            try {
                text = work.apply(callback.getMaid());
            } catch (RuntimeException e) {
                text = "Something went wrong: " + e.getMessage();
            }
            future.complete(callback.addToolResult(text, toolId));
        };
        if (callback.isOnServerThread() || !(callback.getMaid().level() instanceof ServerLevel)) job.run();
        else callback.runOnServerThread(job);
        return future;
    }

    private static final class ReportTool implements ITool<String> {
        @Override
        public String id() {
            return "chef_kitchen_report";
        }

        @Override
        public String summary(EntityMaid maid) {
            return ("Use this when the user asks about your cooking: what you can cook, why you are not cooking, what ingredients are missing, "
                    + "what you would like them to bring, or about your kitchen stations. Returns your kitchen, stations and what is ready, "
                    + "your current and next dish, dishes you can make (and how), dishes that are blocked and why, and the dishes you are "
                    + "closest to making. Answer in character and in plain words; don't read out coordinates.").trim();
        }

        @Override
        public Parameter parameters(ObjectParameter root, EntityMaid maid) {
            root.addProperties("focus", StringParameter.create()
                    .setDescription("Optional: what the user asked about, e.g. 'can make', 'missing', 'stations'. Leave empty for everything."), false);
            return root;
        }

        @Override
        public Codec<String> codec() {
            return Codec.STRING.optionalFieldOf("focus", "").codec();
        }

        @Override
        public LLMCallback onCall(String toolId, String focus, LLMCallback callback) {
            return onCallAsync(toolId, focus, callback, null).join();
        }

        @Override
        public CompletableFuture<LLMCallback> onCallAsync(String toolId, String focus, LLMCallback callback, @Nullable LLMClient client) {
            return onServer(toolId, callback, maid -> {
                if (maid.getTask() != TaskChef.INSTANCE) return "You are not on the Chef task right now, so you are not cooking.";
                if (!(maid.level() instanceof ServerLevel level)) return "Not available.";
                List<String> out = new ArrayList<>();
                out.add("Status: " + TaskChef.data(maid).status());
                for (String raw : Planner.explain(level, maid, TaskChef.data(maid), 6)) {
                    int sep = raw.indexOf(Planner.ITEM_SEP);
                    out.add(sep >= 0 ? raw.substring(0, sep) : raw);
                }
                return String.join("\n", out).replaceAll(" -?\\d+, -?\\d+, -?\\d+", "");
            });
        }

        @Override
        public Component invocationSummaryComponent(String focus) {
            return Component.literal("Checking the kitchen").withStyle(ChatFormatting.GRAY);
        }
    }

    private static final class PrepareTool implements ITool<String> {
        @Override
        public String id() {
            return "chef_prepare";
        }

        @Override
        public String summary(EntityMaid maid) {
            return ("Use this when the user asks you to cook or make a specific food (\"make fried eggs\", \"can you cook beef stew?\"), "
                    + "or to go back to cooking whatever is best (dish = \"best\"). Only works while you are on the Chef task. "
                    + "The result tells you whether you can make it right now and, if not, what is missing.").trim();
        }

        @Override
        public Parameter parameters(ObjectParameter root, EntityMaid maid) {
            root.addProperties("dish", StringParameter.create()
                    .setDescription("The food to prepare, by its in-game name (e.g. 'Fried Egg') or item id, or 'best' to cook whatever is best."));
            return root;
        }

        @Override
        public Codec<String> codec() {
            return Codec.STRING.fieldOf("dish").codec();
        }

        @Override
        public LLMCallback onCall(String toolId, String dish, LLMCallback callback) {
            return onCallAsync(toolId, dish, callback, null).join();
        }

        @Override
        public CompletableFuture<LLMCallback> onCallAsync(String toolId, String dish, LLMCallback callback, @Nullable LLMClient client) {
            return onServer(toolId, callback, maid -> prepare(maid, dish));
        }

        @Override
        public Component invocationSummaryComponent(String dish) {
            return Component.literal("Cooking: " + dish).withStyle(ChatFormatting.GRAY);
        }
    }

    /** Set (or clear) her Prepare-X dish from a name or id; say whether she can make it now. */
    static String prepare(EntityMaid maid, String request) {
        if (maid.getTask() != TaskChef.INSTANCE) {
            return "You are not on the Chef task. Switch to it first (switch_work_task with task_id " + TaskChef.UID + ").";
        }
        ChefData d = TaskChef.data(maid);
        String q = request == null ? "" : request.trim();
        if (q.isEmpty() || q.equalsIgnoreCase("best") || q.equalsIgnoreCase("anything") || q.equalsIgnoreCase("best food")) {
            maid.setAndSyncData(ChefPlugin.CHEF_DATA, d.withSettings(d.radius(), d.target(), d.recentSize(), d.minNutrition(), d.minSaturation(),
                    d.minEffects(), d.blacklist(), d.serveFeasts(), ""));
            return "OK: back to cooking the best food you can.";
        }
        // a potion (or other item told apart by its contents): match against what the stations make
        String variantDish = maid.level() instanceof ServerLevel sl ? findVariant(sl, q) : null;
        Item item = variantDish != null ? Variant.display(variantDish).getItem() : findItem(q);
        if (item == null) return "No food called '" + q + "' exists. Ask the user to check the name.";
        maid.setAndSyncData(ChefPlugin.CHEF_DATA, d.withSettings(d.radius(), d.target(), d.recentSize(), d.minNutrition(), d.minSaturation(),
                d.minEffects(), d.blacklist(), d.serveFeasts(), variantDish != null ? variantDish : BuiltInRegistries.ITEM.getKey(item).toString()));
        String label = variantDish != null ? variantName(Variant.display(variantDish)) : name(item);
        if (!(maid.level() instanceof ServerLevel level)) return "OK: preparing " + label + ".";
        ChefData now = TaskChef.data(maid);
        Planner.Kitchen kitchen = Planner.scan(level, maid, Math.min(now.radius(), ChefConfig.MAX_RADIUS.get()));
        Map<Item, Integer> pool = Planner.pool(level, maid, kitchen);
        Planner.Choice c = Planner.choose(level, maid, kitchen, pool, now);
        if (c.job() != null) return "OK: you will now prepare " + label + ". You can make " + c.job().amount() + " right now: " + c.job().describe();
        return "OK: you will now prepare " + label + ", but you can't make it right now"
                + (c.why() != null ? " (" + c.why() + ")" : "") + ". Tell the user what's missing.";
    }

    /** A potion-like result some station makes whose name matches ("Potion of Swiftness", "swiftness"), as a stored dish. */
    @Nullable
    static String findVariant(ServerLevel level, String q) {
        String want = q.toLowerCase(Locale.ROOT).trim();
        String contains = null;
        for (var st : net.eileenthg.maidchef.station.Stations.all()) {
            for (var need : net.eileenthg.maidchef.station.Stations.recipes(level, st)) {
                if (Variant.key(need.result()).isEmpty()) continue;
                String n = variantName(need.result()).toLowerCase(Locale.ROOT);
                if (n.equals(want)) return Variant.dishOf(need.result());
                String effect = effectNames(need.result()).toLowerCase(Locale.ROOT);
                if (contains == null && (n.contains(want) || (!effect.isEmpty() && (want.contains(effect) || effect.contains(want))))) {
                    contains = Variant.dishOf(need.result());
                }
            }
        }
        return contains;
    }

    private static String effectNames(net.minecraft.world.item.ItemStack stack) {
        var pc = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (pc == null) return "";
        List<String> names = new ArrayList<>();
        pc.getAllEffects().forEach(e -> names.add(e.getEffect().value().getDisplayName().getString()));
        return String.join(" ", names);
    }

    /** "Potion of Swiftness" style name, also for potions that only carry custom effects. */
    static String variantName(net.minecraft.world.item.ItemStack stack) {
        String effects = effectNames(stack);
        String base = stack.getHoverName().getString();
        return effects.isEmpty() || base.toLowerCase(Locale.ROOT).contains(effects.toLowerCase(Locale.ROOT)) ? base : base + " of " + effects;
    }

    /** By item id, else exact display name, else a display name containing the words (food preferred). */
    @Nullable
    static Item findItem(String q) {
        ResourceLocation rl = q.contains(":") ? ResourceLocation.tryParse(q.toLowerCase(Locale.ROOT)) : null;
        if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) return BuiltInRegistries.ITEM.get(rl);
        String want = q.toLowerCase(Locale.ROOT).replaceAll("s$", "");
        Item contains = null;
        double containsValue = -1;
        for (Item it : BuiltInRegistries.ITEM) {
            String n = name(it).toLowerCase(Locale.ROOT);
            if (n.equals(q.toLowerCase(Locale.ROOT)) || n.equals(want)) return it;
            if (n.contains(want)) {
                double v = FoodValue.of(it);
                if (contains == null || v > containsValue || (v == containsValue && n.length() < name(contains).length())) {
                    contains = it;
                    containsValue = v;
                }
            }
        }
        return contains;
    }

    private static String name(Item item) {
        return item.getDefaultInstance().getHoverName().getString();
    }
}
