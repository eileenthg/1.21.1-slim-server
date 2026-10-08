package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.wallev.maidsoulkitchen.init.MkItems;
import com.github.wallev.maidsoulkitchen.inventory.container.item.BagType;
import com.github.wallev.maidsoulkitchen.item.ItemCulinaryHub;
import java.util.List;
import java.util.Map;
import net.eileenthg.maidchef.station.Inv;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** Only class-loaded when Maidsoul Kitchen is installed. */
final class MskHub {
    private MskHub() {
    }

    static HubChests.Bound read(EntityMaid maid) {
        ItemStack hub = maid.getMaidInv().getStackInSlot(Inv.HUB_SLOT);
        if (!hub.is(MkItems.CULINARY_HUB.get())) return HubChests.Bound.NONE;
        Map<BagType, List<BlockPos>> poses = ItemCulinaryHub.getBindPoses(hub);
        return new HubChests.Bound(
                List.copyOf(poses.getOrDefault(BagType.INGREDIENT, List.of())),
                List.copyOf(poses.getOrDefault(BagType.OUTPUT, List.of())));
    }
}
