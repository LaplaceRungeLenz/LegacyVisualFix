# Waila 提示框动画

## 范围与配置

ModernNH 0.4.0 提供客户端尺寸过渡，不修改 Waila 文件或服务端协议。默认开启、150 ms；`config/modernnh/waila-animation.cfg` 中的 `animation.enabled` 和 `animation.durationMs` 在启动时读取。0 ms 等价于关闭动画。加载界面的 `theme.properties` 不控制此功能。

第一版只平滑外框尺寸和定位。文字和内容立即切换，不做整体缩放、旧内容交叉渐隐或出现/消失动画。扩张时暂时裁切超出背景的内容，收缩时保留新内容的正常布局。尺寸以 double 保存，最终落到整数 GUI 像素，避免文字与像素边缘抖动。

## 代码职责

- `waila/TooltipAnimation`：不依赖 Minecraft 的时间插值；连续改变目标从当前尺寸继续，重复目标不会重启动画。
- `waila/WailaAnimationConfig`：独立配置。
- `core/ModernNHLateMixinLoader`、`mixins.modernnh.waila.json`：只在客户端且安装 Waila 时加载；依据 Compat 已在早期读取的 `wailaEnabled` 选择互斥路径。
- `mixin/waila/AccessorTooltip`、`MixinOverlayRenderer`、`waila/WailaAnimationRenderer`：普通 Waila。只临时移动内容原点，替换背景绘制宽高，保留 Waila 的内容度量；绘制后恢复字段和裁切状态。
- `mixin/waila/chromatic/`、`waila/chromatic/`：Chromatic 路径，完全保留 Compat 对 Waila 的绘制替换。动画作用于最终主题根装饰器的尺寸和变换原点，正文仍按原始布局绘制。裁切坐标经过当前 GL 矩阵转换，包含 GUI Scale、Waila scale 和主题变换。

没有整段覆盖 Waila 或 Chromatic 的绘制函数，没有全局字体/提示框注入。普通路径在 Waila 自身恢复 GL 状态之前弹出裁切状态，以匹配 GL attribute stack 顺序。递归绘制不会误用外层背景尺寸。

## Chromatic Tooltips 与资源包

验证基线是 Waila **1.19.34**、ChromaticTooltips **1.0.35-GTNH**、ChromaticTooltipsCompat **1.0.36-GTNH**。这两个 Chromatic 版本通过官方 GitHub 的 dev 产物进行编译；它们是 `compileOnly`，正常运行不强制安装。

Compat 完整替换了 Waila 的 `doRenderOverlay()`，因此不能把普通 Waila 的背景注入同时应用到它。专门适配只在 Waila 的 `renderOverlay()` 调用范围内、且 context 为 `waila` 时执行，背包物品提示框不参与。

Compat 原来会在新建上下文时继承旧 renderer。ModernNH 在 Waila 上下文创建时重新选择当前主题，避免资源包重载后一直沿用旧背景。正常 Waila 每个客户端 tick 重建提示框，所以新主题在下一次内容更新生效；不是每帧重新解析资源包。

已在真实客户端加载作者发布的以下资源包，执行默认 → simple → icon → 移除，验证新主题被选中、动画继续正常、无新增 GL 错误且裁切恢复：

- [GregTech simple](https://github.com/user-attachments/files/24572133/gregtech-tooltips-simple.zip)
- [GregTech icon](https://github.com/user-attachments/files/24591320/gregtech-tooltips-icon.zip)

来源是 [ChromaticTooltips 作者的资源包汇总](https://github.com/slprime/ChromaticTooltips/issues/1)。资源包不随 ModernNH 分发。

## 验证与边界

测试代码在 `src/wailaSmokeTest/java`，仅 `-PwailaSmoke` 编译。真实客户端中创建临时、无连接的 WorldClient 和实体射线目标，经公开 `renderOverlay()` 进入已转换的渲染链，使用文字、物品图标和自定义进度条。测试没有进入完整 GTNH 存档，也没有实际放置 GregTech 机器，不能代替完整整合包验收。

GL scissor 为屏幕轴对齐矩形。平移与缩放主题按变换后的边界裁切；任意旋转主题使用四角的包围矩形，旋转后的角落可能在短暂扩张阶段露出内容。依赖额外帧缓冲、特殊着色器或越界绘制的第三方主题尚未穷尽测试。其他模组版本若改变内部签名，需要重新适配。

正常构建：`gradlew clean build`。Chromatic 官方依赖仓库在 `repositories.gradle` 声明；若本机 Java 无法连接 GitHub，可用 `-PchromaticRepository=file:///absolute/cache/` 指向保留同样目录结构的镜像：`chromatictooltips/releases/download/1.0.35-GTNH/chromatictooltips-1.0.35-GTNH-dev.jar`，以及对应的 Compat 目录。

运行客户端测试前，将上述两份资源包放入开发实例 `run/client/resourcepacks/`：

```text
gradlew runClient -PwailaSmoke --no-configuration-cache
gradlew runClient -PwailaSmoke -PchromaticSmoke --no-configuration-cache
```

测试会改变开发实例的 GUI Scale、Waila scale 和启用的资源包，自动保存 `modernnh-waila-smoke/` 结果与截图后退出。不要对正式游戏目录运行测试。最终 JAR 必须使用不带 smoke 参数的 `clean build` 生成。
