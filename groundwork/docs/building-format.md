# Building definition format

Buildings are loaded from data packs at `data/<namespace>/groundwork/building/<path>.json`. The
building's id is `<namespace>:<path>`. For example, `data/groundwork/groundwork/building/smelter.json`
becomes `groundwork:smelter`. Definitions reload with `/reload`.

```json
{
  "category": "production",
  "clearance": 0,
  "cost": {
    "minecraft:stone_bricks": 10,
    "minecraft:iron_ingot": 5
  },
  "structure": {
    "key": {
      "#": "minecraft:stone_bricks",
      "F": "minecraft:furnace[facing=south]",
      "H": "minecraft:hopper[facing=down]"
    },
    "layers": [
      ["#F#", "###"],
      ["#H#", "###"]
    ],
    "anchor": [1, 0, 0]
  }
}
```

| Field | Required | Meaning |
|---|---|---|
| `cost` | yes | Item ids and positive whole amounts. These are consumed from the builder's inventory. |
| `structure.key` | yes | Single characters mapped to block states, using the same syntax as `/setblock`. |
| `structure.layers` | yes | Layers listed from the bottom up. Each layer is a list of rows from front (nearest the builder) to back. Each character in a row is one block, from left to right. |
| `structure.anchor` | no | `[x, y, z]` inside the layout that lands on the target position. The default is the front-centre of the bottom layer. |
| `category` | no | Groups the building in the catalog. Default `general`. |
| `clearance` | no | Minimum employee clearance. Default `0`. |
| `name` | no | Fallback display name for data packs that ship without a language file. |

A space in a layer means "leave this position alone". It is neither checked nor changed. To clear a
position, map a character to `minecraft:air`. All layers must have the same number of rows, and all
rows the same length.

## Orientation

Write every layout as if the builder stands south of it, looking north. "Front" is the south side,
so a block that should face the builder uses `facing=south`. When the building is placed facing
another way, both the positions and the block states (`facing`, `axis`, `rotation`, ...) are rotated
to match.

## Translations

Add these keys to `assets/<namespace>/lang/en_us.json`:

```json
"building.<namespace>.<path>": "Display name",
"building.<namespace>.<path>.desc": "One-line description",
"category.groundwork.<category>": "Category name"
```

Paths with `/` use `.` in the key. For example, `machines/press` becomes
`building.<namespace>.machines.press`.
