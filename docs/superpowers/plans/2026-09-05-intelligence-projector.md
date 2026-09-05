# Intelligence Projector Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement these tasks inline. Steps use checkbox syntax.

**Goal:** Display completed combined-satellite scans as native cyan architectural projections with exterior and interior controls at CENTRAL.

**Architecture:** Add a bounded geometry pass to combined scans. Persist a snapshot of occupied half-block cells, with terrain classified separately, in the satellite result. A native projector retrieves an exact completed snapshot by frequency, dimension and UUID; STRATCOM sends only that reference. Clients receive compressed geometry on demand and build a cached exposed-face mesh. Controls change clipping, terrain visibility, rotation, size and selected finding.

**Tech Stack:** Minecraft Forge 1.7.10, Java 8, existing HBM packets, OpenComputers callbacks, Lua 5.2/5.3.

**Spec:** The design and acceptance criteria below implement the user's requested detailed exterior and interior views.

## Global Constraints / Design

- Work in `/usr/local/development/hbmNTM/mod` and `/usr/local/development/hbmNTM/stratcom` on their existing feature branches.
- Only combined satellites capture or expose native projection data. Old scans require a rescan.
- Capture all 64 x 64 columns and Y=0..255 in bounded tick work; never load absent chunks. Keep loaded-column coverage explicit.
- Geometry is a scan-time half-block approximation of block bounds. Thin shapes and custom tile/entity models are not exact meshes. Findings are labeled symbols at reported coordinates, not invented physical objects.
- Hide natural terrain initially; allow it to be shown. Exterior retains the whole structure, interior supports a ceiling height, cutaway additionally removes one side. Unknown areas stay empty.
- No live-world rendering at the projector, no geometry through OC modem packets, no mesh rebuild every frame, no automatic attacks.
- Keep existing OC hologram fallback. Native projectors are preferred when unbound; explicit binding wins.

## Tasks

### 1. Capture and persist geometry
Files: new `intel/IntelProjection.java`, `intel/IntelProjectionScanner.java`, `intel/IntelProjectionMesh.java`; modify `IntelScanResult`, `IntelResultCodec`, `SatelliteIntelligenceBase`, `TileEntityMachineSatLink`; test `IntelProjectionTest`.
- [x] Test budget/resumption, missing chunks, negative origin, odd Y blocks, slab mask, persistence and combined-only data (`assertNull(surface.projection)`).
- [x] Capture a fixed byte mask per block plus terrain/glazing bitsets and loaded columns; append a combined scan phase and UUID reference callback.
- [x] Test exposed faces and clipping with adjacent blocks and a hollow room; merge coplanar faces without filling openings. Bound mesh size.
- [x] Run `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home ./gradlew test --no-daemon`.

### 2. Native table and controls
Files: new `BlockIntelProjector`, `TileEntityIntelProjector`, `RenderIntelProjector`, `GUIIntelProjector`; registrations in ModBlocks/TileMappings/ClientProxy; English localization and assembly recipe.
- [x] Test exact snapshot reference acceptance, outdated/other-mode rejection and validated view settings.
- [x] Register a one-block table, directly implement OC SimpleComponent, add `showScan`, `configure`, `getStatus`, `clear`, `getFinding` callbacks on the server thread.
- [x] Synchronize small state through tile packets and request compressed snapshot through existing control/buffer packets; save displayed snapshot for reloads. Do not resend geometry for view changes.
- [x] Render cached cyan faces/edges and coordinate-anchored numbered symbols; provide right-click controls for exterior/interior/cutaway, floor, rotation, scale, terrain and selection.
- [x] Compile/build and check renderer state restoration, culling bounds, dedicated-server optional class references and packet limits.

### 3. STRATCOM integration
Files in stratcom: `runtime/intel.lua`, `central/hologram.lua`, runtime manifest, tests, README, version and release manifest.
- [x] Test native preference, explicit binding, completed reference forwarding without model paging, view controls, legacy fallback and same-summary rescan identity.
- [x] Include native reference in scan frames, call native table from viewer ticks and expose controls through existing hologram command.
- [x] Run all Lua test suites on Lua 5.2 and 5.3 and syntax-check shipped files.

### 4. Verify and publish
- [x] Review changes against the design and rerun checks affected by fixes.
- [ ] Version mod PROD as 1.8 and STRATCOM as 3.4.0, build and push existing feature branches.
- [ ] Generate an immutable STRATCOM release manifest, verify downloaded artifacts, wait for GitHub mod JAR release.
- [ ] Provide setup, rescan, exterior/interior commands and tested limitations. Do not claim in-game visual verification unless actually performed.
