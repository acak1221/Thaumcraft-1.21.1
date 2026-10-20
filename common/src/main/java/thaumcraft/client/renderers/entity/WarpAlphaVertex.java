package thaumcraft.client.renderers.entity;
import com.mojang.blaze3d.vertex.VertexConsumer;
/** Per-vertex alpha survives deferred native buffer flushing; no global shader-color leak. */
final class WarpAlphaVertex implements VertexConsumer {
    private final VertexConsumer out; private final float alpha;
    WarpAlphaVertex(VertexConsumer out,float alpha) { this.out=out;this.alpha=alpha; }
    public VertexConsumer addVertex(float x,float y,float z) { out.addVertex(x,y,z);return this; }
    public VertexConsumer setColor(int r,int g,int b,int a) { out.setColor(r,g,b,(int)(a*alpha));return this; }
    public VertexConsumer setUv(float u,float v) { out.setUv(u,v);return this; }
    public VertexConsumer setUv1(int u,int v) { out.setUv1(u,v);return this; }
    public VertexConsumer setUv2(int u,int v) { out.setUv2(u,v);return this; }
    public VertexConsumer setNormal(float x,float y,float z) { out.setNormal(x,y,z);return this; }
}
