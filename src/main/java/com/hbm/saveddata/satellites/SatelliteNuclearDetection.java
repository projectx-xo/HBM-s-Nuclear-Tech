package com.hbm.saveddata.satellites;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import com.hbm.entity.missile.MissilePayload;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/** Same-dimension observations of loaded entities and reported nuclear bursts. */
public class SatelliteNuclearDetection extends SatelliteBase {
	private static final Map<World, Journal> JOURNALS = new WeakHashMap<World, Journal>();
	@Override public String getType() { return "NUCLEAR_DETECTION"; }
	@Override public IChatComponent[] getInfo(World world) { return new IChatComponent[] {new ChatComponentText("Nuclear Detection Satellite")}; }
	static Journal journal(World world) {
		Journal j=JOURNALS.get(world);
		if(j==null) { j=new Journal();JOURNALS.put(world,j); }
		return j;
	}
	public static void reportExplosion(World world, double x, Double y, double z) {
		if(world==null || world.isRemote) return;
		journal(world).add("EXPLOSION","NUCLEAR",x,y,z,world.provider.dimensionId,world.getTotalWorldTime());
	}
	public static Object[] poll(World world, String epoch, int cursor) {
		Journal j=journal(world);long now=world.getTotalWorldTime();
		if(now!=j.lastScan && (j.lastScan<0 || now-j.lastScan>=20 || now<j.lastScan)) {
			j.lastScan=now;
			List<?> entities=world.loadedEntityList;
			int count=Math.min(256,entities.size());
			for(int i=0;i<count;i++) {
				if(j.scanIndex>=entities.size())j.scanIndex=0;
				Object object=entities.get(j.scanIndex++);
				if(!(object instanceof Entity))continue;
				Entity entity=(Entity)object;
				if(entity.isDead)continue;
				String payload=MissilePayload.classify(entity);
				if(!"NUCLEAR".equals(payload) && !"THERMONUCLEAR".equals(payload))continue;
				String id=entity.getUniqueID().toString();
				Long seen=j.missiles.get(id);
				if(seen==null || now-seen>12000 || now<seen) j.add("MISSILE",payload,entity.posX,entity.posY,entity.posZ,world.provider.dimensionId,now);
				j.missiles.put(id,now);
				while(j.missiles.size()>2048)j.missiles.remove(j.missiles.keySet().iterator().next());
			}
		}
		return j.page(epoch,cursor,now);
	}
	public static final class Journal {
		final String epoch=UUID.randomUUID().toString();
		final LinkedHashMap<String,Long> missiles=new LinkedHashMap<String,Long>();
		final Deque<Event> events=new ArrayDeque<Event>();
		long lastScan=-1;
		int scanIndex, sequence;
		public void add(String kind,String payload,double x,Double y,double z,int dimension,long tick) {
			if(!Double.isFinite(x)||!Double.isFinite(z)||(y!=null&&!Double.isFinite(y)))return;
			events.addLast(new Event(++sequence,tick,kind+","+payload+","+x+","+(y==null?"?":y)+","+z+","+dimension+","+tick));
			while(events.size()>256)events.removeFirst();
		}
		public Object[] page(String previous,int cursor,long now) {
			while(!events.isEmpty() && now-events.peekFirst().tick>12000)events.removeFirst();
			boolean reset=!epoch.equals(previous);
			if(reset)cursor=0;
			if(cursor<0||cursor>sequence)return new Object[]{false,"INVALID_CURSOR"};
			int oldest=events.isEmpty()?sequence+1:events.peekFirst().sequence;
			int lost=Math.max(0,oldest-cursor-1), next=cursor, count=0;
			StringBuilder rows=new StringBuilder();
			for(Event event:events)if(event.sequence>cursor) {
				if(count++==8)break;
				if(rows.length()>0)rows.append('|');rows.append(event.sequence).append(',').append(event.data);next=event.sequence;
			}
			if(events.isEmpty())next=sequence;
			return new Object[]{true,epoch,next,rows.toString(),lost,next<sequence};
		}
	}
	private static final class Event {
		final int sequence;final long tick;final String data;
		Event(int sequence,long tick,String data){this.sequence=sequence;this.tick=tick;this.data=data;}
	}
}
