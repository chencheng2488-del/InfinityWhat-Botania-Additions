package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.container.AcceleratorContainer;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.network.ClientNetwork;
import com.google.common.base.Predicate;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * Botania-styled control panel for the three accelerators.
 *
 * <p>The run page shows the on/off switch, the rate, the rune cooldown multiplier and the scan
 * target list. The setup page edits the per-axis reach, the relative offset and the range box
 * visibility; the box itself is drawn by the tile-entity renderer.
 */
public class GuiAccelerator extends GuiContainer {

	private static final ResourceLocation TEXTURE =
			new ResourceLocation("infwhat_botania", "textures/gui/mana_device.png");

	private static final int WIDTH = 220;
	private static final int HEIGHT = 206;
	private static final int PAGE_SIZE = 5;
	private static final int MAX_COOLDOWN = Integer.MAX_VALUE;

	private static final int COLOR_TEXT = 0x3F2414;
	private static final int COLOR_SUBTLE = 0x7A6A4A;
	private static final int COLOR_NUMBER = 0x00E5FF;
	private static final int COLOR_FRAME = 0xFF3A2112;
	private static final int COLOR_DISABLED = 0x8A2A1E;

	private final AcceleratorContainer menu;
	private boolean settingsPage;
	private GuiTextField rateField;
	private GuiTextField cooldownField;
	private GuiTextField reachXField;
	private GuiTextField reachYField;
	private GuiTextField reachZField;
	private GuiTextField offsetXField;
	private GuiTextField offsetYField;
	private GuiTextField offsetZField;
	private boolean showRange;
	private boolean stateSeen;
	private int listPage;
	private int requestTimer;

	public GuiAccelerator(AcceleratorContainer container, InventoryPlayer inventory) {
		super(container);
		this.menu = container;
		this.xSize = WIDTH;
		this.ySize = HEIGHT;
	}

	@Override
	public void initGui() {
		super.initGui();
		AcceleratorKind kind = menu.kind();
		this.rateField = field(guiLeft + 74, guiTop + 33, 56, 7, false);
		this.cooldownField = field(guiLeft + 74, guiTop + 53, 28, 2, false);
		this.reachXField = field(guiLeft + 58, guiTop + 40, 28, 1, false);
		this.reachYField = field(guiLeft + 98, guiTop + 40, 28, 1, false);
		this.reachZField = field(guiLeft + 138, guiTop + 40, 28, 1, false);
		this.offsetXField = field(guiLeft + 58, guiTop + 70, 28, 2, true);
		this.offsetYField = field(guiLeft + 98, guiTop + 70, 28, 2, true);
		this.offsetZField = field(guiLeft + 138, guiTop + 70, 28, 2, true);
		this.requestTimer = 0;

		AcceleratorState state = menu.state();
		this.showRange = state == null || state.showRange();
		populateFromState();
		applyVisibility();
		ClientNetwork.requestAcceleratorState(menu.pos());
	}

	private GuiTextField field(int x, int y, int width, int maxLength, final boolean signed) {
		GuiTextField box = new GuiTextField(0, fontRenderer, x, y, width, 12);
		box.setEnableBackgroundDrawing(false);
		box.setTextColor(0xBFF8FF);
		box.setMaxStringLength(maxLength);
		box.setValidator(new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				if (input == null) {
					return false;
				}
				for (int index = 0; index < input.length(); index++) {
					char character = input.charAt(index);
					if (!Character.isDigit(character) && !(signed && character == '-')) {
						return false;
					}
				}
				return true;
			}
		});
		return box;
	}

	private void applyVisibility() {
		rateField.setVisible(!settingsPage);
		cooldownField.setVisible(!settingsPage && menu.kind().hasCooldown());
		setVisible(settingsPage, reachXField, reachYField, reachZField,
				offsetXField, offsetYField, offsetZField);
	}

	private static void setVisible(boolean visible, GuiTextField... boxes) {
		for (GuiTextField box : boxes) {
			box.setVisible(visible);
		}
	}

	private void populateFromState() {
		AcceleratorState state = menu.state();
		if (state == null) {
			return;
		}
		setIfIdle(rateField, Integer.toString(state.rate()));
		if (menu.kind().hasCooldown()) {
			setIfIdle(cooldownField, Integer.toString(state.cooldown()));
		}
		setIfIdle(reachXField, Integer.toString(state.reachX()));
		setIfIdle(reachYField, Integer.toString(state.reachY()));
		setIfIdle(reachZField, Integer.toString(state.reachZ()));
		setIfIdle(offsetXField, Integer.toString(state.offsetX()));
		setIfIdle(offsetYField, Integer.toString(state.offsetY()));
		setIfIdle(offsetZField, Integer.toString(state.offsetZ()));
	}

	private static void setIfIdle(GuiTextField box, String value) {
		if (!box.isFocused() && !box.getText().equals(value)) {
			box.setText(value);
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (++requestTimer >= 20) {
			requestTimer = 0;
			ClientNetwork.requestAcceleratorState(menu.pos());
		}
		AcceleratorState state = menu.state();
		if (state == null) {
			return;
		}
		if (!stateSeen) {
			stateSeen = true;
			showRange = state.showRange();
		}
		populateFromState();
		int pages = pageCount(state.targets().size());
		if (listPage >= pages) {
			listPage = pages - 1;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		mc.getTextureManager().bindTexture(TEXTURE);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, WIDTH, HEIGHT);
		if (!settingsPage) {
			drawFieldFrame(guiLeft + 72, guiTop + 31, 60, 16);
			if (menu.kind().hasCooldown()) {
				drawFieldFrame(guiLeft + 72, guiTop + 51, 32, 16);
			}
		} else {
			drawFieldFrame(guiLeft + 56, guiTop + 38, 32, 16);
			drawFieldFrame(guiLeft + 96, guiTop + 38, 32, 16);
			drawFieldFrame(guiLeft + 136, guiTop + 38, 32, 16);
			drawFieldFrame(guiLeft + 56, guiTop + 68, 32, 16);
			drawFieldFrame(guiLeft + 96, guiTop + 68, 32, 16);
			drawFieldFrame(guiLeft + 136, guiTop + 68, 32, 16);
		}
		rateField.drawTextBox();
		cooldownField.drawTextBox();
		reachXField.drawTextBox();
		reachYField.drawTextBox();
		reachZField.drawTextBox();
		offsetXField.drawTextBox();
		offsetYField.drawTextBox();
		offsetZField.drawTextBox();
	}

	private void drawFieldFrame(int x, int y, int width, int height) {
		drawRect(x, y, x + width, y + height, COLOR_FRAME);
		drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2E1B0F);
		drawRect(x + 1, y + 1, x + width - 1, y + 2, 0xFF5A3A1E);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		// drawTexturedModalRect uses the currently bound texture; the button sprites and the
		// pagination arrows below are cut from our own sheet.
		mc.getTextureManager().bindTexture(TEXTURE);
		AcceleratorKind kind = menu.kind();
		AcceleratorState state = menu.state();

		fontRenderer.drawString(trim(kind.title().getFormattedText(), 136), 10, 9, COLOR_TEXT);
		if (state != null && !state.featureEnabled()) {
			int disabledY = settingsPage ? 164 : 20;
			fontRenderer.drawString(I18n.format(
					"gui.infwhat_botania.accelerator.config_disabled"),
					10, disabledY, COLOR_DISABLED);
		}
		drawSmallButton(150, 6, settingsPage, I18n.format(settingsPage
				? "gui.infwhat_botania.back" : "gui.infwhat_botania.settings"));
		drawSmallButton(188, 6, state != null && state.enabled(), I18n.format(
				state != null && state.enabled()
						? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));

		if (settingsPage) {
			renderSettingsPage();
		} else {
			renderRunPage(mouseX - guiLeft, mouseY - guiTop, state);
		}
	}

	private void renderRunPage(int mouseX, int mouseY, AcceleratorState state) {
		AcceleratorKind kind = menu.kind();
		fontRenderer.drawString(kind.rateLabel().getFormattedText(), 10, 35, COLOR_TEXT);
		fontRenderer.drawString(kind.rateUnit().getFormattedText(), 160, 35, COLOR_SUBTLE);

		if (kind.hasCooldown()) {
			fontRenderer.drawString(I18n.format(
					"gui.infwhat_botania.accelerator.cooldown"), 10, 55, COLOR_TEXT);
			fontRenderer.drawString(I18n.format(
					"gui.infwhat_botania.accelerator.multiplier"), 106, 55, COLOR_SUBTLE);
		}

		drawWideButton(118, 72, 62, 16, I18n.format("gui.infwhat_botania.apply_settings"));

		fontRenderer.drawString(I18n.format("gui.infwhat_botania.accelerator.targets"),
				10, 92, COLOR_TEXT);

		if (state == null) {
			return;
		}
		List<AcceleratorTarget> targets = state.targets();
		if (targets.isEmpty()) {
			fontRenderer.drawString(I18n.format(
					"gui.infwhat_botania.accelerator.no_targets"), 12, 106, COLOR_SUBTLE);
			return;
		}

		int start = listPage * PAGE_SIZE;
		for (int row = 0; row < PAGE_SIZE; row++) {
			int index = start + row;
			if (index >= targets.size()) {
				break;
			}
			AcceleratorTarget target = targets.get(index);
			int y = 104 + row * 17;
			boolean hovered = mouseX >= 8 && mouseX < 212
					&& mouseY >= y && mouseY < y + 16;
			if (hovered) {
				drawRect(8, y, 212, y + 16, 0x33D8B45A);
			}
			fontRenderer.drawString(trim(target.label(), 104), 12, y + 3, COLOR_TEXT);
			fontRenderer.drawString(target.pos().getX() + ", " + target.pos().getY() + ", "
					+ target.pos().getZ(), 126, y + 3, COLOR_SUBTLE);
		}

		int pages = pageCount(targets.size());
		if (pages > 1) {
			mc.getTextureManager().bindTexture(TEXTURE);
			if (listPage > 0) {
				drawTexturedModalRect(96, 194, 224, 32, 12, 12);
			}
			if (listPage < pages - 1) {
				drawTexturedModalRect(140, 194, 238, 32, 12, 12);
			}
			String pageText = (listPage + 1) + "/" + pages;
			fontRenderer.drawStringWithShadow(pageText,
					124 - fontRenderer.getStringWidth(pageText) / 2, 196, COLOR_NUMBER);
		}
	}

	private void renderSettingsPage() {
		fontRenderer.drawString(I18n.format("gui.infwhat_botania.range"),
				10, 42, COLOR_TEXT);
		fontRenderer.drawString("X", 60, 30, COLOR_SUBTLE);
		fontRenderer.drawString("Y", 100, 30, COLOR_SUBTLE);
		fontRenderer.drawString("Z", 140, 30, COLOR_SUBTLE);

		fontRenderer.drawString(I18n.format("gui.infwhat_botania.offset"),
				10, 72, COLOR_TEXT);
		fontRenderer.drawString("X", 60, 60, COLOR_SUBTLE);
		fontRenderer.drawString("Y", 100, 60, COLOR_SUBTLE);
		fontRenderer.drawString("Z", 140, 60, COLOR_SUBTLE);

		fontRenderer.drawString(I18n.format("gui.infwhat_botania.show_range"),
				10, 102, COLOR_TEXT);
		drawSmallButton(176, 99, showRange, I18n.format(showRange
				? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));
		drawWideButton(66, 124, 60, 16, I18n.format("gui.infwhat_botania.apply_settings"));
		int reachMax = maxReach();
		int offsetMax = maxOffset();
		fontRenderer.drawString(I18n.format(
				"gui.infwhat_botania.accelerator.range_hint",
				reachMax, offsetMax, offsetMax), 10, 148, COLOR_SUBTLE);
	}

	private void drawSmallButton(int x, int y, boolean on, String label) {
		mc.getTextureManager().bindTexture(TEXTURE);
		drawTexturedModalRect(x, y, 224, on ? 0 : 16, 24, 14);
		int color = on ? 0xE8FFD8 : 0xE0D6BE;
		fontRenderer.drawString(label, x + 12 - fontRenderer.getStringWidth(label) / 2,
				y + 3, color);
	}

	private void drawWideButton(int x, int y, int width, int height, String label) {
		drawRect(x, y, x + width, y + height, COLOR_FRAME);
		drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC9A24A);
		drawRect(x + 1, y + 1, x + width - 1, y + 2, 0xFFF7E3A1);
		fontRenderer.drawString(label, x + width / 2 - fontRenderer.getStringWidth(label) / 2,
				y + 4, COLOR_TEXT);
	}

	private String trim(String text, int maxWidth) {
		if (fontRenderer.getStringWidth(text) <= maxWidth) {
			return text;
		}
		return fontRenderer.trimStringToWidth(text,
				maxWidth - fontRenderer.getStringWidth("...")) + "...";
	}

	private static int pageCount(int size) {
		return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
	}

	private static int maxReach() {
		return BotaniaAdditionConfig.acceleratorMaxReach();
	}

	private static int maxOffset() {
		return BotaniaAdditionConfig.acceleratorMaxOffset();
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		int x = mouseX - guiLeft;
		int y = mouseY - guiTop;
		if (mouseButton == 0 && x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT) {
			clearFocus();
			if (x >= 150 && x < 174 && y >= 6 && y < 20) {
				settingsPage = !settingsPage;
				AcceleratorState state = menu.state();
				if (settingsPage && state != null) {
					showRange = state.showRange();
				}
				applyVisibility();
				populateFromState();
				return;
			}
			if (x >= 188 && x < 212 && y >= 6 && y < 20) {
				ClientNetwork.toggleAccelerator(menu.pos());
				return;
			}
			if (settingsPage) {
				if (x >= 176 && x < 200 && y >= 99 && y < 113) {
					showRange = !showRange;
					return;
				}
				if (x >= 66 && x < 126 && y >= 124 && y < 140) {
					applySettings();
					return;
				}
			} else {
				if (x >= 118 && x < 180 && y >= 72 && y < 88) {
					applyRun();
					return;
				}
				AcceleratorState state = menu.state();
				if (state != null) {
					int pages = pageCount(state.targets().size());
					if (pages > 1) {
						if (listPage > 0 && x >= 96 && x < 108 && y >= 194 && y < 206) {
							listPage--;
							return;
						}
						if (listPage < pages - 1 && x >= 140 && x < 152
								&& y >= 194 && y < 206) {
							listPage++;
							return;
						}
					}
				}
			}
			if (clickField(mouseX, mouseY, mouseButton)) {
				return;
			}
			super.mouseClicked(mouseX, mouseY, mouseButton);
			return;
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	private boolean clickField(int mouseX, int mouseY, int mouseButton) {
		clearFocus();
		if (rateField.mouseClicked(mouseX, mouseY, mouseButton)
				|| cooldownField.mouseClicked(mouseX, mouseY, mouseButton)
				|| reachXField.mouseClicked(mouseX, mouseY, mouseButton)
				|| reachYField.mouseClicked(mouseX, mouseY, mouseButton)
				|| reachZField.mouseClicked(mouseX, mouseY, mouseButton)
				|| offsetXField.mouseClicked(mouseX, mouseY, mouseButton)
				|| offsetYField.mouseClicked(mouseX, mouseY, mouseButton)
				|| offsetZField.mouseClicked(mouseX, mouseY, mouseButton)) {
			return true;
		}
		return false;
	}

	private void clearFocus() {
		rateField.setFocused(false);
		cooldownField.setFocused(false);
		reachXField.setFocused(false);
		reachYField.setFocused(false);
		reachZField.setFocused(false);
		offsetXField.setFocused(false);
		offsetYField.setFocused(false);
		offsetZField.setFocused(false);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) {
			AcceleratorState state = menu.state();
			int mouseX = Mouse.getEventX() * width / mc.displayWidth;
			int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
			int x = mouseX - guiLeft;
			int y = mouseY - guiTop;
			if (!settingsPage && state != null && x >= 8 && x < 212 && y >= 100 && y < 190) {
				int pages = pageCount(state.targets().size());
				if (wheel < 0 && listPage < pages - 1) {
					listPage++;
				} else if (wheel > 0 && listPage > 0) {
					listPage--;
				}
			}
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			if (settingsPage && (reachXField.isFocused() || reachYField.isFocused()
					|| reachZField.isFocused() || offsetXField.isFocused()
					|| offsetYField.isFocused() || offsetZField.isFocused())) {
				applySettings();
				clearFocus();
				return;
			}
			if (!settingsPage && (rateField.isFocused() || cooldownField.isFocused())) {
				applyRun();
				clearFocus();
				return;
			}
		}
		if (rateField.textboxKeyTyped(typedChar, keyCode)
				|| cooldownField.textboxKeyTyped(typedChar, keyCode)
				|| reachXField.textboxKeyTyped(typedChar, keyCode)
				|| reachYField.textboxKeyTyped(typedChar, keyCode)
				|| reachZField.textboxKeyTyped(typedChar, keyCode)
				|| offsetXField.textboxKeyTyped(typedChar, keyCode)
				|| offsetYField.textboxKeyTyped(typedChar, keyCode)
				|| offsetZField.textboxKeyTyped(typedChar, keyCode)) {
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	private void applyRun() {
		AcceleratorState state = menu.state();
		if (state == null) {
			return;
		}
		AcceleratorKind kind = menu.kind();
		int rate = parseField(rateField, state.rate(), kind.minRate(), kind.maxRate());
		int cooldown = kind.hasCooldown()
				? parseField(cooldownField, state.cooldown(), 1, MAX_COOLDOWN) : 1;
		sendSettings(rate, cooldown, state.reachX(), state.reachY(), state.reachZ(),
				state.offsetX(), state.offsetY(), state.offsetZ(), state.showRange());
	}

	private void applySettings() {
		AcceleratorState state = menu.state();
		if (state == null) {
			return;
		}
		int reachMax = maxReach();
		int offsetMax = maxOffset();
		int reachX = parseField(reachXField, state.reachX(), 0, reachMax);
		int reachY = parseField(reachYField, state.reachY(), 0, reachMax);
		int reachZ = parseField(reachZField, state.reachZ(), 0, reachMax);
		int offsetX = parseField(offsetXField, state.offsetX(), -offsetMax, offsetMax);
		int offsetY = parseField(offsetYField, state.offsetY(), -offsetMax, offsetMax);
		int offsetZ = parseField(offsetZField, state.offsetZ(), -offsetMax, offsetMax);
		sendSettings(state.rate(), state.cooldown(), reachX, reachY, reachZ,
				offsetX, offsetY, offsetZ, showRange);
	}

	private void sendSettings(int rate, int cooldown,
							  int reachX, int reachY, int reachZ,
							  int offsetX, int offsetY, int offsetZ, boolean showRange) {
		ClientNetwork.setAcceleratorSettings(menu.pos(), rate, cooldown, reachX, reachY, reachZ,
				offsetX, offsetY, offsetZ, showRange);
	}

	private static int parseField(GuiTextField box, int fallback, int min, int max) {
		try {
			int value = Integer.parseInt(box.getText());
			int clamped = MathHelper.clamp(value, min, max);
			box.setText(Integer.toString(clamped));
			return clamped;
		} catch (NumberFormatException exception) {
			box.setText(Integer.toString(fallback));
			return fallback;
		}
	}
}
