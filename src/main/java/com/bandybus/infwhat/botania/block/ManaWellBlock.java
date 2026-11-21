package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.tile.ManaWellTileEntity;
import com.bandybus.infwhat.botania.container.ModGuiHandler;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.block.SoundType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import com.bandybus.infwhat.botania.creative.ModCreativeTab;

import java.util.List;

/** The Mana Well block. Right-click opens the GUI; sneak-right-click toggles output. */
public class ManaWellBlock extends Block {

	/** Matches the block model element by element, so the well has no invisible volume. */
	private static final AxisAlignedBB[] SHAPE = {
			ShapeUtil.box(0, 0, 0, 16, 3, 16),
			ShapeUtil.box(3, 3, 3, 13, 8, 13),
			ShapeUtil.box(13, 3, 3, 16, 10, 13),
			ShapeUtil.box(0, 3, 0, 16, 10, 3),
			ShapeUtil.box(0, 3, 13, 16, 10, 16),
			ShapeUtil.box(0, 3, 3, 3, 10, 13),
			ShapeUtil.box(13, 10, 3, 16, 11, 13),
			ShapeUtil.box(0, 10, 0, 16, 11, 3),
			ShapeUtil.box(0, 10, 13, 16, 11, 16),
			ShapeUtil.box(0, 10, 3, 3, 11, 13),
			ShapeUtil.box(0, 11, 0, 2, 14, 2),
			ShapeUtil.box(14, 11, 0, 16, 14, 2),
			ShapeUtil.box(0, 11, 14, 2, 14, 16),
			ShapeUtil.box(14, 11, 14, 16, 14, 16)
	};

	public ManaWellBlock() {
		super(Material.ROCK);
		setHardness(2.0F);
		setResistance(6.0F);
		setSoundType(SoundType.STONE);
		setLightLevel(1.0F);
		setLightOpacity(0);
		setCreativeTab(ModCreativeTab.get());
		setHarvestLevel("pickaxe", 1);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new ManaWellTileEntity();
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
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return ShapeUtil.union(SHAPE);
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
									  AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
									  Entity entity, boolean isActualState) {
		ShapeUtil.addCollision(collidingBoxes, entityBox, SHAPE, pos);
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
			if (!world.isRemote && world.getTileEntity(pos) instanceof ManaWellTileEntity) {
				boolean enabled = ((ManaWellTileEntity) world.getTileEntity(pos)).toggleEnabled();
				player.sendStatusMessage(new TextComponentTranslation(enabled
						? "message.infwhat_botania.mana_well.on"
						: "message.infwhat_botania.mana_well.off"), true);
			}
			return true;
		}
		if (!world.isRemote) {
			player.openGui(InfinityWhatBotania.instance, ModGuiHandler.GUI_DEVICE,
					world, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}
}
