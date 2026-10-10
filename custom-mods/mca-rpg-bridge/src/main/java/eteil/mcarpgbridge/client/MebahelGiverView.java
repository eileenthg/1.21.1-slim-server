package eteil.mcarpgbridge.client;

import net.mebahelrpgrquest.quest.QuestGiver;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/** Kept apart from McaMenuButton so Mebahel's classes are only touched once it is known to be installed. */
final class MebahelGiverView {
    private MebahelGiverView() {
    }

    /** Which button the villager should get, from Mebahel's synced entity data; null for none. */
    @Nullable
    static String buttonKey(Entity villager, String requestKey, String rumorKey) {
        if (!(villager instanceof QuestGiver giver)) {
            return null;
        }
        if (giver.mebahelrpgrquest$hasQuest()) {
            return requestKey;
        }
        return giver.mebahelrpgrquest$hasRumor() ? rumorKey : null;
    }
}
