package com.bandybus.infwhat.botania.client.render;

import com.bandybus.infwhat.botania.block.BlockModel;
import com.bandybus.infwhat.botania.block.BlockModelRenderer;
import com.bandybus.infwhat.botania.block.IModelBlock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

/** Draws the 3D block model for the add-on's block items (inventory, hand and dropped item). */
@SideOnly(Side.CLIENT)
public final class BlockItemRenderer implements IItemRenderer {

    private final net.minecraft.block.Block block;

    public BlockItemRenderer(net.minecraft.block.Block block) {
        this.block = block;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY
                || type == ItemRenderType.EQUIPPED
                || type == ItemRenderType.EQUIPPED_FIRST_PERSON
                || type == ItemRenderType.ENTITY;
    }

    /**
     * Let Minecraft apply its standard block transforms for each render type.
     *
     * <p>The previous implementation returned {@code false} and tried to position the model
     * manually, which left the item invisible in the inventory, in hand and as a dropped entity.
     * The model is already expressed in block-local 0..1 coordinates, so the vanilla block
     * helper transforms are the correct frame for all four render types.
     */
    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
                                         ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        BlockModel model;
        if (block instanceof IModelBlock) {
            model = ((IModelBlock) block).model(item.getMetadata());
        } else {
            model = cubeModel(block, item.getMetadata());
        }
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        boolean cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        // The hand-written quads are not guaranteed to wind correctly for every face, and GUI
        // item rendering keeps GL_CULL_FACE enabled. Draw both sides so the block item is always
        // visible in the inventory, in hand and as a dropped entity.
        GL11.glDisable(GL11.GL_CULL_FACE);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        BlockModelRenderer.drawModel(model, true);
        tessellator.draw();
        if (cullFace) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        RenderHelper.enableStandardItemLighting();
        GL11.glPopMatrix();
    }

    /** Full-cube model for the accelerators and the Gaia Beacon, which use standard icons. */
    private static BlockModel cubeModel(net.minecraft.block.Block block, int metadata) {
        BlockModel model = new BlockModel();
        BlockModel.Box box = new BlockModel.Box(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F,
                block.getIcon(0, metadata));
        for (int face = 0; face < 6; face++) {
            box.faces[face] = block.getIcon(face, metadata);
        }
        model.boxes.add(box);
        return model;
    }
}
