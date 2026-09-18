# LegacyVisualFix 0.2.0-combat.4

第四阶段预发布：确认近战命中后的第一人称持物回弹，默认 3°、120 ms。重复命中平滑衔接，幅度不叠加，不改变原有挥动计时、输入、伤害、无敌帧或击退。

- 新增 weaponRecoil、recoilDegrees、recoilDurationMs 独立设置。
- 用近期本地攻击与目标／持物匹配减少换武器后的误触发；协议 v1 不提供精确武器关联，迟到或不确定的反馈可跳过回弹。
- 使用物品、空手、地图、第三人称、切换世界等情况抑制或清理回弹；多层贴图按同帧采样。
- 保留前三阶段功能。第三阶段收到用户测试通过反馈；本阶段仅编译打包与静态审查，未运行游戏、单元或 CI 测试。

[安装、配置、测试步骤与兼容边界](https://github.com/LaplaceRungeLenz/LegacyVisualFix/blob/codex/combat-feedback/docs/combat-stage4-testing.md)

本轮未修改用户实例中的 JAR、配置或存档。仅启用一个 LegacyVisualFix 版本；等待用户测试反馈后再继续。

打包命令：`gradlew.bat spotlessApply assemble -x test`，编译和重新混淆成功。已核对发行包版本、新类和客户端钩子，以及未包含 QA／Smoke 类；未执行测试脚本。提交使用 `[skip ci]`。

SHA-256：`4af6a259a9be536c84511e39bdf7fe8a77c307fc884ee1b9b5b14f99abd54ac9`。
