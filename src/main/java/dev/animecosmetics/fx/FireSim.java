package dev.animecosmetics.fx;

import java.util.Random;

final class FireSim {
   static final float[][] SCARLETS = new float[][]{
      {1.0F, 0.118F, 0.039F}, {0.925F, 0.078F, 0.102F}, {1.0F, 0.235F, 0.071F}, {0.863F, 0.031F, 0.157F}, {1.0F, 0.173F, 0.118F}, {0.949F, 0.275F, 0.157F}
   };
   private static final float[] DARK = new float[]{0.251F, 0.0F, 0.047F};
   private static final float[] CRIMSON = new float[]{0.48F, 0.0F, 0.06F};
   private static final float[] BLACK = new float[]{0.03F, 0.0F, 0.01F};
   final int nx;
   final int ny;
   final float dx;
   private float[] u;
   private float[] v;
   private float[] dr;
   private float[] dg;
   private float[] db;
   private float[] dl;
   private float[] ink;
   private float[] noise;
   private float[] u2;
   private float[] v2;
   private float[] t1;
   private float[] t2;
   private float[] t3;
   private float[] p;
   private float[] div;
   private float[] aw;
   private final float[] gx;
   private final float[] gy;
   private final float diss = 0.982F;
   private final float damp = 0.978F;
   private final float vort = 0.7F;
   private static final float VMAX = 45.0F;
   private final int jacobi = 16;
   static volatile float VIG_W = 0.22F;
   static volatile float VIG_P = 1.5F;

   FireSim(int nx, int ny, float widthBlocks, long seed) {
      this.nx = nx;
      this.ny = ny;
      this.dx = widthBlocks / nx;
      int n = nx * ny;
      this.u = new float[n];
      this.v = new float[n];
      this.dr = new float[n];
      this.dg = new float[n];
      this.db = new float[n];
      this.dl = new float[n];
      this.ink = new float[n];
      this.noise = new float[n];
      this.u2 = new float[n];
      this.v2 = new float[n];
      this.t1 = new float[n];
      this.t2 = new float[n];
      this.t3 = new float[n];
      this.p = new float[n];
      this.div = new float[n];
      this.aw = new float[n];
      this.gx = new float[n];
      this.gy = new float[n];
      Random rs = new Random(seed);

      for (int j = 0; j < ny; j++) {
         for (int i = 0; i < nx; i++) {
            int k = j * nx + i;
            this.gx[k] = (i + 0.5F - nx / 2.0F) * this.dx;
            this.gy[k] = (ny / 2.0F - j - 0.5F) * this.dx;
            this.noise[k] = rs.nextFloat();
         }
      }

      for (int it = 0; it < 2; it++) {
         this.blur(this.noise, this.t1);
      }

      float mn = 1.0F;
      float mx = 0.0F;

      for (float f : this.noise) {
         mn = Math.min(mn, f);
         mx = Math.max(mx, f);
      }

      for (int k = 0; k < n; k++) {
         this.noise[k] = (this.noise[k] - mn) / Math.max(1.0E-6F, mx - mn);
      }
   }

   private void blur(float[] f, float[] tmp) {
      for (int j = 0; j < this.ny; j++) {
         for (int i = 0; i < this.nx; i++) {
            int k = j * this.nx + i;
            tmp[k] = 0.25F
               * (
                  f[j * this.nx + Math.max(0, i - 1)]
                     + f[j * this.nx + Math.min(this.nx - 1, i + 1)]
                     + f[Math.max(0, j - 1) * this.nx + i]
                     + f[Math.min(this.ny - 1, j + 1) * this.nx + i]
               );
         }
      }

      System.arraycopy(tmp, 0, f, 0, f.length);
   }

   void ribbon(float ox, float oy, float ang, float len, float w, float speed, float[] col, boolean dark, float bright) {
      this.ribbon(ox, oy, ang, len, w, speed, col, dark, bright, 1.0F);
   }

   void ribbon(float ox, float oy, float ang, float len, float w, float speed, float[] col, boolean dark, float bright, float amt) {
      float cs = (float)Math.cos(ang);
      float sn = (float)Math.sin(ang);

      for (int k = 0; k < this.gx.length; k++) {
         float pr = ((this.gx[k] - ox) * cs + (this.gy[k] - oy) * sn) / len;
         pr = pr < 0.0F ? 0.0F : Math.min(1.0F, pr);
         float ddx = this.gx[k] - (ox + pr * len * cs);
         float ddy = this.gy[k] - (oy + pr * len * sn);
         float wdt = w * 0.5F * (0.35F + 0.65F * (float)Math.pow(1.0F - pr, 0.7));
         float g = (float)Math.exp(-(ddx * ddx + ddy * ddy) / (2.0F * wdt * wdt));
         if (!(g < 0.01F)) {
            float drag = Math.min(1.0F, g * 0.7F) * amt;
            float sp = speed * (0.6F + 0.6F * pr);
            this.u[k] = this.u[k] * (1.0F - drag) + cs * sp * drag;
            this.v[k] = this.v[k] * (1.0F - drag) + sn * sp * drag;
            if (dark) {
               this.ink[k] = Math.max(this.ink[k], g * 1.3F * amt);
            } else {
               this.dr[k] = Math.max(this.dr[k], g * col[0] * 1.3F * amt);
               this.dg[k] = Math.max(this.dg[k], g * col[1] * 1.3F * amt);
               this.db[k] = Math.max(this.db[k], g * col[2] * 1.3F * amt);
               this.dl[k] = Math.max(this.dl[k], g * 1.3F * bright * amt);
            }
         }
      }
   }

   void emit(float t, float strength, float speed) {
      if (!(strength <= 0.0F)) {
         float[] c1 = SCARLETS[0];
         float[] c2 = SCARLETS[3];

         for (int k = 0; k < this.gx.length; k++) {
            float ex = this.gx[k] / 4.6F;
            float ey = this.gy[k] / 2.0F;
            float re = (float)Math.hypot(ex, ey);
            if (!(re > 3.2F)) {
               float th = (float)Math.atan2(ey, ex);
               float ring = (float)Math.exp(-((re - 1.02F) / 0.25F) * ((re - 1.02F) / 0.25F));
               float mod = 0.5F
                  + 0.5F
                     * (float)((Math.sin(3.0F * th + 0.9 * t) + 0.7 * Math.sin(5.0F * th - 1.3 * t + 1.0) + 0.5 * Math.sin(8.0F * th + 2.1 * t + 2.3)) / 2.2);
               mod = (float)Math.pow(Math.max(0.0F, Math.min(1.0F, mod)), 1.1);
               float src = ring * mod * strength;
               float rl = (float)Math.hypot(this.gx[k], this.gy[k]) + 1.0E-6F;
               float nx_ = this.gx[k] / rl;
               float ny_ = this.gy[k] / rl;
               if (src > 0.01F) {
                  float hk = 0.5F + 0.5F * (float)Math.sin(2.0F * th + 0.6 * t);
                  this.dr[k] = Math.max(this.dr[k], src * (c1[0] * hk + c2[0] * (1.0F - hk)));
                  this.dg[k] = Math.max(this.dg[k], src * (c1[1] * hk + c2[1] * (1.0F - hk)));
                  this.db[k] = Math.max(this.db[k], src * (c1[2] * hk + c2[2] * (1.0F - hk)));
                  float bl = (float)Math.pow(Math.max(0.0, 0.5 + 0.5 * Math.sin(6.0F * th + 1.7 * t + 0.9)), 6.0);
                  this.dl[k] = Math.max(this.dl[k], src * (0.3F + 0.7F * bl));
                  float dm = (float)Math.pow(Math.max(0.0, 0.5 + 0.5 * Math.sin(4.0F * th - 0.7 * t + 0.4)), 4.0);
                  this.ink[k] = Math.max(this.ink[k], src * dm * 0.6F);
               }

               float sp = speed * (0.8F + 0.4F * mod);
               float drag = Math.min(1.0F, ring * 0.35F);
               this.u[k] = this.u[k] * (1.0F - drag) + nx_ * sp * drag;
               this.v[k] = this.v[k] * (1.0F - drag) + ny_ * sp * drag;
               float far = Math.max(0.0F, Math.min(1.0F, (re - 1.0F) / 0.4F)) * Math.max(0.0F, Math.min(1.0F, (3.0F - re) / 1.0F)) * 0.06F;
               this.u[k] = this.u[k] * (1.0F - far) + nx_ * speed * 1.1F * far;
               this.v[k] = this.v[k] * (1.0F - far) + ny_ * speed * 1.1F * far;
            }
         }
      }
   }

   private float sample(float[] f, float x, float y) {
      x = Math.max(0.0F, Math.min(this.nx - 1.001F, x));
      y = Math.max(0.0F, Math.min(this.ny - 1.001F, y));
      int x0 = (int)x;
      int y0 = (int)y;
      float fx = x - x0;
      float fy = y - y0;
      int k = y0 * this.nx + x0;
      return f[k] * (1.0F - fx) * (1.0F - fy) + f[k + 1] * fx * (1.0F - fy) + f[k + this.nx] * (1.0F - fx) * fy + f[k + this.nx + 1] * fx * fy;
   }

   private void advect(float[] f, float[] out, float dt, float keep) {
      for (int j = 0; j < this.ny; j++) {
         for (int i = 0; i < this.nx; i++) {
            int k = j * this.nx + i;
            out[k] = this.sample(f, i - this.u[k] * dt / this.dx, j + this.v[k] * dt / this.dx) * keep;
         }
      }
   }

   void step(float dt) {
      int n = this.nx * this.ny;

      for (int j = 0; j < this.ny; j++) {
         for (int i = 0; i < this.nx; i++) {
            int k = j * this.nx + i;
            int l = j * this.nx + Math.max(0, i - 1);
            int r = j * this.nx + Math.min(this.nx - 1, i + 1);
            int up = Math.max(0, j - 1) * this.nx + i;
            int dn = Math.min(this.ny - 1, j + 1) * this.nx + i;
            float w = 0.5F * (this.v[r] - this.v[l]) + 0.5F * (this.u[dn] - this.u[up]);
            this.div[k] = w;
            this.aw[k] = Math.abs(w);
         }
      }

      for (int j = 0; j < this.ny; j++) {
         for (int i = 0; i < this.nx; i++) {
            int k = j * this.nx + i;
            int l = j * this.nx + Math.max(0, i - 1);
            int r = j * this.nx + Math.min(this.nx - 1, i + 1);
            int up = Math.max(0, j - 1) * this.nx + i;
            int dn = Math.min(this.ny - 1, j + 1) * this.nx + i;
            float ggx = 0.5F * (this.aw[r] - this.aw[l]);
            float ggy = -0.5F * (this.aw[dn] - this.aw[up]);
            float gl = (float)Math.hypot(ggx, ggy) + 1.0E-5F;
            this.u[k] = this.u[k] + 0.7F * (ggy / gl) * this.div[k] * dt * 60.0F;
            this.v[k] = this.v[k] + -0.7F * (ggx / gl) * this.div[k] * dt * 60.0F;
         }
      }

      this.advect(this.u, this.u2, dt, 0.978F);
      this.advect(this.v, this.v2, dt, 0.978F);
      float[] tu = this.u;
      this.u = this.u2;
      this.u2 = tu;
      float[] tv = this.v;
      this.v = this.v2;
      this.v2 = tv;

      for (int j = 0; j < this.ny; j++) {
         for (int i = 0; i < this.nx; i++) {
            int k = j * this.nx + i;
            int l = j * this.nx + Math.max(0, i - 1);
            int r = j * this.nx + Math.min(this.nx - 1, i + 1);
            int up = Math.max(0, j - 1) * this.nx + i;
            int dn = Math.min(this.ny - 1, j + 1) * this.nx + i;
            this.div[k] = 0.5F * (this.u[r] - this.u[l] - (this.v[dn] - this.v[up]));
            this.p[k] = 0.0F;
         }
      }

      for (int it = 0; it < 16; it++) {
         for (int j = 0; j < this.ny; j++) {
            for (int i = 0; i < this.nx; i++) {
               int k = j * this.nx + i;
               int l = j * this.nx + Math.max(0, i - 1);
               int r = j * this.nx + Math.min(this.nx - 1, i + 1);
               int up = Math.max(0, j - 1) * this.nx + i;
               int dn = Math.min(this.ny - 1, j + 1) * this.nx + i;
               this.t1[k] = 0.25F * (this.p[l] + this.p[r] + this.p[up] + this.p[dn] - this.div[k]);
            }
         }

         float[] tp = this.p;
         this.p = this.t1;
         this.t1 = tp;
      }

      for (int j = 0; j < this.ny; j++) {
         for (int i = 0; i < this.nx; i++) {
            int k = j * this.nx + i;
            int l = j * this.nx + Math.max(0, i - 1);
            int r = j * this.nx + Math.min(this.nx - 1, i + 1);
            int up = Math.max(0, j - 1) * this.nx + i;
            int dn = Math.min(this.ny - 1, j + 1) * this.nx + i;
            this.u[k] = this.u[k] - 0.5F * (this.p[r] - this.p[l]);
            this.v[k] = this.v[k] + 0.5F * (this.p[dn] - this.p[up]);
            if (i != 0 && i != this.nx - 1 && j != 0 && j != this.ny - 1) {
               float sp2 = this.u[k] * this.u[k] + this.v[k] * this.v[k];
               if (sp2 > 2025.0F) {
                  float kk = 45.0F / (float)Math.sqrt(sp2);
                  this.u[k] = this.u[k] * kk;
                  this.v[k] = this.v[k] * kk;
               }
            } else {
               this.u[k] = 0.0F;
               this.v[k] = 0.0F;
            }
         }
      }

      this.advect(this.dr, this.t1, dt, 0.982F);
      this.advect(this.dg, this.t2, dt, 0.982F);
      this.advect(this.db, this.t3, dt, 0.982F);
      float[] a = this.dr;
      this.dr = this.t1;
      this.t1 = a;
      a = this.dg;
      this.dg = this.t2;
      this.t2 = a;
      a = this.db;
      this.db = this.t3;
      this.t3 = a;
      this.advect(this.dl, this.t1, dt, 0.982F);
      a = this.dl;
      this.dl = this.t1;
      this.t1 = a;
      this.advect(this.ink, this.t1, dt, 0.978F);
      a = this.ink;
      this.ink = this.t1;
      this.t1 = a;
      this.advect(this.noise, this.t2, dt, 1.0F);
      a = this.noise;
      this.noise = this.t2;
      this.t2 = a;

      for (int j = 0; j < this.ny; j++) {
         for (int ix = 0; ix < this.nx; ix++) {
            if (ix < 2 || ix >= this.nx - 2 || j < 2 || j >= this.ny - 2) {
               int k = j * this.nx + ix;
               this.dr[k] = this.dg[k] = this.db[k] = this.dl[k] = this.ink[k] = 0.0F;
            }
         }
      }
   }

   private static float smooth(float x) {
      x = x < 0.0F ? 0.0F : Math.min(1.0F, x);
      return x * x * (3.0F - 2.0F * x);
   }

   void render(int[] out, int W, int H, float dissolve, float opacity, float soft) {
      float e = 0.03F;

      for (int j = 0; j < H; j++) {
         float sy = (j + 0.5F) * this.ny / H - 0.5F;

         for (int i = 0; i < W; i++) {
            float sx = (i + 0.5F) * this.nx / W - 0.5F;
            float r_ = this.sample(this.dr, sx, sy);
            float g_ = this.sample(this.dg, sx, sy);
            float b_ = this.sample(this.db, sx, sy);
            float dens = Math.max(r_, Math.max(g_, b_));
            float br = dens > 1.0E-4F ? Math.max(0.0F, Math.min(1.0F, this.sample(this.dl, sx, sy) / dens)) : 0.0F;
            float ex = (i + 0.5F) / W;
            float ey = (j + 0.5F) / H;
            float ox = (ex - 0.5F) * 2.0F;
            float oy = (ey - 0.5F) * 2.0F;
            float od = (float)Math.sqrt(ox * ox + oy * oy);
            float vig = (float)Math.pow(smooth((1.0F - od) / VIG_W), VIG_P);
            float inkv = Math.min(1.5F, this.sample(this.ink, sx, sy));
            float nz = this.sample(this.noise, sx, sy);
            float thr = 0.22F + 0.7F * dissolve + 0.22F * (nz - 0.5F);
            float body = Math.max(0.0F, Math.min(1.0F, (dens - (thr + 0.28F)) / e));
            float shade = Math.max(0.0F, Math.min(1.0F, (dens - thr) / e));
            float hr = dens > 1.0E-4F ? r_ / dens : 0.0F;
            float hg = dens > 1.0E-4F ? g_ / dens : 0.0F;
            float hb = dens > 1.0E-4F ? b_ / dens : 0.0F;
            float lum = 0.28F + 0.72F * br;
            float rr = Math.min(1.0F, hr * 1.05F) * lum;
            float rg = Math.min(1.0F, hg * 1.05F) * lum;
            float rb = Math.min(1.0F, hb * 1.05F) * lum;
            float shd = 0.55F + 0.45F * br;
            float cr = CRIMSON[0] * shd * (1.0F - body) + rr * body;
            float cg = CRIMSON[1] * shd * (1.0F - body) + rg * body;
            float cb = CRIMSON[2] * shd * (1.0F - body) + rb * body;
            float ib = Math.max(0.0F, Math.min(1.0F, (inkv - (thr + 0.3F)) / e));
            float ish = Math.max(0.0F, Math.min(1.0F, (inkv - thr) / e));
            float icr = CRIMSON[0] * (1.0F - ib) + BLACK[0] * ib;
            float icg = CRIMSON[1] * (1.0F - ib) + BLACK[1] * ib;
            float icb = CRIMSON[2] * (1.0F - ib) + BLACK[2] * ib;
            float w = ish / Math.max(shade + ish, 1.0E-4F);
            cr = cr * (1.0F - w) + icr * w;
            cg = cg * (1.0F - w) + icg * w;
            cb = cb * (1.0F - w) + icb * w;
            float a = Math.max(shade, ish);
            if (soft > 0.0F) {
               float d1 = Math.min(1.0F, dens);
               float sa = (float)Math.pow(smooth((dens - 0.08F) / 0.45F), 1.1);
               float sbr = 0.55F + 0.45F * (float)Math.sqrt(d1);
               float sr = rr * sbr;
               float sg = rg * sbr;
               float sb = rb * sbr;
               float sink = Math.min(1.0F, inkv * 1.4F);
               sr = sr * (1.0F - sink * 0.8F) + CRIMSON[0] * sink * 0.8F;
               sg = sg * (1.0F - sink * 0.8F) + CRIMSON[1] * sink * 0.8F;
               sb = sb * (1.0F - sink * 0.8F) + CRIMSON[2] * sink * 0.8F;
               sa = Math.max(sa, sink * 0.8F);
               cr = cr * (1.0F - soft) + sr * soft;
               cg = cg * (1.0F - soft) + sg * soft;
               cb = cb * (1.0F - soft) + sb * soft;
               a = a * (1.0F - soft) + sa * soft;
            }

            a *= opacity * vig;
            a = a < 0.12F ? a * a / 0.12F : a;
            int ia = Math.max(0, Math.min(255, (int)(a * 255.0F + 0.5F)));
            out[j * W + i] = ia << 24 | clamp8(cr) << 16 | clamp8(cg) << 8 | clamp8(cb);
         }
      }
   }

   void overlayCoronaRings(int[] out, int W, int H, float t, float tRed, float cor, float world) {
      float wb = this.nx * this.dx;
      float hb = this.ny * this.dx;

      for (int k = 0; k < out.length; k++) {
         float px = (k % W + 0.5F) / W - 0.5F;
         float py = 0.5F - (k / W + 0.5F) / H;
         float bx = px * wb;
         float by = py * hb;
         float rr = (float)Math.hypot(bx / 1.9F, by);
         float ring = 0.0F;
         int k0 = Math.max(0, (int)Math.floor((t - tRed - 0.35F - 1.3F) / 0.55F));

         for (int kk = k0; kk < 400; kk++) {
            float tb = tRed + 0.35F + 0.55F * kk;
            if (tb > t) {
               break;
            }

            float uu = (t - tb) / 1.3F;
            if (uu < 1.0F) {
               float R = 1.2F + 6.5F * (1.0F - (1.0F - uu) * (1.0F - uu));
               float band = (float)Math.pow(Math.max(0.0F, 1.0F - Math.abs(rr - R) / (0.35F + 0.6F * uu)), 0.5);
               ring = Math.max(ring, band * 0.22F * (float)Math.pow(1.0F - uu, 1.5) * world);
            }
         }

         float ca = 0.0F;
         if (cor > 0.01F && rr > 2.3F) {
            float th = (float)Math.atan2(by, bx / 1.9F);
            double st = Math.sin(23.0F * th + 0.3 + (rr * 1.9 - t * 5.0))
               + Math.sin(37.0F * th + 2.1 + (rr * 2.6 - t * 6.5))
               + Math.sin(59.0F * th + 4.0 + (rr * 3.1 - t * 8.0))
               + Math.sin(17.0F * th + 1.1 + (rr * 1.4 - t * 4.0));
            float sv = (float)Math.pow(Math.max(0.0, Math.min(1.0, 0.5 + 0.5 * st / 2.2)), 2.4);
            float radial = smooth((rr - 3.0F) / 1.6F) * (float)Math.pow(Math.max(0.0F, Math.min(1.0F, 1.0F - (rr - 3.0F) / 7.0F)), 1.6);
            ca = sv * radial * 0.42F * cor;
         }

         float vx = (k % W + 0.5F) / W;
         float vy = (k / W + 0.5F) / H;
         float ba = Math.max(ring, ca) * smooth(Math.min(Math.min(vx, 1.0F - vx), Math.min(vy, 1.0F - vy)) / 0.22F);
         if (!(ba <= 0.003F)) {
            int fire = out[k];
            float fa = (fire >>> 24) / 255.0F;
            float fr = (fire >> 16 & 0xFF) / 255.0F;
            float fg = (fire >> 8 & 0xFF) / 255.0F;
            float fb = (fire & 0xFF) / 255.0F;
            float br = 0.95F;
            float bg = 0.13F;
            float bb = 0.07F;
            float oa = fa + ba * (1.0F - fa);
            float orr = (fr * fa + br * ba * (1.0F - fa)) / Math.max(oa, 1.0E-4F);
            float og = (fg * fa + bg * ba * (1.0F - fa)) / Math.max(oa, 1.0E-4F);
            float ob = (fb * fa + bb * ba * (1.0F - fa)) / Math.max(oa, 1.0E-4F);
            out[k] = clamp8(oa) << 24 | clamp8(orr) << 16 | clamp8(og) << 8 | clamp8(ob);
         }
      }
   }

   private static int clamp8(float v) {
      return Math.max(0, Math.min(255, (int)(v * 255.0F + 0.5F)));
   }
}
