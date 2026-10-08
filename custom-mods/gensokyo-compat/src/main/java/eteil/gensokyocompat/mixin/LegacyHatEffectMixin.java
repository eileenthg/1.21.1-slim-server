package eteil.gensokyocompat.mixin;

import dev.xkmc.l2core.init.reg.registrate.LegacyHolder;
import eteil.gensokyocompat.Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Legacy's Suwako / Koishi hats grant YH's Native God Bless / Unconscious instead of Legacy's duplicates. */
@Mixin(targets = {"dev.xkmc.gensokyolegacy.content.item.character.SuwakoHatItem",
        "dev.xkmc.gensokyolegacy.content.item.character.KoishiHatItem"}, remap = false)
public abstract class LegacyHatEffectMixin {
    @Redirect(method = "tick", require = 0, at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETSTATIC,
            target = "Ldev/xkmc/gensokyolegacy/init/registrate/GLEffects;NATIVE:Ldev/xkmc/l2core/init/reg/registrate/LegacyHolder;"))
    private LegacyHolder<?> gensokyocompat$yhNative() {
        return Bridge.yhEffect("NATIVE");
    }

    @Redirect(method = "tick", require = 0, at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETSTATIC,
            target = "Ldev/xkmc/gensokyolegacy/init/registrate/GLEffects;UNCONSCIOUS:Ldev/xkmc/l2core/init/reg/registrate/LegacyHolder;"))
    private LegacyHolder<?> gensokyocompat$yhUnconscious() {
        return Bridge.yhEffect("UNCONSCIOUS");
    }
}
