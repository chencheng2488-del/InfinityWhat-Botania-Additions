package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.item.ModItems;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.botania.api.mana.IManaPool;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws the explicit mana links while the linker is held.
 *
 * <p>Every device tile entity syncs the positions of its neighbours in its update tag, so the
 * client can draw the exact topology without rescanning a radius. Beams are drawn as a thin bright
 * core surrounded by wider, very transparent additive layers, which gives the soft glow of a mana
 * beam without any shader or texture dependency. The block selected as the linker's source is
 * outlined so the player can see what will be connected next.
 */
@Mod.EventBusSubscriber(modid = InfinityWhatBotania.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeviceLinkRenderer {

    private static final int CHUNK_RADIUS = 3;

    private static final String TAG_SOURCE_DIMENSION = "SourceDimension";
    private static final String TAG_SOURCE_POS = "SourcePos";

    /** Additive, colour-only, no depth write: overlapping layers accumulate into a glow. */
    private static final RenderType BEAM = RenderType.create(
            "infwhat_mana_beam",
            DefaultVertexFormats.POSITION_COLOR,
            7,
            1536,
            false,
            false,
            RenderType.State.builder()
                    .setTransparencyState(new RenderState.TransparencyState("infwhat_additive",
                            () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                                        GlStateManager.DestFactor.ONE);
                            },
                            () -> {
                                RenderSystem.disableBlend();
                                RenderSystem.defaultBlendFunc();
                            }))
                    .setCullState(new RenderState.CullState(false))
                    .setWriteMaskState(new RenderState.WriteMaskState(true, false))
                    .setLightmapState(new RenderState.LightmapState(false))
                    .setDiffuseLightingState(new RenderState.DiffuseLightingState(false))
                    .createCompositeState(false));

    /** Outer halo to thin core; all widths are half-widths in blocks. */
    private static final float[] GLOW_WIDTHS = {0.065F, 0.032F, 0.014F, 0.005F};
    private static final float[][] GLOW_COLORS = {
            {0.10F, 0.32F, 0.95F, 0.045F},
            {0.18F, 0.55F, 1.00F, 0.10F},
            {0.40F, 0.78F, 1.00F, 0.22F},
            {0.85F, 0.97F, 1.00F, 0.85F}
    };

    /** Nearby mana pools connected automatically by the well/anchor scan: thinner and paler. */
    private static final float[] AUTO_WIDTHS = {0.040F, 0.018F, 0.005F};
    private static final float[][] AUTO_COLORS = {
            {0.10F, 0.75F, 0.95F, 0.05F},
            {0.25F, 0.90F, 1.00F, 0.10F},
            {0.60F, 1.00F, 1.00F, 0.45F}
    };

    private DeviceLinkRenderer() {
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerEntity player = minecraft.player;
        ClientWorld level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        ItemStack linker = heldLinker(player);
        if (linker.isEmpty()) {
            return;
        }

        Set<Beam> autoBeams = collectAutoBeams(level, player);
        Set<Beam> beams = collectBeams(level, player, autoBeams);
        if (beams.isEmpty() && autoBeams.isEmpty() && linker.getTag() == null) {
            return;
        }

        IRenderTypeBuffer.Impl buffers = minecraft.renderBuffers().bufferSource();
        Vector3d camera = minecraft.gameRenderer.getMainCamera().getPosition();
        MatrixStack pose = event.getMatrixStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = pose.last().pose();
        IVertexBuilder consumer = buffers.getBuffer(BEAM);

        for (Beam beam : beams) {
            drawLayers(matrix, consumer, beam, camera, GLOW_WIDTHS, GLOW_COLORS);
        }
        for (Beam beam : autoBeams) {
            drawLayers(matrix, consumer, beam, camera, AUTO_WIDTHS, AUTO_COLORS);
        }

        BlockPos source = readSource(linker, level);
        if (source != null) {
            drawOutline(matrix, consumer, source, camera);
        }

        pose.popPose();
        buffers.endBatch(BEAM);
    }

    private static void drawLayers(Matrix4f matrix, IVertexBuilder consumer, Beam beam,
                                   Vector3d camera, float[] widths, float[][] colors) {
        Vector3d start = center(beam.a);
        Vector3d end = center(beam.b);
        for (int layer = 0; layer < widths.length; layer++) {
            float[] color = colors[layer];
            drawBeam(matrix, consumer, start, end, camera,
                    widths[layer], color[0], color[1], color[2], color[3]);
        }
    }

    private static ItemStack heldLinker(PlayerEntity player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == ModItems.CHANNEL_LINKER.get()) {
            return main;
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() == ModItems.CHANNEL_LINKER.get() ? offhand : ItemStack.EMPTY;
    }

    private static Set<Beam> collectBeams(ClientWorld level, PlayerEntity player,
                                          Set<Beam> autoBeams) {
        Set<Beam> beams = new HashSet<Beam>();
        int centerX = player.blockPosition().getX() >> 4;
        int centerZ = player.blockPosition().getZ() >> 4;
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                Chunk chunk = level.getChunk(centerX + dx, centerZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (TileEntity tileEntity : chunk.getBlockEntities().values()) {
                    if (!(tileEntity instanceof ManaDeviceHost)) {
                        continue;
                    }
                    ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                    BlockPos from = tileEntity.getBlockPos();
                    for (BlockPos target : host.clientLinks()) {
                        if (!level.isLoaded(target)) {
                            continue;
                        }
                        Beam beam = Beam.of(from, target);
                        if (!autoBeams.contains(beam)) {
                            beams.add(beam);
                        }
                    }
                }
            }
        }
        return beams;
    }

    private static Set<Beam> collectAutoBeams(ClientWorld level, PlayerEntity player) {
        Set<Beam> beams = new HashSet<Beam>();
        int centerX = player.blockPosition().getX() >> 4;
        int centerZ = player.blockPosition().getZ() >> 4;
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                Chunk chunk = level.getChunk(centerX + dx, centerZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (TileEntity tileEntity : chunk.getBlockEntities().values()) {
                    if (!(tileEntity instanceof ManaDeviceHost)) {
                        continue;
                    }
                    ManaDeviceHost host = (ManaDeviceHost) tileEntity;
                    BlockPos from = tileEntity.getBlockPos();
                    for (BlockPos target : host.clientAutoTargets()) {
                        if (!level.isLoaded(target)
                                || !(level.getBlockEntity(target) instanceof IManaPool)) {
                            continue;
                        }
                        Beam beam = Beam.of(from, target);
                        if (!beams.contains(beam)) {
                            beams.add(beam);
                        }
                    }
                }
            }
        }
        return beams;
    }

    private static BlockPos readSource(ItemStack stack, ClientWorld level) {
        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SOURCE_POS)) {
            return null;
        }
        if (!tag.getString(TAG_SOURCE_DIMENSION)
                .equals(level.dimension().location().toString())) {
            return null;
        }
        return BlockPos.of(tag.getLong(TAG_SOURCE_POS));
    }

    private static Vector3d center(BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /** A camera-facing quad; the custom render type has culling disabled so one quad is enough. */
    private static void drawBeam(Matrix4f matrix, IVertexBuilder consumer,
                                 Vector3d start, Vector3d end, Vector3d camera,
                                 float halfWidth, float red, float green, float blue,
                                 float alpha) {
        Vector3d direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vector3d unit = direction.normalize();
        Vector3d midpoint = start.add(end).scale(0.5D);
        Vector3d toCamera = camera.subtract(midpoint);
        if (toCamera.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vector3d side = unit.cross(toCamera.normalize()).normalize().scale(halfWidth);
        vertex(matrix, consumer, start.add(side), red, green, blue, alpha);
        vertex(matrix, consumer, start.subtract(side), red, green, blue, alpha);
        vertex(matrix, consumer, end.subtract(side), red, green, blue, alpha);
        vertex(matrix, consumer, end.add(side), red, green, blue, alpha);
    }

    private static void drawOutline(Matrix4f matrix, IVertexBuilder consumer,
                                    BlockPos pos, Vector3d camera) {
        float low = -0.02F;
        float high = 1.02F;
        Vector3d[] corners = new Vector3d[8];
        for (int index = 0; index < 8; index++) {
            double x = pos.getX() + ((index & 1) == 0 ? low : high);
            double y = pos.getY() + ((index & 2) == 0 ? low : high);
            double z = pos.getZ() + ((index & 4) == 0 ? low : high);
            corners[index] = new Vector3d(x, y, z);
        }
        int[][] edges = {
                {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
                {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
        };
        for (int[] edge : edges) {
            drawBeam(matrix, consumer, corners[edge[0]], corners[edge[1]], camera,
                    0.016F, 0.15F, 0.55F, 1.0F, 0.18F);
            drawBeam(matrix, consumer, corners[edge[0]], corners[edge[1]], camera,
                    0.005F, 0.65F, 0.92F, 1.0F, 0.70F);
        }
    }

    private static void vertex(Matrix4f matrix, IVertexBuilder consumer, Vector3d pos,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    /** Undirected pair with a stable order, so both endpoints do not draw the same beam twice. */
    private static final class Beam {

        private final BlockPos a;
        private final BlockPos b;

        private Beam(BlockPos a, BlockPos b) {
            this.a = a;
            this.b = b;
        }

        private static Beam of(BlockPos first, BlockPos second) {
            if (Long.compare(first.asLong(), second.asLong()) <= 0) {
                return new Beam(first.immutable(), second.immutable());
            }
            return new Beam(second.immutable(), first.immutable());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Beam)) {
                return false;
            }
            Beam beam = (Beam) other;
            return a.equals(beam.a) && b.equals(beam.b);
        }

        @Override
        public int hashCode() {
            return 31 * a.hashCode() + b.hashCode();
        }
    }
}
