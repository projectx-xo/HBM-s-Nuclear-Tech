package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;
import java.util.Random;
import org.junit.Test;
import com.hbm.tileentity.machine.TileEntityIntelProjector;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;

public class IntelProjectorSyncTest {
	@Test public void compressedGeometryLargerThanVanillaNbtPacketLimitSurvivesSync() {
		IntelScanResult scan=new IntelScanResult();scan.mode=IntelScanMode.COMBINED;
		scan.projection=new IntelProjection(-32,-32,64,64);
		new Random(7).nextBytes(scan.projection.cells);
		NBTTagCompound saved=new NBTTagCompound(), scene=new NBTTagCompound();
		scan.writeToNBT(scene);saved.setTag("display",scene);saved.setString("sceneId",scan.projection.id);
		TileEntityIntelProjector server=new TileEntityIntelProjector();server.readFromNBT(saved);
		ByteBuf buffer=Unpooled.buffer();
		try {
			server.serialize(buffer); assertTrue(buffer.readableBytes()>32767); assertTrue(buffer.readableBytes()<2097152);
			TileEntityIntelProjector client=new TileEntityIntelProjector();
			client.readState(server.state()); client.deserialize(buffer); client.acceptPendingSnapshot();
			assertNotNull(client.displayed);assertEquals(scan.projection.id,client.displayed.projection.id);
			assertArrayEquals(scan.projection.cells,client.displayed.projection.cells);
			assertFalse(server.state().hasKey("display")); // Floor/rotation packets never contain geometry.
		} finally { buffer.release(); }
	}
	@Test public void lateGeometryDoesNotReplaceANewerScan() {
		IntelScanResult scan=new IntelScanResult();scan.mode=IntelScanMode.COMBINED;scan.projection=new IntelProjection(0,0,1,1);
		NBTTagCompound saved=new NBTTagCompound(), scene=new NBTTagCompound();scan.writeToNBT(scene);saved.setTag("display",scene);
		TileEntityIntelProjector server=new TileEntityIntelProjector();server.readFromNBT(saved);
		ByteBuf buffer=Unpooled.buffer();
		try {
			server.serialize(buffer);TileEntityIntelProjector client=new TileEntityIntelProjector();
			NBTTagCompound newer=new NBTTagCompound();newer.setString("sceneId","new scan");client.readState(newer);
			client.deserialize(buffer);client.acceptPendingSnapshot();assertNull(client.displayed);
		} finally { buffer.release(); }
	}
}
