package com.github.eileen.llvoices.mixin;

import com.github.eileen.llvoices.voice.SiteRouting;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatData;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MaidAIChatData.class, remap = false)
public abstract class MaidAIChatDataMixin {
    @Inject(method = "getLLMSite", at = @At("HEAD"), cancellable = true)
    private void llvoices$routeToConfiguredSite(CallbackInfoReturnable<LLMSite> cir) {
        LLMSite site = SiteRouting.override();
        if (site != null) {
            cir.setReturnValue(site);
        }
    }
}
