package com.bandybus.infwhat.botania.creative;

import com.bandybus.infwhat.botania.block.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

/**
 * Puts the add-on's items into the Infinity What core creative tab when it is present.
 *
 * <p>The core mod is optional for this add-on, so when it is missing a tab with the same label is
 * created instead. Either way the items do not end up mixed into Botania's creative tab.
 */
public final class ModCreativeTab {

	private static final String LABEL = "infwhat.infinity_what";
	private static CreativeTabs tab;

	public static CreativeTabs get() {
		if (tab != null) {
			return tab;
		}
		tab = coreTab();
		if (tab != null) {
			return tab;
		}
		for (CreativeTabs candidate : CreativeTabs.CREATIVE_TAB_ARRAY) {
			if (candidate != null && LABEL.equals(candidate.getTabLabel())) {
				tab = candidate;
				return tab;
			}
		}
		tab = new CreativeTabs(LABEL) {
			@Override
			public ItemStack createIcon() {
				return new ItemStack(ModBlocks.MANA_WELL);
			}
		};
		return tab;
	}

	/**
	 * Loads the core tab through its public INSTANCE field instead of a compile-time class
	 * reference.
	 *
	 * <p>The core registers its blocks in a {@code RegistryEvent}, which fires after all mods'
	 * preInit. Our blocks are created during preInit, so the core tab may not exist in
	 * {@code CREATIVE_TAB_ARRAY} yet. Loading the class here initialises the same singleton the
	 * core will use later, which avoids creating a duplicate tab with the same label.
	 */
	private static CreativeTabs coreTab() {
		try {
			Class<?> core = Class.forName("com.bandybus.infwhat.creative.ModCreativeTab");
			Object instance = core.getField("INSTANCE").get(null);
			return instance instanceof CreativeTabs ? (CreativeTabs) instance : null;
		} catch (ReflectiveOperationException | LinkageError exception) {
			return null;
		}
	}

	private ModCreativeTab() {
	}
}
