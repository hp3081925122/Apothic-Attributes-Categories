package com.hp.apothicattributescategories.client;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record CategoryDefinition(
    String id,
    String name,
    String icon,
    String iconTexture,
    List<String> attributes,
    List<String> namespaces,
    List<String> keywords
) {
    public static final String ID_ALL = "all";
    public static final String ID_OTHER = "other";

    public Component displayName() {
        if (this.name != null && this.name.startsWith("lang:")) {
            return Component.translatable(this.name.substring(5));
        }
        if (this.name != null && !this.name.isBlank()) {
            return Component.literal(this.name);
        }
        return Component.translatable("apothic_attributes_categories.category." + this.id);
    }

    public String resolvedTextureSpec() {
        if (this.iconTexture != null && !this.iconTexture.isBlank()) {
            return this.iconTexture.trim();
        }
        if (this.icon != null) {
            String trimmed = this.icon.trim();
            if (trimmed.startsWith("file:") || trimmed.endsWith(".png") || trimmed.contains("/")) {
                return trimmed;
            }
        }
        return "";
    }

    public ItemStack createIconStack() {
        if (this.icon != null && !this.icon.isBlank()) {
            String trimmed = this.icon.trim();
            if (!trimmed.startsWith("file:") && !trimmed.endsWith(".png") && !trimmed.contains("/")) {
                ResourceLocation loc = ResourceLocation.tryParse(trimmed);
                if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                    Item item = BuiltInRegistries.ITEM.get(loc);
                    if (item != Items.AIR) {
                        return new ItemStack(item);
                    }
                }
            }
        }
        return new ItemStack(Items.BOOK);
    }

    public boolean isSpecial() {
        return ID_ALL.equals(this.id) || ID_OTHER.equals(this.id);
    }
}
