# Intelligence Satellites Design

## Goal
Add three new persistent HBM intelligence satellite types that can produce reconnaissance products for OpenComputers/CENTCOM and, later, drive an OpenComputers hologram projector.

The three new satellites are:

- `SURFACE_RECON` -> `SatelliteSurfaceRecon`
- `SUBSURFACE_INTEL` -> `SatelliteSubsurfaceIntel`
- `COMBINED_INTEL` -> `SatelliteCombinedIntel`

The system must integrate with HBM's existing satellite frequency, launch, persistence, Satellite ID Manager, Soyuz, `/ntmsatellites orbit`, and Satellite Ground Station flows.

## Existing HBM foundations
HBM already has:

- `ItemSatellite` with enum-backed satellite variants.
- `XSatelliteRegistry` mapping satellite items to persistent satellite classes and IDs.
- `SatelliteSavedData` for world persistence keyed by satellite frequency.
- target coordinates in `SatelliteBase` and satellite command handling.
- `SatelliteMapper` for surface-oriented reconnaissance-style behavior.
- `SatelliteScanner` with type `DEPTH_SCANNER`, but no substantive scan implementation yet.
- Satellite Ground Station / `ntm_satlink` OpenComputers integration.

The new system extends these mechanisms rather than introducing a parallel orbital framework.

## Satellite items and registry
Append three new values to `ItemSatellite.EnumSatType` so existing metadata values remain stable:

- `SURFACE_RECON`
- `SUBSURFACE_INTEL`
- `COMBINED_INTEL`

Add three new persistent satellite classes and assign new unused `XSatelliteRegistry.idToClass` IDs after the current highest ID.

Register each enum item variant with its corresponding class in `itemToClass`.

The new items must work with:

- Satellite ID Manager frequency assignment/copying.
- `/ntmsatellites orbit`.
- Soyuz payload launching.
- `/ntmsatellites list`.
- Satellite SavedData serialization/deserialization.
- Satellite Ground Station selection by frequency.

## Scan targeting
All three satellite types use a designated center coordinate `(targetX, targetZ)` inherited from the existing satellite targeting model.

Default scan footprint:

- 64 x 64 horizontal blocks centered on the target.

The implementation should make the footprint constants easy to change later.

A scan never force-loads chunks. Only already-loaded target chunks are scanned. If part or all of the requested area is unloaded, the scan result records incomplete coverage rather than loading the area.

## Scan execution model
Scanning must be incremental across server ticks.

The system must not synchronously iterate the full 64 x 64 x world-height volume in a single command callback or server tick.

Each satellite maintains a scan job with:

- requested target center.
- requested scan mode.
- scan start world time.
- current progress/cursor.
- covered columns/voxels.
- unloaded/missing coverage.
- completion state.
- most recent completed result.

A bounded number of columns or sample cells is processed per server tick. Constants should control work budget so performance can be tuned after real server profiling.

Only one active scan per satellite frequency is required for the first version. Starting a new scan while one is active should reject with `BUSY`.

## Cached intelligence
Completed intelligence products are cached in the satellite instance and persisted through `SatelliteSavedData`/satellite NBT.

A cached result includes at minimum:

- scan mode/type.
- target center.
- footprint dimensions.
- world/dimension identifier where appropriate.
- scan start and completion world times.
- coverage percentage.
- summary findings.
- downsampled visualization data.

This allows CENTCOM to view the most recent intelligence even after the target chunks unload.

## Surface Reconnaissance Satellite
`SatelliteSurfaceRecon` produces a surface intelligence product.

### Surface height model
For each sampled horizontal cell, determine the highest meaningful non-air surface block/height.

The scan result stores a downsampled 2.5D/3D surface model appropriate for hologram rendering rather than a literal screenshot.

Recommended first representation:

- 64 x 64 source area.
- downsample to a maximum hologram-friendly X/Z resolution determined by the renderer adapter.
- each cell stores relative surface height plus a coarse classification.

### Surface classifications
Surface cells/findings may classify:

- natural terrain.
- constructed structure.
- reinforced/hardened construction.
- exposed HBM machinery.
- exposed launch/missile infrastructure.
- exposed radar/antenna infrastructure.
- exposed power infrastructure.

The classifier should prefer explicit HBM blocks/tile entities where known, with conservative generic heuristics for modded/vanilla construction.

### Surface entity intelligence
Optional surface-visible entity/player observations may be included if inexpensive and consistent with existing spy-satellite behavior.

This is secondary to terrain/structure reconnaissance and must not become a global entity tracker.

## Subsurface Intelligence Satellite
`SatelliteSubsurfaceIntel` produces classified underground intelligence rather than exact block-by-block x-ray output.

### Core principle
The satellite reports geometry and likely facility functions with confidence scores. It does not reveal the exact identity and coordinate of every hidden block.

### Subsurface sampling
Scan from the local surface toward bedrock using an incremental volumetric sampling/downsampling scheme.

The implementation should detect and group meaningful underground features rather than preserve all stone/dirt voxels.

### Underground features
Detect and summarize:

- significant air cavities / enclosed voids.
- tunnel-like cavities.
- bunker-sized chambers.
- reinforced/hardened shells.
- dense machinery concentrations.
- high-value HBM machinery.
- power-generation/storage concentrations.
- missile/launch-related equipment where detectable.
- radar/communications equipment where applicable.

### Facility classifications
Findings may receive classifications such as:

- `CAVITY`
- `TUNNEL`
- `BUNKER`
- `POSSIBLE_SILO`
- `POWER`
- `MACHINERY`
- `COMMUNICATIONS`
- `REINFORCED_STRUCTURE`

Every inferred classification should carry a confidence value rather than pretending to be certain.

Example:

```
BUNKER
bounds: x=-1262..-1231, y=18..31, z=3391..3414
reinforced_shell: true
machinery_density: high
possible_missile_system: true
confidence: 0.84
```

### Confidence model
Confidence is derived from evidence such as:

- cavity size/shape.
- depth.
- shell hardness/reinforced block presence.
- machine density.
- known HBM tile/block types.
- launch-equipment signatures.
- power-system signatures.

The first implementation can use deterministic weighted heuristics. The data model should leave room for future tuning without changing the external API.

## Combined Intelligence Satellite
`SatelliteCombinedIntel` runs both surface and subsurface reconnaissance over the same target and produces a fused site product.

It should reuse shared scanner/analyzer code rather than duplicate both implementations.

The combined result contains:

- surface model.
- subsurface geometry/findings.
- fused site summary.
- correlated markers.
- structural material/strength analysis for detected constructed or reinforced geometry.

Example correlations:

- surface launch infrastructure above a reinforced underground chamber increases `POSSIBLE_SILO` confidence.
- exposed radar/antenna above a machinery bunker may produce a `COMMUNICATIONS` marker.
- surface power equipment correlated with underground machinery may strengthen a `POWER` facility classification.

### Structural engineering intelligence
Only the Combined Intelligence Satellite performs exact structural-material analysis.

For relevant constructed/reinforced cells, the scanner may retain the exact block registry identity and metadata internally and derive the block's effective blast resistance using the HBM/Minecraft block implementation available at scan time.

The stored structural cell model should include, where available:

- block registry ID.
- block metadata.
- effective blast resistance as a floating-point value.
- coarse material/role classification.
- world position or downsampled cell position.

The OC-facing API should not require thousands of one-block calls. Structural data must be available through bounded pages/slices suitable for CENTCOM and hologram rendering.

The Combined result should derive facility-level structural metrics including:

- dominant shell material(s).
- average shell blast resistance.
- maximum shell blast resistance.
- estimated wall thickness.
- estimated roof thickness.
- estimated floor thickness where detected.
- weak-point regions where local resistance/thickness is materially lower than the surrounding shell.
- resistance-band visualization cells.

Example report:

```
FACILITY: BUNKER-01
OUTER SHELL
  dominant material: Red Concrete
  blast resistance: 84.0
  thickness estimate: 3-5 blocks

STRUCTURAL ASSESSMENT
  average shell resistance: 79.2
  maximum resistance: 120.0
  weakest sector: NORTHWEST WALL
  penetration difficulty: HIGH
```

The exact raw block identity/blast-resistance capability is intentionally exclusive to `COMBINED_INTEL`. `SUBSURFACE_INTEL` continues to report classifications such as `REINFORCED_STRUCTURE` and confidence values without exposing exact hidden block identities.

Recommended resistance bands for visualization are initially:

- `< 10`: light.
- `10 .. < 40`: hardened.
- `40 .. < 100`: heavy.
- `100 .. < 500`: extreme.
- `>= 500`: strategic.

These bands are presentation/classification defaults, not changes to HBM explosion physics.

## Scan commands
New intelligence satellites support satellite commands conceptually equivalent to:

- `settarget <x> <z>` -- use the existing `SatelliteBase` target command.
- `scan` -- start the satellite's supported scan.
- `status` -- return idle/scanning/complete/error plus progress.
- `summary` -- return a compact textual summary of the newest completed scan.
- `surface` -- return/prepare surface intelligence data when supported.
- `subsurface` -- return/prepare subsurface intelligence data when supported.
- `structure` -- Combined-only structural analysis summary/data selector.

Exact command transport should follow existing `SatelliteBase.onCommand` patterns.

The first implementation should avoid returning enormous data blobs in one string. Large visualization/structural data should be exposed through a paged/chunked data API or through dedicated OpenComputers callbacks added to the Satellite Ground Station.

## OpenComputers / Satellite Ground Station integration
The Satellite Ground Station is the ground interface for intelligence retrieval.

The HBM-side implementation should expose intelligence-aware OpenComputers callbacks without pretending the satellite itself is a directly attached OC component.

Recommended additions to `ntm_satlink` include concepts such as:

- query current satellite type/frequency.
- set intelligence target.
- start scan.
- query scan status/progress.
- get summary.
- enumerate findings.
- retrieve visualization slices/pages.
- retrieve Combined structural analysis pages and facility-level structural metrics.

Exact names are finalized in the implementation plan.

The implementation must preserve existing RoR and SATCOM functionality.

## Hologram-ready data product
The intelligence scanner does not directly control the OpenComputers hologram projector.

Instead, it produces compact data that CENTCOM can retrieve and render.

### Surface hologram data
Represent the surface as downsampled relative-height columns/voxels plus coarse categories.

### Subsurface hologram data
Represent only meaningful underground geometry/features:

- cavity boundaries or occupancy cells.
- reinforced shells where useful.
- classified feature markers.

Ordinary solid geology is omitted to avoid filling the hologram and obscuring the useful intel.

### Combined hologram
CENTCOM can render:

- surface terrain/structures as the primary layer.
- underground cavities/facility shapes as a secondary layer.
- threat/facility markers such as bunker, tunnel, possible silo, power, communications.
- structural-strength mode using resistance bands for Combined-only scan data.

Layer toggles and colors belong in the later OpenComputer-Scripts/CENTCOM implementation, not the HBM scan engine.

## Data resolution
The source scan covers 64 x 64 blocks horizontally.

The scan engine may internally reduce resolution to fit storage/performance needs. Visualization data must be bounded and suitable for OC/hologram transfer.

The first version should prefer useful geometry over exact block fidelity except for Combined structural cells, where exact source block identity/resistance may be retained before/downsampled during structural analysis.

## Chunk loading and coverage
No intelligence satellite may force chunks to load in this version.

For every scan:

- loaded sections are scanned.
- unloaded sections are skipped.
- coverage percentage is recorded.
- summaries clearly state incomplete coverage.
- cached prior data is not silently presented as current data without its timestamp/age.

This is both a performance requirement and an intentional gameplay limitation.

## Persistence
Satellite-specific scan state/results require serialization through each satellite's existing NBT persistence hooks.

Persist completed results and necessary metadata, including Combined structural analysis pages/metrics subject to hard result-size limits.

An in-progress scan does not have to resume perfectly after a server restart for the first implementation; it may safely reset to idle while retaining the most recent completed result.

## Performance safeguards
The implementation must include hard limits for:

- scan footprint.
- per-tick work.
- number of retained findings.
- maximum visualization cells/voxels.
- maximum structural-analysis cells.
- maximum serialized result size.
- OC data page/chunk size.

The scanner should avoid repeated expensive tile-entity/block classification and blast-resistance lookup work where caching per-block/meta classifications is practical.

## Extensibility
Shared intelligence code should be separated from individual satellite classes so future satellites can reuse it.

Recommended conceptual structure:

- scan job/state model.
- surface scanner.
- subsurface scanner.
- feature classifier/analyzer.
- structural material/blast-resistance analyzer.
- intelligence result/data model.
- serialization helpers.
- satellite wrappers for Surface, Subsurface, Combined.

This also leaves room for later features such as:

- periodic automatic rescans.
- scan scheduling/orbital revisit delays.
- weather/jamming/counter-recon effects.
- change detection between scan products.
- richer emitter/SIGINT classification.

These are not part of the first implementation.

## Security/gameplay behavior
The first version has no cryptographic satellite ownership layer.

Possession of the correct frequency and a ground station is sufficient to operate/read the satellite, consistent with existing HBM frequency-oriented satellite mechanics.

STRATCOM/CENTCOM may later add application-level authorization.

## Failure behavior
The system must fail cleanly for:

- unsupported command for satellite type.
- scan requested while busy.
- target area entirely unloaded.
- partial unloaded coverage.
- missing/invalid satellite frequency.
- malformed target/scan arguments.
- OC payload request outside available result/page bounds.

No malformed intelligence request should crash the server or satellite saved data.

## Initial testing targets
Implementation verification should cover:

1. Each new item can receive a Satellite ID.
2. Each item can be launched by `/ntmsatellites orbit`.
3. `/ntmsatellites list` restores the correct class after save/reload.
4. Surface scan recognizes a known constructed test structure.
5. Subsurface scan detects a known bunker cavity and reinforced shell.
6. Subsurface scan returns classifications/confidence rather than full hidden block inventory.
7. Combined scan contains both result types and correlation markers.
8. Combined scan records the correct block registry identity/metadata and effective blast resistance for known structural test blocks, including an HBM block with a known resistance value.
9. Combined scan derives shell averages/maxima/thickness/weak-point metrics from a controlled test bunker.
10. Unloaded chunks are not force-loaded and reduce coverage.
11. Scan processing is spread over ticks.
12. Completed results survive save/reload.
13. Satellite Ground Station can query status/summary and retrieve bounded visualization/structural data through OC.
14. Existing SatelliteRelay/SATCOM functionality remains operational.

## Non-goals for first implementation
- Literal screenshots or image files of the target.
- Perfect block-for-block underground x-ray from the Subsurface satellite.
- Automatic target chunkloading.
- Cross-dimension reconnaissance.
- Automatic periodic scans/orbital pass simulation.
- Satellite ownership/encryption.
- Hologram rendering code in HBM itself.
- CENTCOM UI implementation.
- Satellite jamming/countermeasures.
- Changing any block's actual HBM/Minecraft explosion resistance or explosion physics.
