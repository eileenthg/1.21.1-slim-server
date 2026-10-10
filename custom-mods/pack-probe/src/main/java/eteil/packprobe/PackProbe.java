package eteil.packprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Diagnostics only. Remove once the maid damage question and the Lost Maid gear tiers are settled. */
@Mod("packprobe")
public class PackProbe {
    public static final Logger LOG = LogUtils.getLogger();

    public PackProbe(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        for (EventPriority p : EventPriority.values()) {
            NeoForge.EVENT_BUS.addListener(p, true, LivingIncomingDamageEvent.class, e -> {
                if (e.getEntity() instanceof Player && isMaid(e.getSource())) {
                    LOG.info("[PackProbe] incoming priority={} canceled={} amount={} source={}", p, e.isCanceled(), e.getAmount(), describe(e.getSource()));
                }
            });
            NeoForge.EVENT_BUS.addListener(p, true, LivingDamageEvent.Pre.class, e -> {
                if (e.getEntity() instanceof Player && isMaid(e.getSource())) {
                    LOG.info("[PackProbe] damage.pre priority={} newDamage={} original={}", p, e.getNewDamage(), e.getOriginalDamage());
                }
            });
        }
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDamageEvent.Post.class, e -> {
            if (e.getEntity() instanceof Player && isMaid(e.getSource())) {
                LOG.info("[PackProbe] damage.post newDamage={} health={}", e.getNewDamage(), e.getEntity().getHealth());
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingShieldBlockEvent.class, e -> {
            if (e.getEntity() instanceof Player && isMaid(e.getDamageSource())) {
                LOG.info("[PackProbe] shield block blocked={} blockedDamage={}", e.getBlocked(), e.getBlockedDamage());
            }
        });
    }

    public static boolean isMaid(DamageSource source) {
        return isMaid(source.getEntity()) || isMaid(source.getDirectEntity());
    }

    public static boolean isMaid(Entity entity) {
        return entity != null && entity.getClass().getName().contains("Maid");
    }

    public static String describe(DamageSource source) {
        return source.type().msgId() + " entity=" + name(source.getEntity()) + " direct=" + name(source.getDirectEntity());
    }

    private static String name(Entity e) {
        return e == null ? "null" : e.getClass().getSimpleName() + "#" + e.getId();
    }

    private void onServerStarted(ServerStartedEvent event) {
        wrapListeners(LivingIncomingDamageEvent.class);
        dumpListeners(LivingIncomingDamageEvent.class);
        dumpListeners(LivingDamageEvent.Pre.class);
        dumpItems();
    }

    /** Swaps every handler of the event for a TracingListener around it, then makes the bus rebuild its cache. */
    @SuppressWarnings("unchecked")
    private static void wrapListeners(Class<?> eventClass) {
        try {
            Field listsField = NeoForge.EVENT_BUS.getClass().getDeclaredField("listenerLists");
            listsField.setAccessible(true);
            Object lists = listsField.get(NeoForge.EVENT_BUS);
            java.lang.reflect.Method get = lists.getClass().getMethod("get", Object.class);
            get.setAccessible(true);
            Object list = get.invoke(lists, eventClass);
            Field prioritiesField = list.getClass().getDeclaredField("priorities");
            prioritiesField.setAccessible(true);
            java.util.List<java.util.List<net.neoforged.bus.api.EventListener>> priorities =
                    (java.util.List<java.util.List<net.neoforged.bus.api.EventListener>>) prioritiesField.get(list);
            int wrapped = 0;
            for (java.util.List<net.neoforged.bus.api.EventListener> group : priorities) {
                for (int i = 0; i < group.size(); i++) {
                    net.neoforged.bus.api.EventListener l = group.get(i);
                    if (!(l instanceof TracingListener)) {
                        group.set(i, new TracingListener(l));
                        wrapped++;
                    }
                }
            }
            java.lang.reflect.Method rebuild = list.getClass().getDeclaredMethod("forceRebuild");
            rebuild.setAccessible(true);
            rebuild.invoke(list);
            LOG.info("[PackProbe] wrapped {} handlers of {}", wrapped, eventClass.getName());
        } catch (Throwable t) {
            LOG.warn("[PackProbe] could not wrap handlers of {}: {}", eventClass.getName(), t.toString());
        }
    }

    /** Prints every handler registered for a damage event, so a cancel seen at one priority can be traced to a mod. */
    private static void dumpListeners(Class<?> eventClass) {
        try {
            // bus 8: EventBus.listenerLists (LockHelper<Class, ListenerList>) -> ListenerList.getListeners(),
            // in dispatch order with EventPriority markers between the groups
            Field listsField = NeoForge.EVENT_BUS.getClass().getDeclaredField("listenerLists");
            listsField.setAccessible(true);
            Object lists = listsField.get(NeoForge.EVENT_BUS);
            java.lang.reflect.Method get = lists.getClass().getMethod("get", Object.class);
            get.setAccessible(true);
            Object list = get.invoke(lists, eventClass);
            if (list == null) throw new IllegalStateException("no listener list yet");
            java.lang.reflect.Method getListeners = list.getClass().getDeclaredMethod("getListeners");
            getListeners.setAccessible(true);
            Object[] listeners = (Object[]) getListeners.invoke(list);
            LOG.info("[PackProbe] {} listeners for {}", listeners.length, eventClass.getName());
            for (Object l : listeners) {
                StringBuilder sb = new StringBuilder(String.valueOf(l));
                for (Field f : l.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object v = f.get(l);
                    if (v != null) sb.append(" | ").append(f.getName()).append('=').append(v.getClass().getName()).append(':').append(v);
                }
                LOG.info("[PackProbe]   {}", sb);
            }
        } catch (Throwable t) {
            LOG.warn("[PackProbe] could not list listeners for {}: {}", eventClass.getName(), t.toString());
        }
    }

    private static void dumpItems() {
        JsonArray out = new JsonArray();
        for (Item item : BuiltInRegistries.ITEM) {
            try {
                ItemStack stack = item.getDefaultInstance();
                JsonObject o = new JsonObject();
                o.addProperty("id", BuiltInRegistries.ITEM.getKey(item).toString());
                o.addProperty("maxDamage", stack.getMaxDamage());
                double attack = 0, armor = 0, toughness = 0;
                ItemAttributeModifiers mods = stack.getAttributeModifiers();
                for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                    double amount = entry.modifier().amount();
                    if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.slot().test(EquipmentSlot.MAINHAND)) attack += amount;
                    if (entry.attribute().is(Attributes.ARMOR)) armor += amount;
                    if (entry.attribute().is(Attributes.ARMOR_TOUGHNESS)) toughness += amount;
                }
                o.addProperty("attack", attack);
                o.addProperty("armor", armor);
                o.addProperty("toughness", toughness);
                if (item instanceof TieredItem tiered) {
                    o.addProperty("tierUses", tiered.getTier().getUses());
                    o.addProperty("tierAttackBonus", tiered.getTier().getAttackDamageBonus());
                    o.addProperty("tier", String.valueOf(tiered.getTier()));
                }
                if (item instanceof ArmorItem armorItem) {
                    o.addProperty("armorMaterial", armorItem.getMaterial().unwrapKey().map(k -> k.location().toString()).orElse("?"));
                    o.addProperty("armorType", armorItem.getType().getName());
                }
                if (attack > 0 || armor > 0 || toughness > 0) out.add(o);
            } catch (Throwable t) {
                LOG.debug("[PackProbe] skipped {}: {}", item, t.toString());
            }
        }
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("packprobe");
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(dir.resolve("items.json"), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(out, w);
            }
            LOG.info("[PackProbe] wrote {} combat items to {}", out.size(), dir.resolve("items.json"));
        } catch (Exception ex) {
            LOG.warn("[PackProbe] could not write items.json: {}", ex.toString());
        }
    }
}
