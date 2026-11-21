package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.block.tile.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.tile.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.block.tile.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.tile.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.ManaDeviceHost;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

/**
 * Re-renders the glowing faces of a block model at full brightness.
 *
 * <p>Vanilla applies the block lightmap to every quad, so a bright texture still goes dark at
 * night. Redrawing only the quads that use the glow sprite with a full-bright lightmap gives a
 * real emissive look while the rest of the model keeps normal lighting and ambient occlusion.
 *
 * <p>The same renderer also draws the scan box of the well, the anchor and the accelerators.
 */
public class RenderManaGlow<T extends TileEntity> extends TileEntitySpecialRenderer<T> {

	private final String glowIcon;
	private final long seed = 42L;

	public RenderManaGlow(String glowIcon) {
		this.glowIcon = glowIcon;
	}

	@Override
	public void render(T tileEntity, double x, double y, double z, float partialTicks,
					   int destroyStage, float alpha) {
		IBlockState state = tileEntity.getWorld().getBlockState(tileEntity.getPos());
		IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
				.getModelForState(state);

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		renderGlow(state, model);
		renderRange(tileEntity);
		GlStateManager.popMatrix();
	}

	private void renderGlow(IBlockState state, IBakedModel model) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.disableLighting();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.depthMask(true);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

		for (EnumFacing side : EnumFacing.values()) {
			putQuads(buffer, model.getQuads(state, side, seed));
		}
		putQuads(buffer, model.getQuads(state, null, seed));

		tessellator.draw();

		GlStateManager.enableLighting();
		RenderHelper.enableStandardItemLighting();
	}

	private void putQuads(BufferBuilder buffer, java.util.List<BakedQuad> quads) {
		for (BakedQuad quad : quads) {
			if (!glowIcon.equals(quad.getSprite().getIconName())) {
				continue;
			}
			int[] data = quad.getVertexData();
			int stride = data.length / 4;
			if (stride < 6) {
				continue;
			}
			for (int vertex = 0; vertex < 4; vertex++) {
				int offset = vertex * stride;
				float px = Float.intBitsToFloat(data[offset]);
				float py = Float.intBitsToFloat(data[offset + 1]);
				float pz = Float.intBitsToFloat(data[offset + 2]);
				float u = Float.intBitsToFloat(data[offset + 4]);
				float v = Float.intBitsToFloat(data[offset + 5]);
				buffer.pos(px, py, pz).color(1.0F, 1.0F, 1.0F, 1.0F)
						.tex(u, v).lightmap(0xF000F0, 0xF000F0).endVertex();
			}
		}
	}

	private void renderRange(TileEntity tileEntity) {
		int reachX;
		int reachY;
		int reachZ;
		int offsetX;
		int offsetY;
		int offsetZ;
		float red = 0.45F;
		float green = 0.90F;
		float blue = 1.0F;
		if (tileEntity instanceof ManaDeviceHost) {
			ManaDeviceHost host = (ManaDeviceHost) tileEntity;
			if (!host.showRange()) {
				return;
			}
			reachX = host.reachX();
			reachY = host.reachY();
			reachZ = host.reachZ();
			offsetX = host.offsetX();
			offsetY = host.offsetY();
			offsetZ = host.offsetZ();
		} else if (tileEntity instanceof RuneAcceleratorTileEntity) {
			// Cyan accent, matching the rune accelerator's texture.
			AbstractAcceleratorTileEntity accelerator =
					(AbstractAcceleratorTileEntity) tileEntity;
			if (!accelerator.showRange()) {
				return;
			}
			reachX = accelerator.reachX();
			reachY = accelerator.reachY();
			reachZ = accelerator.reachZ();
			offsetX = accelerator.offsetX();
			offsetY = accelerator.offsetY();
			offsetZ = accelerator.offsetZ();
		} else if (tileEntity instanceof TerrasteelAcceleratorTileEntity) {
			AbstractAcceleratorTileEntity accelerator =
					(AbstractAcceleratorTileEntity) tileEntity;
			if (!accelerator.showRange()) {
				return;
			}
			reachX = accelerator.reachX();
			reachY = accelerator.reachY();
			reachZ = accelerator.reachZ();
			offsetX = accelerator.offsetX();
			offsetY = accelerator.offsetY();
			offsetZ = accelerator.offsetZ();
			// Green accent, matching the terrasteel accelerator's texture.
			red = 0.45F;
			green = 1.0F;
			blue = 0.55F;
		} else if (tileEntity instanceof FloralOverclockTileEntity) {
			AbstractAcceleratorTileEntity accelerator =
					(AbstractAcceleratorTileEntity) tileEntity;
			if (!accelerator.showRange()) {
				return;
			}
			reachX = accelerator.reachX();
			reachY = accelerator.reachY();
			reachZ = accelerator.reachZ();
			offsetX = accelerator.offsetX();
			offsetY = accelerator.offsetY();
			offsetZ = accelerator.offsetZ();
			// Pink accent, matching the floral overclock's texture.
			red = 1.0F;
			green = 0.55F;
			blue = 0.90F;
		} else {
			return;
		}

		double minX = offsetX - reachX;
		double minY = offsetY - reachY;
		double minZ = offsetZ - reachZ;
		double maxX = offsetX + reachX + 1.0D;
		double maxY = offsetY + reachY + 1.0D;
		double maxZ = offsetZ + reachZ + 1.0D;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(5.0F);
		GlStateManager.depthMask(false);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();

		// Wide translucent halo first, then a bright thin core. Every device type gets the
		// same visual weight, so the accelerators no longer look dim next to the well/anchor.
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		box(buffer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, 0.30F);
		tessellator.draw();

		GlStateManager.glLineWidth(2.5F);
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		box(buffer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, 1.0F);
		tessellator.draw();

		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	private static void box(BufferBuilder buffer, double minX, double minY, double minZ,
							double maxX, double maxY, double maxZ,
							float red, float green, float blue, float alpha) {
		edge(buffer, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
		edge(buffer, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
		edge(buffer, minX, minY, minZ, minX, minY, maxZ, red, green, blue, alpha);
		edge(buffer, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
		edge(buffer, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
		edge(buffer, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
		edge(buffer, minX, maxY, minZ, minX, maxY, maxZ, red, green, blue, alpha);
		edge(buffer, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
		edge(buffer, minX, minY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
		edge(buffer, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
		edge(buffer, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
		edge(buffer, minX, maxY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
	}

	private static void edge(BufferBuilder buffer, double x1, double y1, double z1,
							 double x2, double y2, double z2,
							 float red, float green, float blue, float alpha) {
		buffer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
		buffer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
	}
}
