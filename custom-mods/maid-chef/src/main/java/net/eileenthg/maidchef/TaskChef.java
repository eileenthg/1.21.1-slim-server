package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TaskChef implements IMaidTask {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MaidChef.MOD_ID, "chef");
    public static final TaskChef INSTANCE = new TaskChef();

    private TaskChef() {
    }

    public static ChefData data(EntityMaid maid) {
        return maid.getOrCreateData(ChefPlugin.CHEF_DATA, ChefData.defaults());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return Items.CAKE.getDefaultInstance();
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        // TLM appends its own behaviours to this list, so it must be mutable
        List<Pair<Integer, BehaviorControl<? super EntityMaid>>> list = new ArrayList<>();
        if (!maid.level().isClientSide) {
            list.add(Pair.of(5, new ChefBehavior()));
        }
        return list;
    }

    /** TLM's work meal would move whole stacks of ingredients into her hand and eat them. */
    @Override
    public boolean enableEating(EntityMaid maid) {
        return false;
    }

    /** She stays around the kitchen instead of strolling off between steps. */
    @Override
    public boolean enableLookAndRandomWalk(EntityMaid maid) {
        return false;
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        final int entityId = maid.getId();
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Maid Chef Config");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new ChefConfigContainer(index, playerInventory, entityId);
            }

            @Override
            public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                return false;
            }
        };
    }
}
