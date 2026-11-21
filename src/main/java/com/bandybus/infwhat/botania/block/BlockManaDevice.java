package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.ManaAnchorTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconTileEntity;
import com.bandybus.infwhat.botania.block.entity.ManaWellTileEntity;
import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.container.ModGuiIds;
import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.grid.ManaNetworkData;
import com.bandybus.infwhat.botania.item.ModItems;
import com.bandybus.infwhat.botania.util.Pos;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The mana well, the mana beacon and the mana anchor.
 *
 * <p>All three share the same interaction rules: right-click opens the control panel, sneak
 * right-click toggles the block, and the channel linker is allowed to handle the click itself.
 * They only differ in shape, light and tile entity.
 */
public class BlockManaDevice extends BlockContainer implements IModelBlock {

    /** Which of the three structures this block instance is. */
    public enum Kind {
        WELL,
        BEACON,
        ANCHOR
    }

    private final Kind kind;
    private IIcon stoneIcon;
    private IIcon wallIcon;
    private IIcon rimIcon;
    private IIcon poolIcon;
    private IIcon metalIcon;
    private IIcon stemIcon;
    private IIcon lensIcon;
    private IIcon runeIcon;
    private IIcon coreIcon;
    private BlockModel model;

    public BlockManaDevice(Kind kind, String name) {
        super(Material.iron);
        this.kind = kind;
        this.setUnlocalizedName(InfinityWhatBotania.MODID + "." + name);
        this.setTextureName(InfinityWhatBotania.MODID + ":mana_stone");
        this.setHardness(2.0F);
        this.setResistance(6.0F);
        this.setStepSound(Block.soundTypeStone);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(com.bandybus.infwhat.botania.util.CreativeTabHelper.coreTab());
        if (kind == Kind.WELL) {
            this.setLightLevel(1.0F);
        } else if (kind == Kind.BEACON) {
            this.setLightLevel(12.0F / 15.0F);
        } else {
            this.setLightLevel(8.0F / 15.0F);
        }
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        if (kind == Kind.WELL) {
            return new ManaWellTileEntity();
        }
        if (kind == Kind.BEACON) {
            return new ManaBeaconTileEntity();
        }
        return new ManaAnchorTileEntity();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                    int side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() == ModItems.CHANNEL_LINKER) {
            return false;
        }
        if (player.isSneaking()) {
            if (!world.isRemote) {
                TileEntity tileEntity = world.getTileEntity(x, y, z);
                boolean enabled = true;
                if (kind == Kind.WELL && tileEntity instanceof ManaWellTileEntity) {
                    enabled = ((ManaWellTileEntity) tileEntity).toggleEnabled();
                    player.addChatMessage(new ChatComponentTranslation(enabled
                            ? "message.infwhat_botania.mana_well.on"
                            : "message.infwhat_botania.mana_well.off"));
                } else if (tileEntity instanceof com.bandybus.infwhat.botania.block.entity
                        .AbstractManaEndpointTileEntity) {
                    com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity
                            endpoint = (com.bandybus.infwhat.botania.block.entity
                            .AbstractManaEndpointTileEntity) tileEntity;
                    if (!endpoint.isOwnedBy(player.getUniqueID())) {
                        if (endpoint.getOwnerId() != null) {
                            player.addChatMessage(new ChatComponentTranslation(
                                    "gui.infwhat_botania.not_owner"));
                            return true;
                        }
                        endpoint.setOwner(player.getUniqueID());
                    }
                    enabled = endpoint.toggleEnabled();
                    player.addChatMessage(new ChatComponentTranslation(enabled
                            ? "message.infwhat_botania.endpoint.on"
                            : "message.infwhat_botania.endpoint.off"));
                }
            }
            return true;
        }
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(x, y, z);
            if (tileEntity instanceof com.bandybus.infwhat.botania.block.entity
                    .AbstractManaEndpointTileEntity) {
                com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity
                        endpoint = (com.bandybus.infwhat.botania.block.entity
                        .AbstractManaEndpointTileEntity) tileEntity;
                if (endpoint.getOwnerId() != null
                        && !endpoint.isOwnedBy(player.getUniqueID())) {
                    player.addChatMessage(new ChatComponentTranslation(
                            "gui.infwhat_botania.not_owner"));
                    return true;
                }
            }
            player.openGui(InfinityWhatBotania.instance, ModGuiIds.idFor(kind),
                    world, x, y, z);
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z,
                                net.minecraft.entity.EntityLivingBase placer,
                                ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (world.isRemote || !(placer instanceof EntityPlayer)) {
            return;
        }
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof com.bandybus.infwhat.botania.block.entity
                .AbstractManaEndpointTileEntity) {
            ((com.bandybus.infwhat.botania.block.entity.AbstractManaEndpointTileEntity)
                    tileEntity).setOwner(((EntityPlayer) placer).getUniqueID());
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        if (!world.isRemote) {
            ManaNetworkData data = ManaNetworkData.get(world);
            if (data != null) {
                data.removeNode(world, new Pos(x, y, z));
            }
        }
        super.breakBlock(world, x, y, z, block, metadata);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        if (kind == Kind.WELL) {
            return AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 14.0D / 16.0D, z + 1.0D);
        }
        if (kind == Kind.ANCHOR) {
            return AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 9.0D / 16.0D, z + 1.0D);
        }
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        if (kind == Kind.WELL) {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 14.0F / 16.0F, 1.0F);
        } else if (kind == Kind.ANCHOR) {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 9.0F / 16.0F, 1.0F);
        } else {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return ModRenderIds.blockModel;
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        return stoneIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        stoneIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_stone");
        if (kind == Kind.WELL) {
            wallIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_well_wall");
            rimIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_well_rim");
            poolIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_well_pool");
            metalIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_metal");
        } else if (kind == Kind.BEACON) {
            metalIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_metal");
            stemIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_beacon_stem");
            lensIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_beacon_lens");
        } else {
            metalIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_metal");
            runeIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_anchor_rune");
            coreIcon = register.registerIcon(InfinityWhatBotania.MODID + ":mana_anchor_core");
        }
    }

    @Override
    public BlockModel model(int metadata) {
        if (model == null && stoneIcon != null) {
            model = buildModel();
        }
        return model == null ? new BlockModel() : model;
    }

    private BlockModel buildModel() {
        BlockModel result = new BlockModel();
        if (kind == Kind.WELL) {
            BlockModel.Box base = result.add(0, 0, 0, 16, 3, 16, stoneIcon);
            sides(base, stoneIcon, 16.0F, 3.0F);
            topBottom(base, stoneIcon, 16.0F, 16.0F);

            BlockModel.Box pool = result.add(3, 3, 3, 13, 8, 13, stoneIcon);
            pool.setFace(0, stoneIcon, 0.0F, 0.0F, 10.0F, 10.0F);
            pool.setFace(1, poolIcon, 0.0F, 0.0F, 16.0F, 16.0F);
            sides(pool, stoneIcon, 10.0F, 5.0F);

            BlockModel.Box wall1 = result.add(13, 3, 3, 16, 10, 13, wallIcon);
            wall1.setFace(0, wallIcon, 0.0F, 0.0F, 3.0F, 10.0F);
            wall1.setFace(1, wallIcon, 0.0F, 0.0F, 3.0F, 10.0F);
            wall1.setFace(2, wallIcon, 0.0F, 0.0F, 3.0F, 7.0F);
            wall1.setFace(3, wallIcon, 0.0F, 0.0F, 3.0F, 7.0F);
            wall1.setFace(4, wallIcon, 0.0F, 0.0F, 10.0F, 7.0F);
            wall1.setFace(5, wallIcon, 3.0F, 0.0F, 13.0F, 7.0F);

            BlockModel.Box wall2 = result.add(0, 3, 0, 16, 10, 3, wallIcon);
            wall2.setFace(0, wallIcon, 0.0F, 0.0F, 16.0F, 3.0F);
            wall2.setFace(1, wallIcon, 0.0F, 0.0F, 16.0F, 3.0F);
            wall2.setFace(2, wallIcon, 0.0F, 0.0F, 16.0F, 7.0F);
            wall2.setFace(3, wallIcon, 0.0F, 0.0F, 16.0F, 7.0F);
            wall2.setFace(4, wallIcon, 0.0F, 0.0F, 3.0F, 7.0F);
            wall2.setFace(5, wallIcon, 0.0F, 0.0F, 3.0F, 7.0F);

            BlockModel.Box wall3 = result.add(0, 3, 13, 16, 10, 16, wallIcon);
            wall3.setFace(0, wallIcon, 0.0F, 0.0F, 16.0F, 3.0F);
            wall3.setFace(1, wallIcon, 0.0F, 0.0F, 16.0F, 3.0F);
            wall3.setFace(2, wallIcon, 0.0F, 0.0F, 16.0F, 7.0F);
            wall3.setFace(3, wallIcon, 0.0F, 0.0F, 16.0F, 7.0F);
            wall3.setFace(4, wallIcon, 0.0F, 0.0F, 3.0F, 7.0F);
            wall3.setFace(5, wallIcon, 0.0F, 0.0F, 3.0F, 7.0F);

            BlockModel.Box wall4 = result.add(0, 3, 3, 3, 10, 13, wallIcon);
            wall4.setFace(0, wallIcon, 0.0F, 0.0F, 3.0F, 10.0F);
            wall4.setFace(1, wallIcon, 0.0F, 0.0F, 3.0F, 10.0F);
            wall4.setFace(2, wallIcon, 0.0F, 0.0F, 3.0F, 7.0F);
            wall4.setFace(3, wallIcon, 0.0F, 0.0F, 3.0F, 7.0F);
            wall4.setFace(4, wallIcon, 3.0F, 0.0F, 13.0F, 7.0F);
            wall4.setFace(5, wallIcon, 0.0F, 0.0F, 10.0F, 7.0F);

            BlockModel.Box rim1 = result.add(13, 10, 3, 16, 11, 13, rimIcon);
            rim1.setFace(0, rimIcon, 0.0F, 0.0F, 3.0F, 10.0F);
            rim1.setFace(1, rimIcon, 1.0F, 0.0F, 4.0F, 10.0F);
            rim1.setFace(2, rimIcon, 0.0F, 0.0F, 3.0F, 1.0F);
            rim1.setFace(3, rimIcon, 0.0F, 0.0F, 3.0F, 1.0F);
            rim1.setFace(4, rimIcon, 0.0F, 0.0F, 10.0F, 1.0F);
            rim1.setFace(5, rimIcon, 0.0F, 0.0F, 10.0F, 1.0F);

            BlockModel.Box rim2 = result.add(0, 10, 0, 16, 11, 3, rimIcon);
            rim2.setFace(0, rimIcon, 0.0F, 0.0F, 16.0F, 3.0F);
            rim2.setFace(1, rimIcon, 0.0F, 2.0F, 16.0F, 5.0F);
            rim2.setFace(2, rimIcon, 0.0F, 0.0F, 16.0F, 1.0F);
            rim2.setFace(3, rimIcon, 0.0F, 0.0F, 16.0F, 1.0F);
            rim2.setFace(4, rimIcon, 0.0F, 0.0F, 3.0F, 1.0F);
            rim2.setFace(5, rimIcon, 0.0F, 0.0F, 3.0F, 1.0F);

            BlockModel.Box rim3 = result.add(0, 10, 13, 16, 11, 16, rimIcon);
            rim3.setFace(0, rimIcon, 0.0F, 0.0F, 16.0F, 3.0F);
            rim3.setFace(1, rimIcon, 0.0F, 5.0F, 16.0F, 8.0F);
            rim3.setFace(2, rimIcon, 0.0F, 0.0F, 16.0F, 1.0F);
            rim3.setFace(3, rimIcon, 0.0F, 0.0F, 16.0F, 1.0F);
            rim3.setFace(4, rimIcon, 0.0F, 0.0F, 3.0F, 1.0F);
            rim3.setFace(5, rimIcon, 0.0F, 0.0F, 3.0F, 1.0F);

            BlockModel.Box rim4 = result.add(0, 10, 3, 3, 11, 13, rimIcon);
            rim4.setFace(0, rimIcon, 0.0F, 0.0F, 3.0F, 10.0F);
            rim4.setFace(1, rimIcon, 11.0F, 5.0F, 14.0F, 15.0F);
            rim4.setFace(2, rimIcon, 0.0F, 0.0F, 3.0F, 1.0F);
            rim4.setFace(3, rimIcon, 0.0F, 0.0F, 3.0F, 1.0F);
            rim4.setFace(4, rimIcon, 0.0F, 0.0F, 10.0F, 1.0F);
            rim4.setFace(5, rimIcon, 0.0F, 0.0F, 10.0F, 1.0F);

            column(result, 0, 11, 0, 2, 14, 2, metalIcon);
            column(result, 14, 11, 0, 16, 14, 2, metalIcon);
            column(result, 0, 11, 14, 2, 14, 16, metalIcon);
            column(result, 14, 11, 14, 16, 14, 16, metalIcon);
        } else if (kind == Kind.BEACON) {
            BlockModel.Box base = result.add(0, 0, 0, 16, 3, 16, stoneIcon);
            sides(base, stoneIcon, 16.0F, 3.0F);
            topBottom(base, stoneIcon, 16.0F, 16.0F);

            BlockModel.Box pedestal = result.add(2, 3, 2, 14, 5, 14, metalIcon);
            sides(pedestal, metalIcon, 12.0F, 2.0F);
            topBottom(pedestal, metalIcon, 12.0F, 12.0F);

            BlockModel.Box stem = result.add(6, 5, 6, 10, 12, 10, stemIcon);
            sides(stem, stemIcon, 4.0F, 7.0F);
            topBottom(stem, stemIcon, 4.0F, 4.0F);

            BlockModel.Box platform = result.add(3, 12, 3, 13, 13, 13, metalIcon);
            sides(platform, metalIcon, 10.0F, 1.0F);
            topBottom(platform, metalIcon, 10.0F, 10.0F);

            BlockModel.Box lens = result.add(5, 13, 5, 11, 16, 11, lensIcon);
            allFaces(lens, lensIcon, 16.0F, 16.0F);

            BlockModel.Box ring1 = result.add(5, 7, 5, 11, 8, 11, metalIcon);
            sides(ring1, metalIcon, 6.0F, 1.0F);
            topBottom(ring1, metalIcon, 6.0F, 6.0F);

            BlockModel.Box ring2 = result.add(5, 10, 5, 11, 11, 11, metalIcon);
            sides(ring2, metalIcon, 6.0F, 1.0F);
            topBottom(ring2, metalIcon, 6.0F, 6.0F);

            column(result, 3, 13, 3, 5, 16, 5, metalIcon);
            column(result, 11, 13, 3, 13, 16, 5, metalIcon);
            column(result, 3, 13, 11, 5, 16, 13, metalIcon);
            column(result, 11, 13, 11, 13, 16, 13, metalIcon);
        } else {
            BlockModel.Box base = result.add(0, 0, 0, 16, 3, 16, stoneIcon);
            sides(base, stoneIcon, 16.0F, 3.0F);
            topBottom(base, stoneIcon, 16.0F, 16.0F);

            BlockModel.Box core = result.add(7, 4, 7, 9, 7, 9, coreIcon);
            allFaces(core, coreIcon, 16.0F, 16.0F);

            BlockModel.Box plate = result.add(1, 3, 1, 15, 4, 15, stoneIcon);
            plate.setFace(0, stoneIcon, 0.0F, 0.0F, 14.0F, 14.0F);
            plate.setFace(1, runeIcon, 0.0F, 0.0F, 16.0F, 16.0F);
            sides(plate, stoneIcon, 14.0F, 1.0F);

            column(result, 1, 4, 1, 3, 9, 3, metalIcon);
            column(result, 13, 4, 1, 15, 9, 3, metalIcon);
            column(result, 1, 4, 13, 3, 9, 15, metalIcon);
            column(result, 13, 4, 13, 15, 9, 15, metalIcon);

            BlockModel.Box ring1 = result.add(5, 4, 4, 11, 5, 6, metalIcon);
            ring1.setFace(0, metalIcon, 0.0F, 0.0F, 6.0F, 2.0F);
            ring1.setFace(1, metalIcon, 0.0F, 0.0F, 6.0F, 2.0F);
            ring1.setFace(2, metalIcon, 0.0F, 0.0F, 6.0F, 1.0F);
            ring1.setFace(3, metalIcon, 0.0F, 0.0F, 6.0F, 1.0F);
            ring1.setFace(4, metalIcon, 0.0F, 0.0F, 2.0F, 1.0F);
            ring1.setFace(5, metalIcon, 0.0F, 0.0F, 2.0F, 1.0F);

            BlockModel.Box ring2 = result.add(5, 4, 10, 11, 5, 12, metalIcon);
            ring2.setFace(0, metalIcon, 0.0F, 0.0F, 6.0F, 2.0F);
            ring2.setFace(1, metalIcon, 0.0F, 0.0F, 6.0F, 2.0F);
            ring2.setFace(2, metalIcon, 0.0F, 0.0F, 6.0F, 1.0F);
            ring2.setFace(3, metalIcon, 0.0F, 0.0F, 6.0F, 1.0F);
            ring2.setFace(4, metalIcon, 0.0F, 0.0F, 2.0F, 1.0F);
            ring2.setFace(5, metalIcon, 0.0F, 0.0F, 2.0F, 1.0F);

            BlockModel.Box ring3 = result.add(4, 4, 6, 6, 5, 10, metalIcon);
            ring3.setFace(0, metalIcon, 0.0F, 0.0F, 2.0F, 4.0F);
            ring3.setFace(1, metalIcon, 0.0F, 0.0F, 2.0F, 4.0F);
            ring3.setFace(2, metalIcon, 0.0F, 0.0F, 2.0F, 1.0F);
            ring3.setFace(3, metalIcon, 0.0F, 0.0F, 2.0F, 1.0F);
            ring3.setFace(4, metalIcon, 0.0F, 0.0F, 4.0F, 1.0F);
            ring3.setFace(5, metalIcon, 0.0F, 0.0F, 4.0F, 1.0F);

            BlockModel.Box ring4 = result.add(10, 4, 6, 12, 5, 10, metalIcon);
            ring4.setFace(0, metalIcon, 0.0F, 0.0F, 2.0F, 4.0F);
            ring4.setFace(1, metalIcon, 0.0F, 0.0F, 2.0F, 4.0F);
            ring4.setFace(2, metalIcon, 0.0F, 0.0F, 2.0F, 1.0F);
            ring4.setFace(3, metalIcon, 0.0F, 0.0F, 2.0F, 1.0F);
            ring4.setFace(4, metalIcon, 0.0F, 0.0F, 4.0F, 1.0F);
            ring4.setFace(5, metalIcon, 0.0F, 0.0F, 4.0F, 1.0F);
        }
        return result;
    }

    private static void sides(BlockModel.Box box, IIcon icon, float u1, float v1) {
        box.setFace(2, icon, 0.0F, 0.0F, u1, v1);
        box.setFace(3, icon, 0.0F, 0.0F, u1, v1);
        box.setFace(4, icon, 0.0F, 0.0F, u1, v1);
        box.setFace(5, icon, 0.0F, 0.0F, u1, v1);
    }

    private static void topBottom(BlockModel.Box box, IIcon icon, float u1, float v1) {
        box.setFace(0, icon, 0.0F, 0.0F, u1, v1);
        box.setFace(1, icon, 0.0F, 0.0F, u1, v1);
    }

    private static void allFaces(BlockModel.Box box, IIcon icon, float u1, float v1) {
        for (int face = 0; face < 6; face++) {
            box.setFace(face, icon, 0.0F, 0.0F, u1, v1);
        }
    }

    private static void column(BlockModel result, float x1, float y1, float z1,
                               float x2, float y2, float z2, IIcon icon) {
        BlockModel.Box box = result.add(x1, y1, z1, x2, y2, z2, icon);
        sides(box, icon, Math.max(x2 - x1, z2 - z1), y2 - y1);
        topBottom(box, icon, x2 - x1, z2 - z1);
    }

    /** Direct endpoint gate used by the tile entities. */
    public static boolean canOperate(World world) {
        return BotaniaAdditionConfig.canOperate(world);
    }

    @Override
    public String getLocalizedName() {
        return net.minecraft.util.StatCollector.translateToLocal(getUnlocalizedName() + ".name");
    }

    /** Convenience for the GUI handler. */
    public DeviceKind deviceKind() {
        return kind == Kind.WELL ? DeviceKind.WELL
                : kind == Kind.BEACON ? DeviceKind.BEACON : DeviceKind.ANCHOR;
    }
}

