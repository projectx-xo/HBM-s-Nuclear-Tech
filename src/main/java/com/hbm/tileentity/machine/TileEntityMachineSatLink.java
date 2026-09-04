package com.hbm.tileentity.machine;

import com.hbm.handler.CompatHandler;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteBase;
import com.hbm.saveddata.satellites.SatelliteCombinedIntel;
import com.hbm.saveddata.satellites.SatelliteIntelligenceBase;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.saveddata.satellites.SatelliteRayScan.RayEvent;
import com.hbm.saveddata.satellites.SatelliteRelay;
import com.hbm.saveddata.satellites.intel.IntelFinding;
import com.hbm.saveddata.satellites.intel.IntelScanResult;
import com.hbm.saveddata.satellites.intel.IntelScanMode;
import com.hbm.saveddata.satellites.intel.IntelStructuralCell;
import com.hbm.saveddata.satellites.intel.IntelStructuralSummary;
import com.hbm.saveddata.satellites.intel.IntelSurfaceCell;
import com.hbm.tileentity.TileEntityTickingBase;

import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.IRORValueProvider;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Optional.InterfaceList({@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")})
public class TileEntityMachineSatLink extends TileEntityTickingBase implements IRORValueProvider, IRORInteractive, SimpleComponent, CompatHandler.OCComponent {

	private static final int MIN_PORT = 1;
	private static final int MAX_PORT = 65535;
	private static final int MAX_PAYLOAD_VALUES = 16;
	private static final Set<TileEntityMachineSatLink> LOADED_STATIONS = ConcurrentHashMap.newKeySet();

	private final Map<String, Object> satcomContexts = new ConcurrentHashMap<String, Object>();
	private final Map<String, Set<Integer>> satcomPorts = new ConcurrentHashMap<String, Set<Integer>>();

	public boolean connected;
	public int freq;

	public float rot = INACTIVE_ROT;
	public float prevRot = INACTIVE_ROT;
	public float lift = INACTIVE_LIFT;
	public float prevLift = INACTIVE_LIFT;

	public static final float SPEED = 0.25F;
	public static final float ACTIVE_ROT = -15F;
	public static final float ACTIVE_LIFT = -45F;
	public static final float INACTIVE_ROT = 0F;
	public static final float INACTIVE_LIFT = -85F;

	public IChatComponent[] info = new IChatComponent[0];

	@Override
	public void validate() {
		super.validate();
		if(worldObj != null && !worldObj.isRemote) LOADED_STATIONS.add(this);
	}

	@Override
	public void invalidate() {
		unregisterSatcomStation();
		super.invalidate();
	}

	@Override
	public void onChunkUnload() {
		unregisterSatcomStation();
		super.onChunkUnload();
	}

	private void unregisterSatcomStation() {
		LOADED_STATIONS.remove(this);
		satcomContexts.clear();
		satcomPorts.clear();
	}

	@Override
	public void updateEntity() {

		if(!worldObj.isRemote) {
			LOADED_STATIONS.add(this);
			this.connected = false;

			if(worldObj.getHeightValue(xCoord, zCoord) <= yCoord) {

				SatelliteSavedData dat = SatelliteSavedData.getData(worldObj);
				this.connected = dat.isFreqTaken(freq);
			}

			this.updateInfo(connected);
			this.networkPackNT(150);

		} else {

			this.prevRot = this.rot;
			this.prevLift = this.lift;

			float targetR = this.connected ? ACTIVE_ROT : INACTIVE_ROT;
			float targetL = this.connected ? ACTIVE_LIFT : INACTIVE_LIFT;

			if(Math.abs(rot - targetR) <= SPEED) rot = targetR;
			else if(rot < targetR) rot += SPEED;
			else if(rot > targetR) rot -= SPEED;

			if(Math.abs(lift - targetL) <= SPEED) lift = targetL;
			else if(lift < targetL) lift += SPEED;
			else if(lift > targetL) lift -= SPEED;
		}
	}

	protected void updateInfo(boolean canConnect) {

		if(!canConnect) {
			if(this.info.length > 0) this.info = new IChatComponent[0];
			return;
		}

		SatelliteSavedData dat = SatelliteSavedData.getData(worldObj);
		SatelliteBase sat = dat.getSatFromFreq(freq);

		if(sat != null) {
			this.info = sat.getInfo(worldObj);
		}
	}

	private boolean hasActiveRelaySatellite() {
		if(worldObj == null || worldObj.isRemote || !connected) return false;
		SatelliteBase sat = SatelliteSavedData.getData(worldObj).getSatFromFreq(freq);
		return sat instanceof SatelliteRelay;
	}

	private SatelliteIntelligenceBase getIntelligenceSatellite() {
		if(worldObj == null || worldObj.isRemote || !connected) return null;
		SatelliteBase sat = SatelliteSavedData.getData(worldObj).getSatFromFreq(freq);
		return sat instanceof SatelliteIntelligenceBase ? (SatelliteIntelligenceBase) sat : null;
	}

	private boolean isSameSatcomNetwork(TileEntityMachineSatLink other) {
		if(other == null || other.worldObj == null || this.worldObj == null) return false;
		if(other.isInvalid() || other.worldObj.isRemote) return false;
		return other.worldObj.provider.dimensionId == this.worldObj.provider.dimensionId
				&& other.freq == this.freq
				&& other.hasActiveRelaySatellite();
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(connected);
		buf.writeInt(freq);

		buf.writeInt(info.length);

		for(int i = 0; i < info.length; i++) {
			ByteBufUtils.writeUTF8String(buf, IChatComponent.Serializer.func_150696_a(info[i]));
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.connected = buf.readBoolean();
		this.freq = buf.readInt();

		int length = buf.readInt();
		if(this.info.length != length) this.info = new IChatComponent[length];

		for(int i = 0; i < info.length; i++) {
			info[i] = IChatComponent.Serializer.func_150699_a(ByteBufUtils.readUTF8String(buf));
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.freq = nbt.getInteger("freq");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("freq", freq);
	}

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {

		if(bb == null) {
			bb = AxisAlignedBB.getBoundingBox(
					xCoord - 2,
					yCoord,
					zCoord - 2,
					xCoord + 3,
					yCoord + 10,
					zCoord + 3
			);
		}

		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public String[] getFunctionInfo() {
		return new String[] {
				PREFIX_VALUE + "connected",
				PREFIX_VALUE + "freq",
				PREFIX_VALUE + "rx",
				PREFIX_VALUE + "type",
				PREFIX_FUNCTION + "setfreq" + NAME_SEPARATOR + "freq",
				PREFIX_FUNCTION + "tx" + NAME_SEPARATOR + "payload"
		};
	}

	@Override
	public String provideRORValue(String name) {

		if(name.equals(PREFIX_VALUE + "connected")) {
			return this.connected ? "TRUE" : "FALSE";
		}

		if(name.equals(PREFIX_VALUE + "freq")) {
			return "" + this.freq;
		}

		if(name.equals(PREFIX_VALUE + "type")) {
			SatelliteSavedData dat = SatelliteSavedData.getData(worldObj);
			SatelliteBase sat = dat.getSatFromFreq(this.freq);
			if(sat != null) {
				return sat.getType();
			}
			return "";
		}

		if(name.equals(PREFIX_VALUE + "rx")) {
			SatelliteSavedData dat = SatelliteSavedData.getData(worldObj);
			SatelliteBase sat = dat.getSatFromFreq(this.freq);
			if(sat != null) {
				return sat.tx;
			}
			return "";
		}

		return null;
	}

	@Override
	public String runRORFunction(String name, String[] params) {
		if(name.equals(PREFIX_FUNCTION + "setfreq") && params.length == 1) {
			this.freq = IRORInteractive.parseInt(params[0], 0, 100_000);
			this.markChanged();
		}

		if(name.equals(PREFIX_FUNCTION + "tx")) {
			SatelliteSavedData dat = SatelliteSavedData.getData(worldObj);
			SatelliteBase sat = dat.getSatFromFreq(this.freq);
			String[] cmd = String.join(IRORInteractive.PARAM_SEPARATOR, params).split(" ");
			if(sat != null) {
				sat.onCommand(worldObj, cmd);
				dat.markDirty();
			}
			SatelliteRayScan.reportEvent(worldObj, xCoord, yCoord, zCoord, RayEvent.INFO_RADIO, 300);
			this.markChanged();
		}

		return null;
	}

	@Optional.Method(modid = "OpenComputers")
	private static int checkedPort(Arguments args, int index) {
		int port = args.checkInteger(index);
		if(port < MIN_PORT || port > MAX_PORT) {
			throw new IllegalArgumentException("port must be in range 1..65535");
		}
		return port;
	}

	@Optional.Method(modid = "OpenComputers")
	private static String contextAddress(Context context) {
		if(context == null || context.node() == null) return "";
		String address = context.node().address();
		return address == null ? "" : address;
	}

	@Optional.Method(modid = "OpenComputers")
	private void rememberContext(Context context) {
		String address = contextAddress(context);
		if(!address.isEmpty()) satcomContexts.put(address, context);
	}

	@Optional.Method(modid = "OpenComputers")
	private void pruneContexts() {
		for(Map.Entry<String, Object> entry : new ArrayList<Map.Entry<String, Object>>(satcomContexts.entrySet())) {
			Object raw = entry.getValue();
			if(!(raw instanceof Context)) {
				removeContext(entry.getKey());
				continue;
			}
			Context context = (Context) raw;
			if(!context.isRunning() && !context.isPaused()) removeContext(entry.getKey());
		}
	}

	private void removeContext(String address) {
		satcomContexts.remove(address);
		satcomPorts.remove(address);
	}

	private Set<Integer> portsFor(String address, boolean create) {
		Set<Integer> ports = satcomPorts.get(address);
		if(ports == null && create) {
			Set<Integer> fresh = ConcurrentHashMap.newKeySet();
			Set<Integer> raced = satcomPorts.putIfAbsent(address, fresh);
			ports = raced == null ? fresh : raced;
		}
		return ports;
	}

	@Optional.Method(modid = "OpenComputers")
	private static Object[] extractPayload(Arguments args, int firstIndex) {
		int count = args.count() - firstIndex;
		if(count < 0) count = 0;
		if(count > MAX_PAYLOAD_VALUES) {
			throw new IllegalArgumentException("SATCOM payload may contain at most " + MAX_PAYLOAD_VALUES + " values");
		}

		Object[] payload = new Object[count];
		for(int i = 0; i < count; i++) {
			Object value = args.checkAny(firstIndex + i);
			if(value != null
					&& !(value instanceof Boolean)
					&& !(value instanceof Number)
					&& !(value instanceof byte[])
					&& !(value instanceof String)) {
				throw new IllegalArgumentException("unsupported SATCOM payload value at index " + (i + 1));
			}
			payload[i] = value;
		}
		return payload;
	}

	@Optional.Method(modid = "OpenComputers")
	private boolean deliverTo(String receiverAddress, String senderAddress, int port, Object[] payload) {
		Object raw = satcomContexts.get(receiverAddress);
		Set<Integer> ports = satcomPorts.get(receiverAddress);
		if(!(raw instanceof Context) || ports == null || !ports.contains(port)) return false;

		Context receiver = (Context) raw;
		if(!receiver.isRunning() && !receiver.isPaused()) {
			removeContext(receiverAddress);
			return false;
		}

		Object[] signalArgs = new Object[3 + payload.length];
		signalArgs[0] = receiverAddress;
		signalArgs[1] = senderAddress;
		signalArgs[2] = port;
		System.arraycopy(payload, 0, signalArgs, 3, payload.length);
		return receiver.signal("satlink_message", signalArgs);
	}

	@Optional.Method(modid = "OpenComputers")
	private int broadcastSatcom(Context senderContext, int port, Object[] payload) {
		if(!hasActiveRelaySatellite()) return 0;
		String senderAddress = contextAddress(senderContext);
		if(senderAddress.isEmpty()) return 0;

		int delivered = 0;
		Set<String> attempted = new HashSet<String>();
		for(TileEntityMachineSatLink station : new ArrayList<TileEntityMachineSatLink>(LOADED_STATIONS)) {
			if(!isSameSatcomNetwork(station)) continue;
			station.pruneContexts();
			for(String receiverAddress : new ArrayList<String>(station.satcomContexts.keySet())) {
				if(senderAddress.equals(receiverAddress) || !attempted.add(receiverAddress)) continue;
				if(station.deliverTo(receiverAddress, senderAddress, port, payload)) delivered++;
			}
		}
		return delivered;
	}

	@Optional.Method(modid = "OpenComputers")
	private boolean sendSatcom(Context senderContext, String targetAddress, int port, Object[] payload) {
		if(!hasActiveRelaySatellite()) return false;
		String senderAddress = contextAddress(senderContext);
		if(senderAddress.isEmpty() || targetAddress == null || targetAddress.isEmpty()) return false;

		for(TileEntityMachineSatLink station : new ArrayList<TileEntityMachineSatLink>(LOADED_STATIONS)) {
			if(!isSameSatcomNetwork(station)) continue;
			station.pruneContexts();
			if(station.deliverTo(targetAddress, senderAddress, port, payload)) return true;
		}
		return false;
	}

	private IntelScanResult intelResult() {
		SatelliteIntelligenceBase sat = getIntelligenceSatellite();
		return sat == null ? null : sat.getLastResult();
	}

	private String encodeSurfacePage(java.util.List<IntelSurfaceCell> cells, int start, int end) {
		StringBuilder out = new StringBuilder();
		for(int i = start; i < end; i++) {
			IntelSurfaceCell cell = cells.get(i);
			if(out.length() > 0) out.append('|');
			out.append(cell.x).append(',').append(cell.y).append(',').append(cell.z).append(',')
					.append(cell.classification.name()).append(',').append(cell.structural ? '1' : '0');
		}
		return out.toString();
	}

	private String encodeStructuralPage(java.util.List<IntelStructuralCell> cells, int start, int end) {
		StringBuilder out = new StringBuilder();
		for(int i = start; i < end; i++) {
			IntelStructuralCell cell = cells.get(i);
			if(out.length() > 0) out.append('|');
			out.append(cell.x).append(',').append(cell.y).append(',').append(cell.z).append(',')
					.append(cell.registryId.replace('|', '_').replace(',', '_')).append(',')
					.append(cell.metadata).append(',').append(cell.blastResistance).append(',').append(cell.resistanceBand.name());
		}
		return out.toString();
	}

	// yay opencomputer stuff
	@Override
	@Optional.Method(modid = "OpenComputers")
	public String getComponentName() {
		return "ntm_satlink";
	}

	@Callback(direct = true, doc = "function():boolean -- Returns connection state")
	@Optional.Method(modid = "OpenComputers")
	public Object[] isConnected(Context context, Arguments args) {
		return new Object[] { connected };
	}

	@Callback(direct = true, limit = 4, doc = "function(freq: number) -- Sets satellite frequency")
	@Optional.Method(modid = "OpenComputers")
	public Object[] setFreq(Context context, Arguments args) {
		freq = args.checkInteger(0);
		this.markChanged();
		return new Object[] {};
	}

	@Callback(direct = true, doc = "function():number -- Gets satellite frequency")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getFreq(Context context, Arguments args) {
		return new Object[] { freq };
	}

	@Callback(direct = true, doc = "function():string -- Gets satellite type")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getType(Context context, Arguments args) {
		return new Object[] { provideRORValue(PREFIX_VALUE + "type") };
	}

	@Callback(direct = true, limit = 4, doc = "function(command: string) -- Transmits a command to the satellite")
	@Optional.Method(modid = "OpenComputers")
	public Object[] send(Context context, Arguments args) {
		runRORFunction(PREFIX_FUNCTION + "tx", new String[]{args.checkString(0)});
		return new Object[] {};
	}

	@Callback(direct = true, limit = 4, doc = "function():string -- Gets received command from the satellite")
	@Optional.Method(modid = "OpenComputers")
	public Object[] read(Context context, Arguments args) {
		return new Object[] { provideRORValue(PREFIX_VALUE + "rx") };
	}

	@Callback(direct = true, doc = "function():boolean,string -- Returns whether the tuned relay satellite is usable and its type")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getSatelliteStatus(Context context, Arguments args) {
		SatelliteBase sat = worldObj == null ? null : SatelliteSavedData.getData(worldObj).getSatFromFreq(freq);
		return new Object[] { hasActiveRelaySatellite(), sat == null ? "" : sat.getType() };
	}

	@Callback(direct = true, doc = "function():string -- Returns this computer's SATCOM address")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getAddress(Context context, Arguments args) {
		rememberContext(context);
		return new Object[] { contextAddress(context) };
	}

	@Callback(direct = true, doc = "function(port:number):boolean -- Opens a SATCOM port for this computer")
	@Optional.Method(modid = "OpenComputers")
	public Object[] open(Context context, Arguments args) {
		int port = checkedPort(args, 0);
		String address = contextAddress(context);
		if(address.isEmpty()) return new Object[] { false };
		rememberContext(context);
		return new Object[] { portsFor(address, true).add(port) };
	}

	@Callback(direct = true, doc = "function(port:number):boolean -- Closes a SATCOM port for this computer")
	@Optional.Method(modid = "OpenComputers")
	public Object[] close(Context context, Arguments args) {
		int port = checkedPort(args, 0);
		String address = contextAddress(context);
		Set<Integer> ports = portsFor(address, false);
		return new Object[] { ports != null && ports.remove(port) };
	}

	@Callback(direct = true, doc = "function():boolean -- Closes all SATCOM ports for this computer")
	@Optional.Method(modid = "OpenComputers")
	public Object[] closeAll(Context context, Arguments args) {
		String address = contextAddress(context);
		Set<Integer> ports = portsFor(address, false);
		boolean changed = ports != null && !ports.isEmpty();
		if(ports != null) ports.clear();
		return new Object[] { changed };
	}

	@Callback(direct = true, doc = "function(port:number):boolean -- Returns whether this computer has the SATCOM port open")
	@Optional.Method(modid = "OpenComputers")
	public Object[] isOpen(Context context, Arguments args) {
		int port = checkedPort(args, 0);
		String address = contextAddress(context);
		Set<Integer> ports = portsFor(address, false);
		return new Object[] { ports != null && ports.contains(port) };
	}

	@Callback(direct = true, limit = 4, doc = "function(address:string, port:number, ...):boolean -- Sends a SATCOM packet")
	@Optional.Method(modid = "OpenComputers")
	public Object[] sendPacket(Context context, Arguments args) {
		String target = args.checkString(0);
		int port = checkedPort(args, 1);
		Object[] payload = extractPayload(args, 2);
		rememberContext(context);
		return new Object[] { sendSatcom(context, target, port, payload) };
	}

	@Callback(direct = true, limit = 4, doc = "function(port:number, ...):number -- Broadcasts a SATCOM packet and returns delivery count")
	@Optional.Method(modid = "OpenComputers")
	public Object[] broadcast(Context context, Arguments args) {
		int port = checkedPort(args, 0);
		Object[] payload = extractPayload(args, 1);
		rememberContext(context);
		return new Object[] { broadcastSatcom(context, port, payload) };
	}

	@Callback(direct = true, limit = 4, doc = "function(x:number,z:number):boolean,string -- Sets intelligence target")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelSetTarget(Context context, Arguments args) {
		SatelliteIntelligenceBase sat = getIntelligenceSatellite();
		if(sat == null) return new Object[] { false, "UNSUPPORTED" };
		sat.setTarget(args.checkInteger(0), args.checkInteger(1));
		sat.markDirty();
		SatelliteSavedData.getData(worldObj).markDirty();
		return new Object[] { true, "OK" };
	}

	@Callback(direct = true, limit = 4, doc = "function():boolean,string -- Starts intelligence scan")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelStartScan(Context context, Arguments args) {
		SatelliteIntelligenceBase sat = getIntelligenceSatellite();
		if(sat == null) return new Object[] { false, "UNSUPPORTED" };
		boolean started = sat.startScan(worldObj);
		SatelliteSavedData.getData(worldObj).markDirty();
		return new Object[] { started, started ? "STARTED" : "BUSY" };
	}

	@Callback(direct = true, doc = "function():string,number,number,number -- Gets intelligence scan status")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelStatus(Context context, Arguments args) {
		SatelliteIntelligenceBase sat = getIntelligenceSatellite();
		if(sat == null) return new Object[] { "UNSUPPORTED", 0, 0, 0 };
		String[] parts = sat.getScanStatus().split(";");
		String state = parts.length > 0 ? parts[0] : "IDLE";
		int done = sat.activeJob == null ? 0 : sat.activeJob.processedWork;
		int total = sat.activeJob == null ? 0 : sat.activeJob.totalWork;
		IntelScanResult result = sat.getLastResult();
		int coverage = result == null ? 0 : result.getCoveragePercent();
		return new Object[] { state, done, total, coverage };
	}

	@Callback(direct = true, doc = "function():string -- Gets latest intelligence summary")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelSummary(Context context, Arguments args) {
		SatelliteIntelligenceBase sat = getIntelligenceSatellite();
		return new Object[] { sat == null ? "UNSUPPORTED" : sat.getScanSummary() };
	}

	@Callback(direct = true, doc = "function():number -- Gets intelligence finding count")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelFindingCount(Context context, Arguments args) {
		IntelScanResult result = intelResult();
		return new Object[] { result == null ? 0 : result.findings.size() };
	}

	@Callback(direct = true, doc = "function(index:number):... -- Gets one 1-based intelligence finding")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelGetFinding(Context context, Arguments args) {
		IntelScanResult result = intelResult();
		int index = args.checkInteger(0) - 1;
		if(result == null || index < 0 || index >= result.findings.size()) return new Object[] { false, "OUT_OF_RANGE" };
		IntelFinding f = result.findings.get(index);
		boolean combined = result.mode == IntelScanMode.COMBINED;
		return new Object[] { true, f.classification.forMode(result.mode).name(), f.confidence,
				f.minX, f.minY, f.minZ, f.maxX, f.maxY, f.maxZ,
				f.reinforced, f.machinery, f.power, combined && f.launchInfrastructure, f.communications,
				combined ? f.targetType : "", combined ? f.targetId : "", combined ? f.targetCount : 0 };
	}

	@Callback(direct = true, doc = "function(page:number):boolean,number,string -- Gets 64-cell surface page")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelSurfacePage(Context context, Arguments args) {
		IntelScanResult result = intelResult();
		int page = args.checkInteger(0);
		if(result == null || page < 1) return new Object[] { false, "OUT_OF_RANGE" };
		int start = (page - 1) * IntelScanResult.PAGE_SIZE;
		if(start >= result.surfaceCells.size()) return new Object[] { false, "OUT_OF_RANGE" };
		int end = Math.min(result.surfaceCells.size(), start + IntelScanResult.PAGE_SIZE);
		return new Object[] { true, end - start, encodeSurfacePage(result.surfaceCells, start, end) };
	}

	@Callback(direct = true, doc = "function(page:number):boolean,number,string -- Gets 64-cell subsurface page")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelSubsurfacePage(Context context, Arguments args) {
		IntelScanResult result = intelResult();
		int page = args.checkInteger(0);
		if(result == null || page < 1) return new Object[] { false, "OUT_OF_RANGE" };
		int start = (page - 1) * IntelScanResult.PAGE_SIZE;
		if(start >= result.subsurfaceCells.size()) return new Object[] { false, "OUT_OF_RANGE" };
		int end = Math.min(result.subsurfaceCells.size(), start + IntelScanResult.PAGE_SIZE);
		return new Object[] { true, end - start, encodeSurfacePage(result.subsurfaceCells, start, end) };
	}

	@Callback(direct = true, doc = "function(page:number):boolean,number,string -- Gets Combined structural page")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelStructuralPage(Context context, Arguments args) {
		SatelliteIntelligenceBase sat = getIntelligenceSatellite();
		if(!(sat instanceof SatelliteCombinedIntel)) return new Object[] { false, "UNSUPPORTED" };
		IntelScanResult result = sat.getLastResult();
		int page = args.checkInteger(0);
		if(result == null || page < 1) return new Object[] { false, "OUT_OF_RANGE" };
		int start = (page - 1) * IntelScanResult.PAGE_SIZE;
		if(start >= result.structuralCells.size()) return new Object[] { false, "OUT_OF_RANGE" };
		int end = Math.min(result.structuralCells.size(), start + IntelScanResult.PAGE_SIZE);
		return new Object[] { true, end - start, encodeStructuralPage(result.structuralCells, start, end) };
	}

	@Callback(direct = true, doc = "function():... -- Gets Combined structural summary")
	@Optional.Method(modid = "OpenComputers")
	public Object[] intelStructuralSummary(Context context, Arguments args) {
		SatelliteIntelligenceBase sat = getIntelligenceSatellite();
		if(!(sat instanceof SatelliteCombinedIntel)) return new Object[] { false, "UNSUPPORTED" };
		IntelScanResult result = sat.getLastResult();
		if(result == null || result.structuralSummary == null) return new Object[] { false, "NO_DATA" };
		IntelStructuralSummary s = result.structuralSummary;
		return new Object[] { true, s.dominantMaterial, s.averageResistance, s.maxResistance,
				s.wallThickness, s.roofThickness, s.floorThickness, s.weakPointCount };
	}

	@Override
	@Optional.Method(modid = "OpenComputers")
	public String[] methods() {
		return new String[] {
			"isConnected",
			"setFreq",
			"getFreq",
			"getType",
			"send",
			"read",
			"getSatelliteStatus",
			"getAddress",
			"open",
			"close",
			"closeAll",
			"isOpen",
			"sendPacket",
			"broadcast",
			"intelSetTarget",
			"intelStartScan",
			"intelStatus",
			"intelSummary",
			"intelFindingCount",
			"intelGetFinding",
			"intelSurfacePage",
			"intelSubsurfacePage",
			"intelStructuralPage",
			"intelStructuralSummary"
		};
	}

	@Override
	@Optional.Method(modid = "OpenComputers")
	public Object[] invoke(String method, Context context, Arguments args) throws Exception {
		switch(method) {
			case ("isConnected"): return isConnected(context, args);
			case ("setFreq"): return setFreq(context, args);
			case ("getFreq"): return getFreq(context, args);
			case ("getType"): return getType(context, args);
			case ("send"): return send(context, args);
			case ("read"): return read(context, args);
			case ("getSatelliteStatus"): return getSatelliteStatus(context, args);
			case ("getAddress"): return getAddress(context, args);
			case ("open"): return open(context, args);
			case ("close"): return close(context, args);
			case ("closeAll"): return closeAll(context, args);
			case ("isOpen"): return isOpen(context, args);
			case ("sendPacket"): return sendPacket(context, args);
			case ("broadcast"): return broadcast(context, args);
			case ("intelSetTarget"): return intelSetTarget(context, args);
			case ("intelStartScan"): return intelStartScan(context, args);
			case ("intelStatus"): return intelStatus(context, args);
			case ("intelSummary"): return intelSummary(context, args);
			case ("intelFindingCount"): return intelFindingCount(context, args);
			case ("intelGetFinding"): return intelGetFinding(context, args);
			case ("intelSurfacePage"): return intelSurfacePage(context, args);
			case ("intelSubsurfacePage"): return intelSubsurfacePage(context, args);
			case ("intelStructuralPage"): return intelStructuralPage(context, args);
			case ("intelStructuralSummary"): return intelStructuralSummary(context, args);
		}
		throw new NoSuchMethodException();
	}
}
