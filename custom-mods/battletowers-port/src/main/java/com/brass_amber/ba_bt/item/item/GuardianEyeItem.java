package com.brass_amber.ba_bt.item.item;

import com.brass_amber.ba_bt.BABattleTowers;
import com.brass_amber.ba_bt.util.GolemType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GuardianEyeItem extends Item {
	private GolemType golemType;

	public GuardianEyeItem(GolemType golemType, Item.Properties builder) {
		super(builder);
		this.golemType = golemType;
	}
	
	public GolemType getGolemType() {
		return this.golemType;
	}

	@Override
		public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		if (Screen.hasShiftDown()) {
			tooltip.add(Component.translatable("tooltip.ba_bt." + this.golemType.getLowercaseName() + "_eye").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(BABattleTowers.HOLD_SHIFT_TOOLTIP);
		}
	}
}
