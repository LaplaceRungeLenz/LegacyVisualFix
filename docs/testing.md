# ModernNH 测试记录

## 2026-09-15 0.7.1 发布包回归核对

- 用户实例仅有 `modernnh-0.7.0.jar`，SHA-256 为 `c977bd47145aeaa1397aa2d7520a61117b461ef4380f0aa44e63e0a081ff09b1`，与首次交付包完全相同。字节码缺少 `InventoryScreenEvents` 和 `InventoryMotion.openingFrom`，输入仍是旧 Redirect 写法，未包含随后合并的 0.6.3 修复。GitHub main（7e932dc）与本地修复源码已包含这些修复；此次不是源代码再次丢失修复，而是旧产物以相同版本号交付造成混淆。
- `scripts/verify_release.py` 针对实际 JAR 校验必要修复、UI 类、Java 8 字节码、版本与文件名及 Smoke 类排除。已确认它会拒绝用户当前旧包，错误为缺少 `InventoryScreenEvents.class`。这是产物完整性门槛，不能代替运行时行为测试。
- Java 8 / beta3 相关模组的实际背包测试通过：生存/创造物品页、缩放 1/2、拿取/放回/分堆、MUI 输入事件、首次点击及松开、Baubles/CosmeticArmor 返回不重播。新增连续回归检查飞入结束后的真实物品 renderer 缩放达到约 1.2 倍，再执行真实鼠标输入拿取。采样限定到目标数量堆叠，避免 NEI 其他图标覆盖测量。
- 不修改用户实例或存档。0.7.1 使用独立版本号交付，保留 main 已有输入修复及五项 UI 效果。
- Java 25 + Angelica 2.1.25 / lwjgl3ify 3.0.10 搭配相同 beta3 依赖也通过完整输入/返回/悬停测试；独立 UI 测试验证携带倾斜及回落、拖尾、同类浮动和快捷栏，报告 PASS，相关 GL 检查为 0。此次测试使用隔离客户端，不等于整个整合包存档的全面验收。

## 2026-09-15 玩家物品栏交互修复（0.6.3）

使用用户 GTNH 2.9.0 beta3 实例中的相关 JAR 在独立开发客户端复现：NEI 2.8.130、CodeChickenCore 1.4.19、GTNHLib 0.11.46、ModularUI2 2.3.88、BogoSorter 1.3.50、MouseTweaks 2.5.2、BaublesExpanded 2.2.22、CosmeticArmorReworked 1.0.6。没有修改用户实例。

- 修复前：日志记录 ModularUI2 鼠标/键盘 Redirect 被 ModernNH 抢占，真实 `GuiScreen.handleInput` 左键拿取测试失败；只有 NEI、MouseTweaks 的组合不复现。返回新建背包对象的测试也失败。
- 修复后：可组合输入包装保留原操作链；生存/创造玩家物品页的左键拿取/放回、右键放置/分堆，以及 ModularUI2 Pre/Post 事件通过。测试向 LWJGL 事件队列注入完整按下/松开事件，检查实际槽位和光标堆叠数量。
- 实际 Baubles、CosmeticArmor GUI 返回新建背包不再播放；游戏中首次打开、首次创造模式内部跳转仍播放。从普通 GUI 返回、创造模式返回、GUI 缩放也覆盖。
- GUI 缩放 1/2 下，动画自然结束、面板/按钮平移、NEI 与药水固定坐标、非目标容器排除、异常后的矩阵恢复通过。
- Java 8 和 Temurin Java 25 均已运行相关组合。现代渲染测试使用开发依赖 lwjgl3ify 3.0.10、Angelica 2.1.25；没有将其描述为整个 GTNH 实例的全面实机验收。
- 最终 `clean build` 通过 29 项 JUnit 测试、Spotless、Checkstyle 和重混淆打包；交付 JAR 的新增类为 Java 8 字节码，未包含烟雾测试、NEI 或 Minecraft 类。

复现命令（`inventoryCompatMods` 指向实例 `mods`，`inventoryCompatLibraries` 指向 PrismLauncher 的 `libraries`）：

```text
./gradlew runClient -PinventorySmoke -PinventoryCompatMods=/path/to/mods -PinventoryCompatLibraries=/path/to/libraries
./gradlew runClient25 -PinventorySmoke -PangelicaSmoke -PinventoryCompatMods=/path/to/mods -PinventoryCompatLibraries=/path/to/libraries
./gradlew runClient -PinventorySmoke
./gradlew clean build
```

烟雾测试报告位于 `run/client/modernnh-inventory-smoke/result.txt`；客户端会捕获断言后退出，必须检查报告的 `PASS`，不能只看 Gradle 退出码。输入和返回路径测试均不编入正常交付 JAR。

## 2026-09-14 玩家物品栏动画（0.6.0）

生存物品栏、创造分类/搜索/玩家物品页采用局部绘制平移；NEI 和药水保持屏幕坐标。代码基于已合入 0.5.1 启动修复的主分支。独立代码审查发现并修正了 NEI 中文输入法不带物理按键状态的字符事件处理。

最终不带任何 Smoke 参数的 `spotlessApply clean build --offline --no-configuration-cache` 通过：**23 项 JUnit 测试**、Spotless、Checkstyle 与重混淆。`modernnh-0.6.0.jar` 已检查包含新增 Mixin/refmap 和启动修复，动画类字节码版本为 52（Java 8），无 Smoke 类、Minecraft 类或 NEI 类。SHA-256：`e9ed05e3907e4e7add08a352da4b3ed7f9523ec88ba1f152fa16e27a55fa1600`。本机另传 `chromaticRepository` 指向既有本地依赖缓存。

- 单元测试覆盖首个渲染帧起算、端点和超时落位、不同帧率采样一致、重建不重播、禁用/零时长、点击与拖动/松开配对、滚轮和中文输入法事件。
- 真实客户端测试使用离线 WorldClient、测试玩家及原版 GUI，保存移动/落位截图并检查背景像素、按钮矩阵、药水像素、原始布局坐标、悬停、重建和异常后的矩阵恢复。NEI 开启其世界面板，并通过公开 `IContainerDrawHandler` 验证覆盖绘制保持原位。
- Java 8 / Forge 10.13.4.1614，不安装 NEI 以及安装 NEI 2.7.69-GTNH 两种环境：生存物品栏、创造分类/搜索/玩家物品页、GUI Scale 1/2 全部通过；两者均无 GL 错误。报告：[无 NEI](screenshots/inventory/vanilla-result.txt)、[NEI](screenshots/inventory/nei-result.txt)。
- Java 25 + Angelica 2.1.25 + NEI 2.7.69-GTNH：Scale 1/2 的生存、创造分类、搜索和玩家物品页全部通过位置/矩阵检查；创造分类和搜索无 GL 错误，包含人物模型的页面出现 1280。**关闭动画的对照组同样出现 1280 和药水首帧明暗差异**，不能宣称此环境无 GL 错误。单独的动画矩阵作用域（含异常恢复）无 GL 错误。对照测试先预热再检查位置，不把首帧颜色变化误判为位移。

这不是完整 GTNH beta3 整合包或真实服务器测试；尚未穷尽实体模型、附属按钮、物品拖放组合、全屏和实际光影包。动画期间首次输入会被消费，这是交互设计，不是掉帧。其他模组容器和替换原版界面的子类被排除。

参考：[实现与配置](inventory-animation.md)、[现代环境生存栏中间位置](screenshots/inventory/modern-survival-moving.png)、[创造搜索页](screenshots/inventory/modern-creative-search-moving.png)、[现代环境测试结果](screenshots/inventory/modern-result.txt)。测试截图使用 32 GUI 像素的固定距离，便于做准确像素比较；正常配置默认从屏幕外进入。

## 2026-09-13 GTNH beta3 启动修复（0.5.1）

- 最终 `clean build` 通过：17 项 JUnit 测试、Spotless、Checkstyle 与重混淆打包。0.5.1 JAR 的兼容类为 Java 8 字节码，不含 Smoke 类或第三方模组。
- 从用户日志定位并用 MyCTMLib 1.3.0、Better Loading Screen 1.7.16-GTNH、GTNHLib 0.11.46 复现启动阶段的 `Loader.namedMods` 空指针。
- 修复后，Java 25 + Angelica 2.1.25 的同版本组合启动成功；专用测试确认启动后四个 CTM Map 正常清空，GregTech 检测与已发现模组列表一致。该精简环境没有安装 GregTech，因此不等于完整 GT 连纹渲染验证。
- Java 8 / Forge 普通客户端不安装 MyCTMLib 时也启动成功；两种环境均通过实际 EntityRenderer FOV 冒烟测试。
- 测试仅使用独立目录及临时世界，不修改用户 GTNH 实例或存档。未启动完整 beta3 整合包；复现命令、实现机制及范围见 [启动兼容说明](startup-compatibility.md)。

## 2026-09-13 Waila / Chromatic Tooltips

交付构建 `spotlessApply clean build --no-configuration-cache` 通过：**14 项 JUnit 测试**、Spotless、Checkstyle、重混淆打包。`modernnh-0.4.0.jar` 已确认包含两份 Mixin 配置和 refmap，主类字节码版本 52（Java 8），不含 smoke 测试类或 Waila / Chromatic 依赖类。SHA-256：`a9f322e2d50394728deb98dd6d51b7f89322b9b3d24ad8d534aa0e645e7512e8`。

Windows 11、RTX 4080 Laptop、Java 8u492、Forge 10.13.4.1614；Waila 1.19.34，测试依赖 GTNHLib 0.11.37 解析到 UniMixins 0.3.1。

| 用例 | 结果 |
| --- | --- |
| 普通 Waila，GUI Scale 1/2、Waila scale 0.75/1/1.5 | 公开可见渲染入口、扩张中间帧、收缩终点、隐藏重置、关闭动画、坐标与 scissor 恢复通过 |
| ChromaticTooltips 1.0.35-GTNH + Compat 1.0.36-GTNH，默认主题 | 通过 |
| 同一客户端启用 GregTech simple 资源包 | 主题刷新、纹理边框与过渡通过 |
| 切换到 GregTech icon 资源包 | 主题刷新与过渡通过 |
| 移除专用资源包 | 恢复默认主题，过渡通过 |
| Compat 的 `wailaEnabled=false` | 正确选择普通 Waila 路径，全部 Waila 用例通过 |
| 不安装 Waila / Chromatic，UniMixins 0.2.1 | 原有 `-PclientSmoke` 资源重载、配置、GL 状态、异常恢复和淡出测试通过，无可选类加载错误 |

每个资源包场景均验证自定义进度条 renderer 被调用，并检查没有新增 GL 错误。普通物品 context 在活跃 Waila 动画范围内仍被排除。实际着色与边框截图已目视核对：[默认 Chromatic](screenshots/waila/chromatic-default.png)、[simple 扩张中间帧](screenshots/waila/gregtech-simple-growing.png)、[icon 主题](screenshots/waila/gregtech-icon.png)、[移除后](screenshots/waila/resource-pack-removed.png)。

这是使用临时 WorldClient、实体射线目标和合成内容的客户端渲染测试，不是完整 GTNH 存档/机器测试。未对所有自定义旋转主题或大型光影组合做验证。详细限制与资源包来源见 [Waila 适配说明](waila-animation.md)。原版快速重载时的异步 OpenAL 错误及 Forge 对第三方库 module-info 的扫描警告仍可能出现，不能与新增 GL 错误混为一谈。

本机 Java 直连 GitHub 超时，Chromatic 官方 dev JAR 经系统网络下载后由 `-PchromaticRepository=file:///C:/Users/rog/Documents/ChatGPT/ModernNH/.gradle/chromatic-artifacts/` 读取。仓库默认仍使用官方 GitHub URL，正常 JAR 不包含这两个依赖。

## 2026-09-12 实测结果

| 环境 | 结果 |
| --- | --- |
| `gradlew clean build --no-configuration-cache` | 通过：10 项 JUnit 测试、Spotless、Checkstyle、重混淆与发布打包 |
| Windows，Java 8u492，Forge 10.13.4.1614，UniMixins 0.2.1 | 六次正常重载与异常恢复通过；新版默认/自定义/回退画面已目视检查；0.1 配置升级和备份通过 |
| Windows，Temurin Java 25，Angelica 2.1.25，lwjgl3ify 3.0.10，Hodgepodge 2.7.62，GTNHLib 0.10.0 | 资源用例、实际光影启用/切换/关闭、真实 DeferredWorldRenderingPipeline 编译、无光影管线不触发界面均通过 |

现代环境使用本机 Temurin 25 替代 GTNH 默认请求的 JetBrains JDK，避免重复下载。HotSwap 调试未启用，运行命令加 `-x setupHotswapAgent25`，不修改系统 JDK。机器专属覆盖在不提交的 `addon.late.local.gradle` 中；正常构建不依赖该文件。

默认截图：[default.png](screenshots/default.png)，金色截图：[gold.png](screenshots/gold.png)，缺失图片回退：[fallback.png](screenshots/fallback.png)，现代环境：[modern-java-angelica.png](screenshots/modern-java-angelica.png)。

本次没有启动完整 GTNH 整合包、进入世界或穷尽全屏/着色器设置；下方人工清单仍需在目标整合包中验收。

发布包 `modernnh-0.3.0.jar` 已检查：包含默认 PNG、配置、Mixin 配置与 refmap，主类版本为 52（Java 8），不包含 `ClientSmoke` 或测试模组。SHA-256：`2b017f9fd397d5a59ad179072b5e56db7d9f911a567dd096fd2dd082bbe1f98c`。

## 新增动画与光影检查

- 淡入/淡出 smoothstep 的端点、中点、超时边界和 0ms 关闭模式通过单元测试。
- 实际 GPU 快照以红/绿上下色块和蓝色底图验证 50% 混合的 RGB 值与方向，误差不超过 3/255；检查纹理绑定、着色程序、读写 framebuffer 和新产生的 GL 错误。
- 在正常 Minecraft 渲染帧中观察 300ms 淡出，确认缓存最终释放；中间帧见 [fade-out.png](screenshots/fade-out.png)。
- Angelica 使用两个测试专用的最小 GLSL 光影包，执行启用、切换、关闭及真实管线编译，检查会话计数及绘制失败标志；见 [shader-enable.png](screenshots/shader-enable.png) 和 [shader-pipeline.png](screenshots/shader-pipeline.png)。
- 当前未在完整 GTNH 存档中加载大型实际光影包，未验证所有光影加载器或 Angelica 版本。

## 可重复的测试

- `gradlew test`：进度重置与上界、空监听器列表、嵌套会话所有权、非法配置回退、本地路径约束、小窗口布局。
- `gradlew runClient -PclientSmoke --no-configuration-cache`：真实 Forge 客户端执行重载，测试代码位于 `src/smokeTest/java`，正常发布构建不会包含它。
- `gradlew runClient25 -PclientSmoke -PangelicaSmoke --no-configuration-cache`：GTNH 现代运行环境及 Angelica 2.1.25 的相同测试。

测试端添加一个只用于观察的重载监听器，在加载期间截取前缓冲区，并检查活动纹理单元、纹理绑定、矩阵模式和视口在绘制后保持原值。测试未比较所有 GL 状态，不能据此宣称所有渲染模组都兼容。

0.3 冒烟测试还覆盖原样 0.1 配置迁移及备份、本地 Logo 替换、缺失 Logo 隐藏、纯色条回退和 showDetails。冒烟测试覆盖默认主题、切换到 `zh_CN`、金色进度条、缺失本地背景、非法尺寸/NaN、资源包添加和移除、监听器异常的原样传播、失败后状态清理、随后一次正常重载。资源包主题在成功加载后缓存，因此移除资源包的加载过程仍显示该资源包主题，随后回到默认主题。

故意失败用例会产生监听器异常和原版资源包回退日志。缺失图片用例会产生 ModernNH 回退警告。原版 Forge 的联网版本检查和快速连续重载的异步音频系统也可能输出错误；需和 ModernNH 的绘制错误分别判断。

## 人工验收清单

1. 进入语言选择界面，切换英文/中文，检查加载结束后返回界面且文字正确。
2. 启用/禁用大型资源包，再按 F3+T；确认阶段变化且最后退出加载界面。
3. 修改 `config/modernnh/theme.properties` 和本地 PNG，检查下次重载生效。
4. 在游戏世界内 F3+T，检查之后的世界渲染、字体、物品贴图、着色器和 GUI。
5. 调整窗口、GUI 缩放，切换全屏，检查背景缩放和进度条不越界。
6. GTNH 实际整合包及其指定 Angelica/lwjgl3ify 版本上复测，尤其注意材质上传阶段。

开发冒烟测试自动触发的是界面/按键最终调用的同一 `Minecraft.refreshResources()`，并不等于人工点击了每一个入口。
## 2026-09-13 FOV 验证

- `clean build` 通过：17 项 JUnit 测试，Spotless、Checkstyle、重混淆与打包均通过。其中新增 3 项 FOV 单元测试。
- Java 8 / Forge 10.13.4.1614 / UniMixins 真实客户端：`-PfovSmoke` 通过，验证已注入的 `EntityRenderer` 与真实玩家疾跑属性。
- Java 25 / Angelica 2.1.25 / lwjgl3ify：同一 FOV 冒烟测试通过。覆盖默认 300ms 收敛、反向切换、前一 tick 值、禁用和零时长原版回退。
- 该测试创建临时世界对象，不读取用户存档。未测试完整 GTNH 装备组合、实际光影包或主观手感；原版运行记录仍有异步 OpenAL 初始化错误，FOV 检查报告为 PASS。
- 正常 JAR 已确认含 FOV 类及重混淆 refmap，不含任何 Smoke 类。配置及实现见 [FOV 说明](fov.md)。

## 2026-09-15 五项 UI 效果验证（0.7.0）

测试使用真实 Forge 客户端、变换后的 GUI、物品 renderer 和 GPU，但玩家与容器由测试构造；不读取用户存档，也不是完整 GTNH 游玩验收。

| 实测组合 | 结果与范围 |
| --- | --- |
| Java 8 / Forge 10.13.4.1614 / UniMixins，原版 GUI | 五项效果通过，检查原 renderer、数量、NBT/metadata 匹配、禁用恢复和真实快捷栏立即选中 |
| NEI 2.7.69-GTNH | 真实容器槽位及鼠标携带效果通过；不扩展 NEI 目录/配方图标 |
| NEI 2.8.130-GTNH + MUI 1.3.4 + MUI2 2.3.85-1.7.10，Java 8 | 五项及两个框架的真实 GUI 测试通过；普通槽悬停、幽灵槽排除、携带倾斜/拖尾、矩阵深度和物品数量通过；测试场景新增 GL 错误为 0 |
| NEI 2.7.69-GTNH + 上述 MUI + Angelica 2.1.25 / lwjgl3ify 3.0.10，Java 25 | 同组功能检查通过；存在下述环境 GL 限制，未启用实际光影包 |

可选 MUI 测试另加载 GTNHLib 0.11.37、Baubles-Expanded 2.2.21。MUI2 使用其取消原版绘制后执行的 `ClientScreenHandler` 路径，已实际测试，不能只由 GuiContainer 继承关系推断兼容。

Java 25 / Angelica 组合中，重复绘制原版容器和 MUI2 GUI 的等价禁用对照也产生 `GL_INVALID_ENUM (1280)`，启用效果时错误码相同；MUI1 对照及启用均为 0。单独新增槽位变换/恢复无错误，粒子绘制的矩阵、颜色和相关 GL 状态恢复检查通过。这个结果不构成整个组合无 GL 错误的保证。

额外覆盖粒子数量上限、静止过期、800ms 停顿不补发、倾角回落、低/高帧率收敛、原物品数量不变。截图见 [物品效果](screenshots/ui/nei-trail.png)。完整 GTNH 机器、AE2 专用终端、自定义 HUD 和实际光影包仍需整合包验收，支持边界见 [UI 说明](ui-effects.md)。

复现：`runClient -PuiSmoke -PneiSmoke -PuiMuiSmoke -PuiNeiVersion=2.8.130-GTNH`；Angelica 使用 `runClient25 -PuiSmoke -PneiSmoke -PuiMuiSmoke -PangelicaSmoke`。必须检查 `MODERNNH_UI_SMOKE PASS` 与两个 `MODERNNH_UI_MUI_SMOKE PASS`，不能仅用 Gradle 退出成功判断效果测试通过。

正式构建 `spotlessApply clean build` 通过：34 项 JUnit 测试（本次新增 8 项）、Spotless、Checkstyle、重混淆与打包全部通过。交付 JAR 含 UI 早期/可选后期 Mixins 和 refmap，UI 字节码为 Java 8（major 52），不含 Smoke 类。

PR #6 合入 main 的 0.6.3 修复后再次验证：spotlessApply build 通过，37 项 JUnit 测试通过；NEI 2.8.130 + MUI 1.3.4 / MUI2 2.3.85 的实际 GUI 冒烟测试全部 PASS，测试场景 GL 错误为 0。首次 clean 因 Windows 文件占用失败，停止 Gradle 守护进程后重新构建通过。
- 0.7.1 最终 spotlessApply clean build、37 项 JUnit 测试及实际发布包校验全部通过。CI 在构建后执行相同产物检查。
