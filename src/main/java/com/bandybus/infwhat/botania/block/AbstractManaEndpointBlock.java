package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.tile.AbstractManaEndpointTileEntity;
import com.bandybus.infwhat.botania.container.ModGuiHandler;
import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.block.SoundType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import com.bandybus.infwhat.botania.creative.ModCreativeTab;

/** Shared block behaviour for the two wireless endpoints. */
public abstract class AbstractManaEndpointBlock extends Block {

	protected AbstractManaEndpointBlock(Material material) {
		super(material);
		setHardness(2.0F);
		setResistance(6.0F);
		setSoundType(SoundType.STONE);
		setLightOpacity(0);
		setCreativeTab(ModCreativeTab.get());
		setHarvestLevel("pickaxe", 1);
	}

	public abstract DeviceKind kind();

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
								EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		if (!world.isRemote && placer instanceof EntityPlayer
				&& world.getTileEntity(pos) instanceof AbstractManaEndpointTileEntity) {
			((AbstractManaEndpointTileEntity) world.getTileEntity(pos))
					.setOwner(placer.getUniqueID());
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote) {
			ManaNetworkData data = ManaNetworkData.get(world);
			if (data != null) {
				data.removeNode(world, pos);
			}
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
									EntityPlayer player, EnumHand hand, EnumFacing facing,
									float hitX, float hitY, float hitZ) {
		if (player.getHeldItem(hand).getItem() == ModItems.CHANNEL_LINKER) {
			return false;
		}
		if (player.isSneaking()) {
			toggle(world, pos, player);
			return true;
		}
		if (!world.isRemote) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if (tileEntity instanceof AbstractManaEndpointTileEntity) {
				AbstractManaEndpointTileEntity endpoint =
						(AbstractManaEndpointTileEntity) tileEntity;
				if (endpoint.getOwnerId() != null
						&& !endpoint.isOwnedBy(player.getUniqueID())) {
					player.sendStatusMessage(
							new TextComponentTranslation("gui.infwhat_botania.not_owner"), true);
					return true;
				}
			}
			player.openGui(InfinityWhatBotania.instance, ModGuiHandler.GUI_DEVICE,
					world, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}

	protected void toggle(World world, BlockPos pos, EntityPlayer player) {
		if (!world.isRemote && world.getTileEntity(pos) instanceof AbstractManaEndpointTileEntity) {
			boolean enabled = ((AbstractManaEndpointTileEntity) world.getTileEntity(pos))
					.toggleEnabled();
			player.sendStatusMessage(new TextComponentTranslation(enabled
					? "message.infwhat_botania.endpoint.on"
					: "message.infwhat_botania.endpoint.off"), true);
		}
	}
}
