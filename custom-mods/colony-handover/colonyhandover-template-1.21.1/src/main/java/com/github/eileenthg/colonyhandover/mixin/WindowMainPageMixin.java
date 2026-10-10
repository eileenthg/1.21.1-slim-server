package com.github.eileenthg.colonyhandover.mixin;

import com.github.eileenthg.colonyhandover.client.TownHallButton;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.townhall.WindowMainPage;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the Grant Independence button on the town hall's Actions tab.
 *
 * <p>Deliberately one line. This is the only place the mod reaches inside MineColonies'
 * own classes, and upstream ships snapshots almost daily, so all it does is hand the
 * finished window to {@link TownHallButton}. Injecting at the constructor's tail means
 * MineColonies has already built and wired its own panes.
 *
 * <p>{@code remap = false}: MineColonies is not obfuscated, so there is nothing to remap.
 */
@Mixin(value = WindowMainPage.class, remap = false)
public abstract class WindowMainPageMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void colonyhandover$addGrantButton(BuildingTownHall.View building, CallbackInfo ci) {
        TownHallButton.add((AbstractWindowSkeleton) (Object) this,
                building.getColony().getID(), building.getColony().getName());
    }
}
