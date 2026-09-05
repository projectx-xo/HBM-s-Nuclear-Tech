# Intelligence Projection Table

Introduced in **tjHBM-NTM-v1.8**. Install the same mod JAR on server and clients.

Place the one-block **Intelligence Projection Table** in the command room and connect an OpenComputers adapter to any side, then cable it to CENTRAL. The component name is `ntm_intel_projector`. The table is available in the missile creative tab and through the assembly machine (4 steel scaffolds, 8 aluminium plates, 2 magnetrons, 2 controller circuits).

Use STRATCOM **3.4.0** with intel runtime **1.3.0**. The ground station remains connected to INTEL-1; CENTRAL does not need its own station. Only **combined intelligence satellites** expose projection snapshots.

## Scan and inspect

In CENTRAL's STRATCOM console, run one command per line:

```text
scan INTEL-1 507 1709
scan INTEL-1 status
hologram status
```

Wait for completion. The new capture pass takes about 13 additional seconds at 20 TPS. A native table is preferred automatically if there is exactly one and no explicit binding. To replace a previously bound OC hologram, use `components ntm_intel_projector` in OpenOS, then `hologram bind <full-address>` in STRATCOM.

Right-click the table to change views, floor height, cut position/axis, rotation, size, terrain and selected finding. Equivalent CENTRAL commands:

```text
hologram view exterior
hologram view interior
hologram floor 30
hologram view cutaway
hologram cut z:1709
hologram cut x:508
hologram cut none
hologram floor all
hologram terrain on
hologram terrain off
hologram rotate 90
hologram scale 8
hologram list
hologram select 2
hologram select all
hologram clear
hologram show INTEL-1
```

Exterior resets clipping. Interior initially removes the highest constructed layer; floor N retains Y ≤ N. Side cuts retain X or Z ≤ the entered world coordinate. Rotation accepts −360..360 degrees; scale accepts a longest dimension of 2..12 Minecraft blocks. Default size is 6. Clipping and selection keep the same transform. Terrain changes the fitted extent.

Cyan faces/lines represent captured geometry. Glass is captured separately and rendered translucent so windows remain visible in the exterior. Coral arrows mark missile findings; amber diamonds mark launchers/equipment; violet diamonds mark hatches. The selected finding is white. Finding numbers match the scan results. Symbols stay at reported coordinates even when several findings share a point, while labels are separated. Symbols show through walls and cuts. Inferred regions remain in the findings list and appear as a marker when selected, avoiding large speculative boxes over the building.

## Capture and performance

- Every block in loaded columns of the 64 × 64 footprint, Y=0..255, is captured in a separate bounded pass. This is independent of the older 8,192-cell structural sample limit.
- Eight occupancy bits per block approximate geometry at half-block resolution. Vanilla slabs/stairs and verified local shapes use their bounds. Thin shapes are approximate; custom block models use their occupied block footprint. Animated tiles, inventories and entities are not rendered as live remote meshes. Missiles and launch equipment remain finding symbols.
- Natural terrain is initially hidden. Stone, soil, ores, fluids and vegetation are filtered separately; enable terrain to inspect stone/earth construction. Tile-entity machinery is retained.
- Missing chunks stay empty and are reported through geometry coverage. Shape capture avoids arbitrary cable/pipe connection callbacks that could load an absent multiblock core chunk. A scan is assembled over time, not an atomic world snapshot.
- Each snapshot has a persisted UUID. The table selects by satellite frequency, dimension and exact UUID, rejects stale/non-combined references, and saves the displayed snapshot across reloads. Older scans require a new scan after updating.
- Dense geometry travels through the mod, not OC modem pages. Compressed buffer packets use a 32-bit length rather than vanilla NBT's 32 KiB limit. Clients request a scene when needed; changing a view sends only controls.
- Exposed coplanar surfaces are merged without filling rooms or openings. Client mesh construction advances in bounded slices and compiled geometry is cached. A 50,000-quad limit reports truncation on the table; a tighter floor/side cut reduces complexity.

## OC API

The ground station's `intelProjection()` returns `true, frequency, dimension, snapshotId` only for a completed combined result with geometry. The table provides:

| Callback | Purpose |
| --- | --- |
| `showScan(frequency, dimension, snapshotId)` | Select an exact completed snapshot; returns success and status/error. Source dimension must already be loaded. |
| `configure(action, value)` | Apply a view/floor/cut/select/rotate/scale/terrain setting; both arguments are strings. |
| `getStatus()` | Status string and finding count. |
| `getFinding(index)` | Number, classification, original coordinate bounds, target type and confidence. |
| `clear()` | Clear the display. |

## Verification

JUnit covers capture budgets, missing columns, odd Y levels, slab masks, rooms/openings, glass windows, clipping, terrain filtering, saved data, combined-only references, stale scene rejection and compressed transfers exceeding 32 KiB. All 44 Java tests pass. All nine STRATCOM test suites pass on Lua 5.2 and 5.3, including the runtime-to-viewer flow and CENTRAL's real modem/command routing for native and legacy devices.

The development Minecraft client starts and enters a world without OpenComputers installed. In-world projection appearance remains unverified; the computer-use tool could not attach to the development client's Java window.
