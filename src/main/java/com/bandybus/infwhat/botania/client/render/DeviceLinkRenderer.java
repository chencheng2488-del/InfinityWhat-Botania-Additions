package com.bandybus.infwhat.botania.client.render;

import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.util.Pos;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;
import vazkii.botania.api.mana.IManaPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws the explicit mana links while the linker is held.
 *
 * <p>Every device block entity syncs the positions of its neighbours in its update tag, so the
 * client can draw the exact topology without rescanning a radius. Beams are drawn as a thin bright
 * core surrounded by wider, very transparent additive layers, which gives the soft glow of a mana
 * beam without any shader or texture dependency. The block selected as the linker's source is
 * outlined so the player can see what will be connected next.
 */
@SideOnly(Side.CLIENT)
public final class DeviceLinkRenderer {

    private static final int CHUNK_RADIUS = 3;
    private static final String TAG_SOURCE_DIMENSION = "SourceDimension";
    private static final String TAG_SOURCE_POS = "SourcePos";

    private static final float[] GLOW_WIDTHS = {0.065F, 0.032F, 0.014F, 0.005F};
    private static final float[][] GLOW_COLORS = {
            {0.10F, 0.32F, 0.95F, 0.045F},
            {0.18F, 0.55F, 1.00F, 0.10F},
            {0.40F, 0.78F, 1.00F, 0.22F},
            {0.85F, 0.97F, 1.00F, 0.85F}
    };
    private static final float[] AUTO_WIDTHS = {0.040F, 0.018F, 0.005F};
    private static final float[][] AUTO_COLORS = {
            {0.10F, 0.75F, 0.95F, 0.05F},
            {0.25F, 0.90F, 1.00F, 0.10F},
            {0.60F, 1.00F, 1.00F, 0.45F}
    };

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        World world = minecraft.theWorld;
        if (player == null || world == null) {
            return;
        }
        ItemStack held = player.getHeldItem();
        boolean linkerHeld = held != null && held.getItem() == ModItems.CHANNEL_LINKER;
        boolean deviceGuiOpen = minecraft.currentScreen
                instanceof com.bandybus.infwhat.botania.client.gui.GuiManaDevice
                || minecraft.currentScreen
                instanceof com.bandybus.infwhat.botania.client.gui.GuiAccelerator;
        if (!linkerHeld && !deviceGuiOpen) {
            return;
        }

        Set<String> autoKeys = new HashSet<String>();
        List<Beam> autoBeams = collectAutoBeams(world, player, autoKeys);
        List<Beam> beams = collectBeams(world, player, autoKeys);
        Pos source = linkerHeld ? readSource(held, world) : null;
        if (beams.isEmpty() && autoBeams.isEmpty() && source == null) {
            return;
        }

        double cameraX = RenderManager.instance.viewerPosX;
        double cameraY = RenderManager.instance.viewerPosY;
        double cameraZ = RenderManager.instance.viewerPosZ;

        GL11.glPushMatrix();
        // RenderWorldLastEvent runs with the world already translated by -camera, so undo that
        // translation before drawing beams that use absolute block coordinates.
        GL11.glTranslated(-cameraX, -cameraY, -cameraZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);

        for (Beam beam : beams) {
            drawLayers(beam, cameraX, cameraY, cameraZ, GLOW_WIDTHS, GLOW_COLORS);
        }
        for (Beam beam : autoBeams) {
            drawLayers(beam, cameraX, cameraY, cameraZ, AUTO_WIDTHS, AUTO_COLORS);
        }
        if (source != null) {
            drawOutline(source, cameraX, cameraY, cameraZ);
        }

        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private static void drawLayers(Beam beam, double cameraX, double cameraY, double cameraZ,
                                   float[] widths, float[][] colors) {
        for (int layer = 0; layer < widths.length; layer++) {
            float[] color = colors[layer];
            drawBeam(beam, cameraX, cameraY, cameraZ, widths[layer],
                    color[0], color[1], color[2], color[3]);
        }
    }

    private static List<Beam> collectBeams(World world, EntityPlayer player,
                                           Set<String> autoKeys) {
        List<Beam> beams = new ArrayList<Beam>();
        Set<String> seen = new HashSet<String>();
        for (Object object : world.loadedTileEntityList) {
            if (!(object instanceof ManaDeviceHost) || !(object instanceof TileEntity)) {
                continue;
            }
            TileEntity tileEntity = (TileEntity) object;
            if (outsideRadius(player, tileEntity)) {
                continue;
            }
            ManaDeviceHost host = (ManaDeviceHost) object;
            Pos from = Pos.of(tileEntity);
            for (Pos target : host.clientLinks()) {
                if (!world.blockExists(target.x, target.y, target.z)) {
                    continue;
                }
                Beam beam = Beam.of(from, target);
                String key = beam.key();
                if (!autoKeys.contains(key) && seen.add(key)) {
                    beams.add(beam);
                }
            }
        }
        return beams;
    }

    private static List<Beam> collectAutoBeams(World world, EntityPlayer player,
                                               Set<String> keys) {
        List<Beam> beams = new ArrayList<Beam>();
        for (Object object : world.loadedTileEntityList) {
            if (!(object instanceof ManaDeviceHost) || !(object instanceof TileEntity)) {
                continue;
            }
            TileEntity tileEntity = (TileEntity) object;
            if (outsideRadius(player, tileEntity)) {
                continue;
            }
            ManaDeviceHost host = (ManaDeviceHost) object;
            Pos from = Pos.of(tileEntity);
            for (Pos target : host.clientAutoTargets()) {
                if (!world.blockExists(target.x, target.y, target.z)
                        || !(world.getTileEntity(target.x, target.y, target.z)
                        instanceof IManaPool)) {
                    continue;
                }
                Beam beam = Beam.of(from, target);
                if (keys.add(beam.key())) {
                    beams.add(beam);
                }
            }
        }
        return beams;
    }

    private static boolean outsideRadius(EntityPlayer player, TileEntity tileEntity) {
        int chunkX = net.minecraft.util.MathHelper.floor_double(player.posX) >> 4;
        int chunkZ = net.minecraft.util.MathHelper.floor_double(player.posZ) >> 4;
        int tileChunkX = tileEntity.xCoord >> 4;
        int tileChunkZ = tileEntity.zCoord >> 4;
        return Math.abs(chunkX - tileChunkX) > CHUNK_RADIUS
                || Math.abs(chunkZ - tileChunkZ) > CHUNK_RADIUS;
    }

    private static Pos readSource(ItemStack stack, World world) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_SOURCE_POS)) {
            return null;
        }
        if (tag.getInteger(TAG_SOURCE_DIMENSION) != world.provider.dimensionId) {
            return null;
        }
        return Pos.of(tag.getLong(TAG_SOURCE_POS));
    }

    private static void drawBeam(Beam beam, double cameraX, double cameraY, double cameraZ,
                                 float halfWidth, float red, float green, float blue,
                                 float alpha) {
        drawRaw(beam.a.x + 0.5D, beam.a.y + 0.5D, beam.a.z + 0.5D,
                beam.b.x + 0.5D, beam.b.y + 0.5D, beam.b.z + 0.5D,
                cameraX, cameraY, cameraZ, halfWidth, red, green, blue, alpha);
    }

    private static void drawOutline(Pos pos, double cameraX, double cameraY, double cameraZ) {
        double low = -0.02D;
        double high = 1.02D;
        double[] xs = {pos.x + low, pos.x + high};
        double[] ys = {pos.y + low, pos.y + high};
        double[] zs = {pos.z + low, pos.z + high};
        int[][] edges = {
                {0, 0, 0, 1, 0, 0}, {0, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 0, 1},
                {1, 0, 0, 1, 1, 0}, {1, 0, 0, 1, 0, 1}, {0, 1, 0, 1, 1, 0},
                {0, 1, 0, 0, 1, 1}, {1, 1, 0, 1, 1, 1}, {0, 0, 1, 1, 0, 1},
                {0, 0, 1, 0, 1, 1}, {1, 0, 1, 1, 1, 1}, {0, 1, 1, 1, 1, 1}
        };
        for (int[] edge : edges) {
            drawRaw(xs[edge[0]], ys[edge[1]], zs[edge[2]],
                    xs[edge[3]], ys[edge[4]], zs[edge[5]],
                    cameraX, cameraY, cameraZ, 0.016F, 0.15F, 0.55F, 1.0F, 0.18F);
            drawRaw(xs[edge[0]], ys[edge[1]], zs[edge[2]],
                    xs[edge[3]], ys[edge[4]], zs[edge[5]],
                    cameraX, cameraY, cameraZ, 0.005F, 0.65F, 0.92F, 1.0F, 0.70F);
        }
    }

    private static void drawRaw(double startX, double startY, double startZ,
                                double endX, double endY, double endZ,
                                double cameraX, double cameraY, double cameraZ,
                                float halfWidth, float red, float green, float blue,
                                float alpha) {
        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6D) {
            return;
        }
        dx /= length;
        dy /= length;
        dz /= length;
        double midX = (startX + endX) * 0.5D;
        double midY = (startY + endY) * 0.5D;
        double midZ = (startZ + endZ) * 0.5D;
        double toCameraX = cameraX - midX;
        double toCameraY = cameraY - midY;
        double toCameraZ = cameraZ - midZ;
        double cameraLength = Math.sqrt(toCameraX * toCameraX + toCameraY * toCameraY
                + toCameraZ * toCameraZ);
        if (cameraLength < 1.0E-6D) {
            return;
        }
        toCameraX /= cameraLength;
        toCameraY /= cameraLength;
        toCameraZ /= cameraLength;
        double sideX = dy * toCameraZ - dz * toCameraY;
        double sideY = dz * toCameraX - dx * toCameraZ;
        double sideZ = dx * toCameraY - dy * toCameraX;
        double sideLength = Math.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);
        if (sideLength < 1.0E-6D) {
            return;
        }
        sideX = sideX / sideLength * halfWidth;
        sideY = sideY / sideLength * halfWidth;
        sideZ = sideZ / sideLength * halfWidth;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(red, green, blue, alpha);
        tessellator.addVertex(startX + sideX, startY + sideY, startZ + sideZ);
        tessellator.addVertex(startX - sideX, startY - sideY, startZ - sideZ);
        tessellator.addVertex(endX - sideX, endY - sideY, endZ - sideZ);
        tessellator.addVertex(endX + sideX, endY + sideY, endZ + sideZ);
        tessellator.draw();
    }

    /** Undirected pair with a stable order, so both endpoints do not draw the same beam twice. */
    private static final class Beam {

        private final Pos a;
        private final Pos b;

        private Beam(Pos a, Pos b) {
            this.a = a;
            this.b = b;
        }

        private static Beam of(Pos first, Pos second) {
            return first.compareTo(second) <= 0 ? new Beam(first, second)
                    : new Beam(second, first);
        }

        private String key() {
            return a.asLong() + ":" + b.asLong();
        }
    }
}
