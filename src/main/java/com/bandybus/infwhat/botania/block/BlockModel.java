package com.bandybus.infwhat.botania.block;

import net.minecraft.util.IIcon;

import java.util.ArrayList;
import java.util.List;

/** A tiny box model used by the custom block renderer (1.7.10 has no JSON block models). */
public final class BlockModel {

    /** Face order matches Minecraft's side ids: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. */
    public static final class Box {

        public final float minX;
        public final float minY;
        public final float minZ;
        public final float maxX;
        public final float maxY;
        public final float maxZ;
        public final IIcon[] faces = new IIcon[6];

        /**
         * Per-face texture rectangle in 16x16 texture space: {u0, v0, u1, v1}.
         *
         * <p>These match the {@code uv} values of the modern JSON models. Mapping the full sprite
         * onto a small box face would squash the texture, so every face carries its own crop.
         */
        public final float[][] uvs = new float[6][4];

        public Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                   IIcon icon) {
            this.minX = minX / 16.0F;
            this.minY = minY / 16.0F;
            this.minZ = minZ / 16.0F;
            this.maxX = maxX / 16.0F;
            this.maxY = maxY / 16.0F;
            this.maxZ = maxZ / 16.0F;
            setAll(icon);
        }

        public Box setAll(IIcon icon) {
            for (int face = 0; face < faces.length; face++) {
                setFace(face, icon, 0.0F, 0.0F, 16.0F, 16.0F);
            }
            return this;
        }

        public Box setTop(IIcon icon) {
            return setFace(1, icon, 0.0F, 0.0F, 16.0F, 16.0F);
        }

        public Box setFace(int face, IIcon icon, float u0, float v0, float u1, float v1) {
            faces[face] = icon;
            uvs[face][0] = u0;
            uvs[face][1] = v0;
            uvs[face][2] = u1;
            uvs[face][3] = v1;
            return this;
        }
    }

    public final List<Box> boxes = new ArrayList<Box>();

    public Box add(float minX, float minY, float minZ,
                   float maxX, float maxY, float maxZ, IIcon icon) {
        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ, icon);
        boxes.add(box);
        return box;
    }
}
