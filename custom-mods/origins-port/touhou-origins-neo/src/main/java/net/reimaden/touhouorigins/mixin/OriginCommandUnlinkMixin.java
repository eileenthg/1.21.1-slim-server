package net.reimaden.touhouorigins.mixin;

import com.mojang.brigadier.context.CommandContext;
import dev.overgrown.origins.command.OriginCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.reimaden.touhouorigins.power.ActionOnOriginGuiPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Fires {@code touhouorigins:action_on_origin_gui} for {@code /origin gui}.
 *
 * <p>The second of upstream's two triggers, matching its {@code OriginCommandMixin}. Overgrown's
 * equivalent of {@code openLayerScreen} is the private {@code gui} helper, which takes the whole
 * target collection, so the action is run per player here rather than once.
 */
@Mixin(OriginCommands.class)
public class OriginCommandUnlinkMixin {

    @Inject(method = "gui", at = @At("HEAD"))
    private static void touhouorigins$invokeActions(CommandContext<CommandSourceStack> ctx,
                                                    Collection<ServerPlayer> targets,
                                                    ResourceLocation layerId,
                                                    CallbackInfoReturnable<Integer> cir) {
        for (ServerPlayer player : targets) {
            ActionOnOriginGuiPower.onGuiOpen(player);
        }
    }
}
