package com.github.eileenthg.tamablearcadianfairy;

import com.github.eileenthg.tamablearcadianfairy.compat.TouhouLittleMaidPower;
import com.github.eileenthg.tamablearcadianfairy.data.ModAttachments;
import com.github.eileenthg.tamablearcadianfairy.data.ModGameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lets players tame the fairies from Arcadian Dream, in the style of Tamable Fairy
 * for Touhou Little Maid.
 */
@Mod(TamableArcadianFairy.MOD_ID)
public class TamableArcadianFairy {

    public static final String MOD_ID = "tamablearcadianfairy";
    public static final Logger LOGGER = LoggerFactory.getLogger("Tamable Arcadian Fairy");

    public TamableArcadianFairy(IEventBus modBus) {
        ModAttachments.register(modBus);
        ModGameRules.register();
        TouhouLittleMaidPower.init();
    }
}
