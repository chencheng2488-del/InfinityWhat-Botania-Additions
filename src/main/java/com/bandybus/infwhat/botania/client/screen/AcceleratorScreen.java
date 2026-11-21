package com.bandybus.infwhat.botania.client.screen;

import com.bandybus.infwhat.botania.config.BotaniaAdditionConfig;
import com.bandybus.infwhat.botania.container.AcceleratorContainer;
import com.bandybus.infwhat.botania.device.AcceleratorKind;
import com.bandybus.infwhat.botania.device.AcceleratorState;
import com.bandybus.infwhat.botania.device.AcceleratorTarget;
import com.bandybus.infwhat.botania.network.ModNetwork;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;

/**
 * Botania-styled control panel for the three accelerators.
 *
 * <p>The run page shows the on/off switch, the rate, the rune cooldown multiplier and the scan
 * target list. The setup page edits the per-axis reach, the relative offset and the range box
 * visibility; the box itself is drawn by the block entity renderer.
 */
public class AcceleratorScreen extends ContainerScreen<AcceleratorContainer> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("infwhat_botania", "textures/gui/mana_device.png");

    private static final int WIDTH = 220;
    private static final int HEIGHT = 206;
    private static final int PAGE_SIZE = 5;
    private static final int MAX_COOLDOWN = Integer.MAX_VALUE;

    private static final int COLOR_TEXT = 0xFF3F2414;
    private static final int COLOR_SUBTLE = 0xFF7A6A4A;
    private static final int COLOR_NUMBER = 0xFF00E5FF;
    private static final int COLOR_FRAME = 0xFF3A2112;
    private static final int COLOR_DISABLED = 0xFF8A2A1E;

    private enum Page {
        RUN,
        SETTINGS
    }

    private Page page = Page.RUN;
    private TextFieldWidget rateField;
    private TextFieldWidget cooldownField;
    private TextFieldWidget reachXField;
    private TextFieldWidget reachYField;
    private TextFieldWidget reachZField;
    private TextFieldWidget offsetXField;
    private TextFieldWidget offsetYField;
    private TextFieldWidget offsetZField;
    private boolean showRange;
    private boolean stateSeen;
    private int listPage;
    private int requestTimer;

    public AcceleratorScreen(AcceleratorContainer menu, PlayerInventory inventory,
                             ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.rateField = field(leftPos + 74, topPos + 33, 56, 7, false,
                new TranslationTextComponent("gui.infwhat_botania.accelerator.rate"));
        this.cooldownField = field(leftPos + 74, topPos + 53, 28, 2, false,
                new TranslationTextComponent("gui.infwhat_botania.accelerator.cooldown"));
        this.reachXField = field(leftPos + 58, topPos + 40, 28, 1, false,
                new StringTextComponent("X"));
        this.reachYField = field(leftPos + 98, topPos + 40, 28, 1, false,
                new StringTextComponent("Y"));
        this.reachZField = field(leftPos + 138, topPos + 40, 28, 1, false,
                new StringTextComponent("Z"));
        this.offsetXField = field(leftPos + 58, topPos + 70, 28, 2, true,
                new StringTextComponent("X"));
        this.offsetYField = field(leftPos + 98, topPos + 70, 28, 2, true,
                new StringTextComponent("Y"));
        this.offsetZField = field(leftPos + 138, topPos + 70, 28, 2, true,
                new StringTextComponent("Z"));
        this.requestTimer = 0;

        AcceleratorState state = menu.state();
        this.showRange = state == null || state.showRange();
        populateFromState();
        applyVisibility();
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.AcceleratorRequestState(menu.pos()));
    }

    private TextFieldWidget field(int x, int y, int width, int maxLength, boolean signed,
                                  ITextComponent narration) {
        TextFieldWidget box = new TextFieldWidget(this.font, x, y, width, 12, narration);
        box.setBordered(false);
        box.setTextColor(0xFFBFF8FF);
        box.setMaxLength(maxLength);
        box.setFilter(text -> text.chars().allMatch(c -> Character.isDigit(c)
                || (signed && c == '-')));
        addButton(box);
        return box;
    }

    private void applyVisibility() {
        boolean run = page == Page.RUN;
        boolean settings = page == Page.SETTINGS;
        rateField.setVisible(run);
        cooldownField.setVisible(run && menu.kind().hasCooldown());
        setVisible(settings, reachXField, reachYField, reachZField,
                offsetXField, offsetYField, offsetZField);
    }

    private static void setVisible(boolean visible, TextFieldWidget... boxes) {
        for (TextFieldWidget box : boxes) {
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

    private static void setIfIdle(TextFieldWidget box, String value) {
        if (!box.isFocused() && !box.getValue().equals(value)) {
            box.setValue(value);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++requestTimer >= 20) {
            requestTimer = 0;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.AcceleratorRequestState(menu.pos()));
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
    protected void renderBg(MatrixStack pose, float partialTick, int mouseX, int mouseY) {
        this.minecraft.getTextureManager().bind(TEXTURE);
        this.blit(pose, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
        if (page == Page.RUN) {
            drawFieldFrame(pose, leftPos + 72, topPos + 31, 60, 16);
            if (menu.kind().hasCooldown()) {
                drawFieldFrame(pose, leftPos + 72, topPos + 51, 32, 16);
            }
        } else {
            drawFieldFrame(pose, leftPos + 56, topPos + 38, 32, 16);
            drawFieldFrame(pose, leftPos + 96, topPos + 38, 32, 16);
            drawFieldFrame(pose, leftPos + 136, topPos + 38, 32, 16);
            drawFieldFrame(pose, leftPos + 56, topPos + 68, 32, 16);
            drawFieldFrame(pose, leftPos + 96, topPos + 68, 32, 16);
            drawFieldFrame(pose, leftPos + 136, topPos + 68, 32, 16);
        }
    }

    private void drawFieldFrame(MatrixStack pose, int x, int y, int width, int height) {
        AbstractGui.fill(pose, x, y, x + width, y + height, COLOR_FRAME);
        AbstractGui.fill(pose, x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2E1B0F);
        AbstractGui.fill(pose, x + 1, y + 1, x + width - 1, y + 2, 0xFF5A3A1E);
    }

    @Override
    protected void renderLabels(MatrixStack pose, int mouseX, int mouseY) {
        AcceleratorKind kind = menu.kind();
        AcceleratorState state = menu.state();

        font.draw(pose, trim(kind.title().getString(), 136), 10, 9, COLOR_TEXT);
        if (state != null && !state.featureEnabled()) {
            int disabledY = page == Page.SETTINGS ? 164 : 20;
            font.draw(pose,
                    new TranslationTextComponent("gui.infwhat_botania.accelerator.config_disabled"),
                    10, disabledY, COLOR_DISABLED);
        }
        drawSmallButton(pose, 150, 6, page == Page.SETTINGS,
                new TranslationTextComponent(page == Page.SETTINGS
                        ? "gui.infwhat_botania.back" : "gui.infwhat_botania.settings"));
        drawSmallButton(pose, 188, 6, state != null && state.enabled(),
                new TranslationTextComponent(state != null && state.enabled()
                        ? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));

        if (page == Page.SETTINGS) {
            renderSettingsPage(pose);
        } else {
            renderRunPage(pose, mouseX, mouseY, state, kind);
        }
    }

    private void renderRunPage(MatrixStack pose, int mouseX, int mouseY,
                               AcceleratorState state, AcceleratorKind kind) {
        font.draw(pose, kind.rateLabel(), 10, 35, COLOR_TEXT);
        font.draw(pose, kind.rateUnit(), 160, 35, COLOR_SUBTLE);

        if (kind.hasCooldown()) {
            font.draw(pose,
                    new TranslationTextComponent("gui.infwhat_botania.accelerator.cooldown"),
                    10, 55, COLOR_TEXT);
            font.draw(pose,
                    new TranslationTextComponent("gui.infwhat_botania.accelerator.multiplier"),
                    106, 55, COLOR_SUBTLE);
        }

        drawWideButton(pose, 118, 72, 62, 16,
                new TranslationTextComponent("gui.infwhat_botania.apply_settings"));

        font.draw(pose,
                new TranslationTextComponent("gui.infwhat_botania.accelerator.targets"),
                10, 92, COLOR_TEXT);

        if (state == null) {
            return;
        }
        List<AcceleratorTarget> targets = state.targets();
        if (targets.isEmpty()) {
            font.draw(pose,
                    new TranslationTextComponent("gui.infwhat_botania.accelerator.no_targets"),
                    12, 106, COLOR_SUBTLE);
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
            boolean hovered = mouseX >= leftPos + 8 && mouseX < leftPos + 212
                    && mouseY >= topPos + y && mouseY < topPos + y + 16;
            if (hovered) {
                AbstractGui.fill(pose, 8, y, 212, y + 16, 0x33D8B45A);
            }
            font.draw(pose, trim(target.label(), 104), 12, y + 3,
                    COLOR_TEXT);
            font.draw(pose, target.pos().getX() + ", "
                    + target.pos().getY() + ", " + target.pos().getZ(), 126, y + 3, COLOR_SUBTLE);
        }

        int pages = pageCount(targets.size());
        if (pages > 1) {
            this.minecraft.getTextureManager().bind(TEXTURE);
            if (listPage > 0) {
                this.blit(pose, 96, 194, 224, 32, 12, 12);
            }
            if (listPage < pages - 1) {
                this.blit(pose, 140, 194, 238, 32, 12, 12);
            }
            String pageText = (listPage + 1) + "/" + pages;
            font.draw(pose, pageText, 124 - font.width(pageText) / 2, 196, COLOR_NUMBER);
        }
    }

    private void renderSettingsPage(MatrixStack pose) {
        font.draw(pose,
                new TranslationTextComponent("gui.infwhat_botania.range"), 10, 42, COLOR_TEXT);
        font.draw(pose, "X", 60, 30, COLOR_SUBTLE);
        font.draw(pose, "Y", 100, 30, COLOR_SUBTLE);
        font.draw(pose, "Z", 140, 30, COLOR_SUBTLE);

        font.draw(pose,
                new TranslationTextComponent("gui.infwhat_botania.offset"), 10, 72, COLOR_TEXT);
        font.draw(pose, "X", 60, 60, COLOR_SUBTLE);
        font.draw(pose, "Y", 100, 60, COLOR_SUBTLE);
        font.draw(pose, "Z", 140, 60, COLOR_SUBTLE);

        font.draw(pose,
                new TranslationTextComponent("gui.infwhat_botania.show_range"), 10, 102, COLOR_TEXT);
        drawSmallButton(pose, 176, 99, showRange,
                new TranslationTextComponent(showRange
                        ? "gui.infwhat_botania.on" : "gui.infwhat_botania.off"));
        drawWideButton(pose, 66, 124, 60, 16,
                new TranslationTextComponent("gui.infwhat_botania.apply_settings"));
        int reachMax = maxReach();
        int offsetMax = maxOffset();
        font.draw(pose,
                new TranslationTextComponent("gui.infwhat_botania.accelerator.range_hint",
                        Integer.valueOf(reachMax), Integer.valueOf(offsetMax),
                        Integer.valueOf(offsetMax)).getString(),
                10, 148, COLOR_SUBTLE);
    }

    private void drawSmallButton(MatrixStack pose, int x, int y, boolean on, ITextComponent label) {
        this.minecraft.getTextureManager().bind(TEXTURE);
        this.blit(pose, x, y, 224, on ? 0 : 16, 24, 14);
        int color = on ? 0xFFE8FFD8 : 0xFFE0D6BE;
        font.draw(pose, label, x + 12 - font.width(label) / 2, y + 3, color);
    }

    private void drawWideButton(MatrixStack pose, int x, int y, int width, int height,
                                ITextComponent label) {
        AbstractGui.fill(pose, x, y, x + width, y + height, COLOR_FRAME);
        AbstractGui.fill(pose, x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC9A24A);
        AbstractGui.fill(pose, x + 1, y + 1, x + width - 1, y + 2, 0xFFF7E3A1);
        font.draw(pose, label, x + width / 2 - font.width(label) / 2, y + 4,
                COLOR_TEXT);
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

    private static int maxReach() {
        return BotaniaAdditionConfig.acceleratorMaxReach();
    }

    private static int maxOffset() {
        return BotaniaAdditionConfig.acceleratorMaxOffset();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (button == 0 && x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT) {
            clearFocus();
            if (x >= 150 && x < 174 && y >= 6 && y < 20) {
                page = page == Page.RUN ? Page.SETTINGS : Page.RUN;
                AcceleratorState state = menu.state();
                if (page == Page.SETTINGS && state != null) {
                    showRange = state.showRange();
                }
                applyVisibility();
                populateFromState();
                return true;
            }
            if (x >= 188 && x < 212 && y >= 6 && y < 20) {
                ModNetwork.CHANNEL.sendToServer(
                        new ModNetwork.AcceleratorToggleEnabled(menu.pos()));
                return true;
            }
            if (page == Page.SETTINGS) {
                if (x >= 176 && x < 200 && y >= 99 && y < 113) {
                    showRange = !showRange;
                    return true;
                }
                if (x >= 66 && x < 126 && y >= 124 && y < 140) {
                    applySettings();
                    return true;
                }
            } else {
                if (x >= 118 && x < 180 && y >= 72 && y < 88) {
                    applyRun();
                    return true;
                }
                AcceleratorState state = menu.state();
                if (state != null) {
                    int pages = pageCount(state.targets().size());
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

    private void clearFocus() {
        rateField.setFocus(false);
        cooldownField.setFocus(false);
        reachXField.setFocus(false);
        reachYField.setFocus(false);
        reachZField.setFocus(false);
        offsetXField.setFocus(false);
        offsetYField.setFocus(false);
        offsetZField.setFocus(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        AcceleratorState state = menu.state();
        if (page == Page.RUN && state != null) {
            double localX = mouseX - leftPos;
            double localY = mouseY - topPos;
            if (localX >= 8 && localX < 212 && localY >= 100 && localY < 190) {
                int pages = pageCount(state.targets().size());
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
            if (page == Page.SETTINGS && (reachXField.isFocused() || reachYField.isFocused()
                    || reachZField.isFocused() || offsetXField.isFocused()
                    || offsetYField.isFocused() || offsetZField.isFocused())) {
                applySettings();
                clearFocus();
                return true;
            }
            if (page == Page.RUN && (rateField.isFocused() || cooldownField.isFocused())) {
                applyRun();
                clearFocus();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.AcceleratorSetSettings(
                menu.pos(), rate, cooldown, reachX, reachY, reachZ,
                offsetX, offsetY, offsetZ, showRange));
    }

    private static int parseField(TextFieldWidget box, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(box.getValue());
            int clamped = MathHelper.clamp(value, min, max);
            box.setValue(Integer.toString(clamped));
            return clamped;
        } catch (NumberFormatException exception) {
            box.setValue(Integer.toString(fallback));
            return fallback;
        }
    }
}
