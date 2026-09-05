# Textured Projection Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement inline. Steps use checkbox syntax.

**Goal:** Show a full-color miniature of captured Minecraft blocks, with the existing exterior, interior and cutaway controls.

**Architecture:** Extend the combined scan with a registry-name palette and packed block/metadata values. Render the immutable snapshot through Minecraft's block renderer and a snapshot-only IBlockAccess. Cache compiled geometry in bounded batches. Transfer compressed snapshots in 64 KiB pieces through the existing table buffer/control packets.

**Tech Stack:** Forge 1.7.10, Java 8, Minecraft RenderBlocks, existing HBM packet interfaces. STRATCOM retains its snapshot-reference API.

**Spec:** The user requested visible blocks instead of blue outlines. Use normal block textures, metadata/orientation, opaque surfaces and transparent glass. Preserve terrain filtering and coordinate-anchored selectable finding markers. Custom animated tile/entity meshes are outside this block-surface change.

## Constraints
- Work in the existing `/usr/local/development/hbmNTM/mod` and `stratcom` feature branches.
- Only combined satellites capture/expose the model. Older snapshots require a fresh scan.
- Never read the live remote world from the client renderer or force-load scan chunks.
- Keep scan work bounded and do not rebuild geometry for rotation, scale or finding selection.
- Native block render types use snapshot neighbors; unsupported custom render types use their textured captured bounds.

## Tasks

### 1. Capture and persist block identity
Files: `IntelProjection.java`, `IntelProjectionScanner.java`, `TileEntityMachineSatLink.java`, `IntelProjectionTest.java`.
- [x] Add regression tests for distinct block names, metadata, palette reuse, persistence, old snapshots and invalid state indices.
- [x] Capture packed block IDs/metadata alongside masks, remap IDs to a saved name palette, and reject old textureless references with a rescan message.
- [x] Run projection tests on Java 8.

### 2. Transfer larger snapshots
Files: new `IntelProjectionTransfer.java`, `TileEntityIntelProjector.java`, `IntelProjectorSyncTest.java`.
- [x] Test multi-piece payloads above 2 MiB, bounded piece size, duplicate/late pieces, scene changes and malformed lengths.
- [x] Request the next missing offset and reply with a snapshot ID plus one bounded byte range. Keep view-control packets small and save the full snapshot in tile NBT.
- [x] Run sync tests on Java 8.

### 3. Render textured blocks
Files: new `IntelProjectionBlockAccess.java`, new `IntelProjectionBlockRenderer.java`, `RenderIntelProjector.java`, new renderer/access tests.
- [x] Verify snapshot-only block/metadata lookup, terrain/floor/side clipping and exposed surfaces.
- [x] Bake opaque and transparent native block geometry in bounded display-list batches; preserve render state and invalidate on texture reload.
- [x] Replace the blue mesh with the textured miniature while retaining the table base, markers, fitting and controls.
- [x] Compile and run all Java tests; inspect the local client with a multicolor block/slab/glass fixture.

### 4. Publish
- [x] Update mod version and setup documentation, including fresh-scan and custom-model limitations.
- [x] Review, build and push the existing feature branch; verify the published JAR and update STRATCOM documentation without changing its protocol.

## Validation evidence
- Java 8: 51 tests passed, zero failures/errors. The new Forge padded-buffer regression failed before the receiver fix and passed afterward.
- Peer review resolved snapshot publication/handoff races and camera-dependent glass ordering.
- Local client captured block textures, concrete variants, slab/stair geometry and interiors; no GL errors at fixture capture. User F2 screenshots at 16:50 show the resulting miniature. Live OpenComputers satellite-to-table operation remains outside this local fixture test.

## Published release
- Source: `d4b462f884eecc65cd71cba27d679221babff185`. GitHub Actions run `33991363420` built and published v1.10 successfully.
- Downloaded release JAR passed ZIP integrity/class checks and matches the GitHub asset SHA-256: `ce3c0626b4b6bf4deeea1c089a07f355736f71746035b278184020d2fff01145`. The local smoke-test mod is not in the release.
- STRATCOM 3.4.0 remains compatible; its README now documents v1.10 and a fresh combined scan.
