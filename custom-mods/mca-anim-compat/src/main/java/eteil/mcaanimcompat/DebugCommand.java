package eteil.mcaanimcompat;

import com.mojang.brigadier.CommandDispatcher;
import net.conczin.mca.MCAClient;
import net.conczin.mca.entity.VillagerLike;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/** /mcaanimcompat — what each nearby player is rendered with, for diagnosing animation problems in game. */
final class DebugCommand {
    private DebugCommand() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mcaanimcompat").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return 0;
            ctx.getSource().sendSystemMessage(Component.literal("[MCA Animation Compat] renderer allowed: " + MCAClient.isPlayerRendererAllowed()));
            for (Player p : mc.level.players()) {
                String model = MCAClient.getPlayerData(p.getUUID())
                        .map(v -> v.getPlayerModel().name())
                        .orElse("no MCA data (vanilla)");
                String line = String.format("%s: model=%s villagerRenderer=%s geneticsRenderer=%s animationActive=%s carrier=%s carried=%s",
                        p.getName().getString(), model,
                        MCAClient.useVillagerRenderer(p.getUUID()), MCAClient.useGeneticsRenderer(p.getUUID()),
                        animationActive(p), Carry.isCarrier(p.getUUID()), Carry.isCarried(p.getUUID()));
                ctx.getSource().sendSystemMessage(Component.literal(line));
                McaAnimCompat.LOG.info("[MCA Animation Compat] {}", line);
            }
            return 1;
        }));
    }

    /** Player Animation Library adds playerAnimLib$getAnimManager() to AbstractClientPlayer; read it reflectively. */
    private static String animationActive(Player player) {
        try {
            Method get = player.getClass().getMethod("playerAnimLib$getAnimManager");
            Object manager = get.invoke(player);
            if (manager == null) return "no";
            return String.valueOf(manager.getClass().getMethod("isActive").invoke(manager));
        } catch (ReflectiveOperationException e) {
            return "n/a";
        }
    }
}
