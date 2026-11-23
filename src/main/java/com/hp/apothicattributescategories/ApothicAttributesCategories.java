package com.hp.apothicattributescategories;

import com.hp.apothicattributescategories.config.CategoryClientConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ApothicAttributesCategories.MOD_ID)
public final class ApothicAttributesCategories {
    public static final String MOD_ID = "apothic_attributes_categories";

    public ApothicAttributesCategories() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CategoryClientConfig.SPEC);
    }
}
