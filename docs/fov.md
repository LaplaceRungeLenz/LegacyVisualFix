# FOV 平滑过渡

LegacyVisualFix 0.5.0 在 Minecraft 1.7.10 原有的 FOV 插值上调整响应速度。只需安装在客户端，不修改移动速度、碰撞箱或服务器协议。

## 配置

首次启动生成 `config/legacyvisualfix/fov.cfg`，修改后重启游戏：

```text
fov {
    B:enabled=true
    I:transitionMs=300
}
```

- `enabled=false`：恢复原版 FOV 平滑。
- `transitionMs=300`：在正常 20 TPS 下，约 300 ms 完成目标倍率变化的 95%，随后继续收敛。这不是固定时长结束的动画。
- 范围 `0–2000` ms；`0` 恢复原版平滑，不表示瞬间切换。数值越大，响应越慢。低于约 216 ms 会比原版响应更快。
- 默认不改变最终 FOV 幅度；游戏内 FOV 设置照常生效。

适用于进入、退出疾跑，以及通过原版／Forge 目标倍率管线生效的速度属性、飞行和拉弓变化。直接改变实际位移的装备或外力不一定改变 FOV。本功能不平滑单独的入水投影缩放、第三方缩放镜头或 FOV 设置滑块。

## 实现与维护

1. `fov/FovConfig` 在客户端 preInit 读取独立配置，不依赖 Waila 或加载界面主题。
2. `fov/FovTransition` 将 95% 收敛时间换算为每 tick 的指数滤波系数：`1 - 0.05^(50 / transitionMs)`。
3. `mixin/fov/MixinEntityRenderer` 只修改 `updateFovModifierHand` 内唯一的 `0.5F` 系数。原版保存前一 tick、读取 Forge 目标倍率、倍率上下界及帧间插值均保留。
4. 不新增逐帧状态、事件监听器或世界引用，继续使用原版状态生命周期。暂停及低客户端 TPS 的行为沿用原版；时长按 20 TPS 定义。

精准注入要求恰好匹配一次，避免更新依赖后静默失效。如果其他模组覆盖同一方法或常量，需要针对该组合适配；不能保证与所有 FOV coremod 共存。关闭配置恢复计算行为，但并不卸载 Mixin。

## 验证

单元测试覆盖收敛时间、快速反向切换、关闭及零／负时长回退。

`./gradlew runClient -PfovSmoke --no-configuration-cache` 启动真实 Forge 客户端，以临时 WorldClient 和真实 EntityPlayerSP 验证变换后的 EntityRenderer：疾跑倍率、连续过渡、前一 tick 值、退出疾跑及关闭／零时长的原版结果。报告位于测试游戏目录的 `legacyvisualfix-fov-smoke.txt`。

现代环境使用 `runClient25 -PfovSmoke -PangelicaSmoke`。测试仅调用真实 FOV 更新管线，不替代完整 GTNH 存档内的主观手感、所有装备和实际光影包测试。正常交付构建不要使用 `-PfovSmoke`。
