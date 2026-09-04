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

Only one active scan per satellite frequency is required for the first version. Starting a new scan while one is active should fail cleanly or explicitly replace/cancel the old scan; the preferred initial behavior is to reject with `BUSY`.

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

Example correlations:

- surface launch infrastructure above a reinforced underground chamber increases `POSSIBLE_SILO` confidence.
- exposed radar/antenna above a machinery bunker may produce a `COMMUNICATIONS` marker.
- surface power equipment correlated with underground machinery may strengthen a `POWER` facility classification.

## Scan commands
New intelligence satellites support satellite commands conceptually equivalent to:

- `target <x> <z>` -- set the target center using the existing targeting mechanism where possible.
- `scan` -- start the satellite's supported scan.
- `status` -- return idle/scanning/complete/error plus progress.
- `summary` -- return a compact textual summary of the newest completed scan.
- `surface` -- return/prepare surface intelligence data when supported.
- `subsurface` -- return/prepare subsurface intelligence data when supported.

Exact command transport should follow existing `SatelliteBase.onCommand` patterns.

The first implementation should avoid returning enormous data blobs in one string. Large visualization data should be exposed through a paged/chunked data API or through dedicated OpenComputers callbacks added to the Satellite Ground Station.

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

Exact names will be finalized in the implementation plan after checking current callback naming and payload constraints.

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

Layer toggles and colors belong in the later OpenComputer-Scripts/CENTCOM implementation, not the HBM scan engine.

## Data resolution
The source scan covers 64 x 64 blocks horizontally.

The scan engine may internally reduce resolution to fit storage/performance needs. Visualization data must be bounded and suitable for OC/hologram transfer.

The first version should prefer useful geometry over exact block fidelity.

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

Persist completed results and necessary metadata.

An in-progress scan does not have to resume perfectly after a server restart for the first implementation; it may safely reset to idle while retaining the most recent completed result. If straightforward, persisting resumable cursors is allowed but not required.

## Performance safeguards
The implementation must include hard limits for:

- scan footprint.
- per-tick work.
- number of retained findings.
- maximum visualization cells/voxels.
- maximum serialized result size.
- OC data page/chunk size.

The scanner should avoid repeated expensive tile-entity/block classification work where caching per-block-type classifications is practical.

## Extensibility
Shared intelligence code should be separated from individual satellite classes so future satellites can reuse it.

Recommended conceptual structure:

- scan job/state model.
- surface scanner.
- subsurface scanner.
- feature classifier/analyzer.
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
8. Unloaded chunks are not force-loaded and reduce coverage.
9. Scan processing is spread over ticks.
10. Completed results survive save/reload.
11. Satellite Ground Station can query status/summary and retrieve bounded visualization data through OC.
12. Existing SatelliteRelay/SATCOM functionality remains operational.

## Non-goals for first implementation
- Literal screenshots or image files of the target.
- Perfect block-for-block underground x-ray.
- Automatic target chunkloading.
- Cross-dimension reconnaissance.
- Automatic periodic scans/orbital pass simulation.
- Satellite ownership/encryption.
- Hologram rendering code in HBM itself.
- CENTCOM UI implementation.
- Satellite jamming/countermeasures.
