package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.inventory.container.task.TaskConfigContainer;
import net.minecraft.world.entity.player.Inventory;

public final class ChefConfigContainer extends TaskConfigContainer {
    public ChefConfigContainer(int id, Inventory inventory, int entityId) {
        super(MaidChef.CHEF_CONFIG_MENU.get(), id, inventory, entityId);
    }
}
