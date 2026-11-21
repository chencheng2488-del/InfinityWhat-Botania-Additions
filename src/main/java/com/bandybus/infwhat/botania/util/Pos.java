package com.bandybus.infwhat.botania.util;

import net.minecraft.tileentity.TileEntity;

/** Immutable block position used by the 1.7.10 port (there is no BlockPos). */
public final class Pos implements Comparable<Pos> {

    public final int x;
    public final int y;
    public final int z;

    public Pos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Pos of(TileEntity tileEntity) {
        return new Pos(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
    }

    public static Pos of(long packed) {
        return new Pos((int) (packed >> 38), (int) (packed << 26 >> 52), (int) (packed << 38 >> 38));
    }

    public long asLong() {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (y & 0xFFF) << 26)
                | (z & 0x3FFFFFF);
    }

    public Pos offset(int dx, int dy, int dz) {
        return new Pos(x + dx, y + dy, z + dz);
    }

    public double distanceSq(double px, double py, double pz) {
        double dx = x + 0.5D - px;
        double dy = y + 0.5D - py;
        double dz = z + 0.5D - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pos)) {
            return false;
        }
        Pos pos = (Pos) other;
        return x == pos.x && y == pos.y && z == pos.z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public int compareTo(Pos other) {
        if (x != other.x) {
            return Integer.compare(x, other.x);
        }
        if (y != other.y) {
            return Integer.compare(y, other.y);
        }
        return Integer.compare(z, other.z);
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}
