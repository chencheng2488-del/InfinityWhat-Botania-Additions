package com.bandybus.infwhat.botania.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.block.mana.BlockPool;
import vazkii.botania.common.block.tile.TileTerraPlate;
import vazkii.botania.common.block.tile.mana.TilePool;

/** Adapts Botania 1.7.10's mana objects, including the terra plate that is not an IManaReceiver. */
public final class ManaReceiverHelper {

    private static final int CREATIVE_POOL_META = 1;

    private ManaReceiverHelper() {
    }

    public static IManaReceiver find(World world, Pos pos) {
        if (world == null || pos == null || !world.blockExists(pos.x, pos.y, pos.z)) {
            return null;
        }
        TileEntity tileEntity = world.getTileEntity(pos.x, pos.y, pos.z);
        if (tileEntity instanceof TileTerraPlate) {
            return new PlateReceiver((TileTerraPlate) tileEntity);
        }
        return tileEntity instanceof IManaReceiver ? (IManaReceiver) tileEntity : null;
    }

    public static int space(IManaReceiver receiver) {
        if (receiver == null || receiver.isFull()) {
            return 0;
        }
        if (receiver instanceof TilePool) {
            return Math.max(0, ((TilePool) receiver).getAvailableSpaceForMana());
        }
        if (receiver instanceof PlateReceiver) {
            return Math.max(0, ((PlateReceiver) receiver).availableSpace());
        }
        return Integer.MAX_VALUE;
    }

    public static boolean isCreativePool(World world, Pos pos) {
        if (world == null || pos == null || !world.blockExists(pos.x, pos.y, pos.z)) {
            return false;
        }
        return world.getBlock(pos.x, pos.y, pos.z) instanceof BlockPool
                && world.getBlockMetadata(pos.x, pos.y, pos.z) == CREATIVE_POOL_META;
    }

    /** The 1.7.10 terra plate uses the same method names but does not implement IManaReceiver. */
    private static final class PlateReceiver implements IManaReceiver {

        private final TileTerraPlate plate;

        private PlateReceiver(TileTerraPlate plate) {
            this.plate = plate;
        }

        @Override
        public boolean isFull() {
            return plate.isFull();
        }

        @Override
        public void recieveMana(int mana) {
            plate.recieveMana(mana);
        }

        @Override
        public boolean canRecieveManaFromBursts() {
            return plate.canRecieveManaFromBursts();
        }

        @Override
        public int getCurrentMana() {
            return plate.getCurrentMana();
        }

        private int availableSpace() {
            return plate.getAvailableSpaceForMana();
        }
    }
}
