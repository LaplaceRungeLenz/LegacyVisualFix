# GTNH 彩色拖尾物品清单

依据本地 GTNH 2.9.0 beta3 实例的实际 JAR 字节码和 Minecraft 1.7.10 Forge 源码核查。颜色取自当前 `ItemStack.getRarity().rarityColor`，不是名称颜色、电压等级或闪光效果。物品必须拿在鼠标上移动，且 `trails` 开启。

这是已核查清单，不是全整合包所有 metadata/NBT 组合的穷尽枚举；其他模组仍可能提供更多彩色物品。下列为代码级判定，未逐件在完整整合包中实测。

| 颜色 | 物品或类别 | 判定条件/物品类 |
| --- | --- | --- |
| 黄色 #FFFF55 | 附魔书、原版唱片 | 附魔书含储存附魔；唱片固定 uncommon |
| 青色 #55FFFF | 普通金苹果；附魔原版工具、武器、盔甲、弓、钓鱼竿 | 金苹果 metadata=0；其他继承原版 Item 判定的已附魔堆叠 |
| 粉紫 #FF55FF | 附魔金苹果 | 金苹果 metadata=1 |
| 黄色 | 植物魔法辞典 | ItemLexicon |
| 青色 | 森林法杖 | ItemTwigWand |
| 金色 #FFAA00 | 植物魔法遗物与遗物饰品 | ItemRelic / ItemRelicBauble 子类，rarityRelic=GOLD |
| 黄色 | 神秘时代神秘锭/虚空金属剑、镐、斧、锹、锄；神秘测量仪；手镜 | ItemThaumium* / ItemVoid* 工具、ItemThaumometer、ItemHandMirror |
| 青色 | 神秘时代元素剑、镐、斧、锹、锄 | ItemElementalSword/Pickaxe/Axe/Shovel/Hoe |
| 粉紫 | 元始破坏镐 | ItemPrimalCrusher |
| 黄色 | 暮色森林铁木盔甲、钢叶盔甲、魔法地图 | ItemTFIronwoodArmor、ItemTFSteeleafArmor、ItemTFMagicMap |
| 青色 | 暮色森林骑士金属盔甲/斧/镐/剑；生命吸取、暮色、僵尸权杖；战利品头颅 | ItemTFKnightly*、ItemTFScepterLifeDrain、ItemTFTwilightWand、ItemTFZombieWand、ItemTFTrophy |
| 粉紫 | 暮色森林幻影盔甲、雪怪盔甲 | ItemTFPhantomArmor、ItemTFYetiArmor |
| 黄色 | 龙之研究飞龙盔甲、飞龙剑、传送器 MKI | WyvernArmor、WyvernSword、TeleporterMKI |
| 青色 | 龙之研究神龙剑、传送器 MKII | DraconicSword、TeleporterMKII；不能仅凭神龙名称推断 epic |
| 粉紫 | 龙之研究神龙盔甲 | DraconicArmor |
| 青色 | 无尽贪婪终望珍珠 Endest Pearl | ItemEndestPearl |
| 粉紫 | 无尽贪婪巨型元始珍珠、骷髅剑 | ItemBigPearl、ItemSwordSkulls |
| 黄色 | 无尽贪婪普通奇点 | ItemSingularity，metadata 不等于 11 |
| 红色 #FF5555 | 无尽盔甲、无尽剑/镐/斧/锹、物质团、Armok 宝珠、metadata=11 的奇点 | Cosmic 稀有度；固定红色，不是彩虹渐变 |

普通钻石、未附魔原版工具、普通下界之星，以及实际返回 common 的 GT 材料/机器，现在使用银白色 #D8DEE9。GT++ 部分物品和 BartWorks 生物实验物品按构造参数、metadata、损耗或 NBT 决定稀有度，不能把整个物品基类都列为彩色。模组重写 getRarity 时，“附魔就变青色”也不一定成立。

核查版本：Botania 1.13.34-GTNH、Thaumcraft 4.2.3.5、TwilightForest 2.7.40、Draconic-Evolution 1.5.33-GTNH、Avaritia 1.99、gregtech 5.09.54.133。中文译名随语言包不同可能变化，类名提供精确定位依据。

## 固定稀有度类索引

以下直接返回固定稀有度的类来自上述 JAR。一个类可能对应多个物品；动态分支和继承实现未穷尽，类数不等同于物品数。

| JAR | 物品类 | 稀有度 |
| --- | --- | --- |
| Avaritia-1.99.jar | `fox.spiteful.avaritia.compat.thaumcraft.ItemBigPearl` | epic |
| Avaritia-1.99.jar | `fox.spiteful.avaritia.items.ItemEndestPearl` | rare |
| Avaritia-1.99.jar | `fox.spiteful.avaritia.items.tools.ItemSwordSkulls` | epic |
| Botania-1.13.34-GTNH.jar | `vazkii.botania.common.item.ItemLexicon` | uncommon |
| Botania-1.13.34-GTNH.jar | `vazkii.botania.common.item.ItemTwigWand` | rare |
| Draconic-Evolution-1.5.33-GTNH.jar | `com.brandon3055.draconicevolution.common.items.armor.DraconicArmor` | epic |
| Draconic-Evolution-1.5.33-GTNH.jar | `com.brandon3055.draconicevolution.common.items.armor.WyvernArmor` | uncommon |
| Draconic-Evolution-1.5.33-GTNH.jar | `com.brandon3055.draconicevolution.common.items.tools.TeleporterMKI` | uncommon |
| Draconic-Evolution-1.5.33-GTNH.jar | `com.brandon3055.draconicevolution.common.items.tools.TeleporterMKII` | rare |
| Draconic-Evolution-1.5.33-GTNH.jar | `com.brandon3055.draconicevolution.common.items.weapons.DraconicSword` | rare |
| Draconic-Evolution-1.5.33-GTNH.jar | `com.brandon3055.draconicevolution.common.items.weapons.WyvernSword` | uncommon |
| gregtech-5.09.54.133.jar | `gtPlusPlus.core.item.wearable.armor.ItemArmorTinFoilHat` | uncommon |
| gregtech-5.09.54.133.jar | `toxiceverglades.item.ItemEvergladesPortalTrigger` | epic |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.api.wands.ItemFocusBasic` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.blocks.BlockMirrorItem` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.entities.golems.ItemGolemUpgrade` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.ItemKey` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemCultistBoots` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemCultistLeaderArmor` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemCultistPlateArmor` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemCultistRobeArmor` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemFortressArmor` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemGoggles` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemHoverHarness` | epic |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemRobeArmor` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemThaumiumArmor` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemVoidArmor` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.armor.ItemVoidRobeArmor` | epic |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.baubles.ItemAmuletRunic` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.baubles.ItemGirdleHover` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.baubles.ItemGirdleRunic` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemCrimsonSword` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemElementalAxe` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemElementalHoe` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemElementalPickaxe` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemElementalShovel` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemElementalSword` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemPrimalCrusher` | epic |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemThaumiumAxe` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemThaumiumHoe` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemThaumiumPickaxe` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemThaumiumShovel` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemThaumiumSword` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemVoidAxe` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemVoidHoe` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemVoidPickaxe` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemVoidShovel` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.equipment.ItemVoidSword` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.relics.ItemHandMirror` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.relics.ItemResonator` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.relics.ItemSanityChecker` | uncommon |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.wands.ItemFocusPouch` | rare |
| Thaumcraft-1.7.10-4.2.3.5.jar | `thaumcraft.common.items.wands.ItemWandCasting` | uncommon |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFArcticArmor` | uncommon |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFFieryArmor` | epic |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFFieryPick` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFFierySword` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFGiantPick` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFIronwoodArmor` | uncommon |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFKnightlyArmor` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFKnightlyAxe` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFKnightlyPick` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFKnightlySword` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFMagicMap` | uncommon |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFPhantomArmor` | epic |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFScepterLifeDrain` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFSteeleafArmor` | uncommon |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFTrophy` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFTwilightWand` | rare |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFYetiArmor` | epic |
| TwilightForest-2.7.40.jar | `twilightforest.item.ItemTFZombieWand` | rare |
