# GTNH 2.9.0 beta3 启动兼容修复

## 根因

用户提供的 [启动日志](https://mclo.gs/SHVsbrC) 中，第一个致命异常是 Better Loading Screen 1.7.16 在 Forge 模组列表初始化前调用资源重载。MyCTMLib 1.3.0 的 onClearResources 回调读取 Textures 的静态 Map，触发该类初始化。其静态 final GregTech 检测调用 Loader.isModLoaded，此时 namedMods 为 null，导致 ExceptionInInitializerError。后续 NoClassDefFoundError 是类初始化失败的连锁结果。

日志中的 LegacyVisualFix reload 包装此时只调用原方法；这不是 FOV 系数注入失败。使用实例内相同版本的 MyCTMLib、Better Loading Screen、GTNHLib 在隔离开发环境复现了同样的空指针。

## 修复范围

- 仅在模组发现完成前跳过 MyCTMLib 的资源清理回调，不跳过原版或其他模组的资源重载。
- 进入 CONSTRUCTING 后恢复原样执行，确保首次 Textures 初始化读取到完整模组列表，不永久缓存错误的 GregTech 缺失状态。
- MyCTMLib 1.3.0 把整个包加入 TransformerExclusions，直接对 Textures 注入无效。因此使用 priority 900 的标记 Mixin，在 MyCTMLib 默认 priority 1000 的 Mixin 应用后，由配置插件给已合并的 onClearResources 回调加入口判断。
- 匹配回调的 MyCTMLib 专属名称、CallbackInfo 签名以及对 Textures 的静态字段读取。不安装 MyCTMLib 时不修改任何回调。
- FOV、Waila、资源重载界面的行为保持原有实现。本版本没有引入其他任务中的背包动画。

## 可重复验证

仅测试时使用以下参数；startupCompatMods 指向三个指定 JAR 所在目录，不复制用户配置或存档：

```text
./gradlew runClient25 -PfovSmoke -PangelicaSmoke -PstartupCompatSmoke -PstartupCompatMods=/path/to/mods
```

测试版本为 MyCTMLib 1.3.0、betterloadingscreen 1.7.16-GTNH、GTNHLib 0.11.46。测试专用配置排除 Angelica 传递依赖的旧版 GTNHLib，避免重复加载。

0.5.0 在同组合中复现 namedMods 空指针。修复后到达正常客户端循环，startup smoke 检查 GregTech 检测与实际模组列表一致，并实际调用已变换资源管理器的 clearResources，确认四个 CTM Map 在启动后仍会清空。FOV smoke 同时验证疾跑、反向变化、前一 tick 状态和禁用回退。

报告位于运行目录的 legacyvisualfix-startup-smoke.txt 和 legacyvisualfix-fov-smoke.txt。正常构建不要传入任何 Smoke 参数；不包含第三方模组或测试类。本次不是完整 beta3 整合包／光影／存档测试，无法排除用户额外模组的其他独立问题。
