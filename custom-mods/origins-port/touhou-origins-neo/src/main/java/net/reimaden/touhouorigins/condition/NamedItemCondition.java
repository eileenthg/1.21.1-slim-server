package net.reimaden.touhouorigins.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.WrittenBookContent;
import net.reimaden.touhouorigins.TouhouOrigins;

import java.util.Locale;

/**
 * Matches an item the player has named, or a written book with a given title.
 *
 * <p>Exists because 1.21 moved both of those out of NBT and into components, and Apoli's
 * {@code apoli:nbt} item condition reads only {@code minecraft:custom_data} — a bag for modder
 * NBT that never contains a display name or a book title. Upstream's shikigami link matched
 * {@code {display:{Name:...}}} and {@code {title:Contract}}, which were the 1.20 root-NBT paths;
 * on this version those can never match, so the bond could not be sealed at all.
 *
 * <p>Checks both sources, because the two halves of that link need the same question asked of
 * different items: a lead renamed at an anvil carries {@code minecraft:custom_name}, while a
 * signed book carries its title inside {@code minecraft:written_book_content}.
 *
 * <p>Comparison ignores case, preserving upstream's habit of accepting both {@code Contract} and
 * {@code contract}. Set {@code exact} to true to require the exact casing instead.
 */
public final class NamedItemCondition implements ConditionType<ItemCtx, NamedItemCondition.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("named");

    public record Cfg(String name, boolean exact) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Cfg::name),
            Codec.BOOL.optionalFieldOf("exact", false).forGetter(Cfg::exact)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        Component customName = ctx.stack().get(DataComponents.CUSTOM_NAME);
        if (customName != null && matches(customName.getString(), cfg)) return true;

        WrittenBookContent book = ctx.stack().get(DataComponents.WRITTEN_BOOK_CONTENT);
        // The title is filterable: raw() is what the writer typed, which is what a player renaming
        // their book would expect to be matched.
        return book != null && matches(book.title().raw(), cfg);
    }

    private static boolean matches(String actual, Cfg cfg) {
        if (actual == null) return false;
        String trimmed = actual.trim();
        return cfg.exact()
            ? trimmed.equals(cfg.name())
            : trimmed.toLowerCase(Locale.ROOT).equals(cfg.name().toLowerCase(Locale.ROOT));
    }
}
