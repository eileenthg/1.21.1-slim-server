package com.craisinlord.antos.content;

import com.craisinlord.antos.content.entity.RewardDropEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class AntOSObjects {
    public static Supplier<Item> FLOPPY_DISK = missing("floppy disk");
    public static Supplier<DataComponentType<ResourceLocation>> FLOPPY_DISK_COMPONENT = missing("floppy disk data component");
    public static Supplier<EntityType<RewardDropEntity>> REWARD_DROP_ENTITY = missing("reward drop entity");

    public static void bind(Supplier<Item> disk, Supplier<DataComponentType<ResourceLocation>> diskComponent,
                            Supplier<EntityType<RewardDropEntity>> rewardDropEntity) {
        FLOPPY_DISK = disk;
        FLOPPY_DISK_COMPONENT = diskComponent;
        REWARD_DROP_ENTITY = rewardDropEntity;
    }

    private static <T> Supplier<T> missing(String name) {
        return () -> { throw new IllegalStateException("AntOS registry not initialized: " + name); };
    }

    private AntOSObjects() {}
}
