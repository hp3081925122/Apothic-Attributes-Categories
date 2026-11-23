package com.hp.apothicattributescategories;

import com.hp.apothicattributescategories.config.CategoryClientConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ApothicAttributesCategories.MOD_ID)
public final class ApothicAttributesCategories {
    public static final String MOD_ID = "apothic_attributes_categories";

    public ApothicAttributesCategories(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, CategoryClientConfig.SPEC);
    }
}
