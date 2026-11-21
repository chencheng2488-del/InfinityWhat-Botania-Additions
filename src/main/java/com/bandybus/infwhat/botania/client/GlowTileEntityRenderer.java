package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;

import java.util.Random;

/**
 * Re-renders the glowing faces of a block model at full brightness.
 *
 * <p>Vanilla applies the block lightmap to every quad, so a bright texture still goes dark at
 * night. Redrawing only the quads that use the glow sprite with a full-bright lightmap gives a
 * real emissive look while the rest of the model keeps normal lighting and ambient occlusion.
 */
public class GlowTileEntityRenderer<T extends TileEntity> extends TileEntityRenderer<T> {

    private static final int FULL_BRIGHT = 0xF000F0;

    private final ResourceLocation glowTexture;

    public GlowTileEntityRenderer(TileEntityRendererDispatcher dispatcher,
                                  ResourceLocation glowTexture) {
        super(dispatcher);
        this.glowTexture = glowTexture;
    }

    @Override
    public void render(T tileEntity, float partialTicks, MatrixStack pose,
                       IRenderTypeBuffer buffers, int packedLight, int packedOverlay) {
        IBakedModel model = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(tileEntity.getBlockState());
        IVertexBuilder consumer = buffers.getBuffer(RenderType.cutout());
        Random random = new Random(42L);
        MatrixStack.Entry last = pose.last();

        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : model.getQuads(tileEntity.getBlockState(), direction, random)) {
                if (!quad.getSprite().getName().equals(glowTexture)) {
                    continue;
                }
                consumer.putBulkData(last, quad, 1.0F, 1.0F, 1.0F,
                        FULL_BRIGHT, packedOverlay);
            }
        }

        if (tileEntity instanceof ManaDeviceHost) {
            ManaDeviceHost host = (ManaDeviceHost) tileEntity;
            if (host.showRange()) {
                renderRange(pose, buffers, host.reachX(), host.reachY(), host.reachZ(),
                        host.offsetX(), host.offsetY(), host.offsetZ());
            }
        } else if (tileEntity instanceof AbstractAcceleratorTileEntity) {
            AbstractAcceleratorTileEntity accelerator =
                    (AbstractAcceleratorTileEntity) tileEntity;
            if (accelerator.showRange()) {
                renderRange(pose, buffers, accelerator.reachX(), accelerator.reachY(),
                        accelerator.reachZ(), accelerator.offsetX(), accelerator.offsetY(),
                        accelerator.offsetZ());
            }
        }
    }

    private static void renderRange(MatrixStack pose, IRenderTypeBuffer buffers,
                                    int reachX, int reachY, int reachZ,
                                    int offsetX, int offsetY, int offsetZ) {
        double minX = offsetX - reachX;
        double minY = offsetY - reachY;
        double minZ = offsetZ - reachZ;
        double maxX = offsetX + reachX + 1.0D;
        double maxY = offsetY + reachY + 1.0D;
        double maxZ = offsetZ + reachZ + 1.0D;
        IVertexBuilder lines = buffers.getBuffer(RenderType.lines());
        WorldRenderer.renderLineBox(pose, lines, minX, minY, minZ, maxX, maxY, maxZ,
                0.25F, 0.75F, 1.0F, 0.85F);
    }
}
