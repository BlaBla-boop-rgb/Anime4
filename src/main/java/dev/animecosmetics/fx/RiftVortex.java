package dev.animecosmetics.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.animecosmetics.AnimeCosmetics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class RiftVortex {
   static final Identifier TEX = AnimeCosmetics.id("textures/entity/fx/rift_vortex.png");
   static final Identifier TEX_IRIS = AnimeCosmetics.id("textures/entity/fx/rift_vortex_iris.png");
   static final float R0 = 0.5F;
   static final float R1 = 500.0F;
   static final float THROAT = 0.45F;
   static final float EYE_UP = 190.0F;
   static final float CEIL = 150.0F;
   static final float RIM = 45.0F;
   static final float DROOP = 0.7F;
   static final int SEG = 72;
   static final int RINGS = 24;
   static final float IN_S = 1.2F;
   static final float IN_SPREAD = 2.2F;
   static final float OUT_S = 3.0F;
   private static final float[][] LAYERS = new float[][]{{3.2F, -0.05F, 3.0F, 1.0F, 0.0F}, {4.8F, -0.08F, 5.0F, 0.8F, 12.0F}};
   static final float V_STRETCH = 0.36363637F;
   private static final List<RiftVortex.Vol> VOLS = new ArrayList<>();
   private static volatile float cloudHide;
   private static boolean skyFormat;
   private static boolean inside;
   private static boolean twoSided;
   public static volatile int debugCloudColor;
   public static volatile int cloudRenderCalls;
   private static final float NEAR_IRIS = 420.0F;
   private static final float V0 = 0.001953125F;
   private static final float[] COS = new float[73];
   private static final float[] SIN = new float[73];
   private static final float[][] RING = new float[2][219];
   private static final float[][] FADE = new float[2][73];

   public static float cloudHide() {
      return cloudHide;
   }

   public static float[][] cloudHoles() {
      if (!VOLS.isEmpty()) {
         long now = System.nanoTime();
         float[][] out = new float[VOLS.size()][];

         for (int i = 0; i < VOLS.size(); i++) {
            RiftVortex.Vol v = VOLS.get(i);
            out[i] = new float[]{(float)v.c.x, (float)v.c.z, 500.0F * v.shrink(now), v.strength(now)};
         }

         return out;
      } else {
         return new float[0][];
      }
   }

   public static String debug() {
      StringBuilder c = new StringBuilder();

      for (RiftVortex.Vol v : VOLS) {
         c.append(String.format(Locale.ROOT, " c=%.0f,%.0f,%.0f", v.c.x, v.c.y, v.c.z));
      }

      return String.format(
         Locale.ROOT,
         "vortex: vols %d%s hide %.2f cloudColor %08x cloudRenderCalls %d option %s",
         VOLS.size(),
         c,
         cloudHide,
         debugCloudColor,
         cloudRenderCalls,
         Minecraft.getInstance().options.cloudStatus().get()
      );
   }

   private RiftVortex() {
   }

   public static void clear() {
      VOLS.clear();
      cloudHide = 0.0F;
   }

   /**
    * Anime Cosmetics: the vortex no longer belongs to a rift. While {@code on}, one vortex hangs over the
    * camera (it moves with you like the sky); switched off, it fades out the way Ronova's did.
    */
   static void update(boolean on, Vec3 cam) {
      long now = System.nanoTime();
      RiftVortex.Vol live = null;

      for (RiftVortex.Vol v : VOLS) {
         if (v.endNs < 0L) {
            live = v;
         }
      }

      if (on) {
         if (live == null) {
            live = new RiftVortex.Vol();
            VOLS.add(live);
         }
      } else if (live != null) {
         live.endNs = now;
      }

      for (RiftVortex.Vol v : VOLS) {
         v.c = cam;
         v.inAge = 0.7F + (float)(now - v.t0) / 1.0E9F;
      }

      VOLS.removeIf(vx -> vx.endNs >= 0L && (float)(now - vx.endNs) / 1.0E9F > 3.0F);
   }

   static void submit(LevelRenderHooks.Context ctx, Vec3 cam) {
      if (!VOLS.isEmpty()) {
         long now = System.nanoTime();
         boolean iris = FxTypes.shadersOn();
         List<RiftVortex.Vol> vols = new ArrayList<>(VOLS);
         float hide = 0.0F;

         for (RiftVortex.Vol v : vols) {
            double d = Math.hypot(cam.x - v.c.x, cam.z - v.c.z);
            float near = 1.0F - RiftFx.smooth((float)((d - 400.0) / 250.0));
            hide = Math.max(hide, v.strength(now) * near);
         }

         cloudHide = hide;
         RenderType sky = iris ? RiftTypes.skyIris(TEX_IRIS) : null;
         RenderType type = sky != null ? sky : (iris ? RiftTypes.soft(TEX) : RiftTypes.skyDepthNoSort(TEX));
         skyFormat = sky != null;
         twoSided = iris && sky == null;
         ctx.submitNodeCollector().order(-2).submitCustomGeometry(new PoseStack(), type, (pose, buf) -> {
            for (RiftVortex.Vol v : vols) {
               float t = v.inAge - 0.7F;
               float out = v.out(now);
               if (!(t <= 0.0F) && !(out <= 0.002F)) {
                  float real = (float)(now - v.t0) / 1.0E9F;
                  float shrink = v.shrink(now);
                  float fadeOut = Math.min(1.0F, out * 3.0F);
                  boolean below = cam.y < v.c.y + 90.0;
                  inside = below && Math.hypot(cam.x - v.c.x, cam.z - v.c.z) < 500.0F * shrink * 0.95;

                  for (int i = 0; i < LAYERS.length; i++) {
                     int k = below ? LAYERS.length - 1 - i : i;
                     funnel(pose, buf, cam, v.c, t, real, shrink, fadeOut, LAYERS[k], iris);
                  }
               }
            }
         });
      } else {
         cloudHide = 0.0F;
      }
   }

   private static float radius(float f, float shrink) {
      return (0.5F + 499.5F * (float)Math.pow(f, 1.45)) * shrink;
   }

   private static float height(float f, float shrink) {
      float k = 0.4F + 0.6F * shrink;
      float toCeil = RiftFx.smooth(f / 0.45F);
      float toRim = RiftFx.smooth((f - 0.7F) / 0.3F);
      return (190.0F - 40.0F * toCeil - 105.0F * toRim) * k;
   }

   private static void ring(int slot, Vec3 c, Vec3 cam, float r, float y, boolean iris, float layerK) {
      float[] p = RING[slot];
      float[] h = FADE[slot];

      for (int j = 0; j <= 72; j++) {
         double dx = c.x + COS[j] * r - cam.x;
         double dy = c.y + y - cam.y;
         double dz = c.z + SIN[j] * r - cam.z;
         double d = Math.max(0.001, Math.sqrt(dx * dx + dy * dy + dz * dz));
         float k = iris ? (float)Math.min(1.0, 420.0F * layerK / d) : FarClamp.k(dx, dy, dz) * layerK;
         p[j * 3] = (float)(dx * k);
         p[j * 3 + 1] = (float)(dy * k);
         p[j * 3 + 2] = (float)(dz * k);
         h[j] = horizonFade((float)(dy / d));
      }
   }

   private static float edge(float f) {
      return 1.0F - (float)Math.pow(f, 4.0);
   }

   private static float vis(float f, float t) {
      return RiftFx.smooth((t - (1.0F - f) * 2.2F) / 1.2F);
   }

   private static float horizonFade(float sinElev) {
      return !inside ? 1.0F : RiftFx.smooth((sinElev - 0.05F) / 0.1F);
   }

   private static void vert(Pose pose, VertexConsumer buf, float[] p, int j, float u, float v, float a) {
      if (skyFormat) {
         buf.addVertex(pose, p[j * 3], p[j * 3 + 1], p[j * 3 + 2]).setUv(u, v).setColor(1.0F, 1.0F, 1.0F, a);
      } else {
         buf.addVertex(pose, p[j * 3], p[j * 3 + 1], p[j * 3 + 2])
            .setColor(1.0F, 1.0F, 1.0F, a)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
      }
   }

   private static void funnel(Pose pose, VertexConsumer buf, Vec3 cam, Vec3 c, float t, float real, float shrink, float fadeOut, float[] L, boolean iris) {
      float twist = L[0];
      float rep = L[2];
      float alpha = L[3] * fadeOut;
      float lift = L[4];
      float spin = real * L[1];
      float layerK = 1.0F - lift * 8.0E-4F;
      float flatA = alpha * RiftFx.smooth(t / 2.3000002F);
      if (!skyFormat || !(flatA <= 0.004F)) {
         ring(0, c, cam, radius(0.0F, shrink), height(0.0F, shrink) + lift, iris, layerK);

         for (int i = 0; i < 24; i++) {
            float f0 = i / 24.0F;
            float f1 = (i + 1) / 24.0F;
            int sa = i & 1;
            int sb = sa ^ 1;
            ring(sb, c, cam, radius(f1, shrink), height(f1, shrink) + lift, iris, layerK);
            float a0 = alpha * vis(f0, t) * edge(f0);
            float a1 = alpha * vis(f1, t) * edge(f1);
            if (skyFormat || !(a0 <= 0.004F) || !(a1 <= 0.004F)) {
               float[] pa = RING[sa];
               float[] pb = RING[sb];
               float[] ha = FADE[sa];
               float[] hb = FADE[sb];
               float u0 = twist * (1.0F - f0) + spin;
               float u1 = twist * (1.0F - f1) + spin;
               float v0 = 0.001953125F + f0 * 0.36363637F;
               float v1 = 0.001953125F + f1 * 0.36363637F;

               for (int j = 0; j < 72; j++) {
                  float ua = rep * j / 72.0F;
                  float ub = rep * (j + 1) / 72.0F;
                  float q0 = skyFormat ? flatA : a0 * ha[j];
                  float q1 = skyFormat ? flatA : a0 * ha[j + 1];
                  float q2 = skyFormat ? flatA : a1 * hb[j + 1];
                  float q3 = skyFormat ? flatA : a1 * hb[j];
                  if (!(Math.max(Math.max(q0, q1), Math.max(q2, q3)) <= 0.004F)) {
                     vert(pose, buf, pa, j, ua + u0, v0, q0);
                     vert(pose, buf, pa, j + 1, ub + u0, v0, q1);
                     vert(pose, buf, pb, j + 1, ub + u1, v1, q2);
                     vert(pose, buf, pb, j, ua + u1, v1, q3);
                     if (twoSided) {
                        vert(pose, buf, pb, j, ua + u1, v1, q3);
                        vert(pose, buf, pb, j + 1, ub + u1, v1, q2);
                        vert(pose, buf, pa, j + 1, ub + u0, v0, q1);
                        vert(pose, buf, pa, j, ua + u0, v0, q0);
                     }
                  }
               }
            }
         }
      }
   }

   static {
      for (int j = 0; j <= 72; j++) {
         COS[j] = (float)Math.cos((Math.PI * 2) * j / 72.0);
         SIN[j] = (float)Math.sin((Math.PI * 2) * j / 72.0);
      }
   }

   private static final class Vol {
      Vec3 c = Vec3.ZERO;
      final long t0 = System.nanoTime();
      long endNs = -1L;
      float inAge;

      float in() {
         return RiftFx.smooth((this.inAge - 0.7F) / 2.2F);
      }

      float out(long now) {
         return this.endNs < 0L ? 1.0F : 1.0F - RiftFx.smooth((float)(now - this.endNs) / 1.0E9F / 3.0F);
      }

      float shrink(long now) {
         return 0.3F + 0.7F * this.out(now);
      }

      float strength(long now) {
         return this.in() * Math.min(1.0F, this.out(now) * 3.0F);
      }
   }
}
