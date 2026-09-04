# OpenComputers SATCOM Relay Design

## Goal
Add a dedicated HBM Satellite Link block that lets OpenComputers machines communicate at effectively unlimited range within the same Minecraft dimension while a matching HBM relay satellite is active in orbit.

## Scope
The first release is single-dimension only. It extends HBM's existing orbital `SatelliteRelay` infrastructure and adds a new OpenComputers-facing tile entity/component. Normal OpenComputers modem behavior remains unchanged.

## Hardware model
Add a dedicated HBM block named Satellite Link. Its tile entity exposes an OpenComputers component named:

`ntm_satlink`

The block is attached to an OpenComputers network like any other component. Every Satellite Link is configured with one HBM satellite frequency. The frequency selects the orbital relay network.

## Satellite dependency
A Satellite Link is considered online only when `SatelliteSavedData` for its current world contains a satellite at the configured frequency and that satellite is a `SatelliteRelay` / `DIMENSIONAL_RELAY`.

If no matching relay satellite exists, calls that require SATCOM delivery fail cleanly and no remote packet is delivered.

## Dimension behavior
SATCOM delivery is restricted to Satellite Link endpoints in the same Minecraft dimension as the sender. There is no Nether/End/modded-dimension bridging in this version.

## Network isolation
Two routing layers are intentionally separate:

- Satellite frequency selects the orbital SATCOM network.
- OpenComputers port selects the logical application service on that network.

This allows STRATCOM to continue using ports 4510 and 4511 while multiple independent satellite frequencies coexist.

## Modem-like API
The component provides modem-style operations without registering itself as a normal `modem` component:

- `getFrequency()`
- `setFrequency(frequency)`
- `getSatelliteStatus()`
- `open(port)`
- `close(port)`
- `closeAll()`
- `isOpen(port)`
- `send(address, port, ...)`
- `broadcast(port, ...)`

Ports use the OpenComputers modem range, 1 through 65535.

Each Satellite Link has a stable component/network address supplied by OpenComputers. `send` targets one Satellite Link address on the same dimension and satellite frequency. `broadcast` targets all eligible Satellite Links on that network except the sender.

## Receive semantics
A packet is delivered only when the destination Satellite Link has the supplied port open and its matching relay satellite is active.

Incoming SATCOM packets raise an OpenComputers signal named:

`satlink_message`

Signal payload:

1. receiving Satellite Link address
2. sending Satellite Link address
3. port
4. message values

The event is deliberately distinct from normal `modem_message` so scripts can explicitly distinguish orbital SATCOM traffic from local wireless traffic.

## Payload limits
SATCOM follows OpenComputers-compatible argument types and should reject unsupported values. The implementation should impose a conservative packet-size/value-count limit rather than allowing unbounded serialized data.

## Persistence
Satellite frequency and any operator-facing block configuration are persisted in tile NBT. Open ports are runtime state and reset after reload/restart, matching normal modem-style expectations.

## Endpoint discovery and lifecycle
The server maintains a lightweight registry of loaded Satellite Link endpoints keyed by dimension and satellite frequency. Tile entities register when loaded/validated and unregister when invalidated/chunk-unloaded.

The registry must not force chunks to remain loaded. SATCOM can only reach endpoints whose chunks are currently loaded, consistent with normal tile-entity operation and avoiding hidden chunkloading costs.

## OpenComputers compatibility
The tile entity follows HBM's existing `CompatHandler.OCComponent` / `SimpleComponent` integration pattern. HBM must continue to load without OpenComputers installed; all OC-specific linkage remains optional under the mod's existing compatibility mechanism.

## STRATCOM integration
No STRATCOM Lua changes are part of the HBM-side first implementation. Once the component is verified in game, the separate OpenComputer-Scripts repository can update its bootstrap transport abstraction to support either a normal modem or `ntm_satlink` while preserving the existing STRATCOM protocol.

## Security and failure behavior
- Possessing/tuning a Satellite Link to a frequency is sufficient to join that orbital network in this first version.
- No encryption or authentication is added at the HBM transport layer; STRATCOM continues to own application-level trust.
- Missing satellite, wrong frequency, closed destination port, wrong dimension, unloaded destination, or unknown address causes non-delivery without crashing.
- Normal OC wireless modem range and packets are untouched.

## Non-goals
- Cross-dimension SATCOM.
- Automatic chunkloading.
- Replacing or extending normal OC wireless modem range globally.
- Encryption/key-management UI.
- Satellite warfare or relay destruction mechanics beyond whatever HBM already provides.
