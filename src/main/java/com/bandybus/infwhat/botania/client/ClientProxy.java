package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.block.ModBlocks;
import com.bandybus.infwhat.botania.block.tile.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.tile.ManaWellTileEntity;
import com.bandybus.infwhat.botania.block.tile.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.tile.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Client entrypoint: models, tile renderers, world overlay and state packet receivers. */
public class ClientProxy extends com.bandybus.infwhat.botania.CommonProxy {

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		registerModels();

		ClientRegistry.bindTileEntitySpecialRenderer(ManaWellTileEntity.class,
				new RenderManaGlow<ManaWellTileEntity>("infwhat_botania:block/mana_well_pool"));
		ClientRegistry.bindTileEntitySpecialRenderer(ManaBeaconTileEntity.class,
				new RenderManaGlow<ManaBeaconTileEntity>("infwhat_botania:block/mana_beacon_lens"));
		ClientRegistry.bindTileEntitySpecialRenderer(ManaAnchorTileEntity.class,
				new RenderManaGlow<ManaAnchorTileEntity>("infwhat_botania:block/mana_anchor_core"));
		ClientRegistry.bindTileEntitySpecialRenderer(RuneAcceleratorTileEntity.class,
				new RenderManaGlow<RuneAcceleratorTileEntity>("infwhat_botania:block/rune_accelerator_top"));
		ClientRegistry.bindTileEntitySpecialRenderer(TerrasteelAcceleratorTileEntity.class,
				new RenderManaGlow<TerrasteelAcceleratorTileEntity>("infwhat_botania:block/terrasteel_accelerator_top"));
		ClientRegistry.bindTileEntitySpecialRenderer(FloralOverclockTileEntity.class,
				new RenderManaGlow<FloralOverclockTileEntity>("infwhat_botania:block/floral_overclock_top"));
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new DeviceLinkRenderer());
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
	}

	private static void registerModels() {
		item(ModBlocks.MANA_WELL);
		item(ModBlocks.MANA_BEACON);
		item(ModBlocks.MANA_ANCHOR);
		item(ModBlocks.RUNE_ACCELERATOR);
		item(ModBlocks.TERRASTEEL_ACCELERATOR);
		item(ModBlocks.FLORAL_OVERCLOCK);
		item(ModBlocks.GAIA_BEACON);
		plain(ModItems.CHANNEL_LINKER);
		plain(ModItems.EVERLASTING_FERTILIZER);
		plain(ModItems.RUNE_TEMPLATE);
		plain(ModItems.GAIA_CHARM);
	}

	private static void item(net.minecraft.block.Block block) {
		Item item = Item.getItemFromBlock(block);
		ModelLoader.setCustomModelResourceLocation(item, 0,
				new ModelResourceLocation(block.getRegistryName(), "inventory"));
	}

	private static void plain(Item item) {
		ModelLoader.setCustomModelResourceLocation(item, 0,
				new ModelResourceLocation(item.getRegistryName(), "inventory"));
	}

	@Override
	public void handleDeviceState(final ModNetwork.StateMessage message) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override
			public void run() {
				ClientStateReceiver.receive(message.pos(), message.state());
			}
		});
	}

	@Override
	public void handleAcceleratorState(final ModNetwork.AcceleratorStateMessage message) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override
			public void run() {
				ClientStateReceiver.receiveAccelerator(message.pos(), message.state());
			}
		});
	}
}
