package dev.eteil.butcheryam2.test;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.block.AbstractCarcassBlock;
import dev.eteil.butcheryam2.blockentity.DrawnCarcassBlockEntity;
import dev.eteil.butcheryam2.butcher.Butchering;
import dev.eteil.butcheryam2.butcher.MobDef;
import dev.eteil.butcheryam2.butcher.MobDefs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Development only, off unless {@code -Dbutcheryam2.showcase=true}: lays every creature's stages
 * out in a row and has the client photograph each row, so the converted models can be checked
 * without playing. Optional {@code -Dbutcheryam2.showcase.only=a,b} limits it to some creatures.
 * Screenshots land in the game directory's {@code screenshots/}.
 */
public final class Showcase {

    private static final boolean ON = Boolean.getBoolean("butcheryam2.showcase");
    private static final String ONLY = System.getProperty("butcheryam2.showcase.only", "");
    private static final int Y = -60;
    private static final int ROW = 14;

    private record Placement(BlockPos pos, BlockState state, int count, int cuts) {}

    /** One creature's row, and where the camera stands to photograph it. */
    private record Row(String name, List<Placement> blocks, double camX, double camY, double camZ, float pitch) {}

    private static List<MobDef> mobs() {
        if (ONLY.isBlank()) {
            return MobDefs.ALL;
        }
        List<String> keep = List.of(ONLY.split(","));
        return MobDefs.ALL.stream().filter(d -> keep.contains(d.name())).toList();
    }

    private static List<Row> rows() {
        List<Row> out = new ArrayList<>();
        int z = 0;
        for (MobDef def : mobs()) {
            List<Placement> p = new ArrayList<>();
            int width;
            switch (def.kind()) {
                case FULL -> {
                    int[] ground = {0, 6, 7, 8, 9};
                    for (int i = 0; i < 5; i++) {
                        p.add(place(i * 4, Y, z, def.drained().get(), ground[i]));
                        p.add(place(i * 4, Y + 3, z + 6, def.drained().get(), i + 1));
                    }
                    p.add(place(20, Y, z, def.head().get(), -1));
                    width = 20;
                }
                case STAGED -> {
                    int[] ground = {0, 6, 7};
                    for (int i = 0; i < 3; i++) {
                        p.add(place(i * 4, Y, z, def.carcass().get(), ground[i]));
                        p.add(place(i * 4, Y + 3, z + 6, def.carcass().get(), i + 1));
                    }
                    if (def.head() != null) {
                        p.add(place(12, Y, z, def.head().get(), -1));
                    }
                    width = 12;
                }
                case STEPS -> {
                    int n = dev.eteil.butcheryam2.gen.StepSpecs.STEPS.get(def.name()).size();
                    for (int i = 0; i < n; i++) {
                        p.add(place(i * 4, Y, z, def.carcass().get(), 2 * i));
                        p.add(place(i * 4, Y + 3, z + 6, def.carcass().get(), 2 * i + 1));
                    }
                    width = Math.max(4, (n - 1) * 4);
                }
                case SINGLE -> {
                    p.add(place(0, Y, z, def.carcass().get(), 0));
                    p.add(place(4, Y, z, def.carcass().get(), 1));
                    width = 4;
                }
                case SEGMENTED -> {
                    // the void worm lies over a dozen blocks, like Butchery's Ender Dragon
                    int gap = def.name().startsWith("void_worm") ? (def.name().equals("void_worm") ? 30 : 14) : 5;
                    int[] cuts = {0, 1, 3};
                    for (int i = 0; i < cuts.length; i++) {
                        p.add(new Placement(new BlockPos(i * gap, Y, z), def.carcass().get().defaultBlockState(), 0, cuts[i]));
                    }
                    if (def.head() != null) {
                        p.add(place(3 * gap, Y, z, def.head().get(), -1));
                    }
                    width = 3 * gap;
                }
                default -> { // LARGE: whole, after the first cut, after the knife, near the end
                    int[] cuts = {0, 1, 2, 3};
                    for (int i = 0; i < cuts.length; i++) {
                        p.add(new Placement(new BlockPos(i * 10, Y, z), def.carcass().get().defaultBlockState(), 0, cuts[i]));
                    }
                    if (def.head() != null) {
                        p.add(place(40, Y, z, def.head().get(), -1));
                    }
                    width = 40;
                }
            }
            boolean large = def.kind() == MobDef.Kind.LARGE || def.name().equals("void_worm");
            boolean segmented = def.kind() == MobDef.Kind.SEGMENTED && !large;
            boolean worm = def.name().equals("void_worm");
            out.add(new Row(def.name(), p, width / 2.0 + 0.5, Y + (worm ? 40 : large ? 14 : segmented ? 5 : 4.5),
                    z - (worm ? 34 : large ? 18 : segmented ? 7 : 9.5), worm ? 48 : large ? 38 : segmented ? 34 : 18));
            if (def.kind() == MobDef.Kind.STEPS) {
                // the tool hint, seen empty-handed in front of the whole carcass
                out.add(new Row("hint_" + def.name(), List.of(), 0.5, Y, z - 2.5, 25));
            }
            if (!large && !segmented && def.kind() != MobDef.Kind.SINGLE) {
                // a close look at the hung row and its hooks
                out.add(new Row(def.name() + "_hung", List.of(), width / 2.0 + 0.5, Y + 4.5, z + 6 - 7.5, 8));
            }
            z += large ? ROW * 3 : ROW;
        }
        // one mob turned each way, to check the facing rotation of models, shapes and renderers
        List<Placement> facings = new ArrayList<>();
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int i = 0; i < 4; i++) {
            MobDef gazelle = MobDefs.get("gazelle");
            facings.add(new Placement(new BlockPos(0, Y, z + i * 5),
                    Butchering.withStage(gazelle.drained().get().defaultBlockState(), 0)
                            .setValue(AbstractCarcassBlock.FACING, dirs[i]), 0, 0));
            facings.add(new Placement(new BlockPos(8, Y, z + i * 5),
                    MobDefs.get("anaconda").carcass().get().defaultBlockState()
                            .setValue(AbstractCarcassBlock.FACING, dirs[i]), 0, 0));
        }
        out.add(new Row("facings", facings, 4.5, Y + 14, z - 6, 55));
        return out;
    }

    private static Placement place(int x, int y, int z, Block block, int stage) {
        BlockState state = stage < 0 ? block.defaultBlockState() : Butchering.withStage(block.defaultBlockState(), stage);
        return new Placement(new BlockPos(x, y, z), state, 0, 0);
    }

    @EventBusSubscriber(modid = ButcheryAlexsMobs2.MODID)
    public static final class Server {
        @SubscribeEvent
        public static void onStarted(ServerStartedEvent event) {
            if (Boolean.getBoolean("butcheryam2.halt")) {
                event.getServer().halt(false); // world generation only
                return;
            }
            if (!ON) {
                return;
            }
            MinecraftServer server = event.getServer();
            ServerLevel level = server.overworld();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            level.setDayTime(6000);
            int maxZ = 0;
            for (Row row : rows()) {
                for (Placement p : row.blocks()) {
                    level.setBlock(p.pos(), p.state(), 3);
                    maxZ = Math.max(maxZ, p.pos().getZ());
                    if (p.pos().getY() == Y + 3) {
                        // hung carcasses hang below a Butchery hook, with a second hook beside it
                        Block hook = dev.eteil.butcheryam2.butcher.ButcherUtil.block(dev.eteil.butcheryam2.butcher.ButcherUtil.HOOK);
                        level.setBlock(p.pos().above(), hook.defaultBlockState(), 3);
                        level.setBlock(p.pos().above().east(), hook.defaultBlockState(), 3);
                    }
                    if (level.getBlockEntity(p.pos()) instanceof DrawnCarcassBlockEntity be) {
                        if (p.count() > 0) {
                            be.getPersistentData().putInt(DrawnCarcassBlockEntity.COUNT, p.count());
                        }
                        be.setCuts(p.cuts());
                    }
                }
            }
            // a plain floor under everything, so the photos have a ground line
            for (int bx = -12; bx < 60; bx++) {
                for (int bz = -30; bz < maxZ + 20; bz++) {
                    level.setBlock(new BlockPos(bx, Y - 1, bz), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                }
            }
        }
    }

    @EventBusSubscriber(modid = ButcheryAlexsMobs2.MODID, value = Dist.CLIENT)
    public static final class Client {
        private static int tick;
        private static int shot;
        private static final int SETTLE = 60;

        private static int index() {
            return Math.max(0, (tick - 100) / SETTLE);
        }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (!ON || mc.player == null || mc.getSingleplayerServer() == null) {
                return;
            }
            List<Row> rows = rows();
            tick++;
            boolean hint = index() < rows.size() && rows.get(index()).name().startsWith("hint_");
            mc.options.hideGui = !hint;
            if (tick < 100) {
                return; // let the world finish loading
            }
            int local = (tick - 100) % SETTLE;
            int index = (tick - 100) / SETTLE;
            if (index >= rows.size()) {
                if (local == 0) {
                    mc.stop();
                }
                return;
            }
            Row r = rows.get(index);
            if (local == 0) {
                List<String> cmds = new ArrayList<>();
                cmds.add(r.name().startsWith("hint_") ? "gamemode creative @a" : "gamemode spectator @a");
                cmds.add("clear @a");
                cmds.add(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 0 %.1f",
                        r.camX(), r.camY(), r.camZ(), r.pitch()));
                MinecraftServer server = mc.getSingleplayerServer();
                server.execute(() -> cmds.forEach(c -> server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(), c)));
            } else if (local == SETTLE - 1) {
                Screenshot.grab(mc.gameDirectory, String.format("showcase_%02d_%s.png", shot++, r.name()),
                        mc.getMainRenderTarget(), msg -> {});
            }
        }
    }

    private Showcase() {}
}
