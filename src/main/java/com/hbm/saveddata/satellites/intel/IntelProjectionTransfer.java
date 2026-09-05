package com.hbm.saveddata.satellites.intel;

import java.util.UUID;
import io.netty.buffer.ByteBuf;

/** Sequential, bounded snapshot pieces. A new scene discards any unfinished old transfer. */
public final class IntelProjectionTransfer {
	public static final int CHUNK_SIZE=65536, MAX_BYTES=8*1024*1024;
	private String id="";
	private byte[] bytes;
	private int received;

	public synchronized int offset(String expected) {
		if(!id.equals(expected)) { id=expected;bytes=null;received=0; }
		return received;
	}
	public synchronized void reset() { id="";bytes=null;received=0; }
	public static void write(ByteBuf buf,String id,byte[] data,int offset) {
		if(data.length<1 || data.length>MAX_BYTES || offset<0 || offset>=data.length || offset%CHUNK_SIZE!=0)
			throw new IllegalArgumentException("Invalid projection piece");
		UUID uuid=UUID.fromString(id);int length=Math.min(CHUNK_SIZE,data.length-offset);
		buf.writeLong(uuid.getMostSignificantBits());buf.writeLong(uuid.getLeastSignificantBits());
		buf.writeInt(data.length);buf.writeInt(offset);buf.writeInt(length);buf.writeBytes(data,offset,length);
	}
	public synchronized byte[] accept(ByteBuf buf,String expected) {
		if(buf.readableBytes()<28) throw new IllegalArgumentException("Incomplete projection header");
		String sent=new UUID(buf.readLong(),buf.readLong()).toString();
		if(!sent.equals(expected)) return null;
		offset(expected);
		int total=buf.readInt(),offset=buf.readInt(),length=buf.readInt();
		if(total<1 || total>MAX_BYTES || offset<0 || offset>=total || offset%CHUNK_SIZE!=0
				|| length!=Math.min(CHUNK_SIZE,total-offset) || length>buf.readableBytes())
			throw new IllegalArgumentException("Invalid projection piece");
		if(offset!=received) return null;
		if(bytes==null) bytes=new byte[total];
		if(total!=bytes.length) throw new IllegalArgumentException("Projection size changed");
		buf.readBytes(bytes,offset,length);received+=length;
		return received==total?bytes:null;
	}
}
