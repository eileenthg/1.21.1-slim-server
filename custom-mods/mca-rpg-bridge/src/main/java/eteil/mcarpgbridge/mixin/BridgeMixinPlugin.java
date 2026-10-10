package eteil.mcarpgbridge.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Each mixin targets another mod; skip it when that mod (or the one it reports to) is not installed. */
public class BridgeMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("QuestsReputationVersionMixin")) return loaded("mcaquests") && loaded("mcareputation");
        if (mixinClassName.endsWith("MebahelVillagerClickMixin") || mixinClassName.endsWith("MebahelTrackerOffsetMixin")
                || mixinClassName.endsWith("MebahelMcaScreenMixin")) return loaded("mebahelrpgrquest");
        if (mixinClassName.contains(".Mebahel")) return loaded("mebahelrpgrquest") && loaded("mcareputation");
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
