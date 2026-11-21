package com.bandybus.infwhat.botania.client.gui;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.client.ClientStateReceiver;
import com.bandybus.infwhat.botania.container.ContainerManaDevice;
import com.bandybus.infwhat.botania.device.DeviceKind;
import com.bandybus.infwhat.botania.device.DeviceState;
import com.bandybus.infwhat.botania.device.LinkEntry;
import com.bandybus.infwhat.botania.network.ModNetwork;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Locale;

/**
 * Botania-styled control panel shared by the well, the beacon and the anchor.
 *
 * <p>The run page shows the rate, the master switch, the network label and the connection list.
 * The well and the anchor also have a settings page for the auto-connect scan box, which is the
 * same box drawn in the world when "Show range" is enabled.
 */
@SideOnly(Side.CLIENT)
public class GuiManaDevice extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            InfinityWhatBotania.MODID, "textures/gui/mana_device.png");

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

    private final ContainerManaDevice container;
    private Page screenPage = Page.RUN;
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

    public GuiManaDevice(ContainerManaDevice container, InventoryPlayer inventory) {
        super(container);
        this.container = container;
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
        this.stateSeen = false;

        DeviceState state = state();
        this.rateField.setText(state == null ? "" : Integer.toString(state.effectiveRate()));
        this.showRange = state == null || state.showRange();
        populateFromState();
        ModNetwork.requestDevice(container.pos());
    }

    private GuiTextField field(int x, int y, int width, int maxLength, boolean signed) {
        NumericField box = new NumericField(this.fontRendererObj, x, y, width, 12, signed);
        box.setEnableBackgroundDrawing(false);
        box.setTextColor(0xBFF8FF);
        box.setMaxStringLength(maxLength);
        box.setCanLoseFocus(true);
        return box;
    }

    private DeviceState state() {
        DeviceState received = ClientStateReceiver.device(container.pos());
        return received != null ? received : container.state();
    }

    private void populateFromState() {
        DeviceState state = state();
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
            ModNetwork.requestDevice(container.pos());
        }
        DeviceState state = state();
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
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, WIDTH, HEIGHT);
        if (screenPage == Page.RUN) {
            drawFieldFrame(guiLeft + 72, guiTop + 27, 60, 16);
        } else {
            drawFieldFrame(guiLeft + 56, guiTop + 32, 32, 16);
            drawFieldFrame(guiLeft + 96, guiTop + 32, 32, 16);
            drawFieldFrame(guiLeft + 136, guiTop + 32, 32, 16);
            drawFieldFrame(guiLeft + 56, guiTop + 62, 32, 16);
            drawFieldFrame(guiLeft + 96, guiTop + 62, 32, 16);
            drawFieldFrame(guiLeft + 136, guiTop + 62, 32, 16);
        }
        drawActiveFields();
    }

    private void drawFieldFrame(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, COLOR_FRAME);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2E1B0F);
        drawRect(x + 1, y + 1, x + width - 1, y + 2, 0xFF5A3A1E);
    }

    private void drawActiveFields() {
        if (screenPage == Page.RUN) {
            rateField.drawTextBox();
        } else {
            reachXField.drawTextBox();
            reachYField.drawTextBox();
            reachZField.drawTextBox();
            offsetXField.drawTextBox();
            offsetYField.drawTextBox();
            offsetZField.drawTextBox();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        DeviceState state = state();
        DeviceKind kind = container.kind();

        fontRendererObj.drawString(kind.title(), 10, 9, COLOR_TEXT);
        String network = state == null ? "" : state.networkName();
        if (network == null || network.isEmpty()) {
            network = StatCollector.translateToLocal("gui.infwhat_botania.network.none");
        }
        fontRendererObj.drawString(StatCollector.translateToLocalFormatted(
                "gui.infwhat_botania.network", trim(network, 132)), 10, 18, COLOR_SUBTLE);

        if (kind.hasRange()) {
            drawSmallButton(150, 6, screenPage == Page.SETTINGS,
                    StatCollector.translateToLocal(screenPage == Page.SETTINGS
                            ? "gui.infwhat_botania.back" : "gui.infwhat_botania.settings"));
        }
        drawSmallButton(188, 6, state != null && state.enabled(),
                StatCollector.translateToLocal(state != null && state.enabled()
                        ? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));

        if (screenPage == Page.SETTINGS) {
            renderSettingsPage();
        } else {
            renderRunPage(mouseX, mouseY, state, kind);
        }
    }

    private void renderRunPage(int mouseX, int mouseY, DeviceState state, DeviceKind kind) {
        fontRendererObj.drawString(kind.rateLabel(), 10, 31, COLOR_TEXT);
        fontRendererObj.drawString("/t", 160, 31, COLOR_SUBTLE);
        drawWideButton(136, 27, 18, 16,
                StatCollector.translateToLocal("gui.infwhat_botania.apply"));

        if (kind.showBuffer() && state != null) {
            fontRendererObj.drawString(StatCollector.translateToLocal(
                    "gui.infwhat_botania.buffer"), 10, 48, COLOR_TEXT);
            drawBufferBar(70, 45, 140, 10, state.buffer(), state.bufferMax());
            fontRendererObj.drawString(formatMana(state.buffer()) + " / "
                    + formatMana(state.bufferMax()), 90, 47, 0xFFFFFFFF);
        }

        fontRendererObj.drawString(StatCollector.translateToLocal(
                "gui.infwhat_botania.links"), 10, 62, COLOR_TEXT);
        String direction = StatCollector.translateToLocal("gui.infwhat_botania.direction");
        fontRendererObj.drawString(direction, 212 - fontRendererObj.getStringWidth(direction),
                62, COLOR_SUBTLE);

        if (state == null) {
            return;
        }
        List<LinkEntry> links = state.links();
        if (links.isEmpty()) {
            fontRendererObj.drawString(StatCollector.translateToLocal(
                    "gui.infwhat_botania.no_links"), 12, 78, COLOR_SUBTLE);
            return;
        }

        int localMouseX = mouseX - guiLeft;
        int localMouseY = mouseY - guiTop;
        int start = listPage * PAGE_SIZE;
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = start + row;
            if (index >= links.size()) {
                break;
            }
            LinkEntry entry = links.get(index);
            int y = 74 + row * 20;
            boolean hovered = localMouseX >= 8 && localMouseX < 212
                    && localMouseY >= y && localMouseY < y + 19;
            if (hovered) {
                drawRect(8, y, 212, y + 19, 0x33D8B45A);
            }
            fontRendererObj.drawString(arrow(entry.direction()), 12, y + 8,
                    directionColor(entry.direction()));
            fontRendererObj.drawString(trim(entry.label(), 100), 26, y + 3, COLOR_TEXT);
            if (entry.auto()) {
                fontRendererObj.drawString(StatCollector.translateToLocal(
                        "gui.infwhat_botania.auto"), 136, y + 3, 0xFF1E7F9E);
            }
            fontRendererObj.drawString(entry.pos().x + ", " + entry.pos().y + ", "
                    + entry.pos().z, 26, y + 12, COLOR_SUBTLE);
            String directionLabel = StatCollector.translateToLocal(
                    directionKey(entry.direction()));
            fontRendererObj.drawString(directionLabel,
                    212 - fontRendererObj.getStringWidth(directionLabel), y + 8,
                    directionColor(entry.direction()));
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
            fontRendererObj.drawStringWithShadow(pageText,
                    124 - fontRendererObj.getStringWidth(pageText) / 2, 196, COLOR_NUMBER);
        }
    }

    private void renderSettingsPage() {
        fontRendererObj.drawString(StatCollector.translateToLocal(
                "gui.infwhat_botania.range"), 10, 38, COLOR_TEXT);
        fontRendererObj.drawString("X", 60, 24, COLOR_SUBTLE);
        fontRendererObj.drawString("Y", 100, 24, COLOR_SUBTLE);
        fontRendererObj.drawString("Z", 140, 24, COLOR_SUBTLE);

        fontRendererObj.drawString(StatCollector.translateToLocal(
                "gui.infwhat_botania.offset"), 10, 68, COLOR_TEXT);
        fontRendererObj.drawString("X", 60, 54, COLOR_SUBTLE);
        fontRendererObj.drawString("Y", 100, 54, COLOR_SUBTLE);
        fontRendererObj.drawString("Z", 140, 54, COLOR_SUBTLE);

        fontRendererObj.drawString(StatCollector.translateToLocal(
                "gui.infwhat_botania.show_range"), 10, 98, COLOR_TEXT);
        drawSmallButton(176, 95, showRange,
                StatCollector.translateToLocal(showRange
                        ? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));
        drawWideButton(66, 120, 44, 16,
                StatCollector.translateToLocal("gui.infwhat_botania.apply_settings"));
        drawWideButton(118, 120, 60, 16,
                StatCollector.translateToLocal("gui.infwhat_botania.reset_auto"));
        fontRendererObj.drawString(StatCollector.translateToLocal(
                "gui.infwhat_botania.range.hint"), 10, 144, COLOR_SUBTLE);
    }

    private static String arrow(int direction) {
        if (direction == LinkEntry.OUT) {
            return ">";
        }
        if (direction == LinkEntry.IN) {
            return "<";
        }
        return "=";
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

    private void drawSmallButton(int x, int y, boolean on, String label) {
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(x, y, 224, on ? 0 : 16, 24, 14);
        int color = on ? 0xFFE8FFD8 : 0xFFE0D6BE;
        fontRendererObj.drawString(label,
                x + 12 - fontRendererObj.getStringWidth(label) / 2, y + 3, color);
    }

    private void drawWideButton(int x, int y, int width, int height, String label) {
        drawRect(x, y, x + width, y + height, COLOR_FRAME);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC9A24A);
        drawRect(x + 1, y + 1, x + width - 1, y + 2, 0xFFF7E3A1);
        fontRendererObj.drawString(label,
                x + width / 2 - fontRendererObj.getStringWidth(label) / 2, y + 4, COLOR_TEXT);
    }

    private void drawBufferBar(int x, int y, int width, int height, long value, long max) {
        drawRect(x, y, x + width, y + height, COLOR_FRAME);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, COLOR_WELL);
        if (max > 0L) {
            int filled = (int) Math.round((width - 2)
                    * Math.min(1.0D, value / (double) max));
            if (filled > 0) {
                drawRect(x + 1, y + 1, x + 1 + filled, y + height - 1, 0xFF00C6FF);
            }
        }
    }

    private static String formatMana(long value) {
        if (value >= 1000000L) {
            return String.format(Locale.ROOT, "%.1fM", value / 1000000.0D);
        }
        if (value >= 1000L) {
            return String.format(Locale.ROOT, "%.1fk", value / 1000.0D);
        }
        return Long.toString(value);
    }

    private String trim(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (fontRendererObj.getStringWidth(text) <= maxWidth) {
            return text;
        }
        int ellipsis = fontRendererObj.getStringWidth("...");
        return fontRendererObj.trimStringToWidth(text, maxWidth - ellipsis) + "...";
    }

    private static int pageCount(int size) {
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        DeviceState state = state();
        if (button == 0 && x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT) {
            clearFocus();
            if (container.kind().hasRange() && x >= 150 && x < 174 && y >= 6 && y < 20) {
                screenPage = screenPage == Page.RUN ? Page.SETTINGS : Page.RUN;
                if (screenPage == Page.SETTINGS && state != null) {
                    showRange = state.showRange();
                }
                populateFromState();
                return;
            }
            if (x >= 188 && x < 212 && y >= 6 && y < 20) {
                ModNetwork.toggleDevice(container.pos());
                return;
            }
            if (screenPage == Page.SETTINGS) {
                if (x >= 176 && x < 200 && y >= 95 && y < 109) {
                    showRange = !showRange;
                    return;
                }
                if (x >= 66 && x < 110 && y >= 120 && y < 136) {
                    applySettings();
                    return;
                }
                if (x >= 118 && x < 178 && y >= 120 && y < 136) {
                    ModNetwork.clearExcluded(container.pos());
                    return;
                }
                GuiTextField[] fields = {reachXField, reachYField, reachZField,
                        offsetXField, offsetYField, offsetZField};
                for (GuiTextField field : fields) {
                    field.mouseClicked(mouseX, mouseY, button);
                    if (field.isFocused()) {
                        return;
                    }
                }
                return;
            }
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
            rateField.mouseClicked(mouseX, mouseY, button);
            if (rateField.isFocused()) {
                return;
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
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
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || screenPage != Page.RUN) {
            return;
        }
        DeviceState state = state();
        if (state == null) {
            return;
        }
        int x = Mouse.getEventX() * this.width / this.mc.displayWidth - guiLeft;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1
                - guiTop;
        if (x >= 8 && x < 212 && y >= 72 && y < 194) {
            int pages = pageCount(state.links().size());
            if (wheel < 0 && listPage < pages - 1) {
                listPage++;
            } else if (wheel > 0 && listPage > 0) {
                listPage--;
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 28 || keyCode == 156) {
            if (screenPage == Page.RUN && rateField.isFocused()) {
                applyRate();
                rateField.setFocused(false);
                return;
            }
            if (screenPage == Page.SETTINGS && isSettingsFocused()) {
                applySettings();
                return;
            }
        }
        if (screenPage == Page.RUN) {
            if (rateField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        } else if (isSettingsFocused()) {
            GuiTextField[] fields = {reachXField, reachYField, reachZField,
                    offsetXField, offsetYField, offsetZField};
            for (GuiTextField field : fields) {
                if (field.isFocused() && field.textboxKeyTyped(typedChar, keyCode)) {
                    return;
                }
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean isSettingsFocused() {
        return reachXField.isFocused() || reachYField.isFocused() || reachZField.isFocused()
                || offsetXField.isFocused() || offsetYField.isFocused()
                || offsetZField.isFocused();
    }

    private void applyRate() {
        String text = rateField.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return;
        }
        value = Math.max(0, Math.min(MAX_RATE, value));
        rateField.setText(Integer.toString(value));
        ModNetwork.setRate(container.pos(), value);
    }

    private void applySettings() {
        DeviceState state = state();
        int fallbackX = state == null ? 1 : state.reachX();
        int fallbackY = state == null ? 1 : state.reachY();
        int fallbackZ = state == null ? 1 : state.reachZ();
        int reachX = parseField(reachXField, fallbackX, 0, MAX_REACH);
        int reachY = parseField(reachYField, fallbackY, 0, MAX_REACH);
        int reachZ = parseField(reachZField, fallbackZ, 0, MAX_REACH);
        int offsetX = parseField(offsetXField, 0, -MAX_OFFSET, MAX_OFFSET);
        int offsetY = parseField(offsetYField, 0, -MAX_OFFSET, MAX_OFFSET);
        int offsetZ = parseField(offsetZField, 0, -MAX_OFFSET, MAX_OFFSET);
        ModNetwork.setRange(container.pos(), reachX, reachY, reachZ,
                offsetX, offsetY, offsetZ, showRange);
    }

    private static int parseField(GuiTextField box, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(box.getText());
            int clamped = Math.max(min, Math.min(max, value));
            box.setText(Integer.toString(clamped));
            return clamped;
        } catch (NumberFormatException exception) {
            box.setText(Integer.toString(fallback));
            return fallback;
        }
    }
}
