package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.FloralOverclockTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

/** Ticks nearby generating flowers multiple times per game tick. */
public class FloralOverclockBlock extends AbstractAcceleratorBlock {

    public FloralOverclockBlock(Properties properties) {
        super(properties);
    }

    @Override
    public AcceleratorKind kind() {
        return AcceleratorKind.FLORAL;
    }

    @Override
    public TileEntity newBlockEntity(IBlockReader world) {
        return new FloralOverclockTileEntity();
    }
}
