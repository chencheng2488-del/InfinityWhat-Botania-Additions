package com.bandybus.infwhat.botania.client.render;

import com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

/**
 * Draws the scan box of a device or accelerator while "Show range" is enabled.
 *
 * <p>The box is drawn without depth testing so it stays readable through walls, matching the
 * modern release's line preview.
 */
@SideOnly(Side.CLIENT)
public class RangePreviewRenderer extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z,
                                   float partialTicks) {
        int reachX;
        int reachY;
        int reachZ;
        int offsetX;
        int offsetY;
        int offsetZ;
        boolean show;
        if (tileEntity instanceof ManaDeviceHost) {
            ManaDeviceHost host = (ManaDeviceHost) tileEntity;
            reachX = host.reachX();
            reachY = host.reachY();
            reachZ = host.reachZ();
            offsetX = host.offsetX();
            offsetY = host.offsetY();
            offsetZ = host.offsetZ();
            show = host.showRange();
        } else if (tileEntity instanceof AbstractAcceleratorTileEntity) {
            AbstractAcceleratorTileEntity accelerator =
                    (AbstractAcceleratorTileEntity) tileEntity;
            reachX = accelerator.reachX();
            reachY = accelerator.reachY();
            reachZ = accelerator.reachZ();
            offsetX = accelerator.offsetX();
            offsetY = accelerator.offsetY();
            offsetZ = accelerator.offsetZ();
            show = accelerator.showRange();
        } else {
            return;
        }
        if (!show) {
            return;
        }

        double minX = offsetX - reachX;
        double minY = offsetY - reachY;
        double minZ = offsetZ - reachZ;
        double maxX = offsetX + reachX + 1.0D;
        double maxY = offsetY + reachY + 1.0D;
        double maxZ = offsetZ + reachZ + 1.0D;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.5F);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_F(0.25F, 0.75F, 1.0F, 0.85F);
        line(tessellator, minX, minY, minZ, maxX, minY, minZ);
        line(tessellator, maxX, minY, minZ, maxX, minY, maxZ);
        line(tessellator, maxX, minY, maxZ, minX, minY, maxZ);
        line(tessellator, minX, minY, maxZ, minX, minY, minZ);
        line(tessellator, minX, maxY, minZ, maxX, maxY, minZ);
        line(tessellator, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(tessellator, maxX, maxY, maxZ, minX, maxY, maxZ);
        line(tessellator, minX, maxY, maxZ, minX, maxY, minZ);
        line(tessellator, minX, minY, minZ, minX, maxY, minZ);
        line(tessellator, maxX, minY, minZ, maxX, maxY, minZ);
        line(tessellator, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(tessellator, minX, minY, maxZ, minX, maxY, maxZ);
        tessellator.draw();

        GL11.glDepthMask(true);
        GL11.glLineWidth(1.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void line(Tessellator tessellator,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
    }
}
