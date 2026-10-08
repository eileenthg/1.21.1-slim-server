package net.eileenthg.maidchef.station;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** A stand-in player for stations that only work through a player (stirring, grinding, cranking). */
public final class Fakes {
    private static final GameProfile PROFILE = new GameProfile(UUID.nameUUIDFromBytes("maidchef-hands".getBytes()), "[Maid Chef]");

    private Fakes() {
    }

    /** Her "hands": an empty fake player standing where she stands, holding {@code held}. */
    public static FakePlayer hands(ServerLevel level, EntityMaid maid, ItemStack held) {
        FakePlayer fp = FakePlayerFactory.get(level, PROFILE);
        fp.getInventory().clearContent();
        fp.moveTo(maid.getX(), maid.getY(), maid.getZ(), maid.getYRot(), maid.getXRot());
        fp.setItemInHand(InteractionHand.MAIN_HAND, held);
        return fp;
    }

    /** Move everything the fake player holds into the maid's inventory. Returns how many of {@code result} that was. */
    public static int drain(FakePlayer fp, EntityMaid maid, Item result, Map<Item, Integer> collected, boolean skipHeld) {
        int got = 0;
        Inventory inv = fp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (skipHeld && i == inv.selected) continue;
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            inv.setItem(i, ItemStack.EMPTY);
            collected.merge(s.getItem(), s.getCount(), Integer::sum);
            if (s.is(result)) got += s.getCount();
            Inv.giveOrDrop(maid, s);
        }
        return got;
    }

    public static void clear(FakePlayer fp) {
        fp.getInventory().clearContent();
    }
}
