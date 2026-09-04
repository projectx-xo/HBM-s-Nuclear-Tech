# OpenComputers SATCOM Relay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the existing HBM Satellite Ground Station (`machine_satlink`) into a same-dimension, unlimited-range OpenComputers packet relay backed by an active HBM `SatelliteRelay`.

**Architecture:** Reuse `TileEntityMachineSatLink` and its existing `ntm_satlink` component. Add a loaded-station registry and per-OC-context port subscriptions, validate that the tuned satellite is specifically a `SatelliteRelay`, and deliver packets via `Context.signal("satlink_message", ...)`. Keep the legacy satellite command API intact and add new packet methods under distinct names.

**Tech Stack:** Java 8, Minecraft Forge 1.7.10, HBM NTM satellite saved-data system, OpenComputers 1.7.10/1.8.9 API.

**Spec:** `docs/superpowers/specs/2026-09-04-oc-satcom-relay-design.md`

## Files

- Modify `src/main/java/com/hbm/tileentity/machine/TileEntityMachineSatLink.java`
  - relay-satellite availability check
  - loaded station registry/lifecycle
  - context/port subscriptions
  - targeted and broadcast packet delivery
  - new OC callbacks/method dispatch
- Optional test helper under `src/test/java/...` only if the existing project test harness supports plain unit tests without Forge bootstrapping.
- No new block/model/recipe is required because `machine_satlink` already exists.

## Task 1: Preserve and characterize the existing ground-station API

- [ ] Read `TileEntityMachineSatLink`, `SatelliteRelay`, `SatelliteSavedData`, and `CompatHandler.OCComponent` and record compatibility constraints.
- [ ] Confirm existing component name is `ntm_satlink` and legacy `send(command)` cannot be renamed.
- [ ] Confirm `Context.signal` and `Context.node().address()` are available in the configured OC API.
- [ ] Establish packet limits: port 1..65535, maximum 16 payload values, scalar/byte-array OC-safe values only.

**Acceptance:** Existing methods remain source-compatible and the new packet API can coexist without renaming `send`.

## Task 2: Add SATCOM station registry and relay availability

- [ ] Add a static loaded-station registry owned by `TileEntityMachineSatLink`.
- [ ] Register server-side instances during tile validation/load.
- [ ] Unregister and clear subscriptions on `invalidate()` and `onChunkUnload()`.
- [ ] Add a helper that resolves `SatelliteSavedData.getSatFromFreq(freq)` and returns online only for `SatelliteRelay`.
- [ ] Ensure registry operations never load/force chunks.

**Acceptance:** Only loaded stations participate and SATCOM is offline when the tuned satellite is absent or is not a relay satellite.

## Task 3: Implement modem-style port subscriptions

- [ ] Add runtime subscription storage mapping OC context/computer addresses to open port sets.
- [ ] Implement `getAddress`, `getSatelliteStatus`, `open`, `close`, `closeAll`, and `isOpen` callbacks.
- [ ] Validate port range and prune unusable contexts defensively.
- [ ] Preserve `setFreq`, `getFreq`, `isConnected`, `getType`, legacy `send`, and `read` unchanged in behavior.

**Acceptance:** Two attached OC contexts can independently open/close ports and report their own SATCOM address.

## Task 4: Implement targeted and broadcast SATCOM delivery

- [ ] Add payload extraction/validation with a maximum of 16 values.
- [ ] Implement `sendPacket(address, port, ...)`.
- [ ] Implement `broadcast(port, ...)`.
- [ ] Filter destinations by same world dimension, same satellite frequency, loaded station, active `SatelliteRelay`, and open destination port.
- [ ] Deliver `satlink_message(receiverAddress, senderAddress, port, ...)` using `Context.signal`.
- [ ] Do not echo broadcast back to the sending context.
- [ ] Return useful delivery status/count without throwing for normal non-delivery cases.

**Acceptance:** Same-frequency stations in loaded chunks communicate regardless of coordinate distance; wrong frequency/dimension/closed port/missing relay prevents delivery.

## Task 5: Expose the new methods through HBM's managed-peripheral compatibility layer

- [ ] Add the new method names to `methods()`.
- [ ] Add corresponding `invoke()` dispatch cases.
- [ ] Keep all OC references guarded with the project's existing optional annotations/patterns.
- [ ] Verify HBM can still class-load without OpenComputers installed by following the same pattern already used in this tile.

**Acceptance:** Lua can resolve all new callbacks through `component.ntm_satlink` while legacy callbacks remain available.

## Task 6: Verification

- [ ] Perform a static review for duplicate callback names, invalid Java 8 syntax, lifecycle leaks, concurrent modification risks, and unsupported OC signal argument types.
- [ ] Run `./gradlew compileJava` or `./gradlew build` with JDK 8 if the environment can resolve the legacy ForgeGradle dependencies.
- [ ] If local build is unavailable, do not claim compilation; record that in-game/build-machine verification remains required.
- [ ] Prepare manual OC test commands for two ground stations on the same relay frequency:
  - station B `open(4510)`
  - station A `broadcast(4510, "PING")`
  - station B receives `satlink_message`
  - repeat with no relay, wrong frequency, and closed port to verify rejection.

**Acceptance:** Build succeeds where tooling is available, or static verification is complete with explicit remaining build validation steps.
