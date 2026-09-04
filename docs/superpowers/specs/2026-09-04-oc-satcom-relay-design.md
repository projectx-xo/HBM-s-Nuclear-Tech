# OpenComputers SATCOM Relay Design

## Goal
Extend HBM's existing Satellite Ground Station so OpenComputers machines can communicate at effectively unlimited range within the same Minecraft dimension while a matching HBM relay satellite is active in orbit.

## Scope
The first release is single-dimension only. It extends HBM's existing `machine_satlink` / `TileEntityMachineSatLink`, which already exposes the OpenComputers component name `ntm_satlink`, and HBM's existing orbital `SatelliteRelay`. Normal OpenComputers modem behavior remains unchanged.

## Hardware model
Reuse the existing HBM Satellite Ground Station (`machine_satlink`) as the dedicated SATCOM endpoint. No second satellite-link block is added.

The tile already exposes:

`ntm_satlink`

Every ground station is configured with one HBM satellite frequency. That frequency selects the orbital relay network.

## Satellite dependency
SATCOM packet delivery is online only when `SatelliteSavedData` for the station's current world contains a satellite at the configured frequency and that satellite is a `SatelliteRelay` / `DIMENSIONAL_RELAY`.

The existing ground-station `connected` state and legacy satellite command API remain compatible; the new packet transport performs its stricter relay-satellite check independently.

If no matching relay satellite exists, calls that require SATCOM delivery fail cleanly and no remote packet is delivered.

## Dimension behavior
SATCOM delivery is restricted to Satellite Ground Stations in the same Minecraft dimension as the sender. There is no Nether/End/modded-dimension bridging in this version.

## Network isolation
Two routing layers are intentionally separate:

- Satellite frequency selects the orbital SATCOM network.
- OpenComputers port selects the logical application service on that network.

This allows STRATCOM to continue using ports 4510 and 4511 while multiple independent satellite frequencies coexist.

## Modem-like API
Preserve the existing `ntm_satlink` methods (`isConnected`, `setFreq`, `getFreq`, `getType`, legacy satellite `send`, and `read`) for compatibility, and add packet-network methods with unambiguous names:

- `getSatelliteStatus()`
- `getAddress()`
- `open(port)`
- `close(port)`
- `closeAll()`
- `isOpen(port)`
- `sendPacket(address, port, ...)`
- `broadcast(port, ...)`

`sendPacket` is intentionally distinct from the existing `send(command)` callback.

Ports use the OpenComputers modem range, 1 through 65535.

## Endpoint addressing
A port subscription belongs to the OpenComputers `Context` that opened it. The SATCOM endpoint address is that attached computer context's OC node address (`Context.node().address()`). This gives a stable address for targeted delivery without replacing HBM's existing OC compatibility wrapper.

`sendPacket(address, port, ...)` targets the matching subscribed computer address on another loaded Satellite Ground Station in the same dimension and frequency. `broadcast(port, ...)` targets every eligible subscribed context on that network except the sender context.

## Receive semantics
A packet is delivered only when:

1. Sender has a matching active `SatelliteRelay`.
2. Destination is a loaded Satellite Ground Station in the same dimension.
3. Destination is tuned to the same satellite frequency.
4. Destination also has the matching relay available.
5. The destination computer context has the supplied port open.

Incoming SATCOM packets raise an OpenComputers signal named:

`satlink_message`

Signal payload:

1. receiving computer/SATCOM address
2. sending computer/SATCOM address
3. port
4. message values

The event is deliberately distinct from normal `modem_message` so scripts can explicitly distinguish orbital SATCOM traffic from local wireless traffic.

## Payload limits
SATCOM accepts the value types OpenComputers `Context.signal` supports. The first implementation limits messages to 16 payload values and rejects unsupported aggregate/table values rather than allowing arbitrary object graphs.

## Persistence
Satellite frequency is already persisted by `TileEntityMachineSatLink`. Open ports and live computer-context subscriptions are runtime state and reset when the ground station unloads/reloads or the computer reconnects.

## Endpoint discovery and lifecycle
Maintain a lightweight server-side registry of loaded `TileEntityMachineSatLink` instances. Stations register when validated/loaded and unregister when invalidated or chunk-unloaded.

The registry must not force chunks to remain loaded. SATCOM can only reach endpoints whose chunks are currently loaded.

Stale OpenComputers contexts are pruned when they are no longer running/paused or when signal delivery fails repeatedly; station unload clears all subscriptions.

## OpenComputers compatibility
The tile continues to follow HBM's existing `CompatHandler.OCComponent` / `SimpleComponent` integration pattern. HBM must continue to load without OpenComputers installed; all OC-specific linkage remains optional under the existing compatibility mechanism.

## STRATCOM integration
No STRATCOM Lua changes are part of the HBM-side first implementation. Once the component is verified in game, the separate OpenComputer-Scripts repository can update its bootstrap transport abstraction to use `ntm_satlink.broadcast`/`satlink_message` while preserving the existing STRATCOM envelope and ports.

## Security and failure behavior
- Possessing/tuning a Satellite Ground Station to a relay frequency is sufficient to join that orbital network in this first version.
- No encryption or authentication is added at the HBM transport layer; STRATCOM continues to own application-level trust.
- Missing relay satellite, wrong frequency, closed destination port, wrong dimension, unloaded destination, unknown address, or unavailable computer context causes non-delivery without crashing.
- Normal OC wireless modem range and packets are untouched.

## Non-goals
- Cross-dimension SATCOM.
- Automatic chunkloading.
- Replacing or extending normal OC wireless modem range globally.
- Encryption/key-management UI.
- Satellite warfare or relay destruction mechanics beyond whatever HBM already provides.
