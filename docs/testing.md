# ModernNH 测试记录

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
