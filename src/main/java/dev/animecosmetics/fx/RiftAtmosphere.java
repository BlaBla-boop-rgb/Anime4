package dev.animecosmetics.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public final class RiftAtmosphere {
   static final float R_DARK = 450.0F;
   static final float R_TINT = 210.0F;
   static final int N_DARK = 20;
   static final int N_TINT = 10;
   static final int N_DARK_OLD = 20;
   static final int N_TINT_OLD = 10;
   static final int SKIP_DARK = 0;
   static final int SKIP_TINT = 0;
   static final float CORE_BRIGHT = 0.22F;
   static final float CORE_TINT = 0.35F;
   static final float IN_S = 1.0F;
   static final float OUT_S = 3.0F;
   static final float START_AT = 0.2F;
   static final float RED_BOOST = 1.0F;
   static final float RED_DARK = 0.5F;
   static final float BLUE_CUT = 1.35F;
   private static final int LAT = 12;
   private static final int LON = 24;
   private static final int FULL = 15728880;
   private static final float K_DARK = solve(20, 0, 0.22F, true);
   private static final float K_TINT = solve(10, 0, 0.35F, false);
   private static final float K_DARK_OLD = solve(20, 0, 0.22F, true);
   private static final float K_TINT_OLD = solve(10, 0, 0.35F, false);
   private static final float U_WHITE = (RiftFx.GLOW[0] + RiftFx.GLOW[2]) / 2.0F;
   private static final float V_WHITE = (RiftFx.GLOW[1] + RiftFx.GLOW[3]) / 2.0F;
   private static final float R_IRIS = pow15Inv(0.6666667F);
   private static final List<RiftAtmosphere.Vol> VOLS = new ArrayList<>();
   static final int N_BL_DARK = 8;
   static final int N_BL_TINT = 8;
   static final float BL_CORE_BRIGHT = 0.55F;
   static final float BL_CORE_TINT = 0.25F;
   static final float BL_IN = 0.4F;
   static final float BL_HOLD = 0.25F;
   static final float BL_OUT = 0.7F;
   private static final float K_BL_DARK = solve(8, 0, 0.55F, true);
   private static final float K_BL_TINT = solve(8, 0, 0.25F, false);
   private static final List<RiftAtmosphere.Blast> BLASTS = new ArrayList<>();
   private static final float[] UNIT = new float[975];
   private static final int ST = 8;
   private static final float[] V;

   private static float solve(int n, int skip, float core, boolean linear) {
      float lo = 0.0F;
      float hi = 1.0F;

      for (int it = 0; it < 60; it++) {
         float k = (lo + hi) / 2.0F;
         float p = 1.0F;

         for (int i = skip; i < n; i++) {
            float f = 1.0F - k * weight(i, n, linear);
            p *= f * f;
         }

         if (p > core) {
            lo = k;
         } else {
            hi = k;
         }
      }

      return (lo + hi) / 2.0F;
   }

   private static float weight(int i, int n, boolean linear) {
      float x = 1.0F - (float)i / n;
      return linear ? x : x * x;
   }

   private static float pow15(float x) {
      return x - x * (1.0F - x) * (1.0F - x);
   }

   private static float pow15Inv(float y) {
      y = Math.max(0.0F, Math.min(1.0F, y));
      float lo = 0.0F;
      float hi = 1.0F;

      for (int i = 0; i < 24; i++) {
         float m = (lo + hi) / 2.0F;
         if (pow15(m) < y) {
            lo = m;
         } else {
            hi = m;
         }
      }

      return (lo + hi) / 2.0F;
   }

   public static void blast(Vec3 c, float r, float delayS) {
      RiftAtmosphere.Blast b = new RiftAtmosphere.Blast();
      b.c = c;
      b.r = r;
      b.t0 = System.nanoTime() + (long)(delayS * 1.0E9);
      BLASTS.add(b);
   }

   private RiftAtmosphere() {
   }

   public static boolean active() {
      return !VOLS.isEmpty() || !BLASTS.isEmpty();
   }

   public static void clear() {
      VOLS.clear();
      BLASTS.clear();
   }

   /**
    * Anime Cosmetics: the red atmosphere no longer belongs to a rift. While {@code on}, it surrounds the
    * camera; switched off, it fades out like Ronova's did.
    */
   static void update(boolean on, Vec3 cam) {
      long now = System.nanoTime();
      RiftAtmosphere.Vol live = null;

      for (RiftAtmosphere.Vol v : VOLS) {
         if (v.endNs < 0L) {
            live = v;
         }
      }

      if (on) {
         if (live == null) {
            live = new RiftAtmosphere.Vol();
            VOLS.add(live);
         }
      } else if (live != null) {
         live.endNs = now;
      }

      for (RiftAtmosphere.Vol v : VOLS) {
         v.c = cam;
         v.inAge = (float)(now - v.t0) / 1.0E9F;
      }

      VOLS.removeIf(vx -> vx.endNs >= 0L && (float)(now - vx.endNs) / 1.0E9F > 3.0F);
   }

   static void submit(LevelRenderHooks.Context ctx, Vec3 cam) {
      if (!VOLS.isEmpty() || !BLASTS.isEmpty()) {
         long now = System.nanoTime();
         boolean iris = FxTypes.shadersOn();
         List<float[]> vols = new ArrayList<>();
         BLASTS.removeIf(b -> (float)(now - b.t0) / 1.0E9F > 1.3499999F);

         for (RiftAtmosphere.Blast b : BLASTS) {
            float t = (float)(now - b.t0) / 1.0E9F;
            if (!(t < 0.0F)) {
               float gx = Math.min(1.0F, t / 0.4F);
               float grow = 1.0F - (float)Math.pow(1.0F - gx, 2.5);
               float out = t < 0.65F ? 1.0F : 1.0F - RiftFx.smooth((t - 0.4F - 0.25F) / 0.7F);
               float s = grow * Math.min(1.0F, out * 4.0F);
               if (!(s <= 0.002F) && !(out <= 0.002F)) {
                  vols.add(new float[]{(float)b.c.x, (float)b.c.y, (float)b.c.z, s, 1.0F, grow * out * b.r, 1.0F});
               }
            }
         }

         for (RiftAtmosphere.Vol v : VOLS) {
            float in = RiftFx.smooth((v.inAge - 0.2F) / 1.0F);
            float out = v.endNs < 0L ? 1.0F : 1.0F - RiftFx.smooth((float)(now - v.endNs) / 1.0E9F / 3.0F);
            float s = in * Math.min(1.0F, out * 4.0F);
            if (!(s <= 0.002F) && !(out <= 0.002F)) {
               float t = (float)(now - v.t0) / 1.0E9F;
               float pulse = 0.9F + 0.1F * (float)Math.sin(t * 2.0F * Math.PI / 1.6);
               vols.add(new float[]{(float)v.c.x, (float)v.c.y, (float)v.c.z, s, pulse, out, 0.0F});
            }
         }

         if (!vols.isEmpty()) {
            ctx.submitNodeCollector()
               .order(-3)
               .submitCustomGeometry(
                  new PoseStack(), iris ? RiftTypes.volumeIris(RiftFx.TEX) : RiftTypes.volumeMul(RiftFx.TEX), (pose, buf) -> {
                     for (float[] vx : vols) {
                        if (vx[6] > 0.5F) {
                           for (int i = 0; i < 8; i++) {
                              float k = K_BL_DARK * weight(i, 8, true) * vx[3];
                              shell(pose, buf, cam, vx[0], vx[1], vx[2], vx[5] * 1.6F * (i + 1) / 8.0F, k, 0.0F, iris);
                           }

                           for (int i = 0; i < 8; i++) {
                              float k = K_BL_TINT * weight(i, 8, false) * vx[3];
                              shell(pose, buf, cam, vx[0], vx[1], vx[2], vx[5] * (i + 1) / 8.0F, 0.0F, k, iris);
                           }
                        } else {
                           for (int i = 0; i < 20; i++) {
                              float k1 = K_DARK * weight(i, 20, true) * vx[3];
                              float k2 = i < 10 ? K_TINT * weight(i, 10, false) * vx[3] * vx[4] : 0.0F;
                              shell(pose, buf, cam, vx[0], vx[1], vx[2], 450.0F * (i + 1) / 20.0F * vx[5], k1, k2, iris);
                           }
                        }
                     }
                  }
               );
         }
      }
   }

   private static void shell(Pose pose, VertexConsumer buf, Vec3 cam, float cx, float cy, float cz, float rad, float kd, float kt, boolean iris) {
      float ox = (float)(cam.x - cx);
      float oy = (float)(cam.y - cy);
      float oz = (float)(cam.z - cz);
      boolean outside = ox * ox + oy * oy + oz * oz > rad * rad * 1.0404F;
      int q = 0;

      for (int n = 325; q < n; q++) {
         float nx = UNIT[q * 3];
         float ny = UNIT[q * 3 + 1];
         float nz = UNIT[q * 3 + 2];
         float px = nx * rad;
         float py = ny * rad;
         float pz = nz * rad;
         float vx = ox - px;
         float vy = oy - py;
         float vz = oz - pz;
         float vl = (float)Math.sqrt(vx * vx + vy * vy + vz * vz);
         float dot = nx * vx + ny * vy + nz * vz;
         float rim = vl < 1.0E-4F ? 1.0F : Math.abs(dot / vl);
         float wd = kd * rim;
         float wt = kt * rim;
         if (outside) {
            wd = 1.0F - (1.0F - wd) * (1.0F - wd);
            wt = 1.0F - (1.0F - wt) * (1.0F - wt);
         }

         int o = q * 8;
         V[o] = px - ox;
         V[o + 1] = py - oy;
         V[o + 2] = pz - oz;
         if (iris) {
            float a = 1.0F - (1.0F - wd) * (1.0F - wt);
            V[o + 3] = a > 1.0E-5F ? R_IRIS * wt / a : 0.0F;
            V[o + 4] = 0.0F;
            V[o + 5] = 0.0F;
            V[o + 6] = a;
         } else {
            V[o + 3] = Math.min(1.0F, (1.0F - wd * 0.5F) * (1.0F + wt * 1.0F) / 2.0F);
            V[o + 4] = (1.0F - wd) * (1.0F - wt) / 2.0F;
            V[o + 5] = (1.0F - wd) * Math.max(0.0F, 1.0F - wt * 1.35F) / 2.0F;
            V[o + 6] = 1.0F;
         }

         V[o + 7] = dot > 0.0F ? 1.0F : 0.0F;
      }

      for (int i = 0; i < 12; i++) {
         for (int j = 0; j < 24; j++) {
            int a = i * 25 + j;
            int b = a + 1;
            int c = a + 24 + 2;
            int d = a + 24 + 1;
            if (!outside || V[a * 8 + 7] + V[b * 8 + 7] + V[c * 8 + 7] + V[d * 8 + 7] != 0.0F) {
               emit(pose, buf, a);
               emit(pose, buf, b);
               emit(pose, buf, c);
               emit(pose, buf, d);
            }
         }
      }
   }

   private static void emit(Pose pose, VertexConsumer buf, int q) {
      int o = q * 8;
      buf.addVertex(pose, V[o], V[o + 1], V[o + 2])
         .setColor(V[o + 3], V[o + 4], V[o + 5], V[o + 6])
         .setUv(U_WHITE, V_WHITE * 0.36363637F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   static {
      for (int i = 0; i <= 12; i++) {
         double th = Math.PI * i / 12.0;

         for (int j = 0; j <= 24; j++) {
            double ph = (Math.PI * 2) * j / 24.0;
            int o = (i * 25 + j) * 3;
            UNIT[o] = (float)(Math.sin(th) * Math.cos(ph));
            UNIT[o + 1] = (float)Math.cos(th);
            UNIT[o + 2] = (float)(Math.sin(th) * Math.sin(ph));
         }
      }

      V = new float[2600];
   }

   private static final class Blast {
      Vec3 c;
      long t0;
      float r;
   }

   private static final class Vol {
      Vec3 c = Vec3.ZERO;
      final long t0 = System.nanoTime();
      long endNs = -1L;
      float inAge;
   }
}
