package net.eileenthg.gravetagcleaner;

import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Kept separate so Curios classes are only loaded when Curios is present. */
final class CuriosStrip {
    private CuriosStrip() {}

    static int strip(ServerPlayer player) {
        var handler = CuriosApi.getCuriosInventory(player);
        if (handler.isEmpty()) return 0;
        int n = 0;
        for (ICurioStacksHandler slots : handler.get().getCurios().values()) {
            n += strip(slots.getStacks());
            n += strip(slots.getCosmeticStacks());
        }
        return n;
    }

    private static int strip(IDynamicStackHandler stacks) {
        int n = 0;
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (GraveTagCleaner.strip(stacks.getStackInSlot(i))) n++;
        }
        return n;
    }
}
