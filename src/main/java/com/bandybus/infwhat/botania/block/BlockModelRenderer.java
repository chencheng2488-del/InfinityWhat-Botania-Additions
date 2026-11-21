package com.bandybus.infwhat.botania.block;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

/**
 * Renders the box models of the mana well, beacon and anchor.
 *
 * <p>1.7.10 has no JSON block models, so the same geometry is declared in {@link BlockModel} and
 * drawn here. The world pass adds its own vertices with the exact per-face UV rectangles of the
 * modern JSON models; {@link RenderBlocks#renderFaceYNeg} and friends cannot be used because they
 * map the sprite onto the box's position in block space (which crops and stretches the texture).
 */
public final class BlockModelRenderer implements ISimpleBlockRenderingHandler {

    public static final BlockModelRenderer INSTANCE = new BlockModelRenderer();

    private BlockModelRenderer() {
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId,
                                     RenderBlocks renderer) {
        if (!(block instanceof IModelBlock)) {
            return;
        }
        BlockModel model = ((IModelBlock) block).model(metadata);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.5F, 0.35F, 0.5F);
        GL11.glScalef(0.72F, 0.72F, 0.72F);
        GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(225.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        net.minecraft.client.Minecraft.getMinecraft().getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        drawModel(model, true);
        RenderHelper.enableStandardItemLighting();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block,
                                    int modelId, RenderBlocks renderer) {
        if (!(block instanceof IModelBlock)) {
            return false;
        }
        BlockModel model = ((IModelBlock) block).model(
                world.getBlockMetadata(x, y, z));
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        for (BlockModel.Box box : model.boxes) {
            drawBox(tessellator, box, true, x, y, z);
        }
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return ModRenderIds.blockModel;
    }

    /** Draws every box at its block-local coordinates; used by the item renderer as well. */
    public static void drawModel(BlockModel model, boolean textured) {
        Tessellator tessellator = Tessellator.instance;
        for (BlockModel.Box box : model.boxes) {
            drawBox(tessellator, box, textured, 0.0F, 0.0F, 0.0F);
        }
    }

    private static void drawBox(Tessellator tessellator, BlockModel.Box box, boolean textured,
                                float ox, float oy, float oz) {
        drawBoxFace(tessellator, box, 0, textured,
                ox + box.minX, oy + box.minY, oz + box.maxZ,
                ox + box.minX, oy + box.minY, oz + box.minZ,
                ox + box.maxX, oy + box.minY, oz + box.minZ,
                ox + box.maxX, oy + box.minY, oz + box.maxZ,
                0.0F, -1.0F, 0.0F, 0.5F, false);
        drawBoxFace(tessellator, box, 1, textured,
                ox + box.minX, oy + box.maxY, oz + box.minZ,
                ox + box.minX, oy + box.maxY, oz + box.maxZ,
                ox + box.maxX, oy + box.maxY, oz + box.maxZ,
                ox + box.maxX, oy + box.maxY, oz + box.minZ,
                0.0F, 1.0F, 0.0F, 1.0F, false);
        drawBoxFace(tessellator, box, 2, textured,
                ox + box.maxX, oy + box.minY, oz + box.minZ,
                ox + box.minX, oy + box.minY, oz + box.minZ,
                ox + box.minX, oy + box.maxY, oz + box.minZ,
                ox + box.maxX, oy + box.maxY, oz + box.minZ,
                0.0F, 0.0F, -1.0F, 0.8F, true);
        drawBoxFace(tessellator, box, 3, textured,
                ox + box.minX, oy + box.minY, oz + box.maxZ,
                ox + box.maxX, oy + box.minY, oz + box.maxZ,
                ox + box.maxX, oy + box.maxY, oz + box.maxZ,
                ox + box.minX, oy + box.maxY, oz + box.maxZ,
                0.0F, 0.0F, 1.0F, 0.8F, true);
        drawBoxFace(tessellator, box, 4, textured,
                ox + box.minX, oy + box.minY, oz + box.minZ,
                ox + box.minX, oy + box.minY, oz + box.maxZ,
                ox + box.minX, oy + box.maxY, oz + box.maxZ,
                ox + box.minX, oy + box.maxY, oz + box.minZ,
                -1.0F, 0.0F, 0.0F, 0.6F, true);
        drawBoxFace(tessellator, box, 5, textured,
                ox + box.maxX, oy + box.minY, oz + box.maxZ,
                ox + box.maxX, oy + box.minY, oz + box.minZ,
                ox + box.maxX, oy + box.maxY, oz + box.minZ,
                ox + box.maxX, oy + box.maxY, oz + box.maxZ,
                1.0F, 0.0F, 0.0F, 0.6F, true);
    }

    private static void drawBoxFace(Tessellator tessellator, BlockModel.Box box, int face,
                                    boolean textured,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4,
                                    float nx, float ny, float nz, float shade,
                                    boolean side) {
        float[] uv = box.uvs[face];
        drawFace(tessellator, box.faces[face], textured,
                uv[0], uv[1], uv[2], uv[3],
                x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4,
                nx, ny, nz, shade, side);
    }

    private static void drawFace(Tessellator tessellator, IIcon icon, boolean textured,
                                 float u0, float v0, float u1, float v1,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float nx, float ny, float nz, float shade,
                                 boolean side) {
        if (icon == null) {
            return;
        }
        if (textured) {
            tessellator.setNormal(nx, ny, nz);
            tessellator.setColorOpaque_F(shade, shade, shade);
            float uMin = icon.getInterpolatedU(u0);
            float uMax = icon.getInterpolatedU(u1);
            float vMin = icon.getInterpolatedV(v0);
            float vMax = icon.getInterpolatedV(v1);
            if (side) {
                tessellator.addVertexWithUV(x1, y1, z1, uMax, vMax);
                tessellator.addVertexWithUV(x2, y2, z2, uMin, vMax);
                tessellator.addVertexWithUV(x3, y3, z3, uMin, vMin);
                tessellator.addVertexWithUV(x4, y4, z4, uMax, vMin);
            } else {
                tessellator.addVertexWithUV(x1, y1, z1, uMin, vMin);
                tessellator.addVertexWithUV(x2, y2, z2, uMin, vMax);
                tessellator.addVertexWithUV(x3, y3, z3, uMax, vMax);
                tessellator.addVertexWithUV(x4, y4, z4, uMax, vMin);
            }
        } else {
            tessellator.setNormal(nx, ny, nz);
            tessellator.setColorOpaque_F(shade, shade, shade);
            tessellator.addVertex(x1, y1, z1);
            tessellator.addVertex(x2, y2, z2);
            tessellator.addVertex(x3, y3, z3);
            tessellator.addVertex(x4, y4, z4);
        }
    }
}
