package eteil.mcarpgbridge.mixin;

import dev.otectus.mcareputation.api.McaReputationApi;
import eteil.mcarpgbridge.McaRpgBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * MCA: Quests 1.6.5 only binds to MCA: Reputation when getApiVersion() == 1, but the NeoForge build of
 * Reputation reports 2 (its changelog: "Forge stays at 1, NeoForge at 2", no signature changes). Every
 * Reputation member Quests calls resolves in Reputation 0.5.0, so v2 is presented to Quests as v1.
 * Any other version is passed through, and Quests keeps rejecting it.
 */
@Pseudo
@Mixin(targets = "dev.otectus.mcaquests.compat.reputation.CanonicalReputationBackend", remap = false)
public abstract class QuestsReputationVersionMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Ldev/otectus/mcareputation/api/McaReputationApi;getApiVersion()I"), remap = false)
    private int mcarpgbridge$acceptNeoForgeApi() {
        int version = McaReputationApi.getApiVersion();
        if (version == 2) {
            McaRpgBridge.LOGGER.info("[MCA RPG Bridge] MCA: Reputation reports API v2 (NeoForge); letting MCA: Quests bind to it as v1");
            return 1;
        }
        return version;
    }
}
