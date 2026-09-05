package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;
import java.util.Random;
import org.junit.Test;
import com.hbm.tileentity.machine.TileEntityIntelProjector;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;

public class IntelProjectorSyncTest {
	@Test public void texturedGeometryLargerThanForgeFrameLimitSurvivesSync() {
		IntelScanResult scan=new IntelScanResult();scan.mode=IntelScanMode.COMBINED;
		scan.projection=new IntelProjection(-32,-32,64,64);Random random=new Random(7);
		random.nextBytes(scan.projection.cells);
		for(int i=0;i<2048;i++) scan.projection.setBlock(0,i%256,0,"test:block"+i,0);
		for(int i=0;i<scan.projection.blockStates.length;i++) scan.projection.blockStates[i]=(char)(16+random.nextInt(2048*16));
		NBTTagCompound saved=new NBTTagCompound(), scene=new NBTTagCompound();
		scan.writeToNBT(scene);saved.setTag("display",scene);
		TileEntityIntelProjector server=new TileEntityIntelProjector();server.readFromNBT(saved);
		TileEntityIntelProjector client=new TileEntityIntelProjector();client.readState(server.state());
		int total=0;
		for(int offset=0;client.displayed==null;offset+=IntelProjectionTransfer.CHUNK_SIZE) {
			assertTrue(offset<IntelProjectionTransfer.MAX_BYTES);ByteBuf buffer=Unpooled.buffer();
			try {
				server.serializeChunk(buffer,offset);total+=buffer.readableBytes();
				assertTrue(buffer.readableBytes()<=IntelProjectionTransfer.CHUNK_SIZE+28);
				client.deserialize(buffer);client.acceptPendingSnapshot();
			} finally { buffer.release(); }
		}
		assertTrue(total>2097152);assertEquals(scan.projection.id,client.displayed.projection.id);
		assertArrayEquals(scan.projection.cells,client.displayed.projection.cells);
		assertArrayEquals(scan.projection.blockStates,client.displayed.projection.blockStates);
		assertEquals(scan.projection.blockPalette,client.displayed.projection.blockPalette);
		assertFalse(server.state().hasKey("display")); // Floor/rotation packets never contain geometry.
	}
	@Test public void aMislabeledOldBodyCanBeRetriedWithTheCorrectScene() {
		TileEntityIntelProjector old=table(),current=table(),client=new TileEntityIntelProjector();
		client.readState(current.state());ByteBuf buffer=Unpooled.buffer();
		try {
			old.serialize(buffer);java.util.UUID id=java.util.UUID.fromString(current.sceneId);
			buffer.setLong(0,id.getMostSignificantBits());buffer.setLong(8,id.getLeastSignificantBits());
			client.deserialize(buffer);client.acceptPendingSnapshot();assertNull(client.displayed);
			buffer.clear();current.serialize(buffer);client.deserialize(buffer);client.acceptPendingSnapshot();
			assertEquals(current.sceneId,client.displayed.projection.id);
			// A concurrently changed public scene ID must never relabel the already encoded bytes.
			String encodedId=current.sceneId;current.sceneId=old.sceneId;buffer.clear();current.serialize(buffer);
			assertEquals(encodedId,new java.util.UUID(buffer.readLong(),buffer.readLong()).toString());
		} finally { buffer.release(); }
	}
	private TileEntityIntelProjector table() {
		IntelScanResult scan=new IntelScanResult();scan.mode=IntelScanMode.COMBINED;scan.projection=new IntelProjection(0,0,1,1);
		NBTTagCompound saved=new NBTTagCompound(),scene=new NBTTagCompound();scan.writeToNBT(scene);saved.setTag("display",scene);
		TileEntityIntelProjector table=new TileEntityIntelProjector();table.readFromNBT(saved);return table;
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
