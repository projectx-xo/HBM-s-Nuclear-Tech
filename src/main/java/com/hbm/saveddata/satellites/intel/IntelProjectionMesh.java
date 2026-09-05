package com.hbm.saveddata.satellites.intel;

import java.util.ArrayList;
import java.util.List;

/** Greedy exposed-face meshing, advanced a bounded number of planes at a time. */
public final class IntelProjectionMesh {
	public static final int MAX_QUADS=50000;
	public static final class Quad {
		public final float x,y,z,width,height;
		public final int axis, sign;
		public final boolean glass;
		Quad(int[] xyz,int axis,int sign,int width,int height) {
			x=xyz[0]*.5F; y=xyz[1]*.5F; z=xyz[2]*.5F;
			this.axis=axis; this.sign=sign>0?1:-1; this.glass=Math.abs(sign)==2; this.width=width*.5F; this.height=height*.5F;
		}
	}
	public static final class Builder {
		public final List<Quad> quads=new ArrayList<Quad>();
		public boolean truncated;
		private final IntelProjection p;
		private final int floor,cutAxis,cut;
		private final boolean terrain;
		private final int[] dims;
		private int axis, plane;
		public Builder(IntelProjection p,int floor,int cutAxis,int cut,boolean terrain) {
			this.p=p; this.floor=floor; this.cutAxis=cutAxis; this.cut=cut; this.terrain=terrain;
			dims=new int[]{p.width*2,Math.min(256,floor+1)*2,p.depth*2};
		}
		public boolean step(int budget) {
			while(axis<3 && budget-->0 && !truncated) {
				buildPlane();
				if(++plane>dims[axis]) { axis++; plane=0; }
			}
			return axis==3 || truncated;
		}
		private int material(int x,int y,int z) {
			if(x<0 || y<0 || z<0 || x>=dims[0] || y>=dims[1] || z>=dims[2]) return 0;
			if(cutAxis==0 && p.originX+x*.5>=cut+1 || cutAxis==2 && p.originZ+z*.5>=cut+1) return 0;
			int mask=p.mask(x>>1,y>>1,z>>1);
			if(mask==0 || !terrain && p.natural(x>>1,y>>1,z>>1) || (mask & (1<<((x&1)+2*(z&1)+4*(y&1))))==0) return 0;
			return p.glass(x>>1,y>>1,z>>1)?2:1;
		}
		private void buildPlane() {
			int u=(axis+1)%3, v=(axis+2)%3, w=dims[u], h=dims[v];
			byte[] faces=new byte[w*h]; int[] pos=new int[3]; pos[axis]=plane;
			for(int j=0;j<h;j++) for(int i=0;i<w;i++) {
				pos[u]=i; pos[v]=j; pos[axis]=plane-1;
				int a=material(pos[0],pos[1],pos[2]); pos[axis]=plane;
				int b=material(pos[0],pos[1],pos[2]);
				faces[j*w+i]=(byte)(a==b?0:a==1?1:b==1?-1:a==2?2:-2);
			}
			for(int j=0;j<h;j++) for(int i=0;i<w;) {
				byte sign=faces[j*w+i]; if(sign==0) { i++; continue; }
				int a=1,b=1;
				while(i+a<w && faces[j*w+i+a]==sign) a++;
				outer: while(j+b<h) { for(int k=0;k<a;k++) if(faces[(j+b)*w+i+k]!=sign) break outer; b++; }
				pos[u]=i;pos[v]=j;
				if(quads.size()>=MAX_QUADS) { truncated=true; return; }
				quads.add(new Quad(pos,axis,sign,a,b));
				for(int y=0;y<b;y++) for(int x=0;x<a;x++) faces[(j+y)*w+i+x]=0;
				i+=a;
			}
		}
	}
}
