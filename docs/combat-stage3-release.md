# LegacyVisualFix 0.2.0-combat.3

第三阶段预发布：服务端确认有效近战命中后，目标模型短暂向远离玩家的方向倾斜。默认 3°、140 ms，连续命中从当前姿态衔接并限制幅度。保留旧版连击、真实击退、伤害和碰撞箱。

- 新增独立开关 modelReaction、幅度 reactionDegrees、时长 reactionDurationMs、实体排除名单 reactionExcludedEntities。
- 默认跳过玩家、Boss 接口实体、骑乘／载客及过大模型，死亡或切换世界时不残留反应状态。
- 沿用协议 v1 和前两阶段的准星／粒子。只在攻击者客户端呈现，不新增音效或修改 EFR 配置。
- 本轮仅编译打包与静态审查，按用户要求没有运行测试或启动游戏。模组怪物、Photon 和附加渲染层兼容性尚待用户实测。

[安装、配置、手动测试与兼容范围](https://github.com/LaplaceRungeLenz/LegacyVisualFix/blob/codex/combat-feedback/docs/combat-stage3-testing.md)

替换旧 JAR 时只保留一个启用版本。本轮没有自动替换用户实例内的第二阶段 JAR。交付后暂停，等待反馈。

打包命令：`gradlew.bat spotlessApply assemble -x test`，编译及重新混淆成功。仅核对了发行包版本、必要类、渲染映射及未包含 QA 插件；没有执行测试套件。本次提交使用 `[skip ci]` 避免自动触发 CI 测试。

SHA-256：`17350d6b31b6276b104533bf582b337b662ff926983560a11608ffc420c70f41`。
