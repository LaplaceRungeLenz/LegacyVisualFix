# ModernNH

面向 **Minecraft 1.7.10 / GT New Horizons** 的视觉体验与交互改进模组。各项功能独立配置，后续将会逐渐添加更多功能。

## 已添加的特性

### 1. 资源重载界面

- 切换语言、应用资源包或按 **F3+T** 时，显示重载阶段与进度。
- 支持 **Angelica** 光影启用、关闭、更换、重载及程序编译时的进度显示。
- 默认深蓝主题、GTNH Jappa 风格 Logo、金色进度条和像素字体。
- 支持自定义背景、Logo、颜色、布局与文字；默认淡入 **400 ms**、淡出 **600 ms**。

[配置说明与预览](docs/reload-screen.md)

### 2. Waila 提示框平滑过渡

- 切换注视目标或展开信息时，平滑调整提示框大小与位置，默认 **150 ms**。
- 文字、物品图标和自定义进度条保持正常尺寸；扩张期间裁切暂时放不下的内容。
- 专门适配 **Chromatic Tooltips + Compat**，保留默认主题与资源包样式，支持资源包切换后的主题刷新。
- 仅作用于 Waila；不改变背包中的物品悬停提示框。可单独关闭或调整时长。

[配置、兼容版本与限制](docs/waila-animation.md)

### 3. FOV 平滑过渡

- 调整疾跑、速度属性、飞行和拉弓的原版 FOV 响应速度，默认约 **300 ms** 完成变化的 95%。
- 保留原版最终倍率、Forge FOV 钩子和帧间插值；不修改玩家速度。
- 独立配置，关闭后恢复原版平滑。仅客户端生效。

[配置与实现说明](docs/fov.md)

### 4. 玩家物品栏入场动画

- 原版生存和创造物品栏从下方向上飞入，默认 **250 ms**；创造页签、搜索框与翻页控件同步移动。
- NEI 面板和药水效果保持原位；首次操作会结束动画并被消费，随后恢复正常交互。
- 可独立关闭、调整时长和距离；暂不适配其他模组的容器界面。

[配置、交互与兼容范围](docs/inventory-animation.md)

### 5. 金刚杵九宫格工具交互

- 使用 GT 原生扳手/剪线钳九宫格选择机器朝向和线缆、管道连接方向。
- 独立开关；多人游戏需客户端与服务端均安装。未支持此功能的服务器保留原始交互。
- 仅移植这项功能，不含副手替换、连续挖掘保护或 AE 部件拆卸。

[配置、来源与验证范围](docs/vajra.md)

## 安装

1. 使用 **Minecraft 1.7.10 / Forge 10.13.4.1614**。
2. 安装 **UniMixins 0.2.1 或更新版本**；GTNH 已有 Mixin 环境时，请使用整合包配套版本，避免重复安装。
3. 将 `modernnh-0.6.2.jar` 放入客户端 `mods/`，替换旧版 ModernNH。

**仅使用视觉功能时服务端无需安装；金刚杵九宫格功能需要客户端和服务端均安装。** Waila、Chromatic Tooltips、Compat 和 Angelica 均为可选集成，不随 ModernNH 打包。

## 配置入口

0.5.1 修复 MyCTMLib 1.3.0 与 Better Loading Screen 早期资源重载导致的启动崩溃；保留启动后的 CTM 清理和 GregTech 检测。见 [启动兼容说明](docs/startup-compatibility.md)。

| 功能 | 配置文件 | 生效方式 |
| --- | --- | --- |
| 资源重载界面 | `config/modernnh/theme.properties` | 修改后按 F3+T；资源包主题在成功重载后缓存 |
| Waila 平滑过渡 | `config/modernnh/waila-animation.cfg` | 重启游戏 |
| 金刚杵九宫格 | `config/modernnh/vajra.cfg` | 重启游戏/服务端 |
| FOV 平滑过渡 | `config/modernnh/fov.cfg` | 重启游戏 |
| 玩家物品栏动画 | `config/modernnh/inventory.cfg` | 重启游戏 |

配置自动生成；Waila 动画配置仅在安装 Waila 时生成。资源重载界面的示例见 [theme.properties](examples/gtnh/theme.properties)。

## 开发与验证

构建需要 **JDK 25**，产物保持 **Java 8 字节码**。基于 GTNH Project Starter / GTNHGradle；依赖从 GTNH Maven、Mojang、Maven Central 与 GitHub 获取。

```text
./gradlew setupDecompWorkspace
./gradlew clean build
```

Windows 使用 `gradlew.bat`。正常 JAR 位于 `build/libs/modernnh-0.6.2.jar`，版本在 `addon.gradle` 定义。交付构建不要添加任何 `Smoke` 参数。

- [客户端测试命令与结果](docs/testing.md)
- [Waila / Chromatic 适配结构和专用测试](docs/waila-animation.md)
- [资源重载主题配置](docs/reload-screen.md)

代码按功能组织：`reload/`、`theme/`、`render/` 管理资源重载界面，`waila/` 管理提示框动画，`fov/` 管理 FOV 配置及过渡参数，`mixin/` 提供对应注入。GitHub Actions 只执行构建、测试和格式检查，不自动发布 Release。

## 许可与来源

原有代码及原创素材采用 [MIT](LICENSE)。金刚杵移植模块采用 GPL-3.0-only，包含该模块的组合代码发行包按 GPL-3.0-only 提供，详见 [移植来源与许可](src/main/resources/META-INF/NOTICE-Vajra.md)。GTNH Jappa 风格 Logo 单独适用 **CC BY-NC-SA 4.0**，详见 [素材来源与许可](src/main/resources/META-INF/NOTICE-GTNH.md)。本项目不是 GTNH 官方模组。
