package com.hp.apothicattributescategories.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CategoryClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue HIDE_EMPTY_CATEGORIES;
    public static final ForgeConfigSpec.BooleanValue REMEMBER_SELECTED_CATEGORY;
    public static final ForgeConfigSpec.BooleanValue SHOW_COUNT_IN_TOOLTIP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder
            .comment(
                "Enable category tabs on the left side of the Apothic Attributes GUI.",
                "是否启用 Apothic Attributes 属性界面左侧的分类书签页。"
            )
            .define("enabled", true);
        HIDE_EMPTY_CATEGORIES = builder
            .comment(
                "Hide categories that currently contain no visible attributes (the 'All' tab is always shown).",
                "是否隐藏当前没有可见属性的分类（“全部”标签页始终显示）。"
            )
            .define("hideEmptyCategories", true);
        REMEMBER_SELECTED_CATEGORY = builder
            .comment(
                "Remember the last selected category tab when reopening the Attributes GUI.",
                "重新打开属性面板时是否记住上次选中的分类标签页。"
            )
            .define("rememberSelectedCategory", true);
        SHOW_COUNT_IN_TOOLTIP = builder
            .comment(
                "Show the number of visible attributes in each category tab tooltip.",
                "悬停在分类书签上时是否显示该分类下的可见属性数量。"
            )
            .define("showCountInTooltip", true);
        builder.pop();

        SPEC = builder.build();
    }

    private CategoryClientConfig() {
    }
}
