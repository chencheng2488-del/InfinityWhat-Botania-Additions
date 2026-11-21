package com.bandybus.infwhat.botania.block;

/** Implemented by the blocks that use the custom box renderer. */
public interface IModelBlock {

    BlockModel model(int metadata);
}
