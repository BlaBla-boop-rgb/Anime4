package dev.animecosmetics.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public final class SkyEye {
   private static final float[] BASE_C = new float[]{5.0E-4F, 1.0005F, 0.9995F, 1.4995F};
   private static final float[] LID_C = new float[]{5.0E-4F, 1.5005F, 0.9995F, 1.9995F};
   private static final float[] IRIS_C = new float[]{0.5005F, 0.7505F, 0.7495F, 0.9995F};
   private static final float[] PET_C = new float[]{0.7505F, 0.7505F, 0.9995F, 0.9995F};
   private static final float[] CORE_C = new float[]{0.2505F, 0.5005F, 0.3745F, 0.6245F};
   private static final float[][] FIRE = new float[][]{
      {9.0F, 6.0F, 3.14F, 0.0F, 0.02F},
      {-7.0F, 9.0F, 4.19F, 1.3F, 0.018F},
      {12.0F, -3.0F, 2.09F, 2.6F, 0.014F},
      {-4.0F, 13.0F, 5.24F, 0.7F, 0.016F},
      {14.0F, 10.0F, 4.19F, 3.9F, 0.012F},
      {3.0F, 16.0F, 6.28F, 2.0F, 0.01F}
   };
   private static Vec3 cam = Vec3.ZERO;
   private static boolean doubleSided = true;
   private static float[] skyBlend;
   private static final float detail = 1.0F;

   private SkyEye() {
   }

   static float smooth(float x) {
      x = x < 0.0F ? 0.0F : Math.min(1.0F, x);
      return x * x * (3.0F - 2.0F * x);
   }

   static float hash(int seed, int i, int salt) {
      long h = seed * -7046029254386353131L ^ i * -4417276706812531889L ^ salt * 1609587929392839161L;
      h ^= h >>> 33;
      h *= -49064778989728563L;
      h ^= h >>> 33;
      return (float)(h >>> 40) / 1.6777216E7F;
   }

   private static int lodN(int full, int min) {
      return full;
   }

   private static void gridQuad(
      Pose pose,
      VertexConsumer buf,
      SkyEye.Rift r,
      float[] cell,
      float cx,
      float cy,
      float z,
      float hw,
      float hh,
      int nx,
      int ny,
      float red,
      float g,
      float b,
      float a
   ) {
      if (!(a <= 0.004F)) {
         int var26 = 1;
         int var27 = 1;
         float[] col = new float[]{red, g, b, a};

         for (int j = 0; j < var27; j++) {
            for (int i = 0; i < var26; i++) {
               float gx0 = -1.0F + 2.0F * i / var26;
               float gx1 = -1.0F + 2.0F * (i + 1) / var26;
               float gy0 = -1.0F + 2.0F * j / var27;
               float gy1 = -1.0F + 2.0F * (j + 1) / var27;
               float u0 = cell[0] + (cell[2] - cell[0]) * (gx0 + 1.0F) / 2.0F;
               float u1 = cell[0] + (cell[2] - cell[0]) * (gx1 + 1.0F) / 2.0F;
               float v0 = cell[1] + (cell[3] - cell[1]) * (1.0F - gy0) / 2.0F;
               float v1 = cell[1] + (cell[3] - cell[1]) * (1.0F - gy1) / 2.0F;
               quadV(
                  pose,
                  buf,
                  r.at(cx + gx0 * hw, cy + gy0 * hh, z),
                  r.at(cx + gx1 * hw, cy + gy0 * hh, z),
                  r.at(cx + gx1 * hw, cy + gy1 * hh, z),
                  r.at(cx + gx0 * hw, cy + gy1 * hh, z),
                  new float[]{u0, v0, u1, v0, u1, v1, u0, v1},
                  col,
                  col,
                  col,
                  col
               );
            }
         }
      }
   }

   public static void drawSkyEye(
      Pose pose, VertexConsumer buf, Vec3 camPos, Vec3 dir, float dist, float halfW, float open, float t, int seed, boolean twoSided, float[] tint
   ) {
      cam = camPos;
      boolean ds = doubleSided;
      doubleSided = twoSided;
      skyBlend = tint;

      try {
         SkyEye.Rift r = new SkyEye.Rift(camPos.add(dir.scale(dist)), dir.scale(-1.0), seed, true);
         r.zSign = 1.0F;
         r.gazeU = 0.04F * (float)Math.sin(t * 0.23 + seed);
         r.gazeV = 0.03F * (float)Math.sin(t * 0.17 + seed * 0.7);
         float s = halfW / 10.7008F;
         drawEye(pose, buf, r, 0.0F, 0.0F, s, open, t, 1.0F, 0.0F, seed);
      } finally {
         doubleSided = ds;
         skyBlend = null;
      }
   }

   static void drawEye(Pose pose, VertexConsumer buf, SkyEye.Rift r, float cx, float cy, float s, float open, float t, float alpha, float z, int seed) {
      if (!(open <= 0.002F) && !(alpha <= 0.004F)) {
         float bx = cx + -0.209F * s;
         float by = cy + -0.1045F * s;
         SkyEye.Frame f = new SkyEye.Frame(r, bx, by, 0.0F, 5.3504F * s, 2.6752F * s * open, DeathEyeShape.TOP, DeathEyeShape.BOT);
         float in = smooth(open * 2.0F);
         float gu = r.gazeU * 1.3F * s;
         float gv = r.gazeV * 0.5F * s;
         float pExtra = 0.3936F * s * 0.6F;
         float pu = gu + r.gazeU * pExtra;
         float pv = gv + r.gazeV * pExtra;
         int gx = lodN(28, 8);
         int gy = lodN(10, 4);
         gridQuad(pose, buf, r, BASE_C, bx, by, z, 5.3504F * s, 2.6752F * s * open, gx, gy, 1.0F, 1.0F, 1.0F, alpha);
         drawIris(
            pose,
            buf,
            f,
            -0.11490001F * s + gu,
            -0.010399997F * s + gv,
            1.7312F * s,
            2.09F * s,
            IRIS_C,
            lodN(s < 0.5F ? 32 : 48, 12),
            t,
            alpha * in,
            z + 0.12F * s
         );
         drawPetals(pose, buf, f, s, t, alpha * in, z + 0.22F * s, pu, pv);
         drawCore(
            pose,
            buf,
            f,
            -0.12539999F * s + pu,
            0.031400003F * s + pv,
            0.6688F * s,
            0.5073F * s,
            lodN(s < 0.5F ? 24 : 48, 10),
            t,
            alpha * in,
            z + 0.32F * s,
            seed
         );
         gridQuad(pose, buf, r, LID_C, bx, by, z + 0.42F * s, 5.3504F * s, 2.6752F * s * open, gx, gy, 1.0F, 1.0F, 1.0F, alpha);
      }
   }

   private static float wave(double a, float t) {
      return (float)(Math.sin(7.0 * a - 2.1 * t) + 0.6 * Math.sin(11.0 * a + 3.1 * t) + 0.35 * Math.sin(4.0 * a - 1.05 * t + 1.3));
   }

   static void drawIris(
      Pose pose, VertexConsumer buf, SkyEye.Frame f, float ux, float uy, float R, float cellH, float[] cell, int na, float t, float a, float z
   ) {
      float uc = (cell[0] + cell[2]) / 2.0F;
      float vc = (cell[1] + cell[3]) / 2.0F;
      float uh = (cell[2] - cell[0]) / 2.0F;
      float vh = (cell[3] - cell[1]) / 2.0F;
      float ri = 0.55F * R;
      Vec3 ctr = f.at(ux, f.clip(ux, uy), z);
      float[] white = new float[]{1.0F, 1.0F, 1.0F, a};

      for (int i = 0; i < na; i++) {
         double a0 = i * Math.PI * 2.0 / na;
         double a1 = (i + 1) * Math.PI * 2.0 / na;
         float c0 = (float)Math.cos(a0);
         float s0 = (float)Math.sin(a0);
         float c1 = (float)Math.cos(a1);
         float s1 = (float)Math.sin(a1);
         Vec3 p0 = f.at(ux + c0 * ri, f.clip(ux + c0 * ri, uy + s0 * ri), z);
         Vec3 p1 = f.at(ux + c1 * ri, f.clip(ux + c1 * ri, uy + s1 * ri), z);
         float[] w0c = new float[]{1.0F, 1.0F, 1.0F, a * f.clipA(ux + c0 * ri, uy + s0 * ri)};
         float[] w1c = new float[]{1.0F, 1.0F, 1.0F, a * f.clipA(ux + c1 * ri, uy + s1 * ri)};
         float ti = ri / cellH;
         quadV(
            pose,
            buf,
            ctr,
            ctr,
            p1,
            p0,
            new float[]{uc, vc, uc, vc, uc + c1 * ti * uh, vc - s1 * ti * vh, uc + c0 * ti * uh, vc - s0 * ti * vh},
            white,
            white,
            w1c,
            w0c
         );
         float w0 = wave(a0, t);
         float w1 = wave(a1, t);
         float ro0 = R * (1.0F + 0.028F * w0);
         float ro1 = R * (1.0F + 0.028F * w1);
         float to = R / cellH;
         Vec3 q0 = f.at(ux + c0 * ro0, f.clip(ux + c0 * ro0, uy + s0 * ro0), z);
         Vec3 q1 = f.at(ux + c1 * ro1, f.clip(ux + c1 * ro1, uy + s1 * ro1), z);
         float l0 = Math.min(1.0F, 0.9F + 0.1F * w0);
         float l1 = Math.min(1.0F, 0.9F + 0.1F * w1);
         float qa0 = a * f.clipA(ux + c0 * ro0, uy + s0 * ro0);
         float qa1 = a * f.clipA(ux + c1 * ro1, uy + s1 * ro1);
         quadV(
            pose,
            buf,
            p0,
            p1,
            q1,
            q0,
            new float[]{
               uc + c0 * ti * uh,
               vc - s0 * ti * vh,
               uc + c1 * ti * uh,
               vc - s1 * ti * vh,
               uc + c1 * to * uh,
               vc - s1 * to * vh,
               uc + c0 * to * uh,
               vc - s0 * to * vh
            },
            w0c,
            w1c,
            new float[]{l1, l1, l1, qa1},
            new float[]{l0, l0, l0, qa0}
         );
      }
   }

   private static void drawPetals(Pose pose, VertexConsumer buf, SkyEye.Frame f, float s, float t, float a, float z, float ox, float oy) {
      float pcx = -0.19849999F * s + ox;
      float pcy = 0.020900004F * s + oy;
      float h = 1.3376F * s;
      float fcx = -0.12539999F * s + ox;
      float fcy = 0.031400003F * s + oy;
      float pr = h * 0.9F;
      float uc = (PET_C[0] + PET_C[2]) / 2.0F;
      float vc = (PET_C[1] + PET_C[3]) / 2.0F;
      float uh = (PET_C[2] - PET_C[0]) / 2.0F;
      float vh = (PET_C[3] - PET_C[1]) / 2.0F;
      int n = lodN(s < 0.5F ? 6 : 12, 3);
      Vec3[] pts = new Vec3[(n + 1) * (n + 1)];
      float[] al = new float[pts.length];

      for (int j = 0; j <= n; j++) {
         for (int i = 0; i <= n; i++) {
            float gx = -1.0F + 2.0F * i / n;
            float gy = -1.0F + 2.0F * j / n;
            float x = pcx + gx * h;
            float y = pcy + gy * h;
            float dx = x - fcx;
            float dy = y - fcy;
            float rootW = smooth((float)Math.hypot(dx, dy) / pr / 0.45F);
            float px = 0.0F;
            float py = 0.0F;
            float dens = 0.0F;

            for (float[] fi : FIRE) {
               float k = (float)Math.hypot(fi[0], fi[1]);
               double arg = (fi[0] * dx + fi[1] * dy) / pr + fi[2] * t + fi[3];
               float c = (float)Math.cos(arg);
               px += fi[4] * pr * fi[0] / k * c;
               py += fi[4] * pr * fi[1] / k * c;
               dens += (float)Math.sin(arg + 0.9);
            }

            float vx = x + px * rootW;
            pts[j * (n + 1) + i] = f.at(vx, f.clip(vx, y + py * rootW), z);
            al[j * (n + 1) + i] = Math.max(0.45F, Math.min(1.0F, 0.78F + 0.28F * dens / 2.45F * rootW)) * a * f.clipA(vx, y + py * rootW);
         }
      }

      for (int j = 0; j < n; j++) {
         for (int i = 0; i < n; i++) {
            int k00 = j * (n + 1) + i;
            int k10 = k00 + 1;
            int k01 = k00 + n + 1;
            int k11 = k01 + 1;
            float u0 = uc + (-1.0F + 2.0F * i / n) * uh;
            float u1 = uc + (-1.0F + 2.0F * (i + 1) / n) * uh;
            float v0 = vc - (-1.0F + 2.0F * j / n) * vh;
            float v1 = vc - (-1.0F + 2.0F * (j + 1) / n) * vh;
            quadV(
               pose,
               buf,
               pts[k00],
               pts[k10],
               pts[k11],
               pts[k01],
               new float[]{u0, v0, u1, v0, u1, v1, u0, v1},
               new float[]{1.0F, 1.0F, 1.0F, al[k00]},
               new float[]{1.0F, 1.0F, 1.0F, al[k10]},
               new float[]{1.0F, 1.0F, 1.0F, al[k11]},
               new float[]{1.0F, 1.0F, 1.0F, al[k01]}
            );
         }
      }
   }

   private static void drawCore(
      Pose pose, VertexConsumer buf, SkyEye.Frame f, float ux, float uy, float h, float tipR, int na, float t, float a, float z, int seed
   ) {
      float uc = (CORE_C[0] + CORE_C[2]) / 2.0F;
      float vc = (CORE_C[1] + CORE_C[3]) / 2.0F;
      float uh = (CORE_C[2] - CORE_C[0]) / 2.0F;
      float vh = (CORE_C[3] - CORE_C[1]) / 2.0F;
      int nr = 3;
      float[] spikes = DeathEyeShape.SPIKES;
      float[] ln = new float[spikes.length];
      float[] tw = new float[spikes.length];

      for (int i = 0; i < spikes.length; i++) {
         float w1 = 1.05F + 4.2F * hash(seed, i, 11);
         float w2 = 1.05F + 4.2F * hash(seed, i, 12);
         float w3 = 1.05F + 3.1F * hash(seed, i, 13);
         float w4 = 1.05F + 3.1F * hash(seed, i, 14);
         ln[i] = 0.3F * (float)Math.sin(w1 * t + hash(seed, i, 15) * 6.283F) + 0.18F * (float)Math.sin(w2 * t + hash(seed, i, 16) * 6.283F);
         tw[i] = 0.16F * (float)Math.sin(w3 * t + hash(seed, i, 17) * 6.283F) + 0.08F * (float)Math.sin(w4 * t + hash(seed, i, 18) * 6.283F);
      }

      float pulse = 1.0F + 0.02F * (float)Math.sin(t * 6.3);
      Vec3[] pts = new Vec3[(na + 1) * (nr + 1)];
      float[] uv = new float[pts.length * 2];
      float[] ca = new float[pts.length];

      for (int i = 0; i <= na; i++) {
         double ang = i * Math.PI * 2.0 / na;
         float st = 0.0F;
         float tws = 0.0F;
         float tot = 1.0E-6F;

         for (int k = 0; k < spikes.length; k++) {
            double d = Math.atan2(Math.sin(ang - spikes[k]), Math.cos(ang - spikes[k]));
            float w = (float)Math.exp(-(d / 0.192) * (d / 0.192));
            st += ln[k] * w;
            tws += tw[k] * w;
            tot += w;
         }

         st /= tot;
         tws /= tot;

         for (int k = 0; k <= nr; k++) {
            float rt = h * k / nr;
            float cw = smooth(rt / (0.9F * tipR));
            float rr = rt * (1.0F + st * cw) * pulse;
            double an = ang + tws * cw;
            float x = ux + (float)Math.cos(an) * rr;
            float y = uy + (float)Math.sin(an) * rr;
            int idx = i * (nr + 1) + k;
            pts[idx] = f.at(x, f.clip(x, y), z);
            ca[idx] = f.clipA(x, y);
            uv[idx * 2] = uc + (float)Math.cos(ang) * rt / h * uh;
            uv[idx * 2 + 1] = vc - (float)Math.sin(ang) * rt / h * vh;
         }
      }

      float[] var100001 = new float[]{1.0F, 1.0F, 1.0F, a};

      for (int i = 0; i < na; i++) {
         for (int k = 0; k < nr; k++) {
            int i00 = i * (nr + 1) + k;
            int i01 = i00 + 1;
            int i10 = i00 + nr + 1;
            int i11 = i10 + 1;
            quadV(
               pose,
               buf,
               pts[i00],
               pts[i10],
               pts[i11],
               pts[i01],
               new float[]{uv[i00 * 2], uv[i00 * 2 + 1], uv[i10 * 2], uv[i10 * 2 + 1], uv[i11 * 2], uv[i11 * 2 + 1], uv[i01 * 2], uv[i01 * 2 + 1]},
               new float[]{1.0F, 1.0F, 1.0F, a * ca[i00]},
               new float[]{1.0F, 1.0F, 1.0F, a * ca[i10]},
               new float[]{1.0F, 1.0F, 1.0F, a * ca[i11]},
               new float[]{1.0F, 1.0F, 1.0F, a * ca[i01]}
            );
         }
      }
   }

   private static void vtx(Pose pose, VertexConsumer buf, Vec3 p, float u, float v, float r, float g, float b, float a) {
      if (skyBlend != null) {
         float k = skyBlend[3];
         r += (skyBlend[0] - r) * k;
         g += (skyBlend[1] - g) * k;
         b += (skyBlend[2] - b) * k;
      }

      buf.addVertex(pose, (float)(p.x - cam.x), (float)(p.y - cam.y), (float)(p.z - cam.z))
         .setColor(r, g, b, a)
         .setUv(u, v * 0.36363637F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   static void quadUv(
      Pose pose,
      VertexConsumer buf,
      Vec3 p0,
      Vec3 p1,
      Vec3 p2,
      Vec3 p3,
      float u0,
      float v0,
      float u1,
      float v1,
      float u2,
      float v2,
      float u3,
      float v3,
      float r,
      float g,
      float b,
      float a
   ) {
      if (!(a <= 0.004F)) {
         vtx(pose, buf, p0, u0, v0, r, g, b, a);
         vtx(pose, buf, p1, u1, v1, r, g, b, a);
         vtx(pose, buf, p2, u2, v2, r, g, b, a);
         vtx(pose, buf, p3, u3, v3, r, g, b, a);
         if (doubleSided) {
            vtx(pose, buf, p3, u3, v3, r, g, b, a);
            vtx(pose, buf, p2, u2, v2, r, g, b, a);
            vtx(pose, buf, p1, u1, v1, r, g, b, a);
            vtx(pose, buf, p0, u0, v0, r, g, b, a);
         }
      }
   }

   static void quadV(Pose pose, VertexConsumer buf, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float[] uv, float[] c0, float[] c1, float[] c2, float[] c3) {
      if (!(Math.max(Math.max(c0[3], c1[3]), Math.max(c2[3], c3[3])) <= 0.004F)) {
         vtx(pose, buf, p0, uv[0], uv[1], c0[0], c0[1], c0[2], c0[3]);
         vtx(pose, buf, p1, uv[2], uv[3], c1[0], c1[1], c1[2], c1[3]);
         vtx(pose, buf, p2, uv[4], uv[5], c2[0], c2[1], c2[2], c2[3]);
         vtx(pose, buf, p3, uv[6], uv[7], c3[0], c3[1], c3[2], c3[3]);
         if (doubleSided) {
            vtx(pose, buf, p3, uv[6], uv[7], c3[0], c3[1], c3[2], c3[3]);
            vtx(pose, buf, p2, uv[4], uv[5], c2[0], c2[1], c2[2], c2[3]);
            vtx(pose, buf, p1, uv[2], uv[3], c1[0], c1[1], c1[2], c1[3]);
            vtx(pose, buf, p0, uv[0], uv[1], c0[0], c0[1], c0[2], c0[3]);
         }
      }
   }

   static final class Frame {
      final SkyEye.Rift r;
      final float cx;
      final float cy;
      final float cs;
      final float sn;
      final float hl;
      final float hh;
      final float[] top;
      final float[] bot;

      Frame(SkyEye.Rift r, float cx, float cy, float rot, float hl, float hh, float[] top, float[] bot) {
         this.r = r;
         this.cx = cx;
         this.cy = cy;
         this.cs = (float)Math.cos(rot);
         this.sn = (float)Math.sin(rot);
         this.hl = hl;
         this.hh = hh;
         this.top = top;
         this.bot = bot;
      }

      Vec3 at(float u, float v, double z) {
         return this.r.at(this.cx + u * this.cs - v * this.sn, this.cy + u * this.sn + v * this.cs, z);
      }

      float clipA(float u, float v) {
         float xn = Math.max(-1.0F, Math.min(1.0F, u / this.hl));
         float over = Math.max(v - prof(this.top, xn) * this.hh, -prof(this.bot, xn) * this.hh - v);
         return over <= 0.0F ? 1.0F : Math.max(0.0F, 1.0F - over / 0.12F);
      }

      float clip(float u, float v) {
         float xn = Math.max(-1.0F, Math.min(1.0F, u / this.hl));
         return Math.max(-prof(this.bot, xn) * this.hh, Math.min(prof(this.top, xn) * this.hh, v));
      }

      private static float prof(float[] a, float x) {
         float f = Math.max(0.0F, Math.min((float)(a.length - 1), (x + 1.0F) / 2.0F * (a.length - 1)));
         int k = Math.min(a.length - 2, (int)f);
         return a[k] + (a[k + 1] - a[k]) * (f - k);
      }
   }

   static final class Rift {
      final Vec3 c;
      final Vec3 f;
      final Vec3 x;
      final Vec3 y;
      final int seed;
      float zSign = 1.0F;
      float gazeU;
      float gazeV;

      Rift(Vec3 c, Vec3 f, int seed, boolean light) {
         this.c = c;
         this.f = f.normalize();
         Vec3 up = new Vec3(0.0, 1.0, 0.0);
         Vec3 side = up.cross(this.f);
         this.x = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         this.y = this.f.cross(this.x).normalize();
         this.seed = seed;
      }

      Vec3 at(double px, double py, double pz) {
         return this.c.add(this.x.scale(px * 2.0)).add(this.y.scale(py * 2.0)).add(this.f.scale(pz * 2.0 * this.zSign));
      }
   }
}
