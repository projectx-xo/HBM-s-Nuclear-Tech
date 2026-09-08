# OpenComputers ABM target handoff

`ntm_radar.getTrackedEntityAtIndex(index)` takes a one-based index and returns:
`isPlayer, x, y, z, type, name, entityId, entityUuid, dimension`.
It reads one current radar entry and validates its captured identity against a loaded, living entity. Invalid or lost entries return `nil, reason`. Existing `getEntityAtIndex` return positions are unchanged. The new callback runs on the server thread.

`ntm_launch_pad.getTargetingInfo()` returns `true, dimension`.
`ntm_launch_pad.launchTracked(entityId, entityUuid, dimension)` requires an ABM payload and a matching loaded living missile in the pad's dimension. It uses the native `launchToEntity` path, assigning the target before spawning. It returns `success, optionalReason`; failure never falls back to coordinate launch. It does not load chunks or search the world for a replacement target. UUID checks prevent reused numeric entity IDs from changing the target.

Use direct `component.invoke` calls for these callbacks because existing OpenComputers proxy method lists can omit newly introduced methods. The matching STRATCOM release is 3.5.0, radar runtime 1.2.0 and defense runtime 2.3.0. Old callbacks and native radar-linker behavior are unchanged. Matching mod builds are required on server and clients.

The 1,000-block radius applies to autonomous seeker acquisition, not a target already handed to the ABM. The existing 40-tick activation delay, flight logic and loss/reacquisition behavior remain. A launch acknowledgement is not an interception confirmation.

## Interceptor outcome feedback

`launchTracked(id, uuid, dimension)` preserves its success boolean and now returns a reason followed by the launched interceptor UUID. `getInterceptorStatus(interceptorUuid, targetId, targetUuid, dimension)` reports `IN_FLIGHT`, `MISS`, `TARGET_UNAVAILABLE` or `UNKNOWN`. `MISS` requires the exact recorded interceptor to be dead and the original target to remain loaded, alive and identity-matched. A missing target never proves interception. These queries do not load chunks.

Each pad retains one interceptor reference and original target UUID in memory. A restart loses this evidence; another tracked launch replaces it. STRATCOM polls at most once per second, confirms survival across multiple reports and fresh radar samples, then re-evaluates a confirmed miss for automatic defense. It still requires an incoming hostile target, auto-defense enabled, and a ready, fueled ABM pad. Unknown telemetry does not authorize another shot. No pending launch is persisted or replayed.
