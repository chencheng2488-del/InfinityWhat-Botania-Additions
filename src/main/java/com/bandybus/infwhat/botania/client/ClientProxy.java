package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.CommonProxy;
import com.bandybus.infwhat.botania.block.BlockModelRenderer;
import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.ModRenderIds;
import com.bandybus.infwhat.botania.block.entity.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.block.entity.GaiaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.block.entity.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.entity.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.client.render.BlockItemRenderer;
import com.bandybus.infwhat.botania.client.render.DeviceLinkRenderer;
import com.bandybus.infwhat.botania.client.render.RangePreviewRenderer;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

/** Client-only renderer registration for the 1.7.10 release. */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ModRenderIds.blockModel = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(BlockModelRenderer.INSTANCE);

        Block[] blocks = {
                ModBlocks.MANA_WELL, ModBlocks.MANA_BEACON, ModBlocks.MANA_ANCHOR,
                ModBlocks.RUNE_ACCELERATOR, ModBlocks.TERRASTEEL_ACCELERATOR,
                ModBlocks.FLORAL_OVERCLOCK, ModBlocks.GAIA_BEACON
        };
        for (Block block : blocks) {
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(block),
                    new BlockItemRenderer(block));
        }

        RangePreviewRenderer rangeRenderer = new RangePreviewRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(ManaWellTileEntity.class, rangeRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(ManaBeaconTileEntity.class, rangeRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(ManaAnchorTileEntity.class, rangeRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(RuneAcceleratorTileEntity.class,
                rangeRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(TerrasteelAcceleratorTileEntity.class,
                rangeRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(FloralOverclockTileEntity.class,
                rangeRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(GaiaBeaconTileEntity.class, rangeRenderer);

        MinecraftForge.EVENT_BUS.register(new DeviceLinkRenderer());
    }

    @Override
    public void applyDeviceState(com.bandybus.infwhat.botania.util.Pos pos,
                                 com.bandybus.infwhat.botania.device.DeviceState state) {
        ClientPacketBridge.applyDevice(pos, state);
    }

    @Override
    public void applyAcceleratorState(com.bandybus.infwhat.botania.util.Pos pos,
                                      com.bandybus.infwhat.botania.device.AcceleratorState state) {
        ClientPacketBridge.applyAccelerator(pos, state);
    }
}
