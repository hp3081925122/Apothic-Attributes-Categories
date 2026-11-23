# Apothic Attributes Categories

A client-side addon for **Apothic Attributes** that extends the Attributes GUI with a seamlessly integrated category sidebar, native-style scrollbar, custom category definitions, and full compatibility with **Apothic Attributes Whitelist**.

## Features

- **Integrated Category Panel**: Extends the left side of the Apothic Attributes GUI seamlessly with a matching Minecraft panel frame, title header, and 6-slot icon list.
- **Scrollbar & Mouse Wheel Support**: Includes a native-style draggable scrollbar (plus track click and mouse wheel scrolling) when more than 6 categories are visible.
- **Built-in Default Categories**: Comes preconfigured with 7 categories — All, Combat, Defense, Survival, Mobility & Mining, Magic & Elements, and Other — covering vanilla, Apothic Attributes, and popular magic/RPG mods.
- **Fully Customizable & Hot-Reloadable Config**:
  - Edit `config/apothic_attributes_categories.json` to add, remove, or reorder categories and match attributes by exact attribute ID, mod namespace, or path keywords.
  - Supports both item IDs (`icon`) and custom 16×16 texture paths (`iconTexture`), including resource pack / mod texture paths and local PNG files placed under `config/apothic_attributes_categories_icons/` (via `file:my_icon.png`).
  - Changes reload automatically the next time you open the Attributes GUI — no restart required.
- **Client Options**: Configure `apothic_attributes_categories-client.toml` to toggle the panel, hide empty categories, remember the last selected category, or show attribute counts in tooltips.
- **Zero-Conflict Compatibility**: Designed to run smoothly alongside **Apothic Attributes Whitelist** — whitelist/blacklist filters apply first and category counts/lists update automatically.

## License & Source

- **License**: All Rights Reserved
- **Source Code**: https://github.com/hp3081925122/Apothic-Attributes-Categories

---

# Apothic Attributes Categories（神化属性分类）

一款面向 **Apothic Attributes（神化属性）** 的客户端附属模组，在原版属性界面左侧无缝延展出一套原生风格的分类面板，内置滑块与滚轮支持、支持高度自定义分类与图标路径，并与 **Apothic Attributes Whitelist** 零冲突兼容。

## 功能特点

- **一体式向左延展面板**：与神化属性原界面无缝衔接，配备一致的边框质感、左上角「分类」标题以及 6 格内凹图标列表，属性说明框正常显示在界面最上层不被遮挡。
- **原生滑块与滚轮支持**：左侧提供与右侧风格一致的滚动条轨道与横纹滑块；当可见分类超过 6 个时自动激活，支持拖拽滑块、点击轨道跳转与鼠标滚轮滚动。
- **开箱即用的 7 个默认分类**：内置「全部、攻击、防御、生命与生存、移动与采集、魔法与元素、其他」，自动适配原版、神化属性及主流魔法/RPG 模组属性。
- **支持自定义分类、贴图路径与热重载**：
  - 在 `config/apothic_attributes_categories.json` 中可自由新增、删改或调整分类顺序，按「精确属性 ID → 模组命名空间 → 路径关键词 → 其他兜底」归类。
  - 支持物品 ID 图标（`icon`）与自定义 16×16 贴图路径（`iconTexture`），既可填写资源包/模组贴图路径（如 `minecraft:textures/mob_effect/strength.png`），也可直接将 PNG 放入 `config/apothic_attributes_categories_icons/` 目录并通过 `file:图标名.png` 引用。
  - 修改配置后重新打开属性界面即自动热重载，无需重启游戏。
- **客户端开关配置**：可在 `apothic_attributes_categories-client.toml` 中控制总开关、自动隐藏空分类、记住上次选中的分类以及是否在悬停提示中显示属性数量。
- **完美兼容 Apothic Attributes Whitelist**：不覆盖原方法、不冲突注入点，白名单/黑名单过滤结果会自动同步到各分类数量与属性列表中。

## 许可与源码

- **许可协议**：All Rights Reserved
- **源码仓库**：https://github.com/hp3081925122/Apothic-Attributes-Categories
