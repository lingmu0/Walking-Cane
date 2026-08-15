# Walking Cane

Language: [中文](README.md) · English

Walking Cane is a Minecraft mod for Forge 1.20.1 and NeoForge 1.21.1. It adds walking canes made from several materials, along with dash and teleportation abilities and storage enchantments.

## Cane Features

| Cane | Movement / swim speed bonus | Durability | Displacement ability |
| --- | ---: | ---: | --- |
| Wooden Cane | 15% | 59 | None |
| Iron Cane | 30% | 250 | None |
| Diamond Cane | 50% | 1561 | Dash, 40-tick cooldown |
| Ender Cane | 50% | 1561 | Dash and teleportation; 200-tick base teleport cooldown |
| Netherite Cane | 80% | 2031 | Dash, 30-tick cooldown |

- Holding a cane provides movement speed, swim speed, and step-height bonuses. The active hand can be configured as the main hand, offhand, or both hands.
- Dashing follows the movement input direction, or the look direction when there is no movement input, and costs 1 durability.
- Sneaking and right-clicking with an Ender Cane teleports to the open surface ahead and consumes configured teleport items from the other hand.
- Ender's Grace preserves 1 teleport consumable per enchantment level. Teleport distance is determined by the base distance and the number of consumables.

## Enchantments

### Cooldown Storage

Cooldown Storage can be applied to regular items and canes. While an item is on its vanilla cooldown, stored charges can be consumed to use it again. Charges are replenished one at a time after the cooldown ends. Charges are stored on each item stack, so stacks with different enchantment levels retain separate capacities.

Cooldown Storage is a treasure enchantment and cannot be obtained from the enchanting table, villager trades, or random loot.

### Displacement Storage

Displacement Storage is the renamed Dash Storage enchantment. It works only on Diamond, Ender, and Netherite Canes that support dashing, and it can be obtained from the enchanting table. It conflicts with Cooldown Storage. If both enchantments are forced onto the same cane, the higher effective level is used; their capacities are not added together.

Displacement Storage also stores the teleport cooldown of an Ender Cane.

### Displacement Cooldown Reduction

Displacement Cooldown Reduction works only on canes and has a maximum level of 3. It is not available from the enchanting table, but it can be obtained through villager trades and loot tables. Each level reduces the cane's dash and teleport cooldown by the configured percentage. The default is 10% per level, for a default maximum reduction of 30% at level 3.

Displacement Cooldown Reduction is not a storage enchantment and can coexist with Cooldown Storage, Displacement Storage, and Ender's Grace.

## Configuration

The configuration file is `config/walking_cane.toml` and is generated on the first launch:

| Option | Default | Description |
| --- | ---: | --- |
| `hand_mode` | `BOTH` | Hands in which canes are active: `MAIN_HAND`, `OFF_HAND`, or `BOTH` |
| `dash_strength` | `2.5` | Dash velocity multiplier, from 0.0 to 20.0 |
| `displacement_cooldown_reduction` | `0.10` | Cooldown reduction per Displacement Cooldown Reduction level; `0.10` means 10%, from 0.0 to 1.0 |
| `teleport_base_distance` | `50.0` | Base Ender Cane teleport distance without consumables |
| `teleport_distance_per_pearl` | `100.0` | Additional teleport distance per consumed item |
| `teleport_consumable_items` | `["minecraft:ender_pearl"]` | Item IDs that an Ender Cane can consume from the other hand |

The total cooldown reduction is capped at 100%, and cooldowns always retain at least 1 tick. Restart the game after changing the configuration.

## Versions and Build

- `1.20.1` branch: Minecraft Forge 1.20.1, Java 17.
- `1.21.1` branch: Minecraft NeoForge 1.21.1, Java 21.

Common verification commands:

```powershell
.\gradlew.bat compileJava --rerun-tasks
.\gradlew.bat jar
```

`runServer` can be used to check that registries and resources load correctly in a headless environment.
