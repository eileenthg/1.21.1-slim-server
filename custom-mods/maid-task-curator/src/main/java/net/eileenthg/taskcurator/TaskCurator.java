package net.eileenthg.taskcurator;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = TaskCurator.MOD_ID, dist = Dist.CLIENT)
public final class TaskCurator {
    public static final String MOD_ID = "taskcurator";
    public static final Logger LOG = LoggerFactory.getLogger("TaskCurator");

    private static CuratorLayout layout;

    public TaskCurator(IEventBus modBus) {
        modBus.addListener(FMLLoadCompleteEvent.class, e -> e.enqueueWork(TaskCurator::logLayout));
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("taskcurator.json");
    }

    private static List<String> registeredIds() {
        List<String> ids = new ArrayList<>();
        for (IMaidTask t : TaskManager.getTaskIndex()) {
            ids.add(t.getUid().toString());
        }
        return ids;
    }

    /** The layout, loaded on first use and reconciled with the tasks registered this session. */
    public static CuratorLayout layout() {
        if (layout == null) {
            CuratorLayout loaded = null;
            try {
                loaded = CuratorLayout.load(file());
            } catch (IOException e) {
                LOG.error("Could not read {}; keeping it as .broken and starting from the default layout", file(), e);
                try {
                    Files.move(file(), file().resolveSibling("taskcurator.json.broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                }
            }
            boolean fresh = loaded == null;
            layout = fresh ? CuratorLayout.defaults() : loaded;
            List<String> added = layout.reconcile(registeredIds());
            if (!added.isEmpty()) {
                LOG.info("Placed {} task(s) not in the layout at the top level: {}", added.size(), added);
            }
            if (fresh || !added.isEmpty()) {
                save();
            }
        }
        return layout;
    }

    public static void save() {
        if (layout == null) {
            return;
        }
        try {
            layout.save(file());
        } catch (IOException e) {
            LOG.error("Could not save {}", file(), e);
        }
    }

    public static void resetToDefaults() {
        layout = CuratorLayout.defaults();
        layout.reconcile(registeredIds());
        save();
    }

    private static void logLayout() {
        List<String> ids = registeredIds();
        LOG.info("{} maid tasks registered: {}", ids.size(), ids);
        layout();
    }
}
