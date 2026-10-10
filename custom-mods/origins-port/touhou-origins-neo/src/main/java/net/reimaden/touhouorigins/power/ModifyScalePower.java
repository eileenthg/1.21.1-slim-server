package net.reimaden.touhouorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.scale.RelativeScaleModifier;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;

import java.util.List;

/**
 * Replacement for {@code voile:modify_scale}, used only where vanilla attributes cannot
 * reach — currently just {@code bake_danuki/smelly}'s {@code pehkui:visibility}.
 *
 * <p><b>Relative, not absolute.</b> Voile's version called {@code ScaleData.setScale(x)} every
 * tick, which clobbers whatever else set the scale, and its {@code onAdded} reset every other
 * non-persistent scale type. That fights Origins: Player Scale, which drives Pehkui's <i>base</i>
 * scale through {@code /scale multiply}. Instead we attach a {@link RelativeScaleModifier} to
 * Pehkui's base-value-modifier layer, so our factor composes on top of whatever base is in effect:
 * a micro player at 0.5 visibility becomes 1.0 with this power's x2, not a flat 2.0.
 *
 * <p>The modifier itself is stateless and shared; it reads the live power state each time it is
 * consulted, so this class only has to keep it <i>attached</i>.
 */
public final class ModifyScalePower extends PowerType<ModifyScalePower.Cfg> {

    /** Voile's DEFAULT_SCALE_TYPES, preserved so untyped uses behave the same. */
    public static final List<ResourceLocation> DEFAULT_SCALE_TYPES = List.of(
        pehkui("width"), pehkui("height"), pehkui("drops"), pehkui("visibility"));

    private static ResourceLocation pehkui(String path) {
        return ResourceLocation.fromNamespaceAndPath("pehkui", path);
    }

    public record Cfg(List<ResourceLocation> scaleTypes, float scale) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.listOf().optionalFieldOf("scale_types", DEFAULT_SCALE_TYPES).forGetter(Cfg::scaleTypes),
            Codec.FLOAT.fieldOf("scale").forGetter(Cfg::scale)
        ).apply(i, Cfg::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        attach(cfg, holder);
    }

    /**
     * Re-assert the modifier. Pehkui's {@code ScaleData.resetScale()} clears <i>all</i> base value
     * modifiers, and Player Scale calls {@code /scale reset} in every {@code entity_action_removed},
     * i.e. on every origin swap — so an attach-once approach silently stops working. Re-adding to a
     * {@code SortedSet} is a no-op when already present, so this stays cheap.
     */
    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        attach(cfg, holder);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        Entity entity = holder.rawOwner();
        if (entity == null) return;
        for (ResourceLocation typeId : cfg.scaleTypes()) {
            ScaleType type = ScaleRegistries.SCALE_TYPES.get(typeId);
            if (type == null) continue;
            ScaleData data = type.getScaleData(entity);
            if (data.getBaseValueModifiers().remove(RelativeScaleModifier.INSTANCE)) {
                data.onUpdate();
            }
        }
        // Any other still-active power of this type re-attaches on its next tick.
    }

    private static void attach(Cfg cfg, PowerContainer holder) {
        Entity entity = holder.rawOwner();
        if (entity == null) return;
        for (ResourceLocation typeId : cfg.scaleTypes()) {
            ScaleType type = ScaleRegistries.SCALE_TYPES.get(typeId);
            if (type == null) continue;   // unknown scale type id — ignore rather than crash
            ScaleData data = type.getScaleData(entity);
            if (data.getBaseValueModifiers().add(RelativeScaleModifier.INSTANCE)) {
                data.onUpdate();
            }
        }
    }
}
