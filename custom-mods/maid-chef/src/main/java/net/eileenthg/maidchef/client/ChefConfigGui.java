package net.eileenthg.maidchef.client;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.task.MaidTaskConfigGui;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.eileenthg.maidchef.ChefConfig;
import net.eileenthg.maidchef.ChefConfigContainer;
import net.eileenthg.maidchef.ChefData;
import net.eileenthg.maidchef.TaskChef;
import net.eileenthg.maidchef.net.ExplainPayloads;
import net.eileenthg.maidchef.net.RequestPayloads;
import net.eileenthg.maidchef.net.SetChefSettingsPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Chef task tab: Settings (numbers), Blacklist (foods she won't make), Status (what she's doing, recent dishes). */
public final class ChefConfigGui extends MaidTaskConfigGui<ChefConfigContainer> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "textures/gui/default_task_config.png");
    private static final int PER_PAGE = 5;
    private static final int EXPLAIN_ROWS = 7;
    private static int tab = 0;

    private int radius, target, recentSize, minNutrition, minEffects;
    private float minSaturation;
    private boolean serveFeasts;
    private String dish = "";
    private int scroll;
    private long requestedAt;
    private final List<String> blacklist = new ArrayList<>();
    private int page;
    private EditBox input;
    private String inputText = "";

    public ChefConfigGui(ChefConfigContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        ChefData d = TaskChef.data(this.getMaid());
        radius = d.radius();
        target = d.target();
        recentSize = d.recentSize();
        minNutrition = d.minNutrition();
        minSaturation = d.minSaturation();
        minEffects = d.minEffects();
        serveFeasts = d.serveFeasts();
        dish = d.dish();
        blacklist.addAll(d.blacklist());
    }

    private void send() {
        PacketDistributor.sendToServer(new SetChefSettingsPayload(this.getMaid().getId(), radius, target, recentSize, minNutrition,
                minSaturation, minEffects, List.copyOf(blacklist), serveFeasts, dish));
    }

    private static int step() {
        return Screen.hasShiftDown() ? 5 : 1;
    }

    private int x() {
        return this.leftPos + 88;
    }

    private int y() {
        return this.topPos + 34;
    }

    @Override
    protected void initAdditionWidgets() {
        int x = x(), y = y();
        String[] names = {"Setup", "No-cook", "Requests", "Status"};
        String[] tips = {"Kitchen radius, batch size, filters, feasts", "Foods this maid won't make",
                "Ingredients your maids asked for (shared by all your maids)", "What she's doing and why"};
        for (int i = 0; i < names.length; i++) {
            final int t = i;
            Button b = Button.builder(Component.literal(names[i]), btn -> {
                tab = t;
                this.init();
            }).pos(x + i * 40, y).size(39, 14).tooltip(Tooltip.create(Component.literal(tips[i]))).build();
            b.active = tab != i;
            this.addRenderableWidget(b);
        }
        input = null;
        if (tab == 0) initSettings(x, y + 18);
        else if (tab == 1) initBlacklist(x, y + 18);
        else if (tab == 2) initRequests(x, y + 18);
        else initStatus(x, y + 18);
    }

    // ------------------------------------------------------------------ settings

    private void row(int x, int y, IntSupplier get, IntConsumer set, int min, int max) {
        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            set.accept(Mth.clamp(get.getAsInt() - step(), min, max));
            send();
        }).pos(x + 122, y).size(18, 12).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            set.accept(Mth.clamp(get.getAsInt() + step(), min, max));
            send();
        }).pos(x + 142, y).size(18, 12).build());
    }

    private void initSettings(int x, int y) {
        row(x, y, () -> radius, v -> radius = v, 3, ChefConfig.MAX_RADIUS.get());
        row(x, y + 13, () -> target, v -> target = v, 1, 64);
        row(x, y + 26, () -> recentSize, v -> recentSize = v, 0, ChefData.MAX_RECENT);
        row(x, y + 39, () -> minNutrition, v -> minNutrition = v, 0, 20);
        // saturation moves in steps of 0.5 (shift: 2.5)
        row(x, y + 52, () -> Math.round(minSaturation * 2), v -> minSaturation = v / 2f, 0, 80);
        row(x, y + 65, () -> minEffects, v -> minEffects = v, 0, 10);
        this.addRenderableWidget(Button.builder(Component.literal(serveFeasts ? "On" : "Off"), b -> {
            serveFeasts = !serveFeasts;
            send();
            this.init();
        }).pos(x + 122, y + 78).size(38, 12).tooltip(Tooltip.create(Component.literal(
                "On: she places feasts and serves them into portions (bowls, rice bowls...).\nOff: she delivers the feast block itself."))).build());
        // "Prepare X": the item in your hand, or click a dish in the Status tab
        this.addRenderableWidget(Button.builder(Component.literal("Held"), b -> {
            ItemStack held = Minecraft.getInstance().player.getMainHandItem();
            if (!held.isEmpty()) {
                // a potion keeps its effect: she brews that one
                dish = net.eileenthg.maidchef.Variant.dishOf(held);
                send();
            }
        }).pos(x + 112, y + 92).size(30, 12).tooltip(Tooltip.create(Component.literal(
                "Prepare the item in your main hand instead of the best food.\nTip: you can also click a dish in the Status tab."))).build());
        this.addRenderableWidget(Button.builder(Component.literal("x"), b -> {
            dish = "";
            send();
        }).pos(x + 144, y + 92).size(16, 12).tooltip(Tooltip.create(Component.literal("Back to making the best food she can"))).build());
    }

    // ------------------------------------------------------------------ requests

    private int reqPage;
    private int seenVersion = -1;

    private void initRequests(int x, int y) {
        if (seenVersion < 0) PacketDistributor.sendToServer(new RequestPayloads.Fetch());
        seenVersion = RequestPayloads.VERSION;
        List<RequestPayloads.Row> rows = RequestPayloads.SYNCED;
        reqPage = Mth.clamp(reqPage, 0, Math.max(0, (rows.size() - 1) / PER_PAGE));
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = reqPage * PER_PAGE + i;
            if (idx >= rows.size()) break;
            RequestPayloads.Row row = rows.get(idx);
            int ry = y + 20 + i * 15;
            this.addRenderableWidget(Button.builder(Component.literal(row.blocked() ? "Blocked" : "Allowed"),
                            b -> PacketDistributor.sendToServer(new RequestPayloads.Change(row.item(), row.blocked() ? 1 : 0)))
                    .pos(x + 102, ry).size(42, 13)
                    .tooltip(Tooltip.create(Component.literal(row.blocked() ? "Maids won't ask for this. Click to allow it again."
                            : "Maids may ask for this. Click to block it."))).build());
            this.addRenderableWidget(Button.builder(Component.literal("x"),
                            b -> PacketDistributor.sendToServer(new RequestPayloads.Change(row.item(), 2)))
                    .pos(x + 146, ry).size(14, 13).tooltip(Tooltip.create(Component.literal("Forget this entry (it comes back if a maid asks again)"))).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            reqPage = Math.max(0, reqPage - 1);
            this.init();
        }).pos(x, y + 96).size(16, 13).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            reqPage = Math.min(Math.max(0, (RequestPayloads.SYNCED.size() - 1) / PER_PAGE), reqPage + 1);
            this.init();
        }).pos(x + 144, y + 96).size(16, 13).build());
    }

    private void renderRequests(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (seenVersion != RequestPayloads.VERSION) {
            this.init();
            return;
        }
        int gray = ChatFormatting.DARK_GRAY.getColor();
        List<RequestPayloads.Row> rows = RequestPayloads.SYNCED;
        g.drawString(this.font, "Asked for (all your maids):", x, y + 6, ChatFormatting.BLACK.getColor(), false);
        if (rows.isEmpty()) {
            g.drawWordWrap(this.font, Component.literal("Nothing yet. When a maid asks you for an ingredient it appears here, and you can block it."),
                    x, y + 22, 160, gray);
            return;
        }
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = reqPage * PER_PAGE + i;
            if (idx >= rows.size()) break;
            RequestPayloads.Row row = rows.get(idx);
            int ry = y + 20 + i * 15;
            ResourceLocation rl = ResourceLocation.tryParse(row.item());
            ItemStack stack = rl == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.get(rl).getDefaultInstance();
            g.renderItem(stack, x, ry - 2);
            String name = this.font.plainSubstrByWidth(stack.getHoverName().getString(), 82);
            g.drawString(this.font, name, x + 18, ry + 3, row.blocked() ? ChatFormatting.GRAY.getColor() : gray, false);
            if (mouseX >= x && mouseX < x + 100 && mouseY >= ry - 2 && mouseY < ry + 13) {
                ResourceLocation dish = ResourceLocation.tryParse(row.forDish());
                String dishName = dish == null || row.forDish().isEmpty() ? "?" : BuiltInRegistries.ITEM.get(dish).getDefaultInstance().getHoverName().getString();
                g.renderTooltip(this.font, this.font.split(Component.literal(stack.getHoverName().getString() + "\nLast asked for: " + dishName
                        + "\nAsked " + row.times() + " time" + (row.times() == 1 ? "" : "s")), 200), mouseX, mouseY);
            }
        }
        int pages = Math.max(1, (rows.size() + PER_PAGE - 1) / PER_PAGE);
        g.drawCenteredString(this.font, (reqPage + 1) + "/" + pages, x + 80, y + 99, 0xFFFFFF);
    }

    // ------------------------------------------------------------------ status

    private void request() {
        requestedAt = System.currentTimeMillis();
        PacketDistributor.sendToServer(new ExplainPayloads.Request(this.getMaid().getId()));
    }

    private void initStatus(int x, int y) {
        this.addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> {
            scroll = 0;
            request();
        }).pos(x + 116, y + 32).size(44, 12).tooltip(Tooltip.create(Component.literal("Ask her again what she can cook"))).build());
        if (System.currentTimeMillis() - requestedAt > 2000) request();
    }

    private List<String> explainLines() {
        return ExplainPayloads.RECEIVED.getOrDefault(this.getMaid().getId(), List.of("Asking her..."));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == 3 && button == 0) {
            int x = x(), top = y() + 18 + 47;
            List<String> lines = explainLines();
            int row = (int) Math.floor((mouseY - top) / 10.0);
            if (mouseX >= x && mouseX < x + 160 && row >= 0 && row < EXPLAIN_ROWS && scroll + row < lines.size()) {
                String raw = lines.get(scroll + row);
                int sep = raw.indexOf('\u0000');
                if (sep >= 0) {
                    dish = raw.substring(sep + 1);
                    send();
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("She'll prepare "
                            + BuiltInRegistries.ITEM.get(ResourceLocation.parse(dish)).getDefaultInstance().getHoverName().getString()
                            + " now (Setup tab: x to go back to best food)"), true);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == 3) {
            int max = Math.max(0, explainLines().size() - EXPLAIN_ROWS);
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------ blacklist

    private void initBlacklist(int x, int y) {
        input = new EditBox(this.font, x, y, 94, 14, Component.literal("Food id"));
        input.setMaxLength(128);
        input.setValue(inputText);
        input.setHint(Component.literal("mod:item or #tag").withStyle(ChatFormatting.DARK_GRAY));
        input.setResponder(s -> inputText = s);
        this.addRenderableWidget(input);
        this.addRenderableWidget(Button.builder(Component.literal("Add"), b -> addEntry(input.getValue()))
                .pos(x + 96, y - 1).size(28, 16).tooltip(Tooltip.create(Component.literal("Add the typed food id or #tag"))).build());
        this.addRenderableWidget(Button.builder(Component.literal("Held"), b -> {
            ItemStack held = Minecraft.getInstance().player.getMainHandItem();
            if (!held.isEmpty()) addEntry(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
        }).pos(x + 126, y - 1).size(34, 16).tooltip(Tooltip.create(Component.literal("Add the food in your main hand"))).build());

        page = Mth.clamp(page, 0, Math.max(0, (blacklist.size() - 1) / PER_PAGE));
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= blacklist.size()) break;
            String entry = blacklist.get(idx);
            Button b = Button.builder(Component.literal(shorten(displayName(entry), 23)), btn -> {
                blacklist.remove(entry);
                send();
                this.init();
            }).pos(x + 18, y + 19 + i * 15).size(142, 14)
                    .tooltip(Tooltip.create(Component.literal(entry + "\nClick to remove"))).build();
            this.addRenderableWidget(b);
        }
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page = Math.max(0, page - 1);
            this.init();
        }).pos(x, y + 96).size(16, 13).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page = Math.min(Math.max(0, (blacklist.size() - 1) / PER_PAGE), page + 1);
            this.init();
        }).pos(x + 144, y + 96).size(16, 13).build());
    }

    private void addEntry(String raw) {
        String e = raw.trim();
        if (e.isEmpty()) return;
        if (!e.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(e);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return;
            e = id.toString();
        }
        if (!blacklist.contains(e) && blacklist.size() < ChefData.MAX_BLACKLIST) {
            blacklist.add(e);
            send();
        }
        inputText = "";
        page = (blacklist.size() - 1) / PER_PAGE;
        this.init();
    }

    private static String displayName(String entry) {
        if (entry.startsWith("#")) return entry;
        ResourceLocation id = ResourceLocation.tryParse(entry);
        return id == null ? entry : BuiltInRegistries.ITEM.get(id).getDefaultInstance().getHoverName().getString();
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ------------------------------------------------------------------ render

    @Override
    protected void renderAddition(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        int x = x(), y = y() + 18;
        int gray = ChatFormatting.DARK_GRAY.getColor();
        if (tab == 0) {
            String[] labels = {
                    "Kitchen radius: " + radius,
                    "Dishes per round: " + target,
                    "Remember last: " + recentSize,
                    "Min nutrition: " + minNutrition,
                    "Min saturation: " + String.format("%.1f", minSaturation),
                    "Min good effects: " + minEffects,
                    "Serve feasts:"};
            for (int i = 0; i < labels.length; i++) g.drawString(this.font, labels[i], x, y + 2 + i * 13, gray, false);
            g.drawString(this.font, "Prepare:", x, y + 94, ChatFormatting.BLACK.getColor(), false);
            ItemStack shown = dish.isEmpty() ? ItemStack.EMPTY : net.eileenthg.maidchef.Variant.display(dish);
            if (shown.isEmpty()) {
                g.drawString(this.font, "best food", x + 44, y + 94, gray, false);
            } else {
                ItemStack stack = shown;
                g.renderItem(stack, x + 42, y + 90);
                g.drawString(this.font, this.font.plainSubstrByWidth(stack.getHoverName().getString(), 50), x + 60, y + 94, ChatFormatting.DARK_GREEN.getColor(), false);
                if (mouseX >= x + 42 && mouseX < x + 110 && mouseY >= y + 90 && mouseY < y + 106) g.renderTooltip(this.font, stack, mouseX, mouseY);
            }
            g.drawString(this.font, "Shift-click: 5 at a time", x, y + 110, ChatFormatting.GRAY.getColor(), false);
        } else if (tab == 1) {
            for (int i = 0; i < PER_PAGE; i++) {
                int idx = page * PER_PAGE + i;
                if (idx >= blacklist.size()) break;
                String entry = blacklist.get(idx);
                ItemStack icon = Items.BARRIER.getDefaultInstance();
                if (!entry.startsWith("#")) {
                    ResourceLocation id = ResourceLocation.tryParse(entry);
                    if (id != null) icon = BuiltInRegistries.ITEM.get(id).getDefaultInstance();
                }
                g.renderItem(icon, x, y + 18 + i * 15);
            }
            if (blacklist.isEmpty()) {
                g.drawWordWrap(this.font, Component.literal("No blocked foods. She won't make anything added here."), x, y + 24, 160, gray);
            }
            int pages = Math.max(1, (blacklist.size() + PER_PAGE - 1) / PER_PAGE);
            g.drawCenteredString(this.font, (page + 1) + "/" + pages, x + 80, y + 99, 0xFFFFFF);
        } else if (tab == 2) {
            renderRequests(g, x, y, mouseX, mouseY);
        } else {
            ChefData d = TaskChef.data(this.getMaid());
            String status = d.status().isEmpty() ? "Starting up..." : d.status();
            List<net.minecraft.util.FormattedCharSequence> wrapped = this.font.split(Component.literal(status), 160);
            for (int i = 0; i < Math.min(2, wrapped.size()); i++) g.drawString(this.font, wrapped.get(i), x, y + i * 9, gray, false);
            if (wrapped.size() > 2 && mouseX >= x && mouseX < x + 160 && mouseY >= y && mouseY < y + 18) {
                g.renderTooltip(this.font, this.font.split(Component.literal(status), 200), mouseX, mouseY);
            }
            int ix = x;
            for (String id : d.recent()) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl == null) continue;
                ItemStack stack = BuiltInRegistries.ITEM.get(rl).getDefaultInstance();
                g.renderItem(stack, ix, y + 19);
                if (mouseX >= ix && mouseX < ix + 16 && mouseY >= y + 19 && mouseY < y + 35) g.renderTooltip(this.font, stack, mouseX, mouseY);
                ix += 17;
                if (ix > x + 96) break;
            }
            if (d.recent().isEmpty()) g.drawString(this.font, "Nothing made yet", x, y + 23, ChatFormatting.GRAY.getColor(), false);

            // why she is (or isn't) cooking, from the server; scroll with the mouse wheel
            List<String> lines = explainLines();
            scroll = Mth.clamp(scroll, 0, Math.max(0, lines.size() - EXPLAIN_ROWS));
            int top = y + 47;
            g.fill(x - 2, top - 2, x + 162, top + EXPLAIN_ROWS * 10, 0x22000000);
            String hover = null;
            for (int i = 0; i < EXPLAIN_ROWS && scroll + i < lines.size(); i++) {
                String raw = lines.get(scroll + i);
                int sep = raw.indexOf('\u0000');
                String line = sep >= 0 ? raw.substring(0, sep) : raw;
                boolean hot = sep >= 0 && mouseX >= x && mouseX < x + 160 && mouseY >= top + i * 10 && mouseY < top + i * 10 + 10;
                if (hot) g.fill(x - 1, top + i * 10 - 1, x + 161, top + i * 10 + 9, 0x2200AA00);
                String shown = this.font.plainSubstrByWidth(line, 160);
                int color = line.startsWith("Now:") || line.startsWith("Next") ? ChatFormatting.DARK_GREEN.getColor()
                        : line.startsWith("Would ask") ? ChatFormatting.DARK_AQUA.getColor()
                        : line.endsWith(":") || line.startsWith("Kitchen") ? ChatFormatting.BLACK.getColor() : gray;
                g.drawString(this.font, shown, x, top + i * 10, color, false);
                if (!shown.equals(line) && mouseX >= x && mouseX < x + 160 && mouseY >= top + i * 10 && mouseY < top + i * 10 + 10) hover = line;
            }
            if (lines.size() > EXPLAIN_ROWS) {
                g.drawString(this.font, (scroll + 1) + "-" + Math.min(lines.size(), scroll + EXPLAIN_ROWS) + "/" + lines.size() + " (scroll)",
                        x, top + EXPLAIN_ROWS * 10 + 1, ChatFormatting.GRAY.getColor(), false);
            }
            if (hover != null) g.renderTooltip(this.font, this.font.split(Component.literal(hover.trim() + "\n(click: prepare this)"), 220), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);
        graphics.blit(BG, this.leftPos + 80, this.topPos + 28, 0, 0, this.imageWidth, 137);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (input != null && input.isFocused()) {
            if (keyCode == 256) {
                this.onClose();
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                addEntry(input.getValue());
                return true;
            }
            return input.keyPressed(keyCode, scanCode, modifiers) || input.canConsumeInput() || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
