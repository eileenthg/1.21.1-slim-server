package net.eileenthg.guidelibrary.unlock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.eileenthg.guidelibrary.GuideLibrary;
import net.eileenthg.guidelibrary.LibraryConfig;
import net.eileenthg.guidelibrary.network.SyncUnlocksPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Server side of unlocking. A book is unlocked for a player the first time a physical copy is in
 * their inventory (checked once a second, and at once on pickup). Unlocks are saved with the
 * player, survive death, and are sent to the client whenever they change.
 */
public final class Unlocks {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GuideLibrary.MOD_ID);

    public static final Supplier<AttachmentType<PlayerUnlocks>> UNLOCKS = ATTACHMENTS.register("unlocks",
            () -> AttachmentType.builder(PlayerUnlocks::new).serialize(PlayerUnlocks.CODEC).copyOnDeath().build());

    private static final int SCAN_INTERVAL = 20;

    private Unlocks() {}

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Unlocks::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Unlocks::onPickup);
        NeoForge.EVENT_BUS.addListener(Unlocks::onLogin);
        NeoForge.EVENT_BUS.addListener(Unlocks::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(Unlocks::onRegisterCommands);
    }

    /** What the server treats as unlocked, e.g. when asked to open an item book. */
    public static boolean isUnlocked(Player player, String key) {
        return LibraryConfig.unlockedByConfig(key) || player.getData(UNLOCKS).contains(key);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Staggered so a full server does not scan everyone on the same tick.
        if ((player.tickCount + player.getId()) % SCAN_INTERVAL != 0) return;
        scan(player);
    }

    private static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            String key = BookIds.identify(event.getOriginalStack());
            if (key != null) unlock(player, List.of(key));
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        sync(player, List.of());
        scan(player);
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        // Fires for /reload (no player) and for each joining player; only a reload changes books.
        if (event.getPlayer() == null) BookIds.invalidate();
    }

    private static void scan(ServerPlayer player) {
        if (player.isSpectator()) return;
        Inventory inv = player.getInventory();
        List<String> found = null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            String key = BookIds.identify(inv.getItem(i));
            if (key != null && !player.getData(UNLOCKS).contains(key)) {
                if (found == null) found = new ArrayList<>();
                found.add(key);
            }
        }
        if (found != null) unlock(player, found);
    }

    /** Unlocks {@code keys}; returns how many were new. */
    public static int unlock(ServerPlayer player, Collection<String> keys) {
        PlayerUnlocks data = player.getData(UNLOCKS);
        List<String> added = new ArrayList<>();
        for (String k : keys) {
            if (data.add(k)) added.add(k);
        }
        if (!added.isEmpty()) {
            player.setData(UNLOCKS, data);
            sync(player, added);
        }
        return added.size();
    }

    private static int lock(ServerPlayer player, String pattern) {
        PlayerUnlocks data = player.getData(UNLOCKS);
        List<String> remove = new ArrayList<>();
        for (String k : data.keys()) {
            if (LibraryConfig.matches(pattern, k)) remove.add(k);
        }
        remove.forEach(data::remove);
        if (!remove.isEmpty()) {
            player.setData(UNLOCKS, data);
            sync(player, List.of());
        }
        return remove.size();
    }

    private static void sync(ServerPlayer player, List<String> added) {
        if (!player.connection.hasChannel(SyncUnlocksPayload.TYPE)) return;
        PacketDistributor.sendToPlayer(player,
                new SyncUnlocksPayload(List.copyOf(player.getData(UNLOCKS).keys()), added));
    }

    // ---- /guidelibrary unlock|lock|list -----------------------------------------------------------

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(GuideLibrary.MOD_ID)
                .then(Commands.literal("list")
                        .executes(c -> list(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(s -> s.hasPermission(2))
                                .executes(c -> list(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("books")
                        .requires(s -> s.hasPermission(2))
                        .executes(Unlocks::books))
                .then(Commands.literal("unlock")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("book", StringArgumentType.greedyString())
                                        .suggests(Unlocks::suggestBooks)
                                        .executes(c -> unlockCommand(c)))))
                .then(Commands.literal("lock")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("book", StringArgumentType.greedyString())
                                        .suggests(Unlocks::suggestBooks)
                                        .executes(c -> lockCommand(c))))));
    }

    private static CompletableFuture<Suggestions> suggestBooks(CommandContext<CommandSourceStack> c,
                                                               SuggestionsBuilder b) {
        List<String> keys = new ArrayList<>(BookIds.allKeys());
        keys.add("*");
        return SharedSuggestionProvider.suggest(keys, b);
    }

    private static int unlockCommand(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String pattern = StringArgumentType.getString(c, "book");
        List<String> matching = BookIds.allKeys().stream().filter(k -> LibraryConfig.matches(pattern, k)).toList();
        if (matching.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("guidelibrary.cmd.no_book", pattern));
            return 0;
        }
        int total = 0;
        for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) total += unlock(p, matching);
        int t = total;
        c.getSource().sendSuccess(() -> Component.translatable("guidelibrary.cmd.unlocked", t), true);
        return total;
    }

    private static int lockCommand(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String pattern = StringArgumentType.getString(c, "book");
        int total = 0;
        for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) total += lock(p, pattern);
        int t = total;
        c.getSource().sendSuccess(() -> Component.translatable("guidelibrary.cmd.locked", t), true);
        return total;
    }

    private static int list(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        var keys = p.getData(UNLOCKS).keys();
        c.getSource().sendSuccess(() -> Component.translatable("guidelibrary.cmd.list",
                p.getDisplayName(), keys.size(), String.join(", ", keys)), false);
        return keys.size();
    }

    private static int books(CommandContext<CommandSourceStack> c) {
        List<String> keys = BookIds.allKeys();
        c.getSource().sendSuccess(() -> Component.translatable("guidelibrary.cmd.books",
                keys.size(), String.join(", ", keys)), false);
        return keys.size();
    }
}
