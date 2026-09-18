# 第二阶段 GTNH 实机测试报告

日期：2026-09-19。版本：`0.2.0-combat.2`。结果：本阶段功能与回归检查通过；夜间可见性及未覆盖范围见下文。

## 新功能

服务端确认玩家近战造成实际损失后，攻击者客户端在目标附近显示短粒子：扣血白色，仅消耗吸收生命金色。默认每次 6 粒，单次上限 8 粒，滚动 200 ms 配额及活动上限均为 32；正常寿命 4 个客户端 tick。支持 auto / always / off，尊重游戏粒子设置，并在自动模式下避让 EFR 已开启的伤害粒子。

没有修改伤害、攻击输入、无敌帧、击退、AI、旧版连击或协议 v1。粒子位置是客户端包围盒附近的估算位置，并非精确刀刃接触点；仅攻击者可见。

## 环境与方法

通过 Prism Launcher 界面启动用户指定实例，进入专用 `新的世界 LVF Combat Stage1 QA` 存档。原存档 `新的世界` 未进入，level.dat 修改时间仍为 2026-09-18 23:06:59。

- GTNH 2.9.0-beta-3，Minecraft 1.7.10，Forge 10.13.4.1614，Oracle Java 26.0.2。
- Angelica 2.2.10、lwjgl3ify 3.0.31、Hodgepodge 2.7.196、UniMixins 0.3.1。
- Modernity-GTNH 2026-09-07 与 DarkReimagined 2.0.1；Photon v1.3b 开启、关闭分别运行。
- Special Mobs 3.7.5、Infernal Mobs 1.10.6-GTNH、Thaumcraft 4.2.3.5、Ender Zoo 1.3.6、Twilight Forest 2.7.40。
- GregTech 5.09.54.133、TConstruct 1.14.108-GTNH、Et Futurum Requiem 2.6.58-GTNH。

两轮使用同一份最终发布 JAR，临时 QA 插件调用实际玩家近战方法、记录服务端结果，并观察正常网络消息之后进入原生 EffectRenderer 的粒子。不是用离线模拟器替代游戏。批量样本主要是注册表创建的真实实体类；牛和三个模组目标另外生成进世界，经过网络与客户端渲染链路。批量攻击由 QA 驱动，不等同于全部手动鼠标战斗。

为捕获旧准星提示，测试配置 durationMs 为 500；实际最终采样均约 100–110 ms。新粒子寿命保持生产值 4 tick。交付前已恢复原来的 160 ms 准星提示；本轮不声称重新验证了 160 ms 提示的完整消失时间。

## 结果

| 检查 | Photon 开启 | Photon 关闭 |
| --- | --- | --- |
| 伤害／吸收损失与反馈事件核对 | 42/42 | 42/42 |
| 开关前后状态及受控连续攻击对照 | 2/2 | 2/2 |
| 粒子生成、颜色、寿命、配额、配置检查 | 16/16 | 16/16 |
| 第一阶段准星像素检查 | 5/5 | 5/5 |

本地 `gradlew.bat build` 成功，54 项 JUnit 测试，0 失败、0 错误。发布包检查通过：Java 8 字节码、混入映射和必要类齐全，不含临时 QA 类。

粒子检查覆盖白色／金色和三个模组目标的实际网络事件、过期清理、off、最少粒子、较少粒子、EFR 开启时 auto 避让／always 允许、EFR 关闭时 auto 启用、隐形／缺失／无效目标跳过及百次突发调用上限 32。EFR 开关检查临时修改内存字段并还原，未修改 EFR 配置文件。

开关状态对照包含 health、absorption、motion、hurtTime、hurtResistantTime；连续攻击对照为手动推进受伤计时器的 30 tick 受控轨迹，不代表真实网络下的 20 CPS、完整物理或延迟测试。

截图人工检查：白天牛的白色与金色粒子在两种渲染模式均清楚可见。Photon 下三个模组目标附近可见粒子，其中神秘时代夜间样本较暗。无光影夜间模组样本极暗，不能据此宣称其视觉反馈清晰；客户端对象检查仍确认每次 6 粒。粒子沿用环境光照，未强制全亮；资源包也会改变图案。夜间可见性是当前体验限制。

游戏正常进入、测试和保存退出。日志包含整合包加载阶段的 Angelica 字体纹理缺失、CoreTweaks 查找 minecraft.jar、CTM/NEI 等错误或警告；没有发现本阶段 combat 粒子类的异常栈。此结论不表示整包日志零错误，也不把加载栈经过原有 ReloadScreen 当作新粒子故障。

## 兼容范围

不使用原版怪物白名单，沿用标准近战伤害链路和实际生命／吸收损失确认。实测范围如下：

| 类别 | 具体样本与覆盖程度 |
| --- | --- |
| 原版 | Cow、Zombie、Skeleton；牛另外验证白色／金色网络粒子与截图 |
| Special Mobs | SpecialZombie、BrutishZombie、GiantZombie、FireZombie、PlagueZombie、BrutishSkeleton、VampirePigZombie；BrutishZombie 另外进世界验证粒子 |
| Thaumcraft | BrainyZombie、GiantBrainyZombie、CultistKnight、Wisp；BrainyZombie 另外进世界验证粒子 |
| Ender Zoo | Enderminy、FallenKnight 的伤害确认；本轮没有独立粒子截图 |
| Twilight Forest | Swarm Spider、Minotaur、Helmet Crab、Naga、Twilight Lich；Helmet Crab 另外进世界验证粒子；巫妖无实际损失时没有成功反馈 |
| Infernal Mobs | Bulwark、Vengeance、Regen、1UP 单次攻击样本；没有覆盖再生／复活完整生命周期 |
| 模组武器 | GT 钢制小刀、TiC 铁大剑的玩家近战确认 |

以上共 21 个实体类样本，并非所有模组怪物的保证。Boss 多阶段／多部件代理、自定义血量、绕开标准伤害方法、全部附魔组合、自然生成初始化与长期 AI 行为、独立多人服务器及高延迟尚未完整覆盖。客户端看不到、隐形、被方块遮挡或过远目标时，局部粒子会跳过，准星确认仍可显示。方块遮挡和距离分支有代码防护，本轮未单列实际整包观测用例。

## 已修复的问题

1. 粒子首次渲染可能从坐标原点插值：构造时初始化 prevPos，加入失败后通过的回归测试。
2. 原生渲染器提前淘汰粒子而不标记死亡时，活动配额可能永久占满：按时间回收记录，同时终止残留对象并清零 alpha，覆盖淘汰与过期测试。
3. 初始粒子图案在当前资源包中太小，且可能被突出的牛模型遮住：使用通用闪光图案并调整大小和表面外偏移。
4. 测试工具同步编码整幅 PNG 拖慢帧，造成准星假失败：改为后台编码后重新完成两轮；这是 QA 修复，未通过改变发布代码计时掩盖失败。早期失败与校准日志保留在本地 artifacts 中。

## 交付状态与复核

第二阶段 JAR 已安装；第一阶段 JAR 保留为 `.disabled`。临时 QA 插件已移出实例。Photon 已恢复开启、粒子设置为原来的全部、debug=false、durationMs=160、particleMode=auto、particlesPerHit=6。游戏已保存退出，Prism Launcher 保持打开。

SHA-256：`fd03b9d8245ee401d03fb253de08a1d3cf1d85df5c6ce7d0c7ca568ab75b692f`。

保存的文本证据在 [validation/combat-stage2-gtnh](validation/combat-stage2-gtnh/)，可运行：

```text
python scripts/verify_pack_qa.py docs/validation/combat-stage2-gtnh --stage2
python scripts/verify_release.py artifacts/combat-stage2/legacyvisualfix-0.2.0-combat.2.jar
```

本地完整截图与日志位于 `artifacts/combat-stage2/shader-on/` 和 `shader-off/`；完整游戏日志不发布到 GitHub。手动复测说明见 [第二阶段测试方法](combat-stage2-testing.md)。本阶段交付后等待反馈，再开展下一阶段。
