package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorBlockEntity;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Re-renders the glowing faces of a block model at full brightness.
 *
 * <p>Vanilla applies the block lightmap to every quad, so a bright texture still goes dark at
 * night. Redrawing only the quads that use the glow sprite with {@link LightTexture#FULL_BRIGHT}
 * gives a real emissive look while the rest of the model keeps normal lighting and ambient
 * occlusion.
 */
public class GlowBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    private final ResourceLocation glowTexture;

    public GlowBlockEntityRenderer(ResourceLocation glowTexture) {
        this.glowTexture = glowTexture;
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        Random random = new Random(42L);
        PoseStack.Pose pose = poseStack.last();

        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : model.getQuads(state, direction, random)) {
                if (!quad.getSprite().getName().equals(glowTexture)) {
                    continue;
                }
                consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F,
                        LightTexture.FULL_BRIGHT, packedOverlay);
            }
        }

        if (blockEntity instanceof ManaDeviceHost host && host.showRange()) {
            renderRange(poseStack, bufferSource, host.reachX(), host.reachY(), host.reachZ(),
                    host.offsetX(), host.offsetY(), host.offsetZ());
        } else if (blockEntity instanceof AbstractAcceleratorBlockEntity accelerator
                && accelerator.showRange()) {
            renderRange(poseStack, bufferSource, accelerator.reachX(), accelerator.reachY(),
                    accelerator.reachZ(), accelerator.offsetX(), accelerator.offsetY(),
                    accelerator.offsetZ());
        }
    }

    private static void renderRange(PoseStack poseStack, MultiBufferSource bufferSource,
                                    int reachX, int reachY, int reachZ,
                                    int offsetX, int offsetY, int offsetZ) {
        double minX = offsetX - reachX;
        double minY = offsetY - reachY;
        double minZ = offsetZ - reachZ;
        double maxX = offsetX + reachX + 1.0D;
        double maxY = offsetY + reachY + 1.0D;
        double maxZ = offsetZ + reachZ + 1.0D;
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, lines, minX, minY, minZ, maxX, maxY, maxZ,
                0.25F, 0.75F, 1.0F, 0.85F);
    }
}
