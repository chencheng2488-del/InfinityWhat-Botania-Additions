package com.bandybus.infwhat.botania.block;

import com.bandybus.infwhat.botania.block.entity.RuneAcceleratorTileEntity;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

/** Speeds up nearby runic altars. */
public class RuneAcceleratorBlock extends AbstractAcceleratorBlock {

    public RuneAcceleratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public AcceleratorKind kind() {
        return AcceleratorKind.RUNE;
    }

    @Override
    public TileEntity newBlockEntity(IBlockReader world) {
        return new RuneAcceleratorTileEntity();
    }
}
