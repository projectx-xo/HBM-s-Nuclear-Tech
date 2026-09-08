# tjHBM-NTM 1.13

Ordinary and custom launch pads expose getInventoryMapping(controllerAddress) to OpenComputers. It returns the uniquely adjacent inventory side for a reachable stationary-adapter inventory controller, or nil with a reason. Inventory-capable multiblock proxies resolve to their pad core. The lookup checks loaded blocks only and does not transfer items or change controller state.

Use STRATCOM 3.7.0 and strike runtime 3.3.0 for automatic pairing of missing mappings. Existing mappings remain unchanged. Robots, drones and transposer logistics are not automatically mapped by this callback. The integration uses the installed OpenComputers adapter environment's host accessor and fails closed for unsupported implementations.

Java 8 Gradle build and all 85 JUnit tests passed. Actual adapter topology and dedicated-server/optional-mod checks remain unverified in-game.
