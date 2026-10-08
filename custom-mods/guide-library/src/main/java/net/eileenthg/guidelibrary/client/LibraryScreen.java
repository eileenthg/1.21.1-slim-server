package net.eileenthg.guidelibrary.client;

import net.eileenthg.guidelibrary.LibraryConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The library: every guide book in one searchable list. Left-click opens a book, right-click
 * favorites it. Featured books (from the common config) come first, then favorites, then the
 * rest alphabetically.
 */
public class LibraryScreen extends Screen {

    private static final int ROW_H = 24;
    private static final int PAD = 8;

    private static final int PANEL = 0xF0201A14;
    private static final int PANEL_BORDER = 0xFF8B6B3E;
    private static final int ROW_HOVER = 0x40FFFFFF;
    private static final int ROW_STRIPE = 0x14FFFFFF;
    private static final int GOLD = 0xFFE0B040;

    @Nullable private final Screen parent;
    private final List<BookEntry> books = new ArrayList<>();
    private final List<BookEntry> shown = new ArrayList<>();
    private List<? extends String> featured = List.of();
    private int total;

    private EditBox search;
    private int left, top, panelW, panelH;
    private int listTop, listBottom;
    private double scroll;
    private boolean draggingBar;

    public LibraryScreen(@Nullable Screen parent) {
        super(Component.translatable("guidelibrary.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<BookEntry> all = BookCatalog.collect();
        total = all.size();
        books.clear();
        books.addAll(BookCatalog.unlocked(all));
        featured = LibraryConfig.COMMON.featuredBooks.get();

        panelW = Math.min(width - 20, 300);
        panelH = Math.min(height - 20, 260);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;

        String previous = search == null ? "" : search.getValue();
        search = new EditBox(font, left + PAD, top + 22, panelW - PAD * 2, 16,
                Component.translatable("guidelibrary.search"));
        search.setHint(Component.translatable("guidelibrary.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(previous);
        search.setResponder(s -> refilter());
        addRenderableWidget(search);
        setInitialFocus(search);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + panelW / 2 - 50, top + panelH - 26, 100, 20).build());

        listTop = top + 44;
        listBottom = top + panelH - 32;
        refilter();
    }

    private void refilter() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (BookEntry e : books) {
            if (q.isEmpty() || e.sortName().contains(q)
                    || e.modName().toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(e);
            }
        }
        shown.sort(Comparator.comparingInt(this::rank).thenComparing(BookEntry::sortName));
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    /** Featured books in config order, then favorites, then everything else. */
    private int rank(BookEntry e) {
        for (int i = 0; i < featured.size(); i++) {
            if (LibraryConfig.matches(featured.get(i), e.key())) return i;
        }
        return Favorites.has(e.key()) ? 1000 : 2000;
    }

    private boolean isFeatured(BookEntry e) {
        return LibraryConfig.matchesAny(featured, e.key());
    }

    private int maxScroll() {
        return Math.max(0, shown.size() * ROW_H - (listBottom - listTop));
    }

    private boolean hasBar() {
        return maxScroll() > 0;
    }

    private int rowRight() {
        return left + panelW - PAD - (hasBar() ? 8 : 0);
    }

    @Nullable
    private BookEntry entryAt(double mx, double my) {
        if (mx < left + PAD || mx >= rowRight() || my < listTop || my >= listBottom) return null;
        int i = (int) ((my - listTop + scroll) / ROW_H);
        return i >= 0 && i < shown.size() ? shown.get(i) : null;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.fill(left - 1, top - 1, left + panelW + 1, top + panelH + 1, PANEL_BORDER);
        g.fill(left, top, left + panelW, top + panelH, PANEL);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);

        Component heading = title.copy().append(Component.literal("  ").append(
                Component.translatable("guidelibrary.found", books.size(), total)).withStyle(ChatFormatting.GRAY));
        g.drawString(font, heading, left + PAD, top + 8, 0xFFFFFFFF);

        int right = rowRight();
        g.enableScissor(left + PAD, listTop, right, listBottom);
        int first = (int) (scroll / ROW_H);
        for (int i = first; i < shown.size(); i++) {
            int y = listTop + i * ROW_H - (int) scroll;
            if (y >= listBottom) break;
            BookEntry e = shown.get(i);
            boolean hover = mx >= left + PAD && mx < right && my >= Math.max(y, listTop)
                    && my < Math.min(y + ROW_H, listBottom);
            if (hover) g.fill(left + PAD, y, right, y + ROW_H, ROW_HOVER);
            else if (i % 2 == 1) g.fill(left + PAD, y, right, y + ROW_H, ROW_STRIPE);

            if (!e.icon().isEmpty()) g.renderItem(e.icon(), left + PAD + 4, y + 4);
            int textX = left + PAD + 26;
            boolean fav = Favorites.has(e.key());
            int textW = right - textX - (fav ? 14 : 4);
            g.drawString(font, clip(e.name().getString(), textW), textX, y + 3, 0xFFFFFFFF);
            Component sub = isFeatured(e)
                    ? Component.translatable("guidelibrary.featured").withStyle(ChatFormatting.AQUA)
                    : Component.literal(e.modName()).withStyle(ChatFormatting.GRAY);
            g.drawString(font, clip(sub.getString(), textW), textX, y + 13,
                    isFeatured(e) ? 0xFF55FFFF : 0xFFA0A0A0);
            if (fav) g.drawString(font, "★", right - 11, y + 8, GOLD);
        }
        g.disableScissor();

        if (shown.isEmpty()) {
            String msg = total == 0 ? "guidelibrary.empty"
                    : books.isEmpty() ? "guidelibrary.none_found" : "guidelibrary.no_match";
            int y = listTop + 20;
            for (var line : font.split(Component.translatable(msg), panelW - PAD * 4)) {
                g.drawCenteredString(font, line, left + panelW / 2, y, 0xFFA0A0A0);
                y += 11;
            }
        }

        if (hasBar()) {
            int barX = left + panelW - PAD - 5;
            int trackH = listBottom - listTop;
            int thumbH = Math.max(16, trackH * trackH / (shown.size() * ROW_H));
            int thumbY = listTop + (int) ((trackH - thumbH) * (scroll / maxScroll()));
            g.fill(barX, listTop, barX + 5, listBottom, 0x40000000);
            g.fill(barX, thumbY, barX + 5, thumbY + thumbH, 0xFFA08050);
        }

        BookEntry hovered = entryAt(mx, my);
        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(hovered.name());
            tip.add(Component.literal(hovered.modName() + " · " + hovered.source()).withStyle(ChatFormatting.GRAY));
            if (minecraft != null && minecraft.options.advancedItemTooltips) {
                tip.add(Component.literal(hovered.key()).withStyle(ChatFormatting.DARK_GRAY));
            }
            tip.add(Component.translatable(Favorites.has(hovered.key())
                    ? "guidelibrary.tip.unfavorite" : "guidelibrary.tip.favorite").withStyle(ChatFormatting.DARK_AQUA));
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private String clip(String s, int w) {
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, Math.max(0, w - font.width("…"))) + "…";
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hasBar() && mx >= left + panelW - PAD - 6 && mx < left + panelW - PAD + 1
                && my >= listTop && my < listBottom) {
            draggingBar = true;
            dragBarTo(my);
            return true;
        }
        BookEntry e = entryAt(mx, my);
        if (e != null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                Favorites.toggle(e.key());
                refilter();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                playClick();
                e.opener().run();
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingBar) {
            dragBarTo(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingBar = false;
        return super.mouseReleased(mx, my, button);
    }

    private void dragBarTo(double my) {
        double f = (my - listTop) / (double) (listBottom - listTop);
        scroll = Mth.clamp(f * maxScroll(), 0, maxScroll());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Mth.clamp(scroll - sy * ROW_H, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !shown.isEmpty()) {
            shown.get(0).opener().run();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
