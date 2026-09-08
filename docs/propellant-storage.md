# Propellant storage

The machine creative tab contains three craftable survival tanks and five fixed-fluid creative tanks. All use a horizontal metal vessel with support feet; side bars identify the tier. Each occupies one block and faces the player when placed.

| Survival tank | Capacity |
| --- | ---: |
| Propellant Storage I | 64,000 mB (64 buckets) |
| Propellant Storage II | 256,000 mB (256 buckets) |
| Propellant Storage III | 1,024,000 mB (1,024 buckets) |

Survival tanks start empty, selected for hydrogen peroxide. They also accept ethanol, kerosene, oxygen, and reformed kerosene: the five fluids used by the current missile launch-pad fuel combinations. A tank holds one fluid at a time. Empty it before changing the selected fluid with a fluid identifier (sneak-right-click or the GUI's identifier slot). Unsupported fluids and attempts to change a nonempty tank are rejected.

Right-click to open the existing barrel GUI. Its modes are input (0), buffer (1), output (2), and disabled (3). Use input at a receiving tank and output at a supplying tank, or buffer for bidirectional storage. The container slots fill and empty HBM fluid containers. Breaking a survival tank preserves its fluid, capacity tier, and transfer mode in the dropped item. Inventory contents drop separately. A comparator reports fill level.

Assembly-machine recipes are available in NEI. For tier number N (1–3), each recipe uses 16×N steel plates, 4×N steel pipes, and 4/8/16 technetium steel barrels respectively. They do not consume an earlier tank tier, so upgrading does not silently discard an earlier tank's contents. Creative variants have no survival recipe.

## Creative supplies

Search for **Creative Hydrogen Peroxide Storage** in the creative inventory. Separate entries also exist for ethanol, kerosene, oxygen, and reformed kerosene. Place one and connect it: creative tanks stay in output mode and continuously supply their fixed fluid without depletion. They can fill HBM containers, and they reject incoming fluid and fluid-type changes. The GUI displays a full 1,024,000 mB working tank; this is not a finite total supply. HBM transfer is capped at 16,000 mB per connection attempt.

## Pipes and other mods

HBM fluid pipes connect on any face. Select the matching fluid on the pipes and receiving equipment. The new tanks also implement Forge's `IFluidHandler` for conduit extraction and insertion, including nonmutating simulated transfers.

The Forge bridge uses these dedicated fluid registry names:

- `hbm_propellant_peroxide`
- `hbm_propellant_ethanol`
- `hbm_propellant_kerosene`
- `hbm_propellant_oxygen`
- `hbm_propellant_kerosene_reform`

An empty survival tank can select one of these automatically on an actual Forge fill. Other mods' fluids with similar names are not assumed interchangeable. This bridge applies to these storage tanks; existing HBM launch pads still use the HBM fluid network. Route Ender IO conduits into a receiving propellant tank, then use HBM pipes for the final connection to a launch pad. An AE2 item-storage installation does not by itself add fluid ME storage.

## Verification

Run `./gradlew test --tests '*PropellantTankTest'` and `./gradlew build` with Java 8. Regression coverage includes finite storage/extraction, unlimited creative supply, type and pressure rejection, transfer modes, persistence, and protection against item NBT overriding capacity or granting creative behavior.

For in-game verification, place all tiers and creative variants, check their item names and models, fill and drain through HBM pipes, test Forge simulation and actual transfers, fill containers, and break/re-place a partially filled tank. Save and reload the world. Repeat with and without OpenComputers and on a dedicated server. For Ender IO, configure extraction at the source and insertion at the destination, then compare both tank levels. Check existing worlds separately before treating a staged modpack as ready for normal play.

## Silo logistics integration

Propellant storage also accepts hydrogen, xenon and balefire, including creative variants, for custom missiles. Existing fluid IDs and the first five creative variants are preserved. Survival buffer mode supports both supplying and reclaiming fuel. Creative tanks supply infinitely and reject returns; use a finite return tank.

Launchpads expose gated Forge fluid access for an adjacent OC transposer. The `setServiceMode`, `getLogisticsInfo`, `verifyTransposer`, `verifyMEInterface` and `launchPrepared` callbacks support STRATCOM preparation. Custom pads also expose `recoverSolidFuel`. Existing callbacks retain their return layouts. Modes are `off` (legacy operation), `hold` (launch inhibited, transfers blocked), `drain` (fluid recovery and empty-pad missile extraction), and `fill` (compatible missile insertion and only the required liquid quantities). All saved active modes reload as `hold`. Ordinary redstone and launch callbacks cannot fire a serviced pad; `launchPrepared(x,z)` briefly permits an explicit software launch and restores `hold`.

Missile changes retain incompatible stored liquids until recovered instead of silently clearing them. Readiness verifies fluid types as well as quantities. STRATCOM setup is documented in the sibling repository at `stratcom/docs/silo-logistics.md`.
