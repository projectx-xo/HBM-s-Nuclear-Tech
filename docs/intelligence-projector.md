# Intelligence Projection Table

Introduced in v1.8; use **tjHBM-NTM-v1.10** for a full-color miniature with Minecraft block textures. Install the same mod JAR on server and clients, then run a fresh combined scan. Earlier snapshots did not save block types or metadata and cannot display their original textures.

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

The building uses normal block textures and colors, with opaque walls and transparent glass. Vanilla blocks retain their native shapes and metadata, including stair orientation and slab height. HBM concrete retains its captured color/texture variant. Coral arrows mark missile findings; amber diamonds mark launchers/equipment; violet diamonds mark hatches. The selected finding is white. Finding numbers match the scan results. Symbols stay at reported coordinates even when several findings share a point, while labels are separated. Symbols show through walls and cuts. Inferred regions remain in the findings list and appear as a marker when selected, avoiding large speculative boxes over the building.

## Capture and performance

- Every block in loaded columns of the 64 × 64 footprint, Y=0..255, is captured in a separate bounded pass. This is independent of the older 8,192-cell structural sample limit.
- Registry names and four-bit metadata are saved in a compact palette, so textures do not depend on matching numerical block IDs. Vanilla block shapes use Minecraft's block renderer with captured neighbors. Custom block renderers use textured captured bounds, approximated at half-block resolution; ordinary full-cube HBM blocks retain their exact textures. Animated tile/entity meshes, inventory contents and biome-specific tint are not captured. Missiles and launch equipment remain finding symbols.
- Natural terrain is initially hidden. Stone, soil, ores, HBM mineral clusters/resource stone, naturally generated keyhole stone, fluids and vegetation are filtered separately; enable terrain to inspect stone/earth construction. Tile-entity machinery is retained; the known bedrock ore tile is classified as terrain.
- Missing chunks stay empty and are reported through geometry coverage. Shape capture avoids arbitrary cable/pipe connection callbacks that could load an absent multiblock core chunk. A scan is assembled over time, not an atomic world snapshot.
- Each snapshot has a persisted UUID. The table selects by satellite frequency, dimension and exact UUID, rejects stale/non-combined references, and saves the displayed snapshot across reloads. Older scans require a new scan after updating.
- Block snapshots travel through the mod, not OC modem pages. Clients request compressed snapshots in 64 KiB pieces, keyed by snapshot UUID and byte offset; each piece stays below Minecraft's packet limit. Missing pieces can be retried, and stale pieces cannot replace a newer scan. Changing a view sends only controls.
- Fully enclosed blocks are skipped. The client compiles opaque and transparent block geometry in bounded batches and caches it. Rotation, scale and finding selection reuse the cache; cuts, terrain changes and texture reloads rebuild it. A 65,536-visible-block limit reports truncation on the table; a tighter floor/side cut reduces complexity.

## OC API

The ground station's `intelProjection()` returns `true, frequency, dimension, snapshotId` only for a completed combined result with saved block types and metadata. The table provides:

| Callback | Purpose |
| --- | --- |
| `showScan(frequency, dimension, snapshotId)` | Select an exact completed snapshot; returns success and status/error. Source dimension must already be loaded. |
| `configure(action, value)` | Apply a view/floor/cut/select/rotate/scale/terrain setting; both arguments are strings. |
| `getStatus()` | Status string and finding count. |
| `getFinding(index)` | Number, classification, original coordinate bounds, target type and confidence. |
| `clear()` | Clear the display. |

## Verification

JUnit covers capture budgets, missing columns, odd Y levels, palette reuse, block metadata, saved data, legacy snapshot detection, invalid palette indices, clipping, terrain filtering, combined-only references, stale scene rejection and multi-piece transfers exceeding 2 MiB, including Forge's padded packet buffers. Snapshot lookup tests verify that cuts expose neighboring blocks without shifting coordinates or consulting a live world. A regression test checks that HBM geological deposits stay in the terrain layer and do not shift the building's fitted bounds. STRATCOM's existing tests cover the runtime-to-viewer flow and CENTRAL's modem/command routing on Lua 5.2 and 5.3.

The local test fixture captures a building with colored HBM concrete, stone brick, wood, glass, slabs and differently oriented stairs. It loads a real scanner snapshot directly into the table, so it does not verify live satellite-to-table operation on an OpenComputers server.

The v1.10 development client rendered the textured snapshot without OpenGL errors. User-captured screenshots confirmed the exterior, colored concrete floors, transparent glass, wooden partitions, slab heights, stairs and finding markers viewed from inside the miniature. The native packet path initially exposed Forge backing-array padding; the receiver fix now has a regression test. The complete Java suite passes 51 tests.

Published JAR: [tjHBM-NTM-v1.10.jar](https://github.com/projectx-xo/HBM-s-Nuclear-Tech/releases/download/tjHBM-NTM-v1.10/tjHBM-NTM-v1.10.jar). [GitHub build](https://github.com/projectx-xo/HBM-s-Nuclear-Tech/actions/runs/33991363420) passed; the downloaded asset matches SHA-256 `ce3c0626b4b6bf4deeea1c089a07f355736f71746035b278184020d2fff01145`.
