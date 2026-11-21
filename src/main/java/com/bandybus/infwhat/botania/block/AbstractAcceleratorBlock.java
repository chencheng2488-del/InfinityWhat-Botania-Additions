package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.tile.AbstractAcceleratorTileEntity;
import com.bandybus.infwhat.botania.container.ModGuiHandler;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.block.SoundType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import com.bandybus.infwhat.botania.creative.ModCreativeTab;

/** Shared block behaviour for the three accelerators. */
public abstract class AbstractAcceleratorBlock extends Block {

	protected AbstractAcceleratorBlock(float lightLevel) {
		super(Material.ROCK);
		setHardness(2.0F);
		setResistance(6.0F);
		setSoundType(SoundType.STONE);
		setLightLevel(lightLevel);
		setCreativeTab(ModCreativeTab.get());
		setHarvestLevel("pickaxe", 1);
	}

	public abstract AcceleratorKind kind();

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
									EntityPlayer player, EnumHand hand, EnumFacing facing,
									float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		}
		TileEntity tileEntity = world.getTileEntity(pos);
		if (!(tileEntity instanceof AbstractAcceleratorTileEntity)) {
			return false;
		}
		AbstractAcceleratorTileEntity accelerator = (AbstractAcceleratorTileEntity) tileEntity;
		if (player.isSneaking()) {
			boolean enabled = accelerator.toggleEnabled();
			player.sendStatusMessage(new TextComponentTranslation(enabled
					? "message.infwhat_botania.accelerator.on"
					: "message.infwhat_botania.accelerator.off"), true);
		} else {
			player.openGui(InfinityWhatBotania.instance, ModGuiHandler.GUI_ACCELERATOR,
					world, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}
}
