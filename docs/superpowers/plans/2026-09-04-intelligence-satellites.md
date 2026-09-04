# Intelligence Satellites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three HBM reconnaissance satellites—Surface Recon, Subsurface Intel, and Combined Intel—with incremental loaded-chunk scanning, cached/persistent intelligence products, Combined-only structural material/blast-resistance analysis, and bounded OpenComputers retrieval through `ntm_satlink`.

**Architecture:** Extend the existing HBM satellite enum/registry and per-world `SatelliteSavedData`. Put shared scan state/results/classification/serialization under a focused intelligence package, keep the three satellite wrapper classes thin, run scan work through existing `SatelliteBase.onUpdateTick`, and expose bounded result pages through the existing Satellite Ground Station component. The Combined satellite reuses both scan engines and adds exact structural-source sampling plus derived blast-resistance metrics.

**Tech Stack:** Java 8, Minecraft Forge 1.7.10, HBM NTM satellite system, Minecraft block/tile APIs, OpenComputers 1.7.10/1.8.9 API, NBT persistence.

**Spec:** `docs/superpowers/specs/2026-09-04-intelligence-satellites-design.md`

## Global Constraints

- Minecraft/Forge target remains `1.7.10-10.13.4.1614-1.7.10` and Java 8.
- New enum values are appended after existing `ItemSatellite.EnumSatType` values so existing metadata is stable.
- New persistent satellite IDs are appended after current `XSatelliteRegistry` ID 12.
- Default scan footprint is exactly 64 x 64 horizontal blocks.
- Scans never force-load chunks; unloaded coverage is skipped and reported.
- Scan work is incremental across server ticks with a hard per-tick budget.
- Only one scan job may be active per satellite frequency; a second request returns `BUSY`.
- Surface Recon returns surface/structure intelligence only.
- Subsurface Intel returns classified geometry/findings and confidence, not exact hidden block identities.
- Combined Intel returns both products plus exact structural material/metadata/effective blast resistance for relevant structural cells and derived shell metrics.
- Combined structural analysis does not modify block resistance or explosion physics.
- Result/findings/visualization/structural page sizes are hard-bounded.
- Existing satellite commands, RoR behavior, and SATCOM packet methods remain operational.
- HBM must still class-load when OpenComputers is absent using the existing optional-integration pattern.

---

## File Structure

### New intelligence package
Create `src/main/java/com/hbm/saveddata/satellites/intel/` with focused files:

- `IntelScanMode.java` — enum `SURFACE`, `SUBSURFACE`, `COMBINED`.
- `IntelScanState.java` — enum `IDLE`, `SCANNING`, `COMPLETE`, `ERROR`.
- `IntelClassification.java` — enum for `NATURAL`, `STRUCTURE`, `REINFORCED_STRUCTURE`, `MACHINERY`, `POWER`, `COMMUNICATIONS`, `LAUNCH_INFRASTRUCTURE`, `CAVITY`, `TUNNEL`, `BUNKER`, `POSSIBLE_SILO`.
- `IntelResistanceBand.java` — `<10 LIGHT`, `10..<40 HARDENED`, `40..<100 HEAVY`, `100..<500 EXTREME`, `>=500 STRATEGIC`.
- `IntelFinding.java` — bounded finding DTO with classification, bounds, confidence, evidence flags, NBT serialization.
- `IntelSurfaceCell.java` — downsampled surface cell: relative position, height, classification, optional structural flags.
- `IntelStructuralCell.java` — Combined-only source/derived structural cell: position, registry ID, metadata, effective blast resistance, resistance band.
- `IntelStructuralSummary.java` — dominant materials, average/max resistance, wall/roof/floor thickness estimates, weak-point regions.
- `IntelScanResult.java` — persistent completed-result DTO containing metadata, coverage, findings, surface pages, subsurface pages, Combined structural pages/summary.
- `IntelScanJob.java` — runtime-only scan cursor/work state; resets on reload.
- `IntelBlockClassifier.java` — block/tile classification and cached block/meta property lookup.
- `IntelSurfaceScanner.java` — incremental surface-column scanner.
- `IntelSubsurfaceScanner.java` — incremental cavity/reinforcement/machinery sampler and grouping pass.
- `IntelStructuralAnalyzer.java` — Combined-only exact structural source sampling, effective blast-resistance lookup, shell/thickness/weak-point derivation.
- `IntelResultCodec.java` — bounded NBT serialization/deserialization helpers.

### New satellite wrappers
Create:

- `src/main/java/com/hbm/saveddata/satellites/SatelliteSurfaceRecon.java`
- `src/main/java/com/hbm/saveddata/satellites/SatelliteSubsurfaceIntel.java`
- `src/main/java/com/hbm/saveddata/satellites/SatelliteCombinedIntel.java`
- `src/main/java/com/hbm/saveddata/satellites/SatelliteIntelligenceBase.java` — shared command/status/job/result lifecycle for the three wrappers.

### Existing files to modify

- `src/main/java/com/hbm/items/special/ItemSatellite.java` — append three enum values.
- `src/main/java/com/hbm/saveddata/satellites/XSatelliteRegistry.java` — IDs 13/14/15 and item mappings.
- `src/main/java/com/hbm/tileentity/machine/TileEntityMachineSatLink.java` — intelligence OC callbacks/method dispatch, preserving SATCOM.
- `src/main/resources/assets/hbm/lang/en_US.lang` — item names/status strings.
- `src/main/resources/assets/hbm/textures/items/satellite.surface_recon.png`
- `src/main/resources/assets/hbm/textures/items/satellite.subsurface_intel.png`
- `src/main/resources/assets/hbm/textures/items/satellite.combined_intel.png`
- `.github/workflows/satcom-ci.yml` — continue building/releasing feature-branch JARs after intelligence changes.

### Test/verification harness
The project has no established isolated unit-test suite for Forge world/block logic. Add small pure-Java tests under `src/test/java/com/hbm/saveddata/satellites/intel/` only for logic that can run without Forge bootstrapping. World/block integration is verified by JDK8 Gradle build plus controlled in-game scenarios.

---

### Task 1: Register the three new satellite item variants and persistent IDs

**Files:**
- Modify: `src/main/java/com/hbm/items/special/ItemSatellite.java`
- Modify: `src/main/java/com/hbm/saveddata/satellites/XSatelliteRegistry.java`
- Modify: `src/main/resources/assets/hbm/lang/en_US.lang`
- Add: `src/main/resources/assets/hbm/textures/items/satellite.surface_recon.png`
- Add: `src/main/resources/assets/hbm/textures/items/satellite.subsurface_intel.png`
- Add: `src/main/resources/assets/hbm/textures/items/satellite.combined_intel.png`
- Create temporary wrappers if needed for compilation: the three satellite classes named below.

**Interfaces:**
- Produces enum constants `SURFACE_RECON`, `SUBSURFACE_INTEL`, `COMBINED_INTEL`.
- Produces registry IDs `13 -> SatelliteSurfaceRecon`, `14 -> SatelliteSubsurfaceIntel`, `15 -> SatelliteCombinedIntel`.

- [ ] **Step 1: Add the enum constants at the end of `EnumSatType`**

```java
SCIENCE_SENSOR,
SURFACE_RECON,
SUBSURFACE_INTEL,
COMBINED_INTEL,
```

- [ ] **Step 2: Add item display names**

```properties
item.satellite.surface_recon.name=Surface Reconnaissance Satellite
item.satellite.subsurface_intel.name=Subsurface Intelligence Satellite
item.satellite.combined_intel.name=Combined Intelligence Satellite
```

- [ ] **Step 3: Add the three texture files using the exact multi-texture filenames produced by `ItemEnumMulti.registerIcons()`**

Expected resource names:

```text
assets/hbm/textures/items/satellite.surface_recon.png
assets/hbm/textures/items/satellite.subsurface_intel.png
assets/hbm/textures/items/satellite.combined_intel.png
```

- [ ] **Step 4: Register persistent IDs and item mappings**

```java
idToClass.put(13, SatelliteSurfaceRecon.class);
idToClass.put(14, SatelliteSubsurfaceIntel.class);
idToClass.put(15, SatelliteCombinedIntel.class);

registerSatellite(SatelliteSurfaceRecon.class,
        new ComparableStack(ModItems.satellite, 1, EnumSatType.SURFACE_RECON));
registerSatellite(SatelliteSubsurfaceIntel.class,
        new ComparableStack(ModItems.satellite, 1, EnumSatType.SUBSURFACE_INTEL));
registerSatellite(SatelliteCombinedIntel.class,
        new ComparableStack(ModItems.satellite, 1, EnumSatType.COMBINED_INTEL));
```

- [ ] **Step 5: Run build to catch enum/resource/class registration failures**

Run:

```bash
./gradlew compileJava
```

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/hbm/items/special/ItemSatellite.java \
        src/main/java/com/hbm/saveddata/satellites/XSatelliteRegistry.java \
        src/main/resources/assets/hbm/lang/en_US.lang \
        src/main/resources/assets/hbm/textures/items/satellite.*_intel.png \
        src/main/resources/assets/hbm/textures/items/satellite.surface_recon.png \
        src/main/java/com/hbm/saveddata/satellites/SatelliteSurfaceRecon.java \
        src/main/java/com/hbm/saveddata/satellites/SatelliteSubsurfaceIntel.java \
        src/main/java/com/hbm/saveddata/satellites/SatelliteCombinedIntel.java
git commit -m "feat: register intelligence satellites"
```

**Acceptance:** all three variants appear as distinct `hbm:item.satellite` metadata entries, accept Satellite IDs through existing `ISatChip`, and deserialize via IDs 13/14/15.

---

### Task 2: Define bounded intelligence data models and NBT codec

**Files:**
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelScanMode.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelScanState.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelClassification.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelResistanceBand.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelFinding.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelSurfaceCell.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelStructuralCell.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelStructuralSummary.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelScanResult.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelResultCodec.java`
- Test: `src/test/java/com/hbm/saveddata/satellites/intel/IntelResistanceBandTest.java` if Gradle's current test configuration can execute plain JVM tests.

**Interfaces:**
- `IntelResistanceBand.fromResistance(float resistance)`.
- `IntelScanResult.writeToNBT(NBTTagCompound nbt)` / `IntelScanResult.readFromNBT(NBTTagCompound nbt)` through codec helpers.
- Hard limits:
  - `MAX_FINDINGS = 128`
  - `MAX_SURFACE_CELLS = 4096`
  - `MAX_SUBSURFACE_CELLS = 8192`
  - `MAX_STRUCTURAL_CELLS = 8192`
  - `PAGE_SIZE = 64`

- [ ] **Step 1: Write the pure resistance-band test**

```java
assertEquals(LIGHT, IntelResistanceBand.fromResistance(0F));
assertEquals(LIGHT, IntelResistanceBand.fromResistance(9.99F));
assertEquals(HARDENED, IntelResistanceBand.fromResistance(10F));
assertEquals(HARDENED, IntelResistanceBand.fromResistance(39.99F));
assertEquals(HEAVY, IntelResistanceBand.fromResistance(40F));
assertEquals(HEAVY, IntelResistanceBand.fromResistance(99.99F));
assertEquals(EXTREME, IntelResistanceBand.fromResistance(100F));
assertEquals(EXTREME, IntelResistanceBand.fromResistance(499.99F));
assertEquals(STRATEGIC, IntelResistanceBand.fromResistance(500F));
```

- [ ] **Step 2: Run the test and verify it fails before implementation**

Run the project's Gradle test target if configured; otherwise record that plain tests are unavailable and continue with compile/build verification.

- [ ] **Step 3: Implement the enums/DTOs with immutable-or-bounded collection semantics**

`IntelScanResult` must contain:

```java
public IntelScanMode mode;
public int targetX;
public int targetZ;
public int width;
public int depth;
public int dimension;
public long startedAt;
public long completedAt;
public int coveredColumns;
public int totalColumns;
public final List<IntelFinding> findings;
public final List<IntelSurfaceCell> surfaceCells;
public final List<IntelSurfaceCell> subsurfaceCells;
public final List<IntelStructuralCell> structuralCells;
public IntelStructuralSummary structuralSummary;
```

- [ ] **Step 4: Implement bounded NBT serialization**

Every read and write must clamp list counts to the hard maxima. Missing tags must produce an empty/default result rather than throw.

- [ ] **Step 5: Run tests/compile**

```bash
./gradlew test compileJava
```

Expected: exit 0 where tests are supported; otherwise `compileJava` exit 0 with the test limitation recorded.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/hbm/saveddata/satellites/intel src/test/java/com/hbm/saveddata/satellites/intel
git commit -m "feat: add intelligence scan data model"
```

**Acceptance:** completed scan products can be serialized/deserialized safely with hard count limits and resistance-band boundaries are deterministic.

---

### Task 3: Add shared satellite intelligence lifecycle and commands

**Files:**
- Create: `src/main/java/com/hbm/saveddata/satellites/SatelliteIntelligenceBase.java`
- Modify: `SatelliteSurfaceRecon.java`
- Modify: `SatelliteSubsurfaceIntel.java`
- Modify: `SatelliteCombinedIntel.java`
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelScanJob.java`

**Interfaces:**

`SatelliteIntelligenceBase` provides:

```java
public static final int SCAN_SIZE = 64;
public static final int WORK_BUDGET_PER_TICK = 32;
public IntelScanJob activeJob;
public IntelScanResult lastResult;

public abstract IntelScanMode getScanMode();
public boolean startScan(World world);
public String getScanStatus();
public String getScanSummary();
public IntelScanResult getLastResult();
```

Commands:

```text
scan
status
summary
surface
subsurface
structure
```

Existing target command remains `settarget <x> <z>` from `SatelliteBase`.

- [ ] **Step 1: Implement runtime job state with `IDLE/SCANNING/COMPLETE/ERROR` and progress counters**
- [ ] **Step 2: Implement `SatelliteIntelligenceBase.onCommandImpl()`**
  - `scan` starts only when idle; otherwise `tx = "BUSY"`.
  - `status` returns a compact semicolon-delimited status such as `SCANNING;128;4096;3`.
  - `summary` returns bounded textual summary from `lastResult`.
  - unsupported data commands return `UNSUPPORTED` on satellite types that do not provide that layer.
- [ ] **Step 3: Implement persistence**
  - call `super.writeToNBT/readFromNBT`.
  - persist only `lastResult` and metadata.
  - do not persist `activeJob`; after reload it is `null/IDLE`.
- [ ] **Step 4: Implement wrapper types/names**

```java
Surface:    getType() -> "SURFACE_RECON"
Subsurface: getType() -> "SUBSURFACE_INTEL"
Combined:   getType() -> "COMBINED_INTEL"
```

Each `getInfo()` uses its matching `EnumSatType` item name.

- [ ] **Step 5: Compile**

```bash
./gradlew compileJava
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/hbm/saveddata/satellites/SatelliteIntelligenceBase.java \
        src/main/java/com/hbm/saveddata/satellites/Satellite*Intel.java \
        src/main/java/com/hbm/saveddata/satellites/SatelliteSurfaceRecon.java \
        src/main/java/com/hbm/saveddata/satellites/intel/IntelScanJob.java
git commit -m "feat: add intelligence satellite lifecycle"
```

**Acceptance:** all three satellites support common target/scan/status/summary lifecycle and completed results persist while in-progress jobs reset safely on reload.

---

### Task 4: Implement block/tile classification cache

**Files:**
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelBlockClassifier.java`

**Interfaces:**

```java
public IntelClassification classifySurface(World world, int x, int y, int z);
public IntelClassification classifySubsurface(World world, int x, int y, int z);
public BlockIntelProperties properties(World world, int x, int y, int z);
```

`BlockIntelProperties` includes registry ID, metadata, material-like category, `boolean constructed`, `boolean reinforced`, `boolean machinery`, `boolean power`, `boolean communications`, `boolean launchInfrastructure`, and `float effectiveBlastResistance`.

- [ ] **Step 1: Implement block/meta cache key based on registry name + metadata**
- [ ] **Step 2: Resolve registry identity safely** using `Block.blockRegistry.getNameForObject(block)`; fallback to block class name if no registry name exists.
- [ ] **Step 3: Classify explicit HBM tile/block signatures conservatively**
  - tile entities imply machinery unless specifically excluded.
  - known launcher/missile/radar/satlink/power classes map to their dedicated flags.
  - high-resistance constructed blocks map to reinforced.
- [ ] **Step 4: Resolve effective blast resistance**
  - use the block's actual explosion-resistance API at the scanned coordinates where the 1.7.10 signature permits it.
  - if the coordinate-dependent API requires an exploder/entity and cannot be called safely, use the block's coordinate-independent effective resistance accessor/field exposed by Minecraft/HBM.
  - never hardcode HBM values such as `84.0`; the scan must read the current block implementation so custom pack changes are reflected.
- [ ] **Step 5: Compile and add a diagnostic helper assertion for an HBM structural block in the later in-game test**
- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/hbm/saveddata/satellites/intel/IntelBlockClassifier.java
git commit -m "feat: classify intelligence scan blocks"
```

**Acceptance:** identical block/meta lookups reuse cached classification, known HBM machinery is recognized, and blast resistance is derived from runtime block behavior rather than a separate table.

---

### Task 5: Implement incremental Surface Recon scanning

**Files:**
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelSurfaceScanner.java`
- Modify: `SatelliteIntelligenceBase.java`

**Interfaces:**

```java
public int process(World world, IntelScanJob job, IntelScanResult result, int budget);
```

Returns work units consumed; advances the scan cursor without loading chunks.

- [ ] **Step 1: For each source X/Z column, check `world.getChunkProvider().chunkExists(x >> 4, z >> 4)` before any block reads**
- [ ] **Step 2: If unloaded, increment missing coverage and advance without calling any chunk-loading getter**
- [ ] **Step 3: If loaded, determine highest meaningful surface Y and classify it**
- [ ] **Step 4: Append one bounded `IntelSurfaceCell` per source/downsampled cell**
- [ ] **Step 5: Track exposed structure/machinery/launch/radar/power findings with deduplication and a maximum of 128 findings**
- [ ] **Step 6: Wire Surface Recon and the surface phase of Combined into `onUpdateTick()` using `WORK_BUDGET_PER_TICK`**
- [ ] **Step 7: Compile**

```bash
./gradlew compileJava
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/hbm/saveddata/satellites/intel/IntelSurfaceScanner.java \
        src/main/java/com/hbm/saveddata/satellites/SatelliteIntelligenceBase.java
git commit -m "feat: add surface reconnaissance scanning"
```

**Acceptance:** a 64x64 surface scan advances over multiple ticks, never chunkloads, records coverage, and produces a bounded surface model/findings.

---

### Task 6: Implement incremental Subsurface Intel scanning and facility inference

**Files:**
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelSubsurfaceScanner.java`
- Modify: `SatelliteIntelligenceBase.java`

**Interfaces:**

```java
public int process(World world, IntelScanJob job, IntelScanResult result, int budget);
public void finalizeFindings(IntelScanResult result);
```

- [ ] **Step 1: Sample only loaded columns from local surface toward bedrock using a fixed coarse Y sampling stride**
  - Initial stride: `2` vertical blocks.
  - Do not store ordinary solid geology cells.
- [ ] **Step 2: Record meaningful occupancy/evidence cells only**
  - air cavities.
  - reinforced/constructed boundaries.
  - machinery/power/comms/launch signatures.
- [ ] **Step 3: Group neighboring cavity cells into bounded regions**
  - flood-fill/union only within the already sampled result array; never revisit world blocks during grouping.
- [ ] **Step 4: Infer classifications using deterministic thresholds**
  - long narrow cavity -> `TUNNEL`.
  - large enclosed cavity with reinforced boundary -> `BUNKER`.
  - reinforced cavity + launch/missile machinery evidence -> `POSSIBLE_SILO`.
  - machinery/power/comms evidence generates corresponding findings.
- [ ] **Step 5: Calculate confidence from normalized evidence weights**

Example first-pass weights:

```text
reinforced shell       +0.25
large enclosed cavity  +0.20
machinery density      +0.20
launch signature       +0.25
power signature        +0.10
```

Clamp to `[0.0, 1.0]`.

- [ ] **Step 6: Do not expose exact hidden block ID/meta in Subsurface results**
- [ ] **Step 7: Wire Subsurface and the subsurface phase of Combined into the tick lifecycle**
- [ ] **Step 8: Compile and commit**

```bash
./gradlew compileJava
git add src/main/java/com/hbm/saveddata/satellites/intel/IntelSubsurfaceScanner.java \
        src/main/java/com/hbm/saveddata/satellites/SatelliteIntelligenceBase.java
git commit -m "feat: add subsurface intelligence scanning"
```

**Acceptance:** controlled bunker/tunnel geometry yields classified findings/confidence without a raw hidden-block inventory.

---

### Task 7: Implement Combined structural material and blast-resistance analysis

**Files:**
- Create: `src/main/java/com/hbm/saveddata/satellites/intel/IntelStructuralAnalyzer.java`
- Modify: `SatelliteCombinedIntel.java`
- Modify: `SatelliteIntelligenceBase.java`

**Interfaces:**

```java
public int process(World world, IntelScanJob job, IntelScanResult result, int budget);
public IntelStructuralSummary finalizeSummary(IntelScanResult result);
```

- [ ] **Step 1: Restrict structural analysis to Combined mode**
  - Surface/Subsurface wrappers must never append `IntelStructuralCell`.
- [ ] **Step 2: For constructed/reinforced/machinery boundary cells already identified by the shared scan phases, read the exact source block registry ID/meta and effective blast resistance via `IntelBlockClassifier`**
- [ ] **Step 3: Append bounded `IntelStructuralCell` entries with resistance band**

```java
new IntelStructuralCell(x, y, z, registryId, meta, resistance,
        IntelResistanceBand.fromResistance(resistance));
```

- [ ] **Step 4: Derive shell metrics**
  - dominant material by structural cell count.
  - average resistance = arithmetic mean of sampled shell cells.
  - maximum resistance = max sampled resistance.
  - wall/roof/floor thickness from contiguous structural runs bordering detected facility cavities.
  - weak point = local region whose resistance-weighted thickness is at least 25% below the facility median shell score.
- [ ] **Step 5: Store Combined-only structural summary and `structure` command summary**
- [ ] **Step 6: Verify runtime value capture against a known HBM structural block**
  - Build a controlled red-concrete section in-game.
  - Confirm the scanner reports the pack's current effective value (expected by the user to be `84.0` in their build) without hardcoding that number in Java.
- [ ] **Step 7: Compile and commit**

```bash
./gradlew compileJava
git add src/main/java/com/hbm/saveddata/satellites/intel/IntelStructuralAnalyzer.java \
        src/main/java/com/hbm/saveddata/satellites/SatelliteCombinedIntel.java \
        src/main/java/com/hbm/saveddata/satellites/SatelliteIntelligenceBase.java
git commit -m "feat: add combined structural intelligence"
```

**Acceptance:** Combined reports exact registry/meta/resistance for relevant structural cells, correct resistance bands, and facility-level average/max/thickness/weak-point metrics; Subsurface still does not reveal exact materials.

---

### Task 8: Add fused Combined correlations

**Files:**
- Modify: `SatelliteCombinedIntel.java`
- Modify: `IntelScanResult.java`

**Interfaces:**
- `void correlateCombinedFindings(IntelScanResult result)`.

- [ ] **Step 1: After surface, subsurface, and structural phases complete, correlate findings by overlapping X/Z bounds**
- [ ] **Step 2: Increase `POSSIBLE_SILO` confidence when surface launch infrastructure overlaps a reinforced underground bunker/launch signature**
- [ ] **Step 3: Add `COMMUNICATIONS` when surface radar/antenna overlaps a machinery bunker**
- [ ] **Step 4: Strengthen `POWER` when surface and subsurface power evidence overlap**
- [ ] **Step 5: Include structural shell metrics in the fused textual summary**
- [ ] **Step 6: Compile and commit**

```bash
./gradlew compileJava
git add src/main/java/com/hbm/saveddata/satellites/SatelliteCombinedIntel.java \
        src/main/java/com/hbm/saveddata/satellites/intel/IntelScanResult.java
git commit -m "feat: fuse combined intelligence findings"
```

**Acceptance:** Combined is more than concatenated results; correlations materially update facility findings/confidence and include engineering-strength context.

---

### Task 9: Expose bounded intelligence retrieval through `ntm_satlink`

**Files:**
- Modify: `src/main/java/com/hbm/tileentity/machine/TileEntityMachineSatLink.java`

**Interfaces:**

Add OC callbacks while preserving all existing methods:

```text
intelSetTarget(x, z) -> boolean, string
intelStartScan() -> boolean, string
intelStatus() -> state, done, total, coverage
intelSummary() -> string
intelFindingCount() -> number
intelGetFinding(index) -> classification, confidence, bounds/evidence fields
intelSurfacePage(page) -> count, flattened bounded cell data
intelSubsurfacePage(page) -> count, flattened bounded cell data
intelStructuralPage(page) -> count, flattened Combined-only structural cells
intelStructuralSummary() -> Combined-only summary fields
```

- [ ] **Step 1: Add helper to resolve current satellite and require `SatelliteIntelligenceBase`**
- [ ] **Step 2: Add target/start/status/summary callbacks**
- [ ] **Step 3: Add finding pagination**
  - index is 1-based for Lua.
  - invalid index returns `false, "OUT_OF_RANGE"` rather than throwing.
- [ ] **Step 4: Add page callbacks with exact `PAGE_SIZE = 64`**
  - return primitive OC-safe values only: booleans, numbers, strings, byte arrays.
  - do not return Java maps/lists directly.
- [ ] **Step 5: Structural callbacks return `false, "UNSUPPORTED"` unless current satellite is `SatelliteCombinedIntel`**
- [ ] **Step 6: Add every method to `methods()` and `invoke()` without altering existing SATCOM names**
- [ ] **Step 7: Compile with OpenComputers API on the configured classpath**

```bash
./gradlew compileJava
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/hbm/tileentity/machine/TileEntityMachineSatLink.java
git commit -m "feat: expose satellite intelligence to OpenComputers"
```

**Acceptance:** an OC computer attached to the ground station can start/query scans and retrieve bounded pages; existing `open/broadcast/sendPacket` SATCOM remains source-compatible.

---

### Task 10: Persistence and save/reload verification

**Files:**
- Modify as needed: `SatelliteIntelligenceBase.java`
- Modify as needed: `IntelResultCodec.java`
- Review: `SatelliteSavedData.java`

- [ ] **Step 1: Launch each satellite with a unique Satellite ID and run a completed scan**
- [ ] **Step 2: Save/stop/restart the world**
- [ ] **Step 3: Run `/ntmsatellites list` and confirm IDs deserialize to the correct classes**
- [ ] **Step 4: Query `intelSummary()` and verify the completed result persists**
- [ ] **Step 5: Start a scan, restart before completion, and verify the active job resets to idle while the previous completed result remains intact**
- [ ] **Step 6: Verify no result list exceeds hard caps after reload**
- [ ] **Step 7: Commit any fixes from reload testing**

**Acceptance:** completed intelligence survives `SatelliteSavedData` round trips; active scans never deserialize into corrupt half-job state.

---

### Task 11: Controlled gameplay verification scenarios

**Files:**
- No production file required unless fixes are discovered.
- Add/update developer documentation under `docs/superpowers/` if useful.

- [ ] **Step 1: Build a 64x64 controlled test site in already-loaded chunks**
  - surface building.
  - exposed radar/launcher/power HBM blocks.
  - underground tunnel.
  - reinforced bunker cavity.
  - missile-related machinery/signature.
  - red-concrete shell section.
  - deliberately thinner/weaker wall sector.
- [ ] **Step 2: Surface Recon test**
  - verify structure and exposed infrastructure appear.
  - verify no exact underground block data is returned.
- [ ] **Step 3: Subsurface Intel test**
  - verify tunnel/bunker/possible-silo classifications and confidence.
  - verify exact hidden registry IDs/resistances are absent from its API.
- [ ] **Step 4: Combined test**
  - verify surface + subsurface + correlations.
  - verify red concrete's actual effective resistance value is reported from runtime block data.
  - verify shell average/max/thickness/weak-point calculations react to the deliberately weak sector.
- [ ] **Step 5: Partial coverage test**
  - unload at least one target chunk without deleting it.
  - scan and verify coverage decreases.
  - confirm the scan does not load that chunk.
- [ ] **Step 6: Busy test**
  - request `scan` twice before completion.
  - second request must return `BUSY`.
- [ ] **Step 7: SATCOM regression**
  - keep an active Relay Satellite and two `ntm_satlink` stations.
  - receiver `open(4510)`.
  - sender `broadcast(4510, "SATCOM REGRESSION")`.
  - verify `satlink_message` still arrives.

**Acceptance:** all spec behaviors are demonstrated in-game, including runtime resistance capture and no chunkloading.

---

### Task 12: Full JDK 8 build, release artifact, and final review

**Files:**
- Review/update: `.github/workflows/satcom-ci.yml`
- Review all changed production files.

- [ ] **Step 1: Run full local/CI-equivalent build**

```bash
./gradlew build
```

Expected: exit 0 under JDK 8.

- [ ] **Step 2: Static review checklist**
  - Java 8 syntax only.
  - no accidental chunk-loading getters on unloaded target coordinates.
  - list/page hard bounds enforced on read and write.
  - no exact hidden structural data leaks from `SUBSURFACE_INTEL`.
  - Combined resistance is runtime-derived, not hardcoded.
  - all OpenComputers callbacks use supported signal/return primitive types.
  - existing `SatelliteRelay` and SATCOM callbacks untouched in semantics.
  - all new registry IDs unique and enum values appended.
- [ ] **Step 3: Ensure branch CI uploads `build/libs/*.jar` and publishes a prerelease asset as established for SATCOM builds**
- [ ] **Step 4: Push final commit and wait for CI completion**
- [ ] **Step 5: Inspect workflow result and release asset before claiming completion**

**Acceptance:** fresh JDK8 `./gradlew build` succeeds, the feature JAR is attached to the branch prerelease, and controlled in-game verification has no unresolved failures.
