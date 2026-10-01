# Groundwork

A Minecraft: Java Edition mod inspired by Satisfactory. You sign on as an employee of
**Halcyon Frontier Industries**, pick a building from the company catalog, supply the
materials, and the whole structure goes up in one step.

This first version uses commands for everything, and the buildings are made of vanilla blocks. The
construction framework under it is meant to be reused by later features, such as a build tool,
custom machines and progression.

| | |
|---|---|
| Minecraft | Java Edition 1.21.1 |
| Loader | Fabric (Loader ≥ 0.16.5, Fabric API) |
| Java | 21 |
| Platforms | Developed on macOS, runs anywhere Minecraft runs (Windows, macOS, Linux) |

## Playing

```
/gw                                    help
/gw onboard                            sign your employment contract (required before building)
/gw status                             employee number, clearance, buildings completed
/gw catalog                            approved buildings with costs (click one to start a build command)
/gw info <building>                    dimensions, plus cost against what you're carrying
/gw preview <building> [pos] [facing]  outline the site for 10 s; obstructions are marked red
/gw build <building> [pos] [facing]    construct it
/gw admin clearance <players> <level>  operators only
```

`/groundwork` works the same as `/gw`. If you leave out `pos`, the building is anchored on the block
face you're looking at (up to 32 blocks away). If you leave out `facing`, it faces the way you're
looking. Buildings are placed from the front centre and extend away from you.

**Rules of construction**

1. You must be an employee (`/gw onboard`).
2. Your clearance must meet the building's requirement. Every starter building needs clearance 0.
3. Every block position of the building must be empty or easily replaced, such as air, grass, snow
   or water. It must also be loaded and inside the world.
4. Your main inventory and offhand together must hold the full cost. Worn armour is never used.

Materials are taken only after every check passes, so a failed build costs nothing. Players in
creative mode build for free.

**Starter buildings**: `foundation` (4×4 platform), `wall` (4×4), `storage_depot` (six barrels),
`smelter` (a furnace with a hopper feeding it from above).

## Architecture

```
groundwork/
├── core/      plain Java 21, no Minecraft dependency, unit tested
│   ├── building/      BuildingDefinition, JSON parser, BuildingCatalog
│   ├── structure/     StructureLayout, Facing (rotation), GridPos
│   ├── material/      BillOfMaterials, MaterialSource
│   ├── employee/      EmployeeRecord, EmployeeDirectory
│   └── construction/  ConstructionService and the ConstructionSite port
└── fabric/    the Minecraft adapter
    ├── content/       data pack loader → BuildingRegistry
    ├── world/         LevelConstructionSite, InventoryMaterials, type conversions
    ├── employee/      EmployeeData (world save persistence)
    └── command/       /groundwork, chat messages, particle previews
```

Every gameplay rule lives in `ConstructionService` in the core. It works against two interfaces:
`MaterialSource`, which is where the cost comes from, and `ConstructionSite`, which is the world being
built in. The Fabric module implements those interfaces and adds the user interface on top. This
split leaves room to grow:

- **New buildings are data, not code.** Drop a JSON file in
  `data/<namespace>/groundwork/building/` in the mod or in any data pack. See
  [docs/building-format.md](docs/building-format.md).
- **New ways to build**, such as a build gun item or a holographic placement UI, should call
  `ConstructionService.evaluate` and `construct`. That way they get the same rules as the commands.
- **New material sources**, such as nearby chests or a central storage network, implement
  `MaterialSource`.
- **Machines** can be custom blocks referenced from a layout. A `ConstructionListener` runs after
  each construction to attach behaviour.
- **Another loader or Minecraft version** needs only a new adapter module. The core does not change.

## Development

Install JDK 21 (for example Temurin: `brew install --cask temurin@21` on macOS, or the Windows
installer from adoptium.net). Then:

```
./gradlew build                 # compile, run tests, produce fabric/build/libs/groundwork-<version>.jar
./gradlew :fabric:runClient     # launch Minecraft with the mod
./gradlew :fabric:runServer     # launch a dedicated server
./gradlew :core:test            # framework tests only
```

On Windows, use `gradlew.bat` in place of `./gradlew`. To work only on the core where Fabric's Maven
repository isn't reachable, use `./gradlew -Pgroundwork.coreOnly=true :core:test`.

To install the mod, put the jar from `fabric/build/libs/` (not the `-sources` jar) into your
Minecraft `mods` folder, alongside Fabric API.

The core tests also parse every shipped building definition and check that each one has
translations, so content mistakes fail the build. Checks that need the game itself, such as
unknown block or item names, happen when the data pack loads. Any rejected definition is logged
with the reason.

### CI

`ci/github-workflow.yml` builds and tests on Ubuntu, macOS and Windows and uploads the mod jar. To
turn it on, copy it to `.github/workflows/groundwork.yml` at the repository root.

## Not built yet

These are deliberately left out of this first version: dismantling and refunds, a build tool or
placement UI, custom machine blocks, item-tag costs (for example "any planks"), and a progression
path for raising clearance. Admins set clearance by hand for now.
