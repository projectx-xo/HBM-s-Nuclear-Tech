# tjHBM-NTM 1.11

Companion hardware release for STRATCOM 3.6.0:

- Dedicated Communications Satellite and OpenComputers relay transport.
- Tiered survival and infinite creative propellant storage, with launch-pad loading and fuel recovery callbacks.
- Missile and artillery chunk handoff/ticking fixes.
- Tracked ABM launch outcome reporting for confirmed miss re-engagement.
- Advanced Radome hardware and nuclear/thermonuclear payload classification.
- Intelligence projection rendering, machine layers, and multiblock integration improvements.

The experimental missile assembly tower has been removed and is not included. A world saved with that experimental block may report a missing tower registry entry.

Install the matching JAR on server and clients and restart Minecraft. Update STRATCOM using the 3.6.0 reinstall procedure and deploy its updated role runtimes.

Verification: Java 8 Gradle build and 84 JUnit tests passed locally. Live client/dedicated-server, optional-mod, rendering and gameplay smoke checks remain unverified for this combined release. Refer to the feature documents for the corresponding checks.
