package com.github.eileen.workingmaidcomputers;

import com.craisinlord.antos.content.AntOSObjects;
import com.craisinlord.antos.content.computer.AccountWorkspace;
import com.craisinlord.antos.content.computer.ComputerDesktopState;
import com.craisinlord.antos.content.computer.ComputerTaskProgress;
import com.craisinlord.antos.content.computer.ComputerWorkspaceData;
import com.craisinlord.antos.content.guide.ComputerGuideData;
import com.craisinlord.antos.content.network.AnternetAccountHandler;
import com.craisinlord.antos.content.network.ComputerAccessHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Installs a floppy disk into the player's Anternet profile and consumes it. The vendored AntOS has no computer
 * block to slide a disk into, so disks go straight to the account that is signed in.
 */
public final class FloppyInstaller {
    private FloppyInstaller() {
    }

    public static boolean install(ServerPlayer player, ItemStack floppy) {
        ComputerWorkspaceData.AccountInfo account = AnternetAccountHandler.session(player);
        if (account == null) {
            player.displayClientMessage(Component.translatable("message.workingmaidcomputers.sign_in_first"), true);
            return false;
        }
        ResourceLocation diskId = floppy.get(AntOSObjects.FLOPPY_DISK_COMPONENT.get());
        if (diskId == null) {
            player.displayClientMessage(Component.translatable("message.workingmaidcomputers.empty_disk"), true);
            return false;
        }
        ComputerWorkspaceData data = ComputerWorkspaceData.access(player.serverLevel().getServer());
        if (data.account(account.accountId()) == null) {
            return false;
        }
        if (data.profileDisks(account.accountId()).contains(diskId)) {
            player.displayClientMessage(Component.translatable("message.workingmaidcomputers.disk_installed"), true);
            return false;
        }
        if (!data.addProfileDisk(account.accountId(), diskId)) {
            return false;
        }

        ComputerGuideData.Disk disk = ComputerGuideData.disk(diskId);
        CompoundTag snapshot = data.workspaceSnapshot(account.workspaceId());
        if (snapshot == null) {
            snapshot = new CompoundTag();
        }
        ComputerTaskProgress progress = new ComputerTaskProgress();
        progress.load(snapshot.getCompound("TaskProgress"));
        ComputerDesktopState desktop = new ComputerDesktopState();
        desktop.load(snapshot.getCompound("Desktop"));
        if (disk != null) {
            disk.tasks().forEach(progress::grant);
            disk.entries().forEach(progress::unlockArchiveEntry);
            if (!disk.wallpaper().isBlank()) {
                try {
                    ResourceLocation wallpaperId = ResourceLocation.parse(disk.wallpaper());
                    if (ComputerGuideData.wallpaper(wallpaperId) != null) {
                        desktop.unlockWallpaper(wallpaperId);
                    }
                } catch (RuntimeException ignored) {
                    // 壁纸 ID 无效时只跳过壁纸，不影响磁盘其余内容
                }
            }
        }
        snapshot.put("TaskProgress", progress.save());
        snapshot.put("Desktop", desktop.save());
        data.saveWorkspaceSnapshot(account.workspaceId(), snapshot);
        floppy.shrink(1);
        ComputerAccessHandler.sendArchive(player, new AccountWorkspace(player, account));
        player.displayClientMessage(Component.translatable("message.workingmaidcomputers.disk_added"), true);
        return true;
    }
}
