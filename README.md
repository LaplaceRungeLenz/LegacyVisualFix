# LegacyVisualFix

面向 **Minecraft 1.7.10 / GT New Horizons** 的视觉与交互改进模组，各项功能可独立配置。

## 功能

- **近战反馈**：实际扣血后显示白色准星标记和局部粒子；包含定向模型反应和第一人称持物回弹，提供统一强度预设、粒子最低亮度与兼容排除项。保留原有连击、伤害、碰撞箱和击退。
- **资源重载界面**：显示语言、资源包及 Angelica 光影重载进度，可自定义背景、Logo、颜色和淡入淡出。
- **Waila 动画**：提示框大小与位置平滑过渡，适配 Chromatic Tooltips + Compat。
- **FOV 动画**：平滑处理疾跑、飞行和拉弓等视野变化。
- **背包入场动画**：生存与创造物品栏从下方飞入，NEI 面板和药水效果保持原位。
- **物品动效**：快捷栏选择框平滑移动；悬停物品放大；拿起后随鼠标移动倾斜；同类物品浮动；普通物品带银白色星光拖尾，其他物品按稀有度显示彩色星光拖尾。支持原版容器、NEI 容器槽位及 ModularUI 1/2。
- **金刚杵九宫格**：使用 GT 扳手与剪线钳式九宫格调整机器朝向及线缆、管道连接。

## 安装

需要 **Forge 10.13.4.1614** 与 **UniMixins 0.2.1+**；GTNH 请使用整合包配套的 Mixin 环境。

资源重载、Waila、FOV、背包和物品动效只需客户端安装；金刚杵九宫格和近战命中确认需要客户端与服务端均安装（单机包含本地服务器）。Waila、Chromatic Tooltips 和 Angelica 为可选集成。替换旧 JAR 时只保留一个版本。

## 配置

配置自动生成于 `config/legacyvisualfix/`。除主题可通过 **F3+T** 重载外，其他配置修改后需重启；金刚杵配置也需重启服务端。

| 文件 | 主要配置项 |
| --- | --- |
| `theme.properties` | `enabled` 总开关；`showText`、`showDetails`、`showLogo` 显示内容；`texture.*` 贴图；`color.*` 颜色；`logo.*`、`bar.*` 布局；`animation.fadeInMs` / `animation.fadeOutMs` 淡入淡出 |
| `waila-animation.cfg` | `enabled` 开关；`durationMs` 过渡时长，默认 150 ms |
| `fov.cfg` | `enabled` 开关；`transitionMs` 响应时长，默认 300 ms |
| `inventory.cfg` | `enabled` 开关；`durationMs` 飞入时长，默认 250 ms；`distance` 飞入距离，0 为自动 |
| `ui.cfg` | `enabled` 总开关；`hotbar`、`hover`、`carried`、`matching`、`trails` 分别控制快捷栏、悬停、携带、同类浮动和拖尾 |
| `vajra.cfg` | `enabled` 金刚杵九宫格开关 |
| `combat.cfg` | `enabled` 总开关；`marker` 命中标记；`durationMs` 时长；`debug` 显示 HP/ABS；`soundMode=auto/always/off`、`soundVolume` 控制可选提示音；`particleMode=auto/always/off`、`particlesPerHit` 控制局部粒子；`modelReaction`、`reactionDegrees`、`reactionDurationMs`、`reactionExcludedEntities` 控制模型反应；`weaponRecoil`、`recoilDegrees`、`recoilDurationMs` 控制第一人称回弹 |

`ui.cfg` 还可调整：`hoverScale` / `carriedScale` 放大倍数（默认 1.2）、`rotationDegrees` 倾角、`floatAmplitude` 浮动幅度、`responseSpeed` 响应速度、`maxParticles` / `particleRate` 粒子上限与速率，以及 `excludedScreens` 排除界面的完整类名。

`combat.cfg` 新增 `feedbackPreset=custom/light/standard/strong`；默认 custom 保留已有单项参数。`particleMinLight=6` 改善暗处粒子辨识，设为 0 恢复环境光照；`particleScale` 调整自定义粒子大小。粒子、模型和回弹分别可用 `particleExcludedEntities`、`reactionExcludedEntities`、`recoilExcludedItems` 排除特定对象。完整预设数值与验证边界见[近战反馈文档](docs/combat-feedback.md)。

悬停时仅放大，拿起物品后移动鼠标才会晃动。同类匹配比较物品、metadata 和 NBT，忽略数量；NEI 目录、配方图标、幽灵槽和流体槽不添加这些物品效果。

## 许可

原创代码与素材采用 [MIT](LICENSE)；包含 GPL-3.0-only 金刚杵移植模块的组合代码发行包按 GPL-3.0-only 提供，见 [模块许可](src/main/resources/META-INF/NOTICE-Vajra.md)。GTNH Jappa 风格 Logo 采用 CC BY-NC-SA 4.0，见 [素材许可](src/main/resources/META-INF/NOTICE-GTNH.md)。本项目不是 GTNH 官方模组。
