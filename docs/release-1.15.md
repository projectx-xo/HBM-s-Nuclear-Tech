# tjHBM-NTM 1.15

**Update correction:** node bootstrap code updates through STRATCOM application bundles; it does not require reinstalling. STRATCOM 3.10.0 adds the CENTRAL `upgrade` command. See [the current update guide](https://github.com/projectx-xo/OpenComputer-Scripts/blob/codex/stratcom-reliability/docs/easy-updates.md).

Adds optional HBM BaseCenter asset identification for launch pads, radars and Satellite Ground Stations. `/hbmasset <x> <y> <z> <label>` registers nearby equipment using the player's BaseCenter membership. Registration persists with the tile entity and exposes a read-only `getTeamIdentity()` OpenComputers callback returning team, label, X, Y, Z, dimension, source and owner name. Unsupported/missing team integration reports UNKNOWN.

Use STRATCOM 3.9.0 and bootstrap 3.3.0 for friendly asset labels and advisory launch warnings. All bases remain targetable and destructible; no firing veto or damage immunity is added. See [setup and behavior](team-awareness.md).

Java 8 build and tests pass, including saved registration. Live client/server, BaseCenter/UniMixins startup and optional OpenComputers checks remain unverified.
