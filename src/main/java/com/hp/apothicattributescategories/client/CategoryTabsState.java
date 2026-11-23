package com.hp.apothicattributescategories.client;

import com.hp.apothicattributescategories.config.CategoryClientConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;

public final class CategoryTabsState {
    private static final int EXT_WIDTH = 44;
    private static final int PANEL_HEIGHT = 166;
    private static final int TRACK_OFFSET_X = 6;
    private static final int TRACK_WIDTH = 10;
    private static final int LIST_OFFSET_X = 18;
    private static final int LIST_WIDTH = 24;
    private static final int BOX_OFFSET_Y = 15;
    private static final int BOX_HEIGHT = 134;
    private static final int SLOT_SIZE = 22;
    private static final int VISIBLE_SLOTS = 6;
    private static final int THUMB_HEIGHT = 15;

    private static String rememberedCategoryId = CategoryDefinition.ID_ALL;

    private String selectedCategoryId;
    private float scrollProgress;
    private int scrollIndex;
    private boolean dragging;
    private final List<TabEntry> visibleTabs = new ArrayList<>();
    private TabEntry hoveredTab;

    public CategoryTabsState() {
        if (isConfigEnabled() && CategoryClientConfig.REMEMBER_SELECTED_CATEGORY.get()) {
            this.selectedCategoryId = rememberedCategoryId;
        } else {
            this.selectedCategoryId = CategoryDefinition.ID_ALL;
        }
    }

    public boolean isEnabled() {
        return isConfigEnabled() && CategoryClientConfig.ENABLED.get();
    }

    private static boolean isConfigEnabled() {
        return CategoryClientConfig.SPEC.isLoaded();
    }

    public void filterAfterRefresh(List<AttributeInstance> data) {
        if (!isEnabled()) {
            this.visibleTabs.clear();
            this.dragging = false;
            return;
        }

        List<CategoryDefinition> definitions = CategoryRegistry.getCategories();
        Map<String, Integer> counts = new HashMap<>();
        for (AttributeInstance inst : data) {
            String catId = CategoryRegistry.resolveCategory(inst.getAttribute().value());
            counts.merge(catId, 1, Integer::sum);
        }

        boolean hideEmpty = CategoryClientConfig.HIDE_EMPTY_CATEGORIES.get();
        this.visibleTabs.clear();
        for (CategoryDefinition def : definitions) {
            int count = CategoryDefinition.ID_ALL.equals(def.id())
                ? data.size()
                : counts.getOrDefault(def.id(), 0);
            if (!hideEmpty || CategoryDefinition.ID_ALL.equals(def.id()) || count > 0) {
                this.visibleTabs.add(new TabEntry(def, count));
            }
        }

        boolean selectedStillPresent = false;
        for (TabEntry entry : this.visibleTabs) {
            if (entry.definition().id().equals(this.selectedCategoryId)) {
                selectedStillPresent = true;
                break;
            }
        }
        if (!selectedStillPresent) {
            this.selectedCategoryId = CategoryDefinition.ID_ALL;
            rememberedCategoryId = this.selectedCategoryId;
        }

        int maxScroll = getMaxScrollIndex();
        if (maxScroll == 0) {
            this.scrollProgress = 0.0F;
            this.scrollIndex = 0;
            this.dragging = false;
        } else {
            this.scrollIndex = Mth.clamp(this.scrollIndex, 0, maxScroll);
            if (!this.dragging) {
                this.scrollProgress = (float) this.scrollIndex / (float) maxScroll;
            }
        }

        if (!CategoryDefinition.ID_ALL.equals(this.selectedCategoryId)) {
            String target = this.selectedCategoryId;
            data.removeIf(inst -> !target.equals(CategoryRegistry.resolveCategory(inst.getAttribute().value())));
        }
    }

    public void renderTabs(GuiGraphicsExtractor gfx, Font font, int leftPos, int topPos, int mouseX, int mouseY) {
        this.hoveredTab = null;
        if (!isEnabled() || this.visibleTabs.isEmpty()) {
            return;
        }

        int extLeft = leftPos - EXT_WIDTH;
        drawExtendedPanel(gfx, extLeft, topPos, leftPos);

        Component title = Component.translatable("apothic_attributes_categories.gui.title");
        gfx.text(font, title, extLeft + 7, topPos + 5, 0xFF404040, false);

        int trackX = extLeft + TRACK_OFFSET_X;
        int boxY = topPos + BOX_OFFSET_Y;
        drawScrollbar(gfx, trackX, boxY, mouseX, mouseY);

        int listX = extLeft + LIST_OFFSET_X;
        drawInsetBox(gfx, listX, boxY, LIST_WIDTH, BOX_HEIGHT, 0xFF8B8B8B);

        for (int slot = 0; slot < VISIBLE_SLOTS; slot++) {
            int idx = this.scrollIndex + slot;
            if (idx >= this.visibleTabs.size()) {
                break;
            }
            TabEntry entry = this.visibleTabs.get(idx);
            boolean selected = entry.definition().id().equals(this.selectedCategoryId);
            int slotX = listX + 1;
            int slotY = boxY + 1 + slot * SLOT_SIZE;
            boolean hovered = isInside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            if (hovered) {
                this.hoveredTab = entry;
            }

            drawSlotBackground(gfx, slotX, slotY, SLOT_SIZE, SLOT_SIZE, selected, hovered);

            int iconX = slotX + (SLOT_SIZE - 16) / 2;
            int iconY = slotY + (SLOT_SIZE - 16) / 2;
            Identifier texture = CategoryRegistry.getIconTexture(entry.definition());
            if (texture != null) {
                gfx.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
            } else {
                ItemStack icon = CategoryRegistry.getIcon(entry.definition());
                gfx.fakeItem(icon, iconX, iconY);
            }
        }
    }

    public void renderHoveredTooltip(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY) {
        if (!isEnabled() || this.hoveredTab == null) {
            return;
        }
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(this.hoveredTab.definition().displayName());
        if (CategoryClientConfig.SHOW_COUNT_IN_TOOLTIP.get()) {
            tooltip.add(
                Component.translatable("apothic_attributes_categories.tooltip.count", this.hoveredTab.count())
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        gfx.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int leftPos, int topPos, Runnable onCategoryChanged) {
        this.dragging = false;
        if (button != 0 || !isEnabled() || this.visibleTabs.isEmpty()) {
            return false;
        }

        int extLeft = leftPos - EXT_WIDTH;
        int trackX = extLeft + TRACK_OFFSET_X;
        int boxY = topPos + BOX_OFFSET_Y;

        if (isScrollBarActive() && isInside(mouseX, mouseY, trackX, boxY, TRACK_WIDTH, BOX_HEIGHT)) {
            this.dragging = true;
            updateScrollFromMouseY(mouseY, boxY);
            return true;
        }

        int listX = extLeft + LIST_OFFSET_X;
        for (int slot = 0; slot < VISIBLE_SLOTS; slot++) {
            int idx = this.scrollIndex + slot;
            if (idx >= this.visibleTabs.size()) {
                break;
            }
            int slotX = listX + 1;
            int slotY = boxY + 1 + slot * SLOT_SIZE;
            if (isInside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                TabEntry entry = this.visibleTabs.get(idx);
                if (!entry.definition().id().equals(this.selectedCategoryId)) {
                    this.selectedCategoryId = entry.definition().id();
                    if (CategoryClientConfig.REMEMBER_SELECTED_CATEGORY.get()) {
                        rememberedCategoryId = this.selectedCategoryId;
                    }
                    playClickSound();
                    onCategoryChanged.run();
                }
                return true;
            }
        }

        return isMouseOverTabs(mouseX, mouseY, leftPos, topPos);
    }

    public boolean mouseDragged(double mouseY, int button, int topPos) {
        if (button != 0 || !this.dragging || !isEnabled() || !isScrollBarActive()) {
            return false;
        }
        updateScrollFromMouseY(mouseY, topPos + BOX_OFFSET_Y);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int leftPos, int topPos) {
        if (!isEnabled() || !isScrollBarActive()) {
            return false;
        }
        if (!isMouseOverTabs(mouseX, mouseY, leftPos, topPos)) {
            return false;
        }
        int maxScroll = getMaxScrollIndex();
        int delta = scrollY > 0 ? -1 : (scrollY < 0 ? 1 : 0);
        if (delta != 0) {
            this.scrollIndex = Mth.clamp(this.scrollIndex + delta, 0, maxScroll);
            this.scrollProgress = (float) this.scrollIndex / (float) maxScroll;
        }
        return true;
    }

    public boolean isMouseOverTabs(double mouseX, double mouseY, int leftPos, int topPos) {
        if (!isEnabled() || this.visibleTabs.isEmpty()) {
            return false;
        }
        Rect2i area = getTabsRect(leftPos, topPos);
        return isInside(mouseX, mouseY, area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }

    public Rect2i getTabsRect(int leftPos, int topPos) {
        return new Rect2i(leftPos - EXT_WIDTH, topPos, EXT_WIDTH, PANEL_HEIGHT);
    }

    private boolean isScrollBarActive() {
        return this.visibleTabs.size() > VISIBLE_SLOTS;
    }

    private int getMaxScrollIndex() {
        return Math.max(0, this.visibleTabs.size() - VISIBLE_SLOTS);
    }

    private void updateScrollFromMouseY(double mouseY, int boxY) {
        int innerTop = boxY + 1;
        int travel = BOX_HEIGHT - 2 - THUMB_HEIGHT;
        if (travel <= 0) {
            return;
        }
        float progress = (float) ((mouseY - innerTop - THUMB_HEIGHT / 2.0D) / (double) travel);
        this.scrollProgress = Mth.clamp(progress, 0.0F, 1.0F);
        int maxScroll = getMaxScrollIndex();
        this.scrollIndex = Mth.clamp((int) (this.scrollProgress * maxScroll + 0.5F), 0, maxScroll);
    }

    private void drawExtendedPanel(GuiGraphicsExtractor gfx, int extLeft, int topPos, int leftPos) {
        int joinRight = leftPos + 4;
        int bottom = topPos + PANEL_HEIGHT;

        gfx.fill(extLeft + 2, topPos, joinRight, bottom, 0xFF000000);
        gfx.fill(extLeft + 1, topPos + 1, joinRight, bottom - 1, 0xFF000000);
        gfx.fill(extLeft, topPos + 2, joinRight, bottom - 2, 0xFF000000);

        gfx.fill(extLeft + 1, topPos + 2, joinRight, bottom - 2, 0xFFC6C6C6);
        gfx.fill(extLeft + 2, topPos + 1, joinRight, bottom - 1, 0xFFC6C6C6);

        gfx.fill(extLeft + 2, topPos + 1, joinRight, topPos + 3, 0xFFFFFFFF);
        gfx.fill(extLeft + 1, topPos + 2, extLeft + 3, bottom - 3, 0xFFFFFFFF);

        gfx.fill(extLeft + 2, bottom - 3, joinRight, bottom - 1, 0xFF555555);
        gfx.fill(extLeft + 1, bottom - 3, extLeft + 3, bottom - 2, 0xFFC6C6C6);
    }

    private void drawInsetBox(GuiGraphicsExtractor gfx, int x, int y, int width, int height, int bgColor) {
        gfx.fill(x, y, x + width, y + height, bgColor);
        gfx.fill(x, y, x + width - 1, y + 1, 0xFF373737);
        gfx.fill(x, y, x + 1, y + height - 1, 0xFF373737);
        gfx.fill(x + 1, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        gfx.fill(x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF);
    }

    private void drawScrollbar(GuiGraphicsExtractor gfx, int trackX, int boxY, int mouseX, int mouseY) {
        drawInsetBox(gfx, trackX, boxY, TRACK_WIDTH, BOX_HEIGHT, 0xFF8B8B8B);

        boolean active = isScrollBarActive();
        int travel = BOX_HEIGHT - 2 - THUMB_HEIGHT;
        int thumbX = trackX + 1;
        int thumbY = boxY + 1 + (active ? (int) (this.scrollProgress * travel + 0.5F) : 0);
        int thumbW = TRACK_WIDTH - 2;

        boolean hovered = active && isInside(mouseX, mouseY, thumbX, thumbY, thumbW, THUMB_HEIGHT);
        int fill = !active ? 0xFF7A7A7A : (this.dragging || hovered ? 0xFFD8D8D8 : 0xFFC6C6C6);
        int light = !active ? 0xFF969696 : 0xFFFFFFFF;
        int dark = 0xFF555555;
        int grip = !active ? 0xFF636363 : 0xFF8B8B8B;

        gfx.fill(thumbX, thumbY, thumbX + thumbW, thumbY + THUMB_HEIGHT, fill);
        gfx.fill(thumbX, thumbY, thumbX + thumbW - 1, thumbY + 1, light);
        gfx.fill(thumbX, thumbY, thumbX + 1, thumbY + THUMB_HEIGHT - 1, light);
        gfx.fill(thumbX + 1, thumbY + THUMB_HEIGHT - 1, thumbX + thumbW, thumbY + THUMB_HEIGHT, dark);
        gfx.fill(thumbX + thumbW - 1, thumbY + 1, thumbX + thumbW, thumbY + THUMB_HEIGHT, dark);

        for (int line = 0; line < 4; line++) {
            int ly = thumbY + 3 + line * 2;
            gfx.fill(thumbX + 2, ly, thumbX + thumbW - 2, ly + 1, grip);
        }
    }

    private void drawSlotBackground(GuiGraphicsExtractor gfx, int x, int y, int width, int height, boolean selected, boolean hovered) {
        int fill = selected ? 0xFFC6C6C6 : (hovered ? 0xFFA7A7A7 : 0xFF8B8B8B);
        gfx.fill(x, y, x + width, y + height, fill);
        if (selected) {
            gfx.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
            gfx.fill(x, y, x + 1, y + height - 1, 0xFFFFFFFF);
            gfx.fill(x, y + height - 1, x + width, y + height, 0xFF555555);
        } else {
            gfx.fill(x, y + height - 1, x + width, y + height, 0xFF6E6E6E);
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private record TabEntry(CategoryDefinition definition, int count) {
    }
}
