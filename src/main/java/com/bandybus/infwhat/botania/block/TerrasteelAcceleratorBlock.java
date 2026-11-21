package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.TerrasteelAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

/** Feeds nearby Terrestrial Agglomeration Plates. */
public class TerrasteelAcceleratorBlock extends AbstractAcceleratorBlock {

    public TerrasteelAcceleratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public AcceleratorKind kind() {
        return AcceleratorKind.TERRASTEEL;
    }

    @Override
    public TileEntity newBlockEntity(IBlockReader world) {
        return new TerrasteelAcceleratorTileEntity();
    }
}
