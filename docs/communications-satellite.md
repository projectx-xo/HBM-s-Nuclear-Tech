# Communications Satellite

The Communications Satellite is the orbital relay used by OpenComputers SATCOM. It is separate from the older RoR Relay Satellite and from the three intelligence satellites.

Build the Communications Satellite in an Assembly Machine, set its frequency with a satellite ID chip, and launch it normally. At CENTRAL and every remote STRATCOM site, place a Satellite Ground Station with a clear view of the sky, tune it to the same frequency, and connect its `ntm_satlink` component to the local OpenComputers network.

The satellite carries STRATCOM control packets between loaded ground stations in the same dimension. It does not move missiles, items, or fuel. Missile items still travel through the ME network and propellant remains in the silo field's local tanks.

An INTEL node still needs a ground station tuned to its Combined Intelligence Satellite for scanning. To connect that node to STRATCOM over SATCOM, add a second ground station tuned to the Communications Satellite. A modem can provide the STRATCOM transport instead.

The old RoR Relay Satellite retains its original redstone-over-radio behavior but does not provide OpenComputers SATCOM transport. A ground station reports `SATCOM_RELAY` when tuned to the new satellite.

Both endpoint ground stations and their OpenComputers machines must remain loaded. A missing satellite, mismatched frequency, obstructed station, unloaded endpoint, or closed SATCOM port prevents delivery without loading chunks automatically.
