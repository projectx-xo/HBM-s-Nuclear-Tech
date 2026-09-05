package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;
import java.util.Random;
import java.util.UUID;
import org.junit.Test;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class IntelProjectionTransferTest {
	@Test public void largeSnapshotsTravelInBoundedPiecesAndIgnoreLateOrDuplicatePieces() {
		byte[] data=new byte[3*1024*1024+13];new Random(7).nextBytes(data);
		String id=UUID.randomUUID().toString(),old=UUID.randomUUID().toString();
		IntelProjectionTransfer transfer=new IntelProjectionTransfer();byte[] complete=null;
		for(int offset=0;offset<data.length;offset+=IntelProjectionTransfer.CHUNK_SIZE) {
			assertNull(receive(transfer,old,data,0,id));assertEquals(offset,transfer.offset(id));
			complete=receive(transfer,id,data,offset,id);
			if(offset+IntelProjectionTransfer.CHUNK_SIZE<data.length) {
				assertNull(complete);assertNull(receive(transfer,id,data,offset,id));
			}
		}
		assertArrayEquals(data,complete);
		assertEquals(0,transfer.offset(old));
		assertNull(receive(transfer,id,data,0,old));assertEquals(0,transfer.offset(old));
	}
	@Test public void aMissingOrMalformedPieceDoesNotSkipAheadOrAllocateAnUnboundedSnapshot() {
		byte[] data=new byte[2*IntelProjectionTransfer.CHUNK_SIZE];String id=UUID.randomUUID().toString();
		IntelProjectionTransfer transfer=new IntelProjectionTransfer();
		assertNull(receive(transfer,id,data,IntelProjectionTransfer.CHUNK_SIZE,id));assertEquals(0,transfer.offset(id));
		ByteBuf buf=Unpooled.buffer();
		try {
			IntelProjectionTransfer.write(buf,id,data,0);buf.setInt(16,Integer.MAX_VALUE);
			try { transfer.accept(buf,id);fail("Accepted oversized snapshot"); } catch(IllegalArgumentException expected) { }
			assertEquals(0,transfer.offset(id));
		} finally { buf.release(); }
	}
	@Test public void forgeBackingArrayPaddingDoesNotInvalidateTheDeclaredPiece() {
		byte[] data=new byte[12345];new Random(9).nextBytes(data);String id=UUID.randomUUID().toString();
		ByteBuf encoded=Unpooled.buffer();ByteBuf wire=null;
		try {
			IntelProjectionTransfer.write(encoded,id,data,0);
			// Forge 1.7.10 FMLProxyPacket sends payload.array(), including unused buffer capacity.
			wire=Unpooled.wrappedBuffer(encoded.array());assertTrue(wire.readableBytes()>encoded.writerIndex());
			assertArrayEquals(data,new IntelProjectionTransfer().accept(wire,id));
		} finally { if(wire!=null) wire.release();encoded.release(); }
	}
	private byte[] receive(IntelProjectionTransfer transfer,String sent,byte[] data,int offset,String expected) {
		ByteBuf buf=Unpooled.buffer();
		try {
			IntelProjectionTransfer.write(buf,sent,data,offset);
			assertTrue(buf.readableBytes()<=IntelProjectionTransfer.CHUNK_SIZE+28);
			return transfer.accept(buf,expected);
		} finally { buf.release(); }
	}
}
