# Combined satellite target detection

The **Combined Intelligence Satellite** adds a dedicated target pass after its terrain and structural scans. It inspects loaded chunks across the same 64 × 64 target area, one chunk per tick. It checks equipment at every height, independently of the terrain scan's alternating height samples and cell limit.

It reports:

- Standard, large, and rusted launchpads; custom missile launch tables and compact launchers.
- Small and large silo hatches, radar installations, and missile assembly machines.
- Missiles loaded in launch equipment, including the rusted launchpad's built-in missile.
- Complete missile items stored in block inventories.
- Loaded missile entities in flight, including custom missiles and interceptors, at their scan-time positions.

Multiblock proxy ports are skipped so they do not report the same inventory repeatedly. Explicit targets take precedence over general terrain findings when the 128-finding result limit is reached. No chunks are force-loaded. A launcher whose core is outside the target area is outside this scan; an empty shaft without launch hardware still relies on the existing structural inference.

Flying missiles are counted once per scan by entity UUID, even if they move between scanned chunks. Flying missiles and stored missile items do not count as installed launch infrastructure when inferring a possible silo.

Surface Recon and Subsurface Intel do not run the target pass. They report general machinery, communications, and structures rather than identifying missiles, launch equipment, silo hatches, or radar hardware.

## Reading the results

Install the updated mod on the client and server, tune to a **COMBINED_INTEL** satellite, and start a **new scan**. Previous scans are snapshots and do not gain new detections retroactively. Flying missile contacts are snapshots too; this does not turn the satellite into a continuous radar feed.

The existing OpenComputers `intelFindingCount()` and `intelGetFinding(index)` calls include the new results. Existing return positions 1–14 are retained. The last three return values are appended:

| Position | Field | Meaning |
| --- | --- | --- |
| 15 | `targetType` | `LAUNCHPAD`, `LAUNCH_TABLE`, `COMPACT_LAUNCHER`, `SILO_HATCH`, `RADAR`, `MISSILE_ASSEMBLY`, `LOADED_MISSILE`, `STORED_MISSILE`, or `FLYING_MISSILE`; empty for general findings |
| 16 | `targetId` | Block/item registry identifier, or the flying entity's registered name |
| 17 | `targetCount` | Number of targets represented by this finding |

Missile findings use classification `MISSILE`; hatch and radar findings use `SILO_HATCH` and `RADAR`. Launch equipment uses `LAUNCH_INFRASTRUCTURE`. The normal scan summary includes explicit missile, launcher, silo-hatch, and radar counts for Combined scans.

To print explicit contacts from an OpenComputers Lua prompt or program:

```lua
local sat = require("component").ntm_satlink
for i = 1, sat.intelFindingCount() do
  local f = {sat.intelGetFinding(i)}
  if f[1] and f[15] and f[15] ~= "" then
    print(f[15], f[16], f[17], f[4], f[5], f[6])
  end
end
```

The coordinates are the launcher's/inventory's core block for loaded or stored missiles and the observed block position for flying missiles. Target details are saved with the satellite result and survive world reloads.
