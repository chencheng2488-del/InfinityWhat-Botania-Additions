package com.bandybus.infwhat.botania.client;

import com.bandybus.infwhat.botania.container.ManaDeviceContainer;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
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
import java.util.Locale;

/**
 * Botania-styled control panel shared by the well, the beacon and the anchor.
 *
 * <p>The run page shows the rate, the master switch, the network label and the connection list.
 * The well and the anchor also have a settings page for the auto-connect scan box, which is the
 * same box drawn in the world when "Show range" is enabled.
 */
public class GuiManaDevice extends GuiContainer {

	private static final ResourceLocation TEXTURE =
			new ResourceLocation("infwhat_botania", "textures/gui/mana_device.png");

	private static final int WIDTH = 220;
	private static final int HEIGHT = 206;
	private static final int PAGE_SIZE = 6;
	private static final int MAX_RATE = Integer.MAX_VALUE;
	private static final int MAX_REACH = 8;
	private static final int MAX_OFFSET = 8;

	private static final int COLOR_TEXT = 0x3F2414;
	private static final int COLOR_SUBTLE = 0x7A6A4A;
	private static final int COLOR_NUMBER = 0x00E5FF;
	private static final int COLOR_FRAME = 0xFF3A2112;
	private static final int COLOR_WELL = 0xFF063F57;

	private final ManaDeviceContainer menu;
	private boolean settingsPage;
	private int listPage;
	private int requestTimer;

	private GuiTextField rateField;
	private GuiTextField reachXField;
	private GuiTextField reachYField;
	private GuiTextField reachZField;
	private GuiTextField offsetXField;
	private GuiTextField offsetYField;
	private GuiTextField offsetZField;
	private boolean showRange;
	private boolean stateSeen;

	public GuiManaDevice(ManaDeviceContainer container, InventoryPlayer inventory) {
		super(container);
		this.menu = container;
		this.xSize = WIDTH;
		this.ySize = HEIGHT;
	}

	@Override
	public void initGui() {
		super.initGui();
		this.rateField = field(guiLeft + 74, guiTop + 29, 56, 7, false);
		this.reachXField = field(guiLeft + 58, guiTop + 34, 28, 1, false);
		this.reachYField = field(guiLeft + 98, guiTop + 34, 28, 1, false);
		this.reachZField = field(guiLeft + 138, guiTop + 34, 28, 1, false);
		this.offsetXField = field(guiLeft + 58, guiTop + 64, 28, 2, true);
		this.offsetYField = field(guiLeft + 98, guiTop + 64, 28, 2, true);
		this.offsetZField = field(guiLeft + 138, guiTop + 64, 28, 2, true);
		this.requestTimer = 0;

		DeviceState state = menu.state();
		this.rateField.setText(state == null ? "" : Integer.toString(state.effectiveRate()));
		this.showRange = state == null || state.showRange();
		populateFromState();
		applyVisibility();
		ClientNetwork.requestDeviceState(menu.pos());
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
		setVisible(settingsPage, reachXField, reachYField, reachZField,
				offsetXField, offsetYField, offsetZField);
	}

	private static void setVisible(boolean visible, GuiTextField... boxes) {
		for (GuiTextField box : boxes) {
			box.setVisible(visible);
		}
	}

	private void populateFromState() {
		DeviceState state = menu.state();
		if (state == null) {
			return;
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
			ClientNetwork.requestDeviceState(menu.pos());
		}
		DeviceState state = menu.state();
		if (state == null) {
			return;
		}
		if (!stateSeen) {
			stateSeen = true;
			rateField.setText(Integer.toString(state.effectiveRate()));
			showRange = state.showRange();
		}
		populateFromState();
		int pages = pageCount(state.links().size());
		if (listPage >= pages) {
			listPage = pages - 1;
		}
		if (!rateField.isFocused()) {
			String value = Integer.toString(state.effectiveRate());
			if (!rateField.getText().equals(value)) {
				rateField.setText(value);
			}
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		mc.getTextureManager().bindTexture(TEXTURE);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, WIDTH, HEIGHT);
		if (!settingsPage) {
			drawFieldFrame(guiLeft + 72, guiTop + 27, 60, 16);
		} else {
			drawFieldFrame(guiLeft + 56, guiTop + 32, 32, 16);
			drawFieldFrame(guiLeft + 96, guiTop + 32, 32, 16);
			drawFieldFrame(guiLeft + 136, guiTop + 32, 32, 16);
			drawFieldFrame(guiLeft + 56, guiTop + 62, 32, 16);
			drawFieldFrame(guiLeft + 96, guiTop + 62, 32, 16);
			drawFieldFrame(guiLeft + 136, guiTop + 62, 32, 16);
		}
		rateField.drawTextBox();
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
		DeviceState state = menu.state();
		boolean hasRange = menu.kind().hasRange();

		fontRenderer.drawString(menu.kind().title().getFormattedText(), 10, 9, COLOR_TEXT);
		String network = state == null ? "" : state.networkName();
		if (network == null || network.isEmpty()) {
			network = I18n.format("gui.infwhat_botania.network.none");
		}
		fontRenderer.drawString(I18n.format("gui.infwhat_botania.network",
				trim(network, 132)), 10, 18, COLOR_SUBTLE);

		if (hasRange) {
			drawSmallButton(150, 6, settingsPage, I18n.format(settingsPage
					? "gui.infwhat_botania.back" : "gui.infwhat_botania.settings"));
		}
		drawSmallButton(188, 6, state != null && state.enabled(), I18n.format(
				state != null && state.enabled()
						? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));

		if (settingsPage) {
			renderSettingsPage();
		} else {
			renderRunPage(mouseX - guiLeft, mouseY - guiTop, state);
		}
	}

	private void renderRunPage(int mouseX, int mouseY, DeviceState state) {
		fontRenderer.drawString(menu.kind().rateLabel().getFormattedText(), 10, 31, COLOR_TEXT);
		fontRenderer.drawString("/t", 160, 31, COLOR_SUBTLE);
		drawWideButton(136, 27, 18, 16, I18n.format("gui.infwhat_botania.apply"));

		if (menu.kind().showBuffer() && state != null) {
			fontRenderer.drawString(I18n.format("gui.infwhat_botania.buffer"),
					10, 48, COLOR_TEXT);
			drawBufferBar(70, 45, 140, 10, state.buffer(), state.bufferMax());
			fontRenderer.drawString(formatMana(state.buffer()) + " / "
					+ formatMana(state.bufferMax()), 90, 47, 0xFFFFFF);
		}

		fontRenderer.drawString(I18n.format("gui.infwhat_botania.links"), 10, 62, COLOR_TEXT);
		fontRenderer.drawString(I18n.format("gui.infwhat_botania.direction"),
				188, 62, COLOR_SUBTLE);

		if (state == null) {
			return;
		}
		List<LinkEntry> links = state.links();
		if (links.isEmpty()) {
			fontRenderer.drawString(I18n.format("gui.infwhat_botania.no_links"),
					12, 78, COLOR_SUBTLE);
			return;
		}

		int start = listPage * PAGE_SIZE;
		for (int row = 0; row < PAGE_SIZE; row++) {
			int index = start + row;
			if (index >= links.size()) {
				break;
			}
			LinkEntry entry = links.get(index);
			int y = 74 + row * 20;
			boolean hovered = mouseX >= 8 && mouseX < 212
					&& mouseY >= y && mouseY < y + 19;
			if (hovered) {
				drawRect(8, y, 212, y + 19, 0x33D8B45A);
			}
			fontRenderer.drawString(arrow(entry.direction()), 12, y + 8,
					directionColor(entry.direction()));
			fontRenderer.drawString(trim(entry.label(), 100), 26, y + 3, COLOR_TEXT);
			if (entry.auto()) {
				fontRenderer.drawString(I18n.format("gui.infwhat_botania.auto"),
						136, y + 3, 0x1E7F9E);
			}
			fontRenderer.drawString(entry.pos().getX() + ", " + entry.pos().getY() + ", "
					+ entry.pos().getZ(), 26, y + 12, COLOR_SUBTLE);
			String direction = I18n.format(directionKey(entry.direction()));
			fontRenderer.drawString(direction, 212 - fontRenderer.getStringWidth(direction),
					y + 8, directionColor(entry.direction()));
		}

		int pages = pageCount(links.size());
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
				10, 38, COLOR_TEXT);
		fontRenderer.drawString("X", 60, 24, COLOR_SUBTLE);
		fontRenderer.drawString("Y", 100, 24, COLOR_SUBTLE);
		fontRenderer.drawString("Z", 140, 24, COLOR_SUBTLE);

		fontRenderer.drawString(I18n.format("gui.infwhat_botania.offset"),
				10, 68, COLOR_TEXT);
		fontRenderer.drawString("X", 60, 54, COLOR_SUBTLE);
		fontRenderer.drawString("Y", 100, 54, COLOR_SUBTLE);
		fontRenderer.drawString("Z", 140, 54, COLOR_SUBTLE);

		fontRenderer.drawString(I18n.format("gui.infwhat_botania.show_range"),
				10, 98, COLOR_TEXT);
		drawSmallButton(176, 95, showRange, I18n.format(showRange
				? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));
		drawWideButton(66, 120, 44, 16, I18n.format("gui.infwhat_botania.apply_settings"));
		drawWideButton(118, 120, 60, 16, I18n.format("gui.infwhat_botania.reset_auto"));
		fontRenderer.drawString(I18n.format("gui.infwhat_botania.range.hint"),
				10, 144, COLOR_SUBTLE);
	}

	private static String arrow(int direction) {
		if (direction == LinkEntry.OUT) {
			return "\u2192";
		}
		if (direction == LinkEntry.IN) {
			return "\u2190";
		}
		return "\u2194";
	}

	private static String directionKey(int direction) {
		if (direction == LinkEntry.OUT) {
			return "gui.infwhat_botania.dir.out";
		}
		if (direction == LinkEntry.IN) {
			return "gui.infwhat_botania.dir.in";
		}
		return "gui.infwhat_botania.dir.peer";
	}

	private static int directionColor(int direction) {
		if (direction == LinkEntry.OUT) {
			return 0x1E7F9E;
		}
		if (direction == LinkEntry.IN) {
			return 0x8A6A1E;
		}
		return 0x5A4A8A;
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

	private void drawBufferBar(int x, int y, int width, int height, long value, long max) {
		drawRect(x, y, x + width, y + height, COLOR_FRAME);
		drawRect(x + 1, y + 1, x + width - 1, y + height - 1, COLOR_WELL);
		if (max > 0L) {
			int filled = (int) Math.round((width - 2) * Math.min(1.0D, value / (double) max));
			if (filled > 0) {
				drawGradientRect(x + 1, y + 1, x + 1 + filled, y + height - 1,
						0xFF7EEAFF, 0xFF00C6FF);
			}
		}
	}

	private static String formatMana(long value) {
		if (value >= 1_000_000L) {
			return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0D);
		}
		if (value >= 1_000L) {
			return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
		}
		return Long.toString(value);
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

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		int x = mouseX - guiLeft;
		int y = mouseY - guiTop;
		DeviceState state = menu.state();
		if (mouseButton == 0 && x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT) {
			clearFocus();
			if (menu.kind().hasRange() && x >= 150 && x < 174 && y >= 6 && y < 20) {
				settingsPage = !settingsPage;
				if (settingsPage && state != null) {
					showRange = state.showRange();
				}
				applyVisibility();
				populateFromState();
				return;
			}
			if (x >= 188 && x < 212 && y >= 6 && y < 20) {
				ClientNetwork.toggleDevice(menu.pos());
				return;
			}
			if (settingsPage) {
				if (x >= 176 && x < 200 && y >= 95 && y < 109) {
					showRange = !showRange;
					return;
				}
				if (x >= 66 && x < 110 && y >= 120 && y < 136) {
					applySettings();
					return;
				}
				if (x >= 118 && x < 178 && y >= 120 && y < 136) {
					ClientNetwork.clearExcluded(menu.pos());
					return;
				}
			} else {
				if (x >= 136 && x < 154 && y >= 27 && y < 43) {
					applyRate();
					return;
				}
				if (state != null) {
					List<LinkEntry> links = state.links();
					int pages = pageCount(links.size());
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
		if (rateField.mouseClicked(mouseX, mouseY, mouseButton)) {
			return true;
		}
		if (reachXField.mouseClicked(mouseX, mouseY, mouseButton)
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
			DeviceState state = menu.state();
			int mouseX = Mouse.getEventX() * width / mc.displayWidth;
			int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
			int x = mouseX - guiLeft;
			int y = mouseY - guiTop;
			if (!settingsPage && state != null && x >= 8 && x < 212 && y >= 72 && y < 194) {
				int pages = pageCount(state.links().size());
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
			if (rateField.isFocused()) {
				applyRate();
				rateField.setFocused(false);
				return;
			}
			if (reachXField.isFocused() || reachYField.isFocused() || reachZField.isFocused()
					|| offsetXField.isFocused() || offsetYField.isFocused()
					|| offsetZField.isFocused()) {
				applySettings();
				return;
			}
		}
		if (rateField.textboxKeyTyped(typedChar, keyCode)
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

	private void applyRate() {
		String text = rateField.getText();
		if (text.isEmpty()) {
			return;
		}
		int value;
		try {
			value = Integer.parseInt(text);
		} catch (NumberFormatException exception) {
			return;
		}
		value = MathHelper.clamp(value, 0, MAX_RATE);
		rateField.setText(Integer.toString(value));
		ClientNetwork.setRate(menu.pos(), value);
	}

	private void applySettings() {
		DeviceState state = menu.state();
		int fallbackX = state == null ? 1 : state.reachX();
		int fallbackY = state == null ? 1 : state.reachY();
		int fallbackZ = state == null ? 1 : state.reachZ();
		int reachX = parseField(reachXField, fallbackX, 0, MAX_REACH);
		int reachY = parseField(reachYField, fallbackY, 0, MAX_REACH);
		int reachZ = parseField(reachZField, fallbackZ, 0, MAX_REACH);
		int offsetX = parseField(offsetXField, 0, -MAX_OFFSET, MAX_OFFSET);
		int offsetY = parseField(offsetYField, 0, -MAX_OFFSET, MAX_OFFSET);
		int offsetZ = parseField(offsetZField, 0, -MAX_OFFSET, MAX_OFFSET);
		ClientNetwork.setRange(menu.pos(), reachX, reachY, reachZ,
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
