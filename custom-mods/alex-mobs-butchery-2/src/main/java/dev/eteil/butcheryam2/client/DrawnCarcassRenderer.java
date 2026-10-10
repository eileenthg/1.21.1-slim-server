package dev.eteil.butcheryam2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.block.AbstractCarcassBlock;
import dev.eteil.butcheryam2.block.DrawnCarcassBlock;
import dev.eteil.butcheryam2.blockentity.DrawnCarcassBlockEntity;
import dev.eteil.butcheryam2.gen.LargeLayers;
import dev.eteil.butcheryam2.gen.SegmentedLayers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

/**
 * Draws the carcasses too big or too long for block models from Alex's Mobs' own models, the way
 * Butchery draws its Ender Dragon carcass: whatever has been cut away is simply not drawn.
 */
public class DrawnCarcassRenderer implements BlockEntityRenderer<DrawnCarcassBlockEntity> {

    private final Map<String, ModelPart> parts = new HashMap<>();

    public DrawnCarcassRenderer(BlockEntityRendererProvider.Context context) {
        SegmentedLayers.LOOKS.forEach((mob, look) ->
                look.layers().keySet().forEach(piece -> parts.put(mob + "/" + piece, context.bakeLayer(layer(mob, piece)))));
        LargeLayers.LOOKS.keySet().forEach(mob -> parts.put(mob, context.bakeLayer(layer(mob, "main"))));
    }

    public static ModelLayerLocation layer(String mob, String piece) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ButcheryAlexsMobs2.MODID, mob + "_carcass"), piece);
    }

    @Override
    public void render(DrawnCarcassBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!(be.getBlockState().getBlock() instanceof DrawnCarcassBlock block)) {
            return;
        }
        pose.pushPose();
        // turn the model with the block, as blockstates turn the other carcass models
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facingDegrees(be.getBlockState().getValue(AbstractCarcassBlock.FACING))));
        pose.translate(-0.5, 0, -0.5);
        if (SegmentedLayers.LOOKS.containsKey(block.mob())) {
            renderSegmented(block.mob(), be, pose, buffers, light, overlay);
        } else if (LargeLayers.LOOKS.containsKey(block.mob())) {
            renderLarge(block.mob(), be, pose, buffers, light, overlay);
        }
        pose.popPose();
    }

    private void renderSegmented(String mob, DrawnCarcassBlockEntity be, PoseStack pose, MultiBufferSource buffers,
                                 int light, int overlay) {
        SegmentedLayers.Look look = SegmentedLayers.LOOKS.get(mob);
        String texture = be.variant() && look.variantTexture() != null ? look.variantTexture() : look.texture();
        List<String> left = DrawnCarcassBlock.piecesLeft(mob, be);
        int first = be.cuts() == 0 ? 0 : 1; // coil index of the first piece still there
        for (int i = 0; i < left.size(); i++) {
            float[] at = look.coil()[Math.min(first + i, look.coil().length - 1)];
            pose.pushPose();
            pose.translate(at[0] / 16.0F, 0, at[1] / 16.0F);
            pose.mulPose(Axis.YP.rotationDegrees(at[2]));
            float size = look.scale() * pieceSize(look, first + i, left.get(i));
            pose.scale(size, size, size);
            pose.translate(0, look.lift() / 16.0F, 0);
            if (!look.onBack()) {
                pose.scale(-1.0F, -1.0F, 1.0F); // the entity renderer's flip: upright
            }
            // some bodies texture each piece separately (the bone serpent's head, middle and tail)
            String pieceTexture = look.pieceTextures().getOrDefault(left.get(i), texture);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(pieceTexture)));
            parts.get(mob + "/" + left.get(i)).render(pose, vc, light, overlay);
            pose.popPose();
        }
    }

    /** Some bodies grow along their length and end in smaller tail pieces (the void worm's). */
    private static float pieceSize(SegmentedLayers.Look look, int index, String piece) {
        if (index == 0) {
            return 1.0F;
        }
        float size = 1.0F + look.grow() * (index - 1) / Math.max(look.coil().length - 1, 1);
        return piece.equals("tail") ? size * look.tailScale() : size;
    }

    private void renderLarge(String mob, DrawnCarcassBlockEntity be, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        LargeLayers.Look look = LargeLayers.LOOKS.get(mob);
        int stage = Math.min(be.cuts(), look.hidden().size() - 1);
        String texture = look.skinned()[stage] && look.skinnedTexture() != null ? look.skinnedTexture() : look.texture();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(texture)));
        ModelPart root = parts.get(mob);
        root.getAllParts().forEach(p -> p.visible = true);
        for (String path : look.always()) {
            find(root, path).visible = false;
        }
        for (String path : look.hidden().get(stage)) {
            find(root, path).visible = false;
        }
        float[] o = look.offset();
        float[] q = look.rotation();
        pose.translate(o[0] / 16.0F, o[1] / 16.0F, o[2] / 16.0F);
        pose.mulPose(new Quaternionf(q[0], q[1], q[2], q[3]));
        pose.scale(look.scale(), look.scale(), look.scale());
        pose.translate(0, -24.016F / 16.0F, 0); // LivingEntityRenderer's drop to the feet
        root.render(pose, vc, light, overlay);
    }

    private static ModelPart find(ModelPart root, String path) {
        ModelPart part = root;
        for (String name : path.split("/")) {
            part = part.getChild(name);
        }
        return part;
    }

    private static float facingDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(DrawnCarcassBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(DrawnCarcassBlockEntity be) {
        double reach = 5.0;
        if (be.getBlockState().getBlock() instanceof DrawnCarcassBlock block && SegmentedLayers.LOOKS.containsKey(block.mob())) {
            reach = SegmentedLayers.LOOKS.get(block.mob()).reach();
        }
        return new AABB(be.getBlockPos()).inflate(reach);
    }
}
