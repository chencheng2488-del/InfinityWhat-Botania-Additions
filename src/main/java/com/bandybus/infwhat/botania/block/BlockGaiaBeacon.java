package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.GaiaBeaconTileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/** Finishes an ongoing Gaia fight once the boss bar is ready; it never summons the guardian. */
public class BlockGaiaBeacon extends BlockContainer {

    private IIcon topIcon;
    private IIcon sideIcon;

    public BlockGaiaBeacon(String name) {
        super(Material.iron);
        this.setUnlocalizedName(InfinityWhatBotania.MODID + "." + name);
        this.setTextureName(InfinityWhatBotania.MODID + ":gaia_beacon_side");
        this.setHardness(2.0F);
        this.setResistance(6.0F);
        this.setStepSound(Block.soundTypeStone);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(com.bandybus.infwhat.botania.util.CreativeTabHelper.coreTab());
        this.setLightLevel(12.0F / 15.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new GaiaBeaconTileEntity();
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        if (side == 1) {
            return topIcon;
        }
        if (side == 0) {
            return Blocks.obsidian.getIcon(0, 0);
        }
        return sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        topIcon = register.registerIcon(InfinityWhatBotania.MODID + ":gaia_beacon_top");
        sideIcon = register.registerIcon(InfinityWhatBotania.MODID + ":gaia_beacon_side");
    }
}

