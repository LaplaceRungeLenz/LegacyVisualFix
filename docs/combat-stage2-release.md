# LegacyVisualFix 0.2.0-combat.2

第二阶段预发布：服务端确认近战损失后，在目标附近生成短命中粒子。

- 实际扣血为白色；仅消耗吸收生命为金色。保留第一阶段的准星确认。
- 默认每次 6 粒，最多 8 粒；滚动 200 ms 内最多 32 粒，并限制活动粒子数。正常粒子寿命为 4 个客户端 tick。
- 自动模式检测 EFR 伤害粒子开关，避免重复叠加；最少粒子设置禁用此效果，较少粒子设置减量。
- 粒子位置根据客户端目标包围盒估算。跳过缺失、隐形、被方块遮挡、无效坐标及过远目标。
- 修复测试中发现的首帧错误插值与渲染器淘汰后配额无法回收问题；调整粒子位置及图案，改善牛模型和当前资源包下的可见性。
- 不改变伤害、无敌帧、AI、攻击输入、旧版连击或击退；不加入攻击冷却、模型后仰或范围伤害。

实机验收：GTNH 2.9.0-beta-3、Java 26、Photon 开启／关闭两轮，每轮 42 项伤害观测、16 项粒子检查、5 项准星检查通过；本地 54 项单元测试通过。粒子跟随环境光照，无光影夜间可见性较弱。

配置与亲测步骤见 [第二阶段测试方法](https://github.com/LaplaceRungeLenz/LegacyVisualFix/blob/codex/combat-feedback/docs/combat-stage2-testing.md)。具体怪物样本、测试方法和局限见 [实机测试报告](https://github.com/LaplaceRungeLenz/LegacyVisualFix/blob/codex/combat-feedback/docs/combat-stage2-gtnh-test-report-2026-09-19.md)。

沿用第一阶段 v1 协议。多人建议两端使用同一阶段版本；旧第一阶段客户端仍仅显示原反馈，无服务端支持时不猜测命中。

本阶段交付后暂停，等待用户反馈。
