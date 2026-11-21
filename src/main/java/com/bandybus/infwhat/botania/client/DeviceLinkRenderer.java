package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import com.bandybus.infwhat.botania.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import vazkii.botania.common.block.tile.mana.TilePool;

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
public final class DeviceLinkRenderer {

	private static final int CHUNK_RADIUS = 3;

	private static final String TAG_SOURCE_DIMENSION = "SourceDimension";
	private static final String TAG_SOURCE_POS = "SourcePos";

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

	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		Minecraft minecraft = Minecraft.getMinecraft();
		EntityPlayer player = minecraft.player;
		World world = minecraft.world;
		if (player == null || world == null) {
			return;
		}
		ItemStack linker = heldLinker(player);
		if (linker.isEmpty()) {
			return;
		}

		Set<Beam> autoBeams = collectAutoBeams(world, player);
		Set<Beam> beams = collectBeams(world, player, autoBeams);
		BlockPos source = readSource(linker, world);
		if (beams.isEmpty() && autoBeams.isEmpty() && source == null) {
			return;
		}

		double viewerX = minecraft.getRenderManager().viewerPosX;
		double viewerY = minecraft.getRenderManager().viewerPosY;
		double viewerZ = minecraft.getRenderManager().viewerPosZ;
		Vec3d camera = new Vec3d(viewerX, viewerY, viewerZ);

		GlStateManager.pushMatrix();
		GlStateManager.translate(-viewerX, -viewerY, -viewerZ);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		for (Beam beam : beams) {
			drawLayers(buffer, beam, camera, GLOW_WIDTHS, GLOW_COLORS);
		}
		for (Beam beam : autoBeams) {
			drawLayers(buffer, beam, camera, AUTO_WIDTHS, AUTO_COLORS);
		}
		if (source != null) {
			drawOutline(buffer, source, camera);
		}
		tessellator.draw();

		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
	}

	private static void drawLayers(BufferBuilder buffer, Beam beam, Vec3d camera,
								   float[] widths, float[][] colors) {
		Vec3d start = center(beam.a);
		Vec3d end = center(beam.b);
		for (int layer = 0; layer < widths.length; layer++) {
			float[] color = colors[layer];
			drawBeam(buffer, start, end, camera, widths[layer],
					color[0], color[1], color[2], color[3]);
		}
	}

	private static ItemStack heldLinker(EntityPlayer player) {
		ItemStack main = player.getHeldItemMainhand();
		if (main.getItem() == ModItems.CHANNEL_LINKER) {
			return main;
		}
		ItemStack offhand = player.getHeldItemOffhand();
		return offhand.getItem() == ModItems.CHANNEL_LINKER ? offhand : ItemStack.EMPTY;
	}

	private static Set<Beam> collectBeams(World world, EntityPlayer player, Set<Beam> autoBeams) {
		Set<Beam> beams = new HashSet<Beam>();
		ChunkProviderClient provider = (ChunkProviderClient) world.getChunkProvider();
		for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
			for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
				Chunk chunk = provider.getLoadedChunk(player.chunkCoordX + dx,
						player.chunkCoordZ + dz);
				if (chunk == null) {
					continue;
				}
				for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
					if (!(tileEntity instanceof ManaDeviceHost)) {
						continue;
					}
					BlockPos from = tileEntity.getPos();
					for (BlockPos target : ((ManaDeviceHost) tileEntity).clientLinks()) {
						if (!world.isBlockLoaded(target)) {
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

	private static Set<Beam> collectAutoBeams(World world, EntityPlayer player) {
		Set<Beam> beams = new HashSet<Beam>();
		ChunkProviderClient provider = (ChunkProviderClient) world.getChunkProvider();
		for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
			for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
				Chunk chunk = provider.getLoadedChunk(player.chunkCoordX + dx,
						player.chunkCoordZ + dz);
				if (chunk == null) {
					continue;
				}
				for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
					if (!(tileEntity instanceof ManaDeviceHost)) {
						continue;
					}
					BlockPos from = tileEntity.getPos();
					for (BlockPos target : ((ManaDeviceHost) tileEntity).clientAutoTargets()) {
						if (!world.isBlockLoaded(target)
								|| !(world.getTileEntity(target) instanceof TilePool)) {
							continue;
						}
						beams.add(Beam.of(from, target));
					}
				}
			}
		}
		return beams;
	}

	private static BlockPos readSource(ItemStack stack, World world) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || !tag.hasKey(TAG_SOURCE_POS)) {
			return null;
		}
		if (tag.getInteger(TAG_SOURCE_DIMENSION) != world.provider.getDimension()) {
			return null;
		}
		return BlockPos.fromLong(tag.getLong(TAG_SOURCE_POS));
	}

	private static Vec3d center(BlockPos pos) {
		return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
	}

	/** A camera-facing quad. */
	private static void drawBeam(BufferBuilder buffer, Vec3d start, Vec3d end, Vec3d camera,
								 float halfWidth, float red, float green, float blue,
								 float alpha) {
		Vec3d direction = end.subtract(start);
		if (direction.lengthSquared() < 1.0E-6D) {
			return;
		}
		Vec3d unit = direction.normalize();
		Vec3d midpoint = start.add(end).scale(0.5D);
		Vec3d toCamera = camera.subtract(midpoint);
		if (toCamera.lengthSquared() < 1.0E-6D) {
			return;
		}
		Vec3d side = unit.crossProduct(toCamera.normalize()).normalize().scale(halfWidth);
		vertex(buffer, start.add(side), red, green, blue, alpha);
		vertex(buffer, start.subtract(side), red, green, blue, alpha);
		vertex(buffer, end.subtract(side), red, green, blue, alpha);
		vertex(buffer, end.add(side), red, green, blue, alpha);
	}

	private static void drawOutline(BufferBuilder buffer, BlockPos pos, Vec3d camera) {
		float low = -0.02F;
		float high = 1.02F;
		Vec3d[] corners = new Vec3d[8];
		for (int index = 0; index < 8; index++) {
			double x = pos.getX() + ((index & 1) == 0 ? low : high);
			double y = pos.getY() + ((index & 2) == 0 ? low : high);
			double z = pos.getZ() + ((index & 4) == 0 ? low : high);
			corners[index] = new Vec3d(x, y, z);
		}
		int[][] edges = {
				{0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
				{2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
		};
		for (int[] edge : edges) {
			drawBeam(buffer, corners[edge[0]], corners[edge[1]], camera,
					0.016F, 0.15F, 0.55F, 1.0F, 0.18F);
			drawBeam(buffer, corners[edge[0]], corners[edge[1]], camera,
					0.005F, 0.65F, 0.92F, 1.0F, 0.70F);
		}
	}

	private static void vertex(BufferBuilder buffer, Vec3d pos,
							   float red, float green, float blue, float alpha) {
		buffer.pos(pos.x, pos.y, pos.z).color(red, green, blue, alpha).endVertex();
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
			if (Long.compare(first.toLong(), second.toLong()) <= 0) {
				return new Beam(first.toImmutable(), second.toImmutable());
			}
			return new Beam(second.toImmutable(), first.toImmutable());
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
