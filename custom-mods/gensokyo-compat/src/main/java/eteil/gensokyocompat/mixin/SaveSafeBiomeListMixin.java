package eteil.gensokyocompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Biolith registers each replacement biome (our magical forest) at an out-of-range climate point (3.01) so the
 * noise never picks it directly. TerraBlender then bakes the overworld's whole biome list, placeholder included,
 * into the biome source, and when level.dat is saved the point fails the codec's [-2, 2] check. Vanilla then
 * silently writes level.dat WITHOUT WorldGenSettings and the world no longer loads. Leave such points out when
 * encoding: they are unreachable by design, and Biolith adds them back on every startup.
 */
@Mixin(Climate.ParameterList.class)
public abstract class SaveSafeBiomeListMixin {
    private static final long MAX_COORD = Climate.quantizeCoord(2.0F);
    private static final long MAX_OFFSET = Climate.quantizeCoord(1.0F);

    @ModifyReturnValue(method = "codec", at = @At("RETURN"))
    private static <T> Codec<Climate.ParameterList<T>> gensokyocompat$skipOutOfRange(Codec<Climate.ParameterList<T>> codec) {
        return codec.xmap(list -> list, list -> {
            List<com.mojang.datafixers.util.Pair<Climate.ParameterPoint, T>> kept =
                list.values().stream().filter(entry -> encodable(entry.getFirst())).toList();
            return kept.size() == list.values().size() || kept.isEmpty() ? list : new Climate.ParameterList<>(kept);
        });
    }

    private static boolean encodable(Climate.ParameterPoint point) {
        return inRange(point.temperature()) && inRange(point.humidity()) && inRange(point.continentalness())
            && inRange(point.erosion()) && inRange(point.depth()) && inRange(point.weirdness())
            && point.offset() >= 0 && point.offset() <= MAX_OFFSET;
    }

    private static boolean inRange(Climate.Parameter parameter) {
        return parameter.min() >= -MAX_COORD && parameter.max() <= MAX_COORD;
    }
}
