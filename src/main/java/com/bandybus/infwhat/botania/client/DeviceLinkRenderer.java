package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.item.ModItems;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.botania.api.mana.IManaPool;

import java.util.HashSet;
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
@Mod.EventBusSubscriber(modid = InfinityWhatBotania.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeviceLinkRenderer {

    private static final int CHUNK_RADIUS = 3;

    private static final String TAG_SOURCE_DIMENSION = "SourceDimension";
    private static final String TAG_SOURCE_POS = "SourcePos";

    /** Additive, colour-only, no depth write: overlapping layers accumulate into a glow. */
    private static final RenderType BEAM = RenderType.create(
            "infwhat_mana_beam",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            GameRenderer::getPositionColorShader))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                            "infwhat_additive",
                            () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                                        GlStateManager.DestFactor.ONE);
                            },
                            () -> {
                                RenderSystem.disableBlend();
                                RenderSystem.defaultBlendFunc();
                            }))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .setLightmapState(new RenderStateShard.LightmapStateShard(false))
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
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ClientLevel level = minecraft.level;
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

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(BEAM);

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

        poseStack.popPose();
        buffers.endBatch(BEAM);
    }

    private static void drawLayers(Matrix4f matrix, VertexConsumer consumer, Beam beam, Vec3 camera,
                                   float[] widths, float[][] colors) {
        Vec3 start = center(beam.a);
        Vec3 end = center(beam.b);
        for (int layer = 0; layer < widths.length; layer++) {
            float[] color = colors[layer];
            drawBeam(matrix, consumer, start, end, camera,
                    widths[layer], color[0], color[1], color[2], color[3]);
        }
    }

    private static ItemStack heldLinker(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.CHANNEL_LINKER.get())) {
            return main;
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(ModItems.CHANNEL_LINKER.get()) ? offhand : ItemStack.EMPTY;
    }

    private static Set<Beam> collectBeams(ClientLevel level, Player player,
                                          Set<Beam> autoBeams) {
        Set<Beam> beams = new HashSet<>();
        int centerX = player.chunkPosition().x;
        int centerZ = player.chunkPosition().z;
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                LevelChunk chunk = level.getChunk(centerX + dx, centerZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof ManaDeviceHost host)) {
                        continue;
                    }
                    BlockPos from = blockEntity.getBlockPos();
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

    private static Set<Beam> collectAutoBeams(ClientLevel level, Player player) {
        Set<Beam> beams = new HashSet<>();
        int centerX = player.chunkPosition().x;
        int centerZ = player.chunkPosition().z;
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                LevelChunk chunk = level.getChunk(centerX + dx, centerZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof ManaDeviceHost host)) {
                        continue;
                    }
                    BlockPos from = blockEntity.getBlockPos();
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

    private static BlockPos readSource(ItemStack stack, ClientLevel level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SOURCE_POS)) {
            return null;
        }
        if (!tag.getString(TAG_SOURCE_DIMENSION)
                .equals(level.dimension().location().toString())) {
            return null;
        }
        return BlockPos.of(tag.getLong(TAG_SOURCE_POS));
    }

    private static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /** A camera-facing quad; the custom render type has culling disabled so one quad is enough. */
    private static void drawBeam(Matrix4f matrix, VertexConsumer consumer,
                                 Vec3 start, Vec3 end, Vec3 camera,
                                 float halfWidth, float red, float green, float blue, float alpha) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 unit = direction.normalize();
        Vec3 midpoint = start.add(end).scale(0.5D);
        Vec3 toCamera = camera.subtract(midpoint);
        if (toCamera.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 side = unit.cross(toCamera.normalize()).normalize().scale(halfWidth);
        vertex(matrix, consumer, start.add(side), red, green, blue, alpha);
        vertex(matrix, consumer, start.subtract(side), red, green, blue, alpha);
        vertex(matrix, consumer, end.subtract(side), red, green, blue, alpha);
        vertex(matrix, consumer, end.add(side), red, green, blue, alpha);
    }

    private static void drawOutline(Matrix4f matrix, VertexConsumer consumer,
                                    BlockPos pos, Vec3 camera) {
        float low = -0.02F;
        float high = 1.02F;
        Vec3[] corners = new Vec3[8];
        for (int index = 0; index < 8; index++) {
            double x = pos.getX() + ((index & 1) == 0 ? low : high);
            double y = pos.getY() + ((index & 2) == 0 ? low : high);
            double z = pos.getZ() + ((index & 4) == 0 ? low : high);
            corners[index] = new Vec3(x, y, z);
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

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, Vec3 pos,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    /** Undirected pair with a stable order, so both endpoints do not draw the same beam twice. */
    private record Beam(BlockPos a, BlockPos b) {

        static Beam of(BlockPos first, BlockPos second) {
            if (Long.compare(first.asLong(), second.asLong()) <= 0) {
                return new Beam(first.immutable(), second.immutable());
            }
            return new Beam(second.immutable(), first.immutable());
        }
    }
}
