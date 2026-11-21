package com.bandybus.infwhat.botania.client.screen;

import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.menu.ManaDeviceMenu;
import com.bandybus.infwhat.botania.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

/**
 * Botania-styled control panel shared by the well, the beacon and the anchor.
 *
 * <p>The run page shows the rate, the master switch, the network label and the connection list.
 * The well and the anchor also have a settings page for the auto-connect scan box, which is the
 * same box drawn in the world when "Show range" is enabled.
 */
public class ManaDeviceScreen extends AbstractContainerScreen<ManaDeviceMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("infwhat_botania", "textures/gui/mana_device.png");

    private static final int WIDTH = 220;
    private static final int HEIGHT = 206;
    private static final int PAGE_SIZE = 6;
    private static final int MAX_RATE = Integer.MAX_VALUE;
    private static final int MAX_REACH = 8;
    private static final int MAX_OFFSET = 8;

    private static final int COLOR_TEXT = 0xFF3F2414;
    private static final int COLOR_SUBTLE = 0xFF7A6A4A;
    private static final int COLOR_NUMBER = 0xFF00E5FF;
    private static final int COLOR_FRAME = 0xFF3A2112;
    private static final int COLOR_WELL = 0xFF063F57;

    private enum Page {
        RUN,
        SETTINGS
    }

    private Page screenPage = Page.RUN;
    private int listPage;
    private int requestTimer;

    private EditBox rateField;
    private EditBox reachXField;
    private EditBox reachYField;
    private EditBox reachZField;
    private EditBox offsetXField;
    private EditBox offsetYField;
    private EditBox offsetZField;
    private boolean showRange;
    private boolean stateSeen;

    public ManaDeviceScreen(ManaDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.rateField = field(leftPos + 74, topPos + 29, 56, 7, false,
                Component.translatable("gui.infwhat_botania.rate"));
        this.reachXField = field(leftPos + 58, topPos + 34, 28, 1, false, Component.literal("X"));
        this.reachYField = field(leftPos + 98, topPos + 34, 28, 1, false, Component.literal("Y"));
        this.reachZField = field(leftPos + 138, topPos + 34, 28, 1, false, Component.literal("Z"));
        this.offsetXField = field(leftPos + 58, topPos + 64, 28, 2, true, Component.literal("X"));
        this.offsetYField = field(leftPos + 98, topPos + 64, 28, 2, true, Component.literal("Y"));
        this.offsetZField = field(leftPos + 138, topPos + 64, 28, 2, true, Component.literal("Z"));
        this.requestTimer = 0;

        DeviceState state = menu.state();
        this.rateField.setValue(state == null ? "" : Integer.toString(state.effectiveRate()));
        this.showRange = state == null || state.showRange();
        populateFromState();
        applyVisibility();
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.RequestState(menu.pos()));
    }

    private EditBox field(int x, int y, int width, int maxLength, boolean signed,
                          Component narration) {
        EditBox box = new EditBox(this.font, x, y, width, 12, narration);
        box.setBordered(false);
        box.setTextColor(0xFFBFF8FF);
        box.setMaxLength(maxLength);
        box.setFilter(text -> text.chars().allMatch(c -> Character.isDigit(c)
                || (signed && c == '-')));
        addRenderableWidget(box);
        return box;
    }

    private void applyVisibility() {
        boolean settings = screenPage == Page.SETTINGS;
        rateField.setVisible(!settings);
        setVisible(settings, reachXField, reachYField, reachZField,
                offsetXField, offsetYField, offsetZField);
    }

    private static void setVisible(boolean visible, EditBox... boxes) {
        for (EditBox box : boxes) {
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

    private static void setIfIdle(EditBox box, String value) {
        if (!box.isFocused() && !box.getValue().equals(value)) {
            box.setValue(value);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (++requestTimer >= 20) {
            requestTimer = 0;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.RequestState(menu.pos()));
        }
        DeviceState state = menu.state();
        if (state == null) {
            return;
        }
        if (!stateSeen) {
            stateSeen = true;
            rateField.setValue(Integer.toString(state.effectiveRate()));
            showRange = state.showRange();
        }
        populateFromState();
        int pages = pageCount(state.links().size());
        if (listPage >= pages) {
            listPage = pages - 1;
        }
        if (!rateField.isFocused()) {
            String value = Integer.toString(state.effectiveRate());
            if (!rateField.getValue().equals(value)) {
                rateField.setValue(value);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
        if (screenPage == Page.RUN) {
            drawFieldFrame(graphics, leftPos + 72, topPos + 27, 60, 16);
        } else {
            drawFieldFrame(graphics, leftPos + 56, topPos + 32, 32, 16);
            drawFieldFrame(graphics, leftPos + 96, topPos + 32, 32, 16);
            drawFieldFrame(graphics, leftPos + 136, topPos + 32, 32, 16);
            drawFieldFrame(graphics, leftPos + 56, topPos + 62, 32, 16);
            drawFieldFrame(graphics, leftPos + 96, topPos + 62, 32, 16);
            drawFieldFrame(graphics, leftPos + 136, topPos + 62, 32, 16);
        }
    }

    private void drawFieldFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, COLOR_FRAME);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2E1B0F);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF5A3A1E);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        DeviceState state = menu.state();
        DeviceKind kind = menu.kind();

        graphics.drawString(font, kind.title(), 10, 9, COLOR_TEXT, false);
        String network = state == null ? "" : state.networkName();
        if (network == null || network.isEmpty()) {
            network = Component.translatable("gui.infwhat_botania.network.none").getString();
        }
        graphics.drawString(font, Component.translatable("gui.infwhat_botania.network",
                trim(network, 132)).getString(), 10, 18, COLOR_SUBTLE, false);

        if (kind.hasRange()) {
            drawSmallButton(graphics, 150, 6, screenPage == Page.SETTINGS,
                    Component.translatable(screenPage == Page.SETTINGS
                            ? "gui.infwhat_botania.back" : "gui.infwhat_botania.settings"));
        }
        drawSmallButton(graphics, 188, 6, state != null && state.enabled(),
                Component.translatable(state != null && state.enabled()
                        ? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));

        if (screenPage == Page.SETTINGS) {
            renderSettingsPage(graphics);
        } else {
            renderRunPage(graphics, mouseX, mouseY, state, kind);
        }
    }

    private void renderRunPage(GuiGraphics graphics, int mouseX, int mouseY,
                               DeviceState state, DeviceKind kind) {
        graphics.drawString(font, kind.rateLabel(), 10, 31, COLOR_TEXT, false);
        graphics.drawString(font, "/t", 160, 31, COLOR_SUBTLE, false);
        drawApplyButton(graphics, 136, 27);

        if (kind.showBuffer() && state != null) {
            graphics.drawString(font, Component.translatable("gui.infwhat_botania.buffer"),
                    10, 48, COLOR_TEXT, false);
            drawBufferBar(graphics, 70, 45, 140, 10, state.buffer(), state.bufferMax());
            graphics.drawString(font, formatMana(state.buffer()) + " / " + formatMana(state.bufferMax()),
                    90, 47, 0xFFFFFFFF, true);
        }

        graphics.drawString(font, Component.translatable("gui.infwhat_botania.links"),
                10, 62, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.infwhat_botania.direction"),
                188, 62, COLOR_SUBTLE, false);

        if (state == null) {
            return;
        }
        List<LinkEntry> links = state.links();
        if (links.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.infwhat_botania.no_links"),
                    12, 78, COLOR_SUBTLE, false);
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
            boolean hovered = mouseX >= leftPos + 8 && mouseX < leftPos + 212
                    && mouseY >= topPos + y && mouseY < topPos + y + 19;
            if (hovered) {
                graphics.fill(8, y, 212, y + 19, 0x33D8B45A);
            }
            graphics.drawString(font, arrow(entry.direction()), 12, y + 8,
                    directionColor(entry.direction()), false);
            graphics.drawString(font, trim(entry.label(), 100), 26, y + 3, COLOR_TEXT, false);
            if (entry.auto()) {
                graphics.drawString(font, Component.translatable("gui.infwhat_botania.auto"),
                        136, y + 3, 0xFF1E7F9E, false);
            }
            graphics.drawString(font, entry.pos().getX() + ", " + entry.pos().getY() + ", "
                    + entry.pos().getZ(), 26, y + 12, COLOR_SUBTLE, false);
            Component direction = Component.translatable(directionKey(entry.direction()));
            graphics.drawString(font, direction, 212 - font.width(direction), y + 8,
                    directionColor(entry.direction()), false);
        }

        int pages = pageCount(links.size());
        if (pages > 1) {
            if (listPage > 0) {
                graphics.blit(TEXTURE, 96, 194, 224, 32, 12, 12);
            }
            if (listPage < pages - 1) {
                graphics.blit(TEXTURE, 140, 194, 238, 32, 12, 12);
            }
            // The arrows sit at 96..108 and 140..152, so 124 is the visual midpoint.
            String pageText = (listPage + 1) + "/" + pages;
            graphics.drawString(font, pageText, 124 - font.width(pageText) / 2, 196,
                    COLOR_NUMBER, true);
        }
    }

    private void renderSettingsPage(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("gui.infwhat_botania.range"),
                10, 38, COLOR_TEXT, false);
        graphics.drawString(font, "X", 60, 24, COLOR_SUBTLE, false);
        graphics.drawString(font, "Y", 100, 24, COLOR_SUBTLE, false);
        graphics.drawString(font, "Z", 140, 24, COLOR_SUBTLE, false);

        graphics.drawString(font, Component.translatable("gui.infwhat_botania.offset"),
                10, 68, COLOR_TEXT, false);
        graphics.drawString(font, "X", 60, 54, COLOR_SUBTLE, false);
        graphics.drawString(font, "Y", 100, 54, COLOR_SUBTLE, false);
        graphics.drawString(font, "Z", 140, 54, COLOR_SUBTLE, false);

        graphics.drawString(font, Component.translatable("gui.infwhat_botania.show_range"),
                10, 98, COLOR_TEXT, false);
        drawSmallButton(graphics, 176, 95, showRange,
                Component.translatable(showRange
                        ? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));
        drawWideButton(graphics, 66, 120, 44, 16,
                Component.translatable("gui.infwhat_botania.apply_settings"));
        drawWideButton(graphics, 118, 120, 60, 16,
                Component.translatable("gui.infwhat_botania.reset_auto"));
        graphics.drawString(font, Component.translatable("gui.infwhat_botania.range.hint"),
                10, 144, COLOR_SUBTLE, false);
    }

    private static String arrow(int direction) {
        if (direction == LinkEntry.OUT) {
            return "→";
        }
        if (direction == LinkEntry.IN) {
            return "←";
        }
        return "↔";
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
            return 0xFF1E7F9E;
        }
        if (direction == LinkEntry.IN) {
            return 0xFF8A6A1E;
        }
        return 0xFF5A4A8A;
    }

    private void drawSmallButton(GuiGraphics graphics, int x, int y, boolean on, Component label) {
        graphics.blit(TEXTURE, x, y, 224, on ? 0 : 16, 24, 14);
        int color = on ? 0xFFE8FFD8 : 0xFFE0D6BE;
        graphics.drawString(font, label, x + 12 - font.width(label) / 2, y + 3, color, false);
    }

    private void drawApplyButton(GuiGraphics graphics, int x, int y) {
        drawWideButton(graphics, x, y, 18, 16,
                Component.translatable("gui.infwhat_botania.apply"));
    }

    private void drawWideButton(GuiGraphics graphics, int x, int y, int width, int height,
                                Component label) {
        graphics.fill(x, y, x + width, y + height, COLOR_FRAME);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC9A24A);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFFF7E3A1);
        graphics.drawString(font, label, x + width / 2 - font.width(label) / 2, y + 4,
                COLOR_TEXT, false);
    }

    private void drawBufferBar(GuiGraphics graphics, int x, int y, int width, int height,
                               long value, long max) {
        graphics.fill(x, y, x + width, y + height, COLOR_FRAME);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, COLOR_WELL);
        if (max > 0L) {
            int filled = (int) Math.round((width - 2) * Math.min(1.0D, value / (double) max));
            if (filled > 0) {
                graphics.fillGradient(x + 1, y + 1, x + 1 + filled, y + height - 1,
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
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
    }

    private static int pageCount(int size) {
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        DeviceState state = menu.state();
        if (button == 0 && x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT) {
            rateField.setFocused(false);
            if (screenPage == Page.SETTINGS) {
                for (EditBox box : new EditBox[]{reachXField, reachYField, reachZField,
                        offsetXField, offsetYField, offsetZField}) {
                    box.setFocused(false);
                }
            }
            if (menu.kind().hasRange() && x >= 150 && x < 174 && y >= 6 && y < 20) {
                screenPage = screenPage == Page.RUN ? Page.SETTINGS : Page.RUN;
                if (screenPage == Page.SETTINGS && state != null) {
                    showRange = state.showRange();
                }
                applyVisibility();
                populateFromState();
                return true;
            }
            if (x >= 188 && x < 212 && y >= 6 && y < 20) {
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.ToggleEnabled(menu.pos()));
                return true;
            }
            if (screenPage == Page.SETTINGS) {
                if (x >= 176 && x < 200 && y >= 95 && y < 109) {
                    showRange = !showRange;
                    return true;
                }
                if (x >= 66 && x < 110 && y >= 120 && y < 136) {
                    applySettings();
                    return true;
                }
                if (x >= 118 && x < 178 && y >= 120 && y < 136) {
                    ModNetwork.CHANNEL.sendToServer(new ModNetwork.ClearExcluded(menu.pos()));
                    return true;
                }
            } else {
                if (x >= 136 && x < 154 && y >= 27 && y < 43) {
                    applyRate();
                    return true;
                }
                if (state != null) {
                    List<LinkEntry> links = state.links();
                    int pages = pageCount(links.size());
                    if (pages > 1) {
                        if (listPage > 0 && x >= 96 && x < 108 && y >= 194 && y < 206) {
                            listPage--;
                            return true;
                        }
                        if (listPage < pages - 1 && x >= 140 && x < 152 && y >= 194 && y < 206) {
                            listPage++;
                            return true;
                        }
                    }
                }
            }
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        DeviceState state = menu.state();
        if (screenPage == Page.RUN && state != null) {
            double localX = mouseX - leftPos;
            double localY = mouseY - topPos;
            if (localX >= 8 && localX < 212 && localY >= 72 && localY < 194) {
                int pages = pageCount(state.links().size());
                if (delta < 0 && listPage < pages - 1) {
                    listPage++;
                    return true;
                }
                if (delta > 0 && listPage > 0) {
                    listPage--;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            if (rateField.isFocused()) {
                applyRate();
                rateField.setFocused(false);
                return true;
            }
            if (reachXField.isFocused() || reachYField.isFocused() || reachZField.isFocused()
                    || offsetXField.isFocused() || offsetYField.isFocused()
                    || offsetZField.isFocused()) {
                applySettings();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applyRate() {
        String text = rateField.getValue();
        if (text.isEmpty()) {
            return;
        }
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return;
        }
        value = Mth.clamp(value, 0, MAX_RATE);
        rateField.setValue(Integer.toString(value));
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetRate(menu.pos(), value));
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
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetRange(menu.pos(),
                reachX, reachY, reachZ, offsetX, offsetY, offsetZ, showRange));
    }

    private static int parseField(EditBox box, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(box.getValue());
            int clamped = Mth.clamp(value, min, max);
            box.setValue(Integer.toString(clamped));
            return clamped;
        } catch (NumberFormatException exception) {
            box.setValue(Integer.toString(fallback));
            return fallback;
        }
    }
}
