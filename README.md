# ModernNH

Minecraft **1.7.10 / Forge 10.13.4.1614** 客户端模组。切换语言、应用资源包或按 F3+T 时显示可定制的资源重载进度界面。

基于 [GTNH Project Starter](https://github.com/GTNewHorizons/ExampleMod1.7.10) 和 GTNHGradle 构建。默认背景为本项目生成的原创纹理，不含 GTNH 官方美术资源。

![默认加载界面](docs/screenshots/default.png)

## 安装

1. 将 `build/libs/modernnh-0.1.0.jar` 放入客户端 `mods/`。
2. 需要 **UniMixins 0.2.1 或更新版本**。GTNH 已带有 Mixin 环境时，先核对其版本，避免重复安装不同 Mixin 加载器。
3. 启动游戏后自动生成 `config/modernnh/theme.properties`。

服务端无需安装。模组通过客户端 Mixin 工作，不修改服务端协议。

## 主题定制

编辑游戏目录下 `config/modernnh/theme.properties`，然后按 F3+T：

```properties
enabled=true
showText=true
texture.background=file:background.png
texture.track=file:bar_track.png
texture.fill=file:bar_fill.png
background.fit=cover
bar.x=0.5
bar.y=0.75
bar.width=240
bar.height=12
color.background=FF101820
color.track=FFFFFFFF
color.fill=FFFFFFFF
color.text=FFE5EEF4
```

对应 PNG 放在 `config/modernnh/` 下。也可使用 `modernnh:textures/gui/background.png` 这样的资源位置，由资源包覆盖；设为空字符串则绘制纯色。

| 配置 | 含义 |
| --- | --- |
| `enabled` / `showText` | 开关加载界面 / 阶段文字 |
| `texture.background` | 背景 PNG |
| `texture.track` / `texture.fill` | 进度条底图 / 填充 PNG |
| `background.fit` | `cover` 裁切铺满、`contain` 完整显示、`stretch` 拉伸 |
| `bar.x` / `bar.y` | 进度条中心占屏幕宽高的比例，范围 0–1 |
| `bar.width` / `bar.height` | GUI 缩放后的像素尺寸；界面较小时自动收缩并限制在屏幕内 |
| `color.*` | `RRGGBB` 或 `AARRGGBB`，可带 `#`；材质颜色会乘以配置颜色 |

填充通过裁切材质右侧表示进度，保持剩余部分的 UV 比例。用 `FFFFFFFF` 保留原材质颜色。PNG 单边最多 4096 像素，总计最多 4,194,304 像素。无效图片回退纯色，无效字段使用默认值。

GTNH 风格的金色配置见 [examples/gtnh/theme.properties](examples/gtnh/theme.properties)。资源包示例目录：

```text
YourTheme/
  pack.mcmeta
  assets/modernnh/textures/gui/
    background.png
    bar_track.png
    bar_fill.png
```

`pack.mcmeta`：

```json
{"pack":{"pack_format":1,"description":"ModernNH theme"}}
```

加载期间使用独立纹理快照。切换资源包时，本次加载仍显示旧主题；新资源包主题在成功重载后缓存，从下一次加载开始显示。本地配置和图片在下一次重载开始前读取。坏资源包失败期间保留上一次缓存。

## 构建和开发

构建工具需要 **JDK 25**；产物保持 **Java 8 字节码**。Gradle wrapper 固定为 9.4.0，GTNHGradle 固定为 2.0.20。初次构建需要访问 GTNH Maven、Maven Central、Mojang 和 GitHub。

```powershell
.\gradlew.bat setupDecompWorkspace
.\gradlew.bat test build
.\gradlew.bat runClient
# GTNH 的现代 Java 开发环境：
.\gradlew.bat runClient25
```

版本在 `addon.gradle` 中定义为 `0.1.0`，可用环境变量 `VERSION` 覆盖。上游 `build.gradle.kts` 保持原样，依赖放在 `dependencies.gradle`。

GitHub Actions 仅运行构建、测试和格式检查；当前没有自动创建 Release 的工作流。

代码职责：`reload/` 管理会话与进度；`theme/` 解析配置与缓存图片；`render/` 独立绘制并恢复 GL 状态；`mixin/` 观察原版资源监听器和 Forge 纹理进度。启动屏幕仍由 Forge/整合包负责。

## 进度含义与限制

主进度是**已完成监听器数 / 总监听器数**，不是剩余时间。纹理文字显示 Forge 当前处理的项，未将“开始处理”当作“处理完成”。游戏仍同步执行重载；在监听器、纹理加载、mipmap 和上传的可用边界刷新，单个第三方长步骤仍可能暂停画面。

纹理细分 Mixin 对替换过原版纹理方法的渲染模组采用可选注入；监听器阶段进度仍保留。尚未穷尽所有整合包、显卡、着色器与全屏组合。准确测试记录和重现命令见 [docs/testing.md](docs/testing.md)。

## 验证

```powershell
.\gradlew.bat test
.\gradlew.bat runClient -PclientSmoke --no-configuration-cache
.\gradlew.bat runClient25 -PclientSmoke -PangelicaSmoke --no-configuration-cache
```

冒烟测试会修改**开发客户端**的配置和测试资源包，保存截图与 `modernnh-smoke/result.txt` 后退出。它只在显式使用 `-PclientSmoke` 时编译；**不要携带该参数构建发布包**。发布前执行 `gradlew clean build`，确保没有测试模组残留。

## 许可

MIT，见 [LICENSE](LICENSE)。使用 GTNH 开发工具不表示 GTNH 官方认可或维护本项目。
