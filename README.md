> 当前分支：`1.18.2-Forge`

# Infinity What: Botania Additions

简体中文 | [English](#english)

Infinity What 的可选 Botania 附属。它把 Botania 里重复出现的花田供魔、发射器布线、符文/泰拉钢读条、盖亚召唤等流程，做成可配置的机器与物品。本体 `infwhat` 不依赖 Botania，只有本附属需要。

## 功能

- **魔力之井 `mana_well`**：持续向半径内的魔力池和工作中的注魔设备供魔；可调输出速率、范围、偏移、六面开关和红石控制。
- **魔力信标 `mana_beacon` / 魔力锚点 `mana_anchor`**：把“发射器 + 魔力池 + 视线”的布线替换成无线网络。支持显式链接和自动扫描，按实际抽取量守恒传输，不复制魔力；跨维度默认关闭。
- **频道链接器 `channel_linker`**：选择源、连接/断开两端、取消自动连接。
- **符文祭坛加速器 `rune_accelerator`**：为附近符文祭坛注魔、自动触发合成，并缩短合成冷却。
- **泰拉凝聚板加速器 `terrasteel_accelerator`**：为附近泰拉凝聚板持续注入魔力。
- **产能花超频 `floral_overclock`**：额外推进附近产能花的 tick，只作用于产能花；倍率 1x–16x，燃料、寿命和粒子同步加快。
- **盖亚信标 `gaia_beacon`**：放在盖亚战场附近，血条就绪后消耗魔力结算战斗；掉落、击杀触发和成就按 Botania 原流程处理。
- **盖亚召唤符 `gaia_charm`**：右键直接召唤普通/困难盖亚，不需要信标、盖亚柱或空场地；可配置是否消耗。
- **符文模板 `rune_template`**：绑定一种符文后，消耗活石 + 魔力直接产出对应符文。
- **永续花肥 `everlasting_fertilizer`**：无限使用的 Botania 花肥。
- **门控配方**：盖亚之魂直出、泰拉钢短流程；默认关闭，服务器可用数据包移除。
- **JEI / Jade / Waila**：可选兼容。1.7.10 使用 NEI + Waila；1.12.2 使用 JEI；1.16.5+ 使用 JEI + Jade。
- **服务器治理**：专用服务器开关、维度黑名单、速率/距离上限，所有功能独立配置。

## 支持的版本

本仓库的每个分支都是一个**对应单个 Minecraft 版本 + 加载器的完整独立工程**。选择匹配的分支构建，或直接从 Releases 下载 jar。

| 分支 | Minecraft | 加载器 | Botania | Infinity What | Java |
|---|---|---|---|---|---|
| `1.7.10-Forge` | 1.7.10 | Forge 10.13.4.1614 | r1.8-249 | 2.2.0 | 8 |
| `1.12.2-Forge` | 1.12.2 | Forge 14.23.5.2860 | 1.12.2-1.10-364.4 | 2.2.0 | 8 |
| `1.16.5-Forge` | 1.16.5 | Forge 36.2.42 | 1.16.5-420.3 | 2.2.0 | 8 |
| `1.18.2-Forge` | 1.18.2 | Forge 40.3.12 | 1.18.2-435 | 2.2.0 | 17 |
| `1.20.1-Forge` | 1.20.1 | Forge 47.4.10 | 1.20.1-456-FORGE | 2.2.0 | 17 |
| `1.20.1-Fabric` | 1.20.1 | Fabric Loader 0.20.1 | 1.20.1-456-FABRIC | 2.2.0 | 17 |

## 安装

1. 安装对应版本的 Forge / Fabric。
2. 将 **Infinity What 本体**、**Botania** 和本附属 jar 放入 `mods`。
3. 按 Botania 的要求安装前置：
   - 1.7.10：Baubles 1.0.1.16；可选 NEI + Waila。
   - 1.12.2：Baubles 1.12-1.5.2；可选 JEI。
   - 1.16.5 / 1.18.2：Patchouli、Curios；可选 JEI + Jade。
   - 1.20.1 Forge：Patchouli、Curios；可选 JEI + Jade。
   - 1.20.1 Fabric：Fabric API；可选 JEI + Jade。
4. 首次启动生成 `config/infwhat_botania.toml`。

## 配置

配置文件按功能分区，默认值偏保守：

- `[mana_well]`：魔力之井开关、专用服务器开关、默认速率、上限、半径、维度黑名单。
- `[mana_wireless]`：无线网络开关、单频道上限、损耗、跨维度、端点数。
- `[rune_accelerator]` / `[terrasteel_accelerator]` / `[floral_overclock]`：各自开关、倍率、半径。
- `[gaia]`：盖亚之魂配方、盖亚信标、盖亚召唤符、困难模式。
- `[terrasteel]`：泰拉钢短流程配方。
- `[rune_template]`：符文模板开关与魔力消耗。
- `[qol]`：永续花肥。

专用服务器上魔力之井默认关闭；所有“跳过进度”类功能默认关闭。

## 构建

每个分支独立构建：

```bash
./gradlew build
```

- 1.7.10 / 1.12.2 / 1.16.5 使用 Java 8；1.18.2 / 1.20.1 使用 Java 17。
- 分支内的 `libs/` 带有编译用依赖（Botania、JEI、Jade、Patchouli、Curios 等），不会打进附属 jar；更新依赖时替换为对应版本即可。
- 产物在 `build/libs/`。

## 许可

Apache License 2.0。Botania 与 Infinity What 的代码和资源归各自作者所有；本仓库只包含附属自身代码和编译用依赖。

---

## English

**Infinity What: Botania Additions** is an optional Botania add-on for Infinity What. It turns Botania's repeated mana infrastructure into configurable machines: a finite mana well, wireless mana beacon/anchor endpoints, ranged rune/terrasteel/flower accelerators, a Gaia beacon and charm, a rune template and an everlasting fertilizer.

The core mod `infwhat` does not depend on Botania; only this add-on does. Each branch of this repository is a complete standalone project for one Minecraft version and loader. Pick the branch that matches your game, or download the jar from Releases.

Compatibility: Botania and Infinity What are required; JEI, Jade and Waila are optional (1.7.10 uses NEI + Waila). Defaults are conservative: the mana well is disabled on dedicated servers and all progress-skipping recipes are off unless enabled in `config/infwhat_botania.toml`.

Licensed under Apache License 2.0.
