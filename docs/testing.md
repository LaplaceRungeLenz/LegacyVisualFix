# ModernNH 测试记录

## 2026-09-12 实测结果

| 环境 | 结果 |
| --- | --- |
| `gradlew clean build --no-configuration-cache` | 通过：7 项 JUnit 测试、Spotless、Checkstyle、重混淆与发布打包 |
| Windows，Java 8u492，Forge 10.13.4.1614，UniMixins 0.2.1 | 六次正常重载与异常恢复通过；默认/金色/回退画面已目视检查 |
| Windows，Temurin Java 25，Angelica 2.1.25，lwjgl3ify 3.0.10，Hodgepodge 2.7.62，GTNHLib 0.10.0 | 相同冒烟用例通过；截图确认可见，未出现 ModernNH 绘制错误 |

现代环境使用本机 Temurin 25 替代 GTNH 默认请求的 JetBrains JDK，避免重复下载。HotSwap 调试未启用，运行命令加 `-x setupHotswapAgent25`，不修改系统 JDK。机器专属覆盖在不提交的 `addon.late.local.gradle` 中；正常构建不依赖该文件。

默认截图：[default.png](screenshots/default.png)，金色截图：[gold.png](screenshots/gold.png)，缺失图片回退：[fallback.png](screenshots/fallback.png)，现代环境：[modern-java-angelica.png](screenshots/modern-java-angelica.png)。

本次没有启动完整 GTNH 整合包、进入世界或穷尽全屏/着色器设置；下方人工清单仍需在目标整合包中验收。

发布包 `modernnh-0.1.0.jar` 已检查：包含默认 PNG、配置、Mixin 配置与 refmap，主类版本为 52（Java 8），不包含 `ClientSmoke` 或测试模组。SHA-256：`b787f65bc0d36e8ed4f1010baf4f9635b0e027e2a4c09d8f94aafe9477c8f432`。

## 可重复的测试

- `gradlew test`：进度重置与上界、空监听器列表、嵌套会话所有权、非法配置回退、本地路径约束、小窗口布局。
- `gradlew runClient -PclientSmoke --no-configuration-cache`：真实 Forge 客户端执行重载，测试代码位于 `src/smokeTest/java`，正常发布构建不会包含它。
- `gradlew runClient25 -PclientSmoke -PangelicaSmoke --no-configuration-cache`：GTNH 现代运行环境及 Angelica 2.1.25 的相同测试。

测试端添加一个只用于观察的重载监听器，在加载期间截取前缓冲区，并检查活动纹理单元、纹理绑定、矩阵模式和视口在绘制后保持原值。测试未比较所有 GL 状态，不能据此宣称所有渲染模组都兼容。

冒烟测试覆盖默认主题、切换到 `zh_CN`、金色进度条、缺失本地背景、非法尺寸/NaN、资源包添加和移除、监听器异常的原样传播、失败后状态清理、随后一次正常重载。资源包主题在成功加载后缓存，因此移除资源包的加载过程仍显示该资源包主题，随后回到默认主题。

故意失败用例会产生监听器异常和原版资源包回退日志。缺失图片用例会产生 ModernNH 回退警告。原版 Forge 的联网版本检查和快速连续重载的异步音频系统也可能输出错误；需和 ModernNH 的绘制错误分别判断。

## 人工验收清单

1. 进入语言选择界面，切换英文/中文，检查加载结束后返回界面且文字正确。
2. 启用/禁用大型资源包，再按 F3+T；确认阶段变化且最后退出加载界面。
3. 修改 `config/modernnh/theme.properties` 和本地 PNG，检查下次重载生效。
4. 在游戏世界内 F3+T，检查之后的世界渲染、字体、物品贴图、着色器和 GUI。
5. 调整窗口、GUI 缩放，切换全屏，检查背景缩放和进度条不越界。
6. GTNH 实际整合包及其指定 Angelica/lwjgl3ify 版本上复测，尤其注意材质上传阶段。

开发冒烟测试自动触发的是界面/按键最终调用的同一 `Minecraft.refreshResources()`，并不等于人工点击了每一个入口。
