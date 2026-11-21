package com.bandybus.infwhat.botania.client.gui;

import net.minecraft.client.gui.GuiTextField;

/** Text field that only accepts digits and, when allowed, a leading minus sign. */
public class NumericField extends GuiTextField {

    private final boolean signed;

    public NumericField(net.minecraft.client.gui.FontRenderer fontRenderer,
                        int x, int y, int width, int height, boolean signed) {
        super(fontRenderer, x, y, width, height);
        this.signed = signed;
    }

    @Override
    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        if (Character.isDigit(typedChar) || (signed && typedChar == '-')) {
            return super.textboxKeyTyped(typedChar, keyCode);
        }
        // Allow the editing and cursor keys handled by the vanilla field.
        if (keyCode == 14 || keyCode == 211 || keyCode == 203 || keyCode == 205
                || keyCode == 200 || keyCode == 208 || keyCode == 199 || keyCode == 207) {
            return super.textboxKeyTyped(typedChar, keyCode);
        }
        return false;
    }
}
