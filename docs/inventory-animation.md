# 玩家物品栏入场动画

ModernNH 0.6.0 为原版生存和创造模式物品栏加入从下方向上飞入的动画。默认 250 毫秒，以 cubic ease-out 减速落位；按渲染帧和单调时钟推进，不受 20 TPS 步进限制。此版本同时包含 0.5.1 的 GTNH 启动兼容修复。

0.6.3 修复与 ModularUI2/BogoSorter 的输入冲突，以及饰品栏、时装盔甲界面返回背包时重复播放的问题；包含 0.6.2 的其他功能修复。

## 配置

客户端启动时生成 `config/modernnh/inventory.cfg`，修改后重启：

```text
inventory {
    B:enabled=true
    I:durationMs=250
    I:distance=0
}
```

| 配置 | 含义 |
| --- | --- |
| `enabled` | 独立开关，不影响其他 ModernNH 功能 |
| `durationMs` | 动画时长，0–1000 毫秒；0 关闭 |
| `distance` | 位移距离，0–1000 GUI 像素；0 自动从屏幕下沿之外进入 |

偏好轻量上滑可设 `distance=32`、`durationMs=180`。默认自动距离包含创造栏顶部页签、翻页按钮和页码所需的边距。

## 范围和交互

- 只匹配原版 `GuiInventory` 和 `GuiContainerCreative` 两个具体类。箱子、机器、其他模组容器及替换原版界面的子类不启用。
- 移动底板、人物、槽位、物品、前景文字和原版按钮/标签；创造栏分类页签、搜索框、滚动条、翻页按钮及页码同步移动。
- NEI 列表、搜索框、工具栏、书签和药水面板保持正常坐标。动画中的面板可能暂时经过固定的底部 NEI 搜索栏，由原有绘制顺序决定遮挡。
- 动画期间隐藏移动槽位的悬停高亮和提示；固定的 NEI 控件继续正常绘制。
- 动画期间的首次点击、滚轮或普通文字/操作键会立即结束动画，**该次操作被消费**，随后正常操作。对应的鼠标松开和按住期间的拖动也被消费，避免错误选取或发出物品操作。NEI 的中文输入法字符事件采用相同规则。
- Esc 和物品栏关闭键沿用原有处理，Shift/Ctrl/Alt 修饰键不结束动画。鼠标移动本身不结束动画。
- 调整 GUI 尺寸会结束当前动画；切换创造页签不重新播放。从其他 GUI 返回背包不播放动画，即使模组创建了新的背包实例。回到游戏后重新打开背包仍正常播放，首次打开创造物品栏的内部跳转也保留动画。

## 实现与兼容边界

不修改 `guiLeft/guiTop`、槽位位置、容器数据或网络协议。Mixin 只在目标绘制调用周围添加 model-view 平移，离开调用时恢复矩阵。全屏背景、药水效果和 NEI 的容器外覆盖绘制不进入动画变换。NEI 注入到槽位绘制内部的物品覆盖效果随槽位移动。

输入使用 MixinExtras `WrapOperation` 并调用原有操作链，保留 ModularUI2 的输入 Pre/Post 事件和依赖这些事件的 BogoSorter。旧版独占 `Redirect` 会抢占 ModularUI2 的同位置钩子，已通过实际模组组合复现点击失效并验证修复。打开来源通过客户端 Forge `GuiOpenEvent` 记录，不依赖饰品或时装模组类名，也不为这些模组容器添加动画。

不使用截图动画、像素回读、额外 framebuffer、模糊或同步等待；正常游戏循环继续运行。最终流畅度仍取决于客户端帧率。部分模组通过独立事件绘制的背包附属按钮尚未专门适配，可能保持固定；不能据此宣称整个 GTNH 模组组合已全面兼容。对相同原版方法进行互斥替换的第三方 Coremod 仍可能需要单独处理。

当前未增加任意模组容器的白名单或全局动画开关。NEI 是可选测试依赖，交付 JAR 不包含 NEI，也不要求安装它。

## 验证

```text
./gradlew test
./gradlew runClient -PinventorySmoke
./gradlew runClient -PinventorySmoke -PneiSmoke
./gradlew runClient25 -PinventorySmoke -PneiSmoke -PangelicaSmoke
./gradlew clean build
```

测试代码位于 `src/inventorySmokeTest/java`。它创建离线测试玩家，使用真实变换后的 GUI 和 OpenGL，比较面板移动像素、药水固定像素和按钮矩阵；安装 NEI 时还通过其公开绘制钩子检查屏幕坐标保持不变。只在显式添加 `inventorySmoke` 参数时编译，交付构建必须不带任何 Smoke 参数。

具体版本、结果和未覆盖场景见 [测试记录](testing.md)。
