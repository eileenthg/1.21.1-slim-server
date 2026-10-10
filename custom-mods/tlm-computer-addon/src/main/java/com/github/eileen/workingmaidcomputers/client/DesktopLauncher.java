package com.github.eileen.workingmaidcomputers.client;

import com.craisinlord.antos.content.client.screen.ComputerScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Kept in its own class so the common event handler never loads client-only screen code on a dedicated server. */
@OnlyIn(Dist.CLIENT)
public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isAlive()) {
            ComputerScreen.openComputer();
        }
    }
}
