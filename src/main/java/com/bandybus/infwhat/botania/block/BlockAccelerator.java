package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.block.entity.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.block.entity.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.container.ModGuiIds;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.item.ModItems;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * The three ranged accelerators. They are full cubes with a front texture; metadata 0-3 stores
 * the horizontal facing and never changes the block name.
 */
public class BlockAccelerator extends BlockContainer {

    /** Front side (Minecraft side id) for each metadata value. */
    private static final int[] FRONT_SIDES = {3, 4, 2, 5};

    private final AcceleratorKind kind;
    private IIcon topIcon;
    private IIcon sideIcon;
    private IIcon frontIcon;
    private IIcon bottomIcon;

    public BlockAccelerator(AcceleratorKind kind, String name) {
        super(kind == AcceleratorKind.FLORAL ? Material.wood : Material.iron);
        this.kind = kind;
        this.setUnlocalizedName(InfinityWhatBotania.MODID + "." + name);
        this.setTextureName(InfinityWhatBotania.MODID + ":" + name + "_top");
        this.setHardness(2.0F);
        this.setResistance(6.0F);
        this.setStepSound(kind == AcceleratorKind.FLORAL ? Block.soundTypeWood : Block.soundTypeStone);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(com.bandybus.infwhat.botania.util.CreativeTabHelper.coreTab());
        this.setLightLevel((kind == AcceleratorKind.FLORAL ? 7.0F : 9.0F) / 15.0F);
    }

    public AcceleratorKind kind() {
        return kind;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        if (kind == AcceleratorKind.RUNE) {
            return new RuneAcceleratorTileEntity();
        }
        if (kind == AcceleratorKind.TERRASTEEL) {
            return new TerrasteelAcceleratorTileEntity();
        }
        return new FloralOverclockTileEntity();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                    int side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() == ModItems.CHANNEL_LINKER) {
            return false;
        }
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(x, y, z);
            if (tileEntity instanceof com.bandybus.infwhat.botania.block.entity
                    .AbstractAcceleratorTileEntity) {
                com.bandybus.infwhat.botania.block.entity.AbstractAcceleratorTileEntity
                        accelerator = (com.bandybus.infwhat.botania.block.entity
                        .AbstractAcceleratorTileEntity) tileEntity;
                if (player.isSneaking()) {
                    boolean enabled = accelerator.toggleEnabled();
                    player.addChatMessage(new ChatComponentTranslation(enabled
                            ? "message.infwhat_botania.accelerator.on"
                            : "message.infwhat_botania.accelerator.off"));
                } else {
                    player.openGui(InfinityWhatBotania.instance, ModGuiIds.idFor(kind),
                            world, x, y, z);
                }
            }
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer,
                                ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        int facing = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        // The front texture points at the player who placed the block.
        int metadata = (facing + 2) & 3;
        world.setBlockMetadataWithNotify(x, y, z, metadata, 2);
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        if (side == 0) {
            return bottomIcon;
        }
        if (side == 1) {
            return topIcon;
        }
        int front = FRONT_SIDES[metadata & 3];
        return side == front ? frontIcon : sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        String prefix = InfinityWhatBotania.MODID + ":";
        String name = kind == AcceleratorKind.RUNE ? "rune_accelerator"
                : kind == AcceleratorKind.TERRASTEEL ? "terrasteel_accelerator"
                : "floral_overclock";
        topIcon = register.registerIcon(prefix + name + "_top");
        sideIcon = register.registerIcon(prefix + name + "_side");
        frontIcon = register.registerIcon(prefix + name + "_front");
        bottomIcon = register.registerIcon(prefix + "accelerator_bottom");
    }
}

