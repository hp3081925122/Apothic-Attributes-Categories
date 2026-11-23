package com.hp.apothicattributescategories.mixin;

import com.hp.apothicattributescategories.client.CategoryTabsState;
import dev.shadowsoffire.apothic_attributes.client.AttributesGui;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AttributesGui.class, remap = false)
public abstract class AttributesGuiMixin {
    @Shadow
    protected static float scrollOffset;

    @Shadow
    @Final
    protected Font font;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected int startIndex;

    @Shadow
    protected List<AttributeInstance> data;

    @Shadow
    protected boolean open;

    @Shadow
    public abstract void refreshData();

    @Shadow
    protected abstract int getOffScreenRows();

    @Unique
    private CategoryTabsState apothicAttributesCategories$tabs;

    @Unique
    private CategoryTabsState apothicAttributesCategories$getTabs() {
        if (this.apothicAttributesCategories$tabs == null) {
            this.apothicAttributesCategories$tabs = new CategoryTabsState();
        }
        return this.apothicAttributesCategories$tabs;
    }

    @Inject(method = "refreshData", at = @At("TAIL"))
    private void apothicAttributesCategories$afterRefreshData(CallbackInfo ci) {
        this.apothicAttributesCategories$getTabs().filterAfterRefresh(this.data);
        int offScreen = Math.max(0, this.getOffScreenRows());
        if (offScreen == 0) {
            scrollOffset = 0.0F;
            this.startIndex = 0;
        } else {
            scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
            this.startIndex = (int) (scrollOffset * offScreen + 0.5D);
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Ldev/shadowsoffire/apothic_attributes/client/AttributesGui;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"
        )
    )
    private void apothicAttributesCategories$beforeRenderTooltip(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!this.open) {
            return;
        }
        this.apothicAttributesCategories$getTabs().renderTabs(gfx, this.font, this.leftPos, this.topPos, mouseX, mouseY);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void apothicAttributesCategories$afterRender(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!this.open) {
            return;
        }
        this.apothicAttributesCategories$getTabs().renderHoveredTooltip(gfx, this.font, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void apothicAttributesCategories$beforeMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!this.open) {
            return;
        }
        boolean handled = this.apothicAttributesCategories$getTabs().mouseClicked(mouseX, mouseY, button, this.leftPos, this.topPos, () -> {
            scrollOffset = 0.0F;
            this.startIndex = 0;
            this.refreshData();
        });
        if (handled) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void apothicAttributesCategories$beforeMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (!this.open) {
            return;
        }
        if (this.apothicAttributesCategories$getTabs().mouseDragged(mouseY, button, this.topPos)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void apothicAttributesCategories$beforeMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (!this.open) {
            return;
        }
        if (this.apothicAttributesCategories$getTabs().mouseScrolled(mouseX, mouseY, scrollY, this.leftPos, this.topPos)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isMouseOver", at = @At("RETURN"), cancellable = true)
    private void apothicAttributesCategories$afterIsMouseOver(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!this.open || cir.getReturnValueZ()) {
            return;
        }
        if (this.apothicAttributesCategories$getTabs().isMouseOverTabs(mouseX, mouseY, this.leftPos, this.topPos)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getExclusionAreas", at = @At("RETURN"))
    private void apothicAttributesCategories$afterGetExclusionAreas(CallbackInfoReturnable<List<Rect2i>> cir) {
        if (!this.open || !this.apothicAttributesCategories$getTabs().isEnabled()) {
            return;
        }
        List<Rect2i> areas = cir.getReturnValue();
        if (areas != null) {
            areas.add(this.apothicAttributesCategories$getTabs().getTabsRect(this.leftPos, this.topPos));
        }
    }
}
