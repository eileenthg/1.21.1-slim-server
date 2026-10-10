package eteil.mcaanimcompat.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Each mixin needs the mod it bridges to: the carry fixes (render, targeting, sneak, teleport) need MCA Romantic Expansion, the animation fix needs PAL, the sit-camera fix needs Take a Seat. */
public class CompatMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("PlayerModelCarryMixin") || mixinClassName.endsWith("PlayerRendererCarryMixin")
                || mixinClassName.endsWith("GameRendererPickMixin") || mixinClassName.endsWith("RomanticShiftMixin")
                || mixinClassName.endsWith("ServerTeleportMixin")) {
            return loaded("mcaromanticexpansion");
        }
        if (mixinClassName.endsWith("McaPlayerModelAnimMixin")) return loaded("player_animation_library");
        if (mixinClassName.endsWith("TakeASeatCameraScaleMixin")) return loaded("takeaseat");
        if (mixinClassName.endsWith("LyingHeadMixin") || mixinClassName.endsWith("SideLyingHeadTurnMixin")) {
            return loaded("takeaseat") && loaded("player_animation_library");
        }
        return true;
    }

    private static boolean loaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
