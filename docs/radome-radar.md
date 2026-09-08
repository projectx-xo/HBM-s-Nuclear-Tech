# Advanced Radome Radar

A higher-tier radar with a stationary smooth-shaded white dome with faint panel joints, six-block equipment tower, perimeter catwalk, ladder and rooftop beacon. The dome is 6.9 blocks across, above a 4.34-block-wide tower. Place it in a clear 7 × 7 footprint with thirteen blocks of height (the model stands about 12.3 blocks tall, versus 10 for the Large Radar). Connect power/OC at the four base-edge ports, or use an OC adapter at the core. The normal radar minimum installation altitude still applies.

Default range is 4,000 blocks in each horizontal axis, compared with 3,000 for the Large Radar. It uses the same height buffer, power requirements, scan toggles, GUI, radar links, redstone output and loaded-entity scanning rules as the existing radar. Range is configurable through the `radar_radome` machine entry's `I:radomeRange`. Increasing range does not load or generate distant chunks.

Only this tier detects Stealth Missiles. Standard and Large Radars remain unable to detect them. The legacy radar converter no longer bypasses a modern entity's visibility restriction. The missile scan toggle still applies.

The radome exposes `ntm_radome`, with the same radar callbacks as `ntm_radar`, including the entity-handoff callbacks. STRATCOM 3.5.1 with radar runtime 1.2.1 and defense runtime 2.3.0 can pass a detected stealth missile's identity to an ABM. The ABM's own autonomous seeker still cannot discover stealth missiles; the radome provides the initial lock.

Find **Advanced Radome Radar** in the missile creative tab. Its assembly-machine recipe upgrades a Large Radar using welded steel plates, magnetrons, advanced circuits and rubber. Existing projector and entity-handoff changes are included in the accompanying production JAR. Update server and client JARs together.

Verification: regression coverage exercises all radar converters to prove stealth invisibility on old tiers, visibility on the radome, and scan-toggle behavior. Gameplay checks should cover placement/removal, power and OC ports, radar controls and entity handoff in a loaded world.

After replacing the mod JAR, reboot the radar node to refresh OC component discovery. Deploy radar runtime 1.2.1 before using the renamed radome. Standard and large radars remain `ntm_radar`; older radome builds using that name are still supported by STRATCOM.

## Nuclear payload identification

The radome identifies nuclear and thermonuclear missile payloads. Its display uses `Missile (Nuclear Payload)` or `Missile (Thermonuclear Payload)` while retaining the original blip/tier and scan filters. Standard and Large Radars do not gain this capability.

Fixed nuclear, micro-nuclear and Doomsday missiles report `NUCLEAR`; the thermonuclear missile reports `THERMONUCLEAR`. Custom missiles read their actual warhead attributes: `NUCLEAR`, `TX`, and `BUSTER_THERMONUCLEAR` are recognized. Conventional, malformed, unknown and other exotic warheads are not assumed nuclear. Classification does not depend on tier, missile name, or propellant.

`getTrackedEntityAtIndex` appends a tenth result, `NUCLEAR`, `THERMONUCLEAR` or `UNKNOWN`, preserving the first nine positions. Updated STRATCOM radar software carries `payloadClass` in tracks and adds the nuclear classification to type labels used in alerts. An ordinary radar observation does not erase an existing positive identification of the same tracked entity. Old runtimes can ignore the appended value. Install matching server/client JARs and update the radar runtime and CENTRAL to expose it throughout STRATCOM.

In-game checks still required: compare identical nuclear targets on all radar tiers; check custom nuclear and thermonuclear bunker-buster payloads against conventional custom missiles; verify GUI tooltips and OC alerts on a client and dedicated server, including operation without OpenComputers.
