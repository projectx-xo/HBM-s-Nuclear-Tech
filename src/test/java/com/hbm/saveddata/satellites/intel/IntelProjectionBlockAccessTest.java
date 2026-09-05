package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Test;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.RegistrySimple;

public class IntelProjectionBlockAccessTest {
	@Test public void clippingExposesRealNeighborsWithoutMovingBlocksOrUsingALiveWorld() throws Exception {
		Field field=RegistrySimple.class.getDeclaredField("registryObjects");field.setAccessible(true);
		Map names=(Map)field.get(Block.blockRegistry);Block concrete=new Block(Material.rock) { };
		names.put("test:projection_concrete",concrete);
		try {
			IntelProjection p=new IntelProjection(507,1709,3,3);
			for(int x=0;x<3;x++) for(int y=4;y<7;y++) for(int z=0;z<3;z++) {
				p.set(x,y,z,255,false);p.setBlock(x,y,z,"test:projection_concrete",(x+y+z)&15);
			}
			IntelProjectionView view=new IntelProjectionView();IntelProjectionBlockAccess access=new IntelProjectionBlockAccess(p,view);
			assertSame(concrete,access.getBlock(1,5,1));assertEquals(7,access.getBlockMetadata(1,5,1));
			assertTrue(access.enclosed(1,5,1));assertFalse(access.enclosed(0,5,1));
			assertSame(Blocks.air,access.getBlock(-1,5,1));assertNull(access.getTileEntity(1,5,1));
			view.floor=5;view.cutAxis=0;view.cut=508;access=new IntelProjectionBlockAccess(p,view);
			assertSame(Blocks.air,access.getBlock(1,6,1));assertSame(Blocks.air,access.getBlock(2,5,1));
			assertSame(concrete,access.getBlock(1,5,1));assertFalse(access.enclosed(1,5,1));
			view.floor=255;view.cutAxis=-1;p.set(1,5,1,255,true);
			assertSame(Blocks.air,new IntelProjectionBlockAccess(p,view).getBlock(1,5,1));
			view.terrain=true;access=new IntelProjectionBlockAccess(p,view);view.terrain=false;
			assertSame(concrete,access.getBlock(1,5,1)); // Controls are copied for an incremental bake.
		} finally { names.remove("test:projection_concrete"); }
	}
}
