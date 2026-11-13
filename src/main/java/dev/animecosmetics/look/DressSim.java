package dev.animecosmetics.look;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DressSim {
   public static float AIR = 13.5F;
   public static float OMEGA_ROOT = 30.0F;
   public static float OMEGA_FALL = 0.8F;
   public static float OMEGA_HEM = 11.0F;
   public static float FLOAT_SPREAD = 0.25F;
   public static float MOON_OMEGA = 0.6F;
   public static float MOON_AIR = 0.5F;
   public static float SWAY_PX = 1.4F;
   public static float SWAY_HZ = 0.32F;
   public static float HANG_DOWN = 0.7F;
   private static final float AIR_T = 0.12F;
   private static final float HANG_MAX = 0.9F;
   public static float HANG = 0.9F;
   private static final float PIVOT_Y = 10.0F;
   private static final float HANG_LEN = 16.0F;
   public static float GROUND_FRICTION = 0.2F;
   public static float LAYER_LIFT = 0.025F;
   private static final float H = 0.008333334F;
   private static final int ITER = 5;
   private final float[] restV;
   private final int[] wBone;
   private final float[] wVal;
   private final int nodes;
   private final float[] restN;
   private final int[] parent;
   private final boolean[] fixed;
   private final float[] len;
   private final float[] omega2;
   private final float[] hangN;
   private final float[] floorN;
   private static final float FEET = 24.0F;
   private final int[] link;
   private final float[] linkMax;
   private static final float LINK_SLACK = 1.04F;
   public static float SQUASH = 0.6F;
   private final float hangRoom;
   private final float[] hang;
   private final float[] rest01;
   private final int bones;
   private final int[] boneHead;
   private final int[] boneNext;
   private final float[] restR;
   private float[] restNrm;
   private float[] outNrm;
   private static final float HOVER = 0.15F;
   private final float[] pos;
   private final float[] prev;
   private boolean started;
   private float air;
   private double lastT;
   private final float[] lastM = new float[9];
   private double lex;
   private double ley;
   private double lez;
   private float lastH;
   private final float[] lastA = new float[9];
   private final float[] lastC = new float[3];
   private final DressSim.Config cfg;
   private float[] rootRel;
   private float[] rootOff;
   private float[] skinRot;
   private float[] skinHeadU;
   private float[] skinHeadR;

   public DressSim(float[] v, float[][] w, float[][] heads, int[] parents) {
      this(v, w, heads, parents, new DressSim.Config());
   }

   public void setRootFrame(float[] rel, float[] off) {
      this.rootRel = rel;
      this.rootOff = off;
   }

   public DressSim(float[] v, float[][] w, float[][] heads, int[] parents, DressSim.Config cfg) {
      this.cfg = cfg;
      int nv = v.length / 3;
      this.restV = (float[])v.clone();
      this.wBone = new int[nv * 4];
      this.wVal = new float[nv * 4];
      Arrays.fill(this.wBone, -1);

      for (int i = 0; i < nv; i++) {
         for (int k = 0; k < 4 && k * 2 + 1 < w[i].length; k++) {
            this.wBone[i * 4 + k] = (int)w[i][k * 2];
            this.wVal[i * 4 + k] = w[i][k * 2 + 1];
         }
      }

      this.bones = heads.length;
      int[] childCount = new int[this.bones];

      for (int b = 0; b < this.bones; b++) {
         if (parents[b] >= 0) {
            childCount[parents[b]]++;
         }
      }

      int leaves = 0;

      for (int bx = 0; bx < this.bones; bx++) {
         if (childCount[bx] == 0) {
            leaves++;
         }
      }

      this.nodes = this.bones + leaves;
      this.restN = new float[this.nodes * 3];
      this.parent = new int[this.nodes];
      this.boneHead = new int[this.bones];
      this.boneNext = new int[this.bones];
      int[] level = new int[this.nodes];

      for (int bxx = 0; bxx < this.bones; this.boneHead[bxx] = bxx++) {
         this.restN[bxx * 3] = heads[bxx][0];
         this.restN[bxx * 3 + 1] = heads[bxx][1];
         this.restN[bxx * 3 + 2] = heads[bxx][2];
         this.parent[bxx] = parents[bxx];
      }

      for (int bxx = 0; bxx < this.bones; bxx++) {
         int l = 0;

         for (int p = parents[bxx]; p >= 0; p = parents[p]) {
            l++;
         }

         level[bxx] = l;
      }

      int tip = this.bones;

      for (int bxx = 0; bxx < this.bones; bxx++) {
         if (childCount[bxx] <= 0) {
            double sx = 0.0;
            double sy = 0.0;
            double sz = 0.0;
            double sw = 0.0;

            for (int i = 0; i < nv; i++) {
               for (int k = 0; k < 4; k++) {
                  if (this.wBone[i * 4 + k] == bxx && this.wVal[i * 4 + k] >= 0.4F) {
                     sx += v[i * 3] * this.wVal[i * 4 + k];
                     sy += v[i * 3 + 1] * this.wVal[i * 4 + k];
                     sz += v[i * 3 + 2] * this.wVal[i * 4 + k];
                     sw += this.wVal[i * 4 + k];
                  }
               }
            }

            float hx = heads[bxx][0];
            float hy = heads[bxx][1];
            float hz = heads[bxx][2];
            float cx = sw > 0.0 ? (float)(sx / sw) : hx;
            float cy = sw > 0.0 ? (float)(sy / sw) : hy + 3.0F;
            float cz = sw > 0.0 ? (float)(sz / sw) : hz;
            float dx = (cx - hx) * 1.6F;
            float dy = (cy - hy) * 1.6F;
            float dz = (cz - hz) * 1.6F;
            if (dx * dx + dy * dy + dz * dz < 1.0F) {
               dy = 3.0F;
            }

            this.restN[tip * 3] = hx + dx;
            this.restN[tip * 3 + 1] = hy + dy;
            this.restN[tip * 3 + 2] = hz + dz;
            this.parent[tip] = bxx;
            level[tip] = level[bxx] + 1;
            this.boneNext[bxx] = tip++;
         }
      }

      for (int bxxx = 0; bxxx < this.bones; bxxx++) {
         if (childCount[bxxx] != 0) {
            int best = -1;
            float bestD = -1.0F;

            for (int c = 0; c < this.bones; c++) {
               if (parents[c] == bxxx) {
                  float d = dist(this.restN, c, bxxx);
                  if (d > bestD) {
                     bestD = d;
                     best = c;
                  }
               }
            }

            this.boneNext[bxxx] = best;
         }
      }

      this.fixed = new boolean[this.nodes];

      for (int n = 0; n < this.nodes; n++) {
         this.fixed[n] = this.parent[n] < 0 || this.restN[n * 3 + 1] < cfg.pinAboveY;
      }

      this.len = new float[this.nodes];
      this.omega2 = new float[this.nodes];
      this.hang = new float[this.nodes];
      this.rest01 = new float[this.nodes];

      for (int n = 0; n < this.nodes; n++) {
         this.len[n] = this.parent[n] >= 0 ? dist(this.restN, n, this.parent[n]) : 0.0F;
         float om = Math.max(OMEGA_HEM, OMEGA_ROOT * (float)Math.pow(OMEGA_FALL, Math.max(0, level[n] - 1)));
         this.omega2[n] = om * om;
         float kx = Math.max(0.0F, Math.min(1.0F, (this.restN[n * 3 + 1] - cfg.pivotY) / cfg.hangLen));
         this.hang[n] = cfg.hangMax * kx * kx * (3.0F - 2.0F * kx);
         this.rest01[n] = this.ellipse(this.restN[n * 3], this.restN[n * 3 + 2]);
      }

      this.hangN = (float[])this.restN.clone();

      for (int pass = 0; pass < 12; pass++) {
         for (int n = 0; n < this.nodes; n++) {
            int p = this.parent[n];
            if (p >= 0 && !this.fixed[n]) {
               float dx = this.restN[n * 3] - this.restN[p * 3];
               float dy = this.restN[n * 3 + 1] - this.restN[p * 3 + 1];
               float dz = this.restN[n * 3 + 2] - this.restN[p * 3 + 2];
               float l = this.len[n] + 1.0E-6F;
               float kx = Math.max(0.0F, Math.min(1.0F, (this.restN[n * 3 + 1] - cfg.hangFromY) / cfg.hangFromLen));
               float f = cfg.hangDown * kx * kx * (3.0F - 2.0F * kx);
               float rx = this.restN[n * 3];
               float rz = this.restN[n * 3 + 2];
               float rl = (float)Math.sqrt(rx * rx + rz * rz) + 1.0E-6F;
               float sp = cfg.floatSpread * kx;
               float nx = dx / l * (1.0F - f) + rx / rl * sp;
               float ny = dy / l * (1.0F - f) + f;
               float nz = dz / l * (1.0F - f) + rz / rl * sp;
               float nl = (float)Math.sqrt(nx * nx + ny * ny + nz * nz) + 1.0E-6F;
               this.hangN[n * 3] = this.hangN[p * 3] + nx / nl * l;
               this.hangN[n * 3 + 1] = this.hangN[p * 3 + 1] + ny / nl * l;
               this.hangN[n * 3 + 2] = this.hangN[p * 3 + 2] + nz / nl * l;
            }
         }
      }

      if (!Float.isNaN(cfg.hugBack)) {
         float[] h0 = (float[])this.hangN.clone();

         for (int nx = 0; nx < this.nodes; nx++) {
            int p = this.parent[nx];
            if (p >= 0 && !this.fixed[nx]) {
               float tz = h0[nx * 3 + 2] > cfg.hugBack ? cfg.hugBack + (h0[nx * 3 + 2] - cfg.hugBack) * cfg.hugKeep : h0[nx * 3 + 2];
               float dx = h0[nx * 3] - this.hangN[p * 3];
               float dy = h0[nx * 3 + 1] - this.hangN[p * 3 + 1];
               float dz = tz - this.hangN[p * 3 + 2];
               float dl = (float)Math.sqrt(dx * dx + dy * dy + dz * dz) + 1.0E-6F;
               this.hangN[nx * 3] = this.hangN[p * 3] + dx / dl * this.len[nx];
               this.hangN[nx * 3 + 1] = this.hangN[p * 3 + 1] + dy / dl * this.len[nx];
               this.hangN[nx * 3 + 2] = this.hangN[p * 3 + 2] + dz / dl * this.len[nx];
            }
         }
      }

      this.floorN = (float[])this.restN.clone();

      for (int pass = 0; pass < 12; pass++) {
         for (int nxx = 0; nxx < this.nodes; nxx++) {
            int p = this.parent[nxx];
            if (p >= 0 && !this.fixed[nxx]) {
               float dx = this.restN[nxx * 3] - this.restN[p * 3];
               float dy = this.restN[nxx * 3 + 1] - this.restN[p * 3 + 1];
               float dz = this.restN[nxx * 3 + 2] - this.restN[p * 3 + 2];
               float px0 = this.floorN[p * 3];
               float py0 = this.floorN[p * 3 + 1];
               float pz0 = this.floorN[p * 3 + 2];
               float l = this.len[nxx];
               float ny = py0 + dy;
               if (ny > 24.0F) {
                  float down = Math.max(0.0F, 24.0F - py0);
                  float hl = (float)Math.sqrt(dx * dx + dz * dz) + 1.0E-6F;
                  float rest = (float)Math.sqrt(Math.max(0.0F, l * l - down * down));
                  this.floorN[nxx * 3] = px0 + dx / hl * rest;
                  this.floorN[nxx * 3 + 1] = py0 + down;
                  this.floorN[nxx * 3 + 2] = pz0 + dz / hl * rest;
               } else {
                  this.floorN[nxx * 3] = px0 + dx;
                  this.floorN[nxx * 3 + 1] = ny;
                  this.floorN[nxx * 3 + 2] = pz0 + dz;
               }
            }
         }
      }

      float[] share = new float[this.bones * this.bones];

      for (int i = 0; i < nv; i++) {
         for (int k1 = 0; k1 < 4; k1++) {
            for (int k2 = k1 + 1; k2 < 4; k2++) {
               int b1 = this.wBone[i * 4 + k1];
               int b2 = this.wBone[i * 4 + k2];
               if (b1 >= 0 && b2 >= 0) {
                  float m = Math.min(this.wVal[i * 4 + k1], this.wVal[i * 4 + k2]);
                  share[b1 * this.bones + b2] = share[b1 * this.bones + b2] + m;
                  share[b2 * this.bones + b1] = share[b2 * this.bones + b1] + m;
               }
            }
         }
      }

      List<int[]> pairs = new ArrayList<>();

      for (int b1 = 0; b1 < this.bones; b1++) {
         for (int b2 = b1 + 1; b2 < this.bones; b2++) {
            if (!(share[b1 * this.bones + b2] < 1.5F) && parents[b1] != b2 && parents[b2] != b1) {
               int[][] cand = new int[][]{{this.boneHead[b1], this.boneHead[b2]}, {this.boneNext[b1], this.boneNext[b2]}};

               for (int[] pr : cand) {
                  if (pr[0] != pr[1] && (this.parent[pr[0]] >= 0 || this.parent[pr[1]] >= 0)) {
                     pairs.add(pr);
                  }
               }
            }
         }
      }

      this.link = new int[pairs.size() * 2];
      this.linkMax = new float[pairs.size()];

      for (int i = 0; i < pairs.size(); i++) {
         int na = pairs.get(i)[0];
         int nb = pairs.get(i)[1];
         this.link[i * 2] = na;
         this.link[i * 2 + 1] = nb;
         this.linkMax[i] = Math.max(dist(this.restN, na, nb), Math.max(dist(this.floorN, na, nb), dist(this.hangN, na, nb))) * 1.04F;
      }

      float low = 28.0F;

      for (int nxxx = 0; nxxx < this.nodes; nxxx++) {
         low = Math.max(low, this.hangN[nxxx * 3 + 1]);
      }

      this.hangRoom = low - 24.0F;
      this.restR = new float[nv];

      for (int i = 0; i < nv; i++) {
         this.restR[i] = (float)Math.sqrt(v[i * 3] * v[i * 3] + v[i * 3 + 2] * v[i * 3 + 2]);
      }

      this.pos = new float[this.nodes * 3];
      this.prev = new float[this.nodes * 3];
      this.skinRot = new float[this.bones * 9];
      this.skinHeadU = new float[this.bones * 3];
      this.skinHeadR = new float[this.bones * 3];
   }

   private static float dist(float[] p, int a, int b) {
      float dx = p[a * 3] - p[b * 3];
      float dy = p[a * 3 + 1] - p[b * 3 + 1];
      float dz = p[a * 3 + 2] - p[b * 3 + 2];
      return (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private float ellipse(float bx, float bz) {
      float ex = bx / this.cfg.ellX;
      float ez = bz / this.cfg.ellZ;
      return ex * ex + ez * ez;
   }

   public float[] nodes() {
      return this.pos;
   }

   public void setNormals(float[] n) {
      this.restNrm = (float[])n.clone();
      this.outNrm = new float[n.length];
   }

   public float[] normals() {
      return this.outNrm;
   }

   public int linkCount() {
      return this.linkMax.length;
   }

   public int nodeCount() {
      return this.nodes;
   }

   public void step(float[] aIn, float[] cIn, float[] m, double ex, double ey, double ez, double t, DressSim.Ground ground, float[] out) {
      if (!this.started || t < this.lastT - 0.01 || t - this.lastT > 0.5 || sq(ex - this.lex) + sq(ey - this.ley) + sq(ez - this.lez) > 64.0) {
         this.reset(aIn, cIn, m, ex, ey, ez, t);
      }

      if (t - this.lastT < 1.0E-6) {
         this.skin(aIn, cIn, ground, out);
      } else {
         double ox = this.lex;
         double oy = this.ley;
         double oz = this.lez;
         float[] m0 = (float[])this.lastM.clone();
         float[] a0 = (float[])this.lastA.clone();
         float[] c0 = (float[])this.lastC.clone();
         float[] a = new float[9];
         float[] c = new float[3];
         float[] ms = new float[9];
         float[] tmp = new float[3];
         double dtAll = Math.min(0.1, t - this.lastT);
         this.lastT = t;
         int steps = (int)Math.ceil(dtAll / 0.008333334F);
         float h = steps > 0 ? (float)(dtAll / steps) : 0.0F;
         float damp = (float)Math.exp(-AIR * (1.0F - (1.0F - MOON_AIR) * this.air) * h);
         float moon = 1.0F - (1.0F - MOON_OMEGA * MOON_OMEGA) * this.air;
         float swayT = (float)((Math.PI * 2) * SWAY_HZ * t);
         float fr = (float)Math.pow(1.0F - GROUND_FRICTION, h * 60.0F);
         float height = ground.px(aIn[1] * 10.0F + cIn[0], aIn[7] * 10.0F + cIn[2]) - (aIn[4] * 10.0F + cIn[1] + 24.0F - 10.0F);
         float shapeMix = Math.max(this.air, this.cfg.groundHang);
         float airTarget = Math.max(0.0F, Math.min(1.0F, height / this.hangRoom));
         this.air = this.air + (airTarget * airTarget * (3.0F - 2.0F * airTarget) - this.air) * (1.0F - (float)Math.exp(-dtAll / 0.12F));

         for (int s = 0; s < steps; s++) {
            float f = (s + 1.0F) / steps;

            for (int k = 0; k < 9; k++) {
               ms[k] = m0[k] + (m[k] - m0[k]) * f;
               a[k] = a0[k] + (aIn[k] - a0[k]) * f;
            }

            for (int k = 0; k < 3; k++) {
               c[k] = c0[k] + (cIn[k] - c0[k]) * f;
            }

            double sx = ox + (ex - ox) * f;
            double sy = oy + (ey - oy) * f;
            double sz = oz + (ez - oz) * f;

            for (int n = 0; n < this.nodes; n++) {
               if (!this.fixed[n]) {
                  this.toNewU(this.pos, n, ms, sx, sy, sz, tmp);
                  this.toNewU(this.prev, n, ms, sx, sy, sz, tmp);
               }
            }

            System.arraycopy(ms, 0, this.lastM, 0, 9);
            this.lex = sx;
            this.ley = sy;
            this.lez = sz;
            float pvx = a[1] * this.cfg.pivotY + c[0];
            float pvy = a[4] * this.cfg.pivotY + c[1];
            float pvz = a[7] * this.cfg.pivotY + c[2];
            float scale = (s == 0 && this.lastH > 0.0F ? h / this.lastH : 1.0F) * damp;

            for (int nx = 0; nx < this.nodes; nx++) {
               int o = nx * 3;
               if (this.fixed[nx]) {
                  this.rootToU(a, c, this.restN, nx, this.pos, o);
                  this.prev[o] = this.pos[o];
                  this.prev[o + 1] = this.pos[o + 1];
                  this.prev[o + 2] = this.pos[o + 2];
               } else {
                  float vx = (this.pos[o] - this.prev[o]) * scale;
                  float vy = (this.pos[o + 1] - this.prev[o + 1]) * scale;
                  float vz = (this.pos[o + 2] - this.prev[o + 2]) * scale;
                  this.prev[o] = this.pos[o];
                  this.prev[o + 1] = this.pos[o + 1];
                  this.prev[o + 2] = this.pos[o + 2];
                  this.pos[o] = this.pos[o] + vx;
                  this.pos[o + 1] = this.pos[o + 1] + vy;
                  this.pos[o + 2] = this.pos[o + 2] + vz;
                  float k = this.omega2[nx] * moon * h * h;
                  float bx = this.floorN[o] + (this.hangN[o] - this.floorN[o]) * shapeMix;
                  float by = this.floorN[o + 1] + (this.hangN[o + 1] - this.floorN[o + 1]) * shapeMix;
                  float bz = this.floorN[o + 2] + (this.hangN[o + 2] - this.floorN[o + 2]) * shapeMix;
                  tmp[0] = a[0] * bx + a[1] * by + a[2] * bz + c[0];
                  tmp[1] = a[3] * bx + a[4] * by + a[5] * bz + c[1];
                  tmp[2] = a[6] * bx + a[7] * by + a[8] * bz + c[2];
                  float hw = this.hang[nx];
                  tmp[0] += (bx + pvx - tmp[0]) * hw;
                  tmp[1] += (by - this.cfg.pivotY + pvy - tmp[1]) * hw;
                  tmp[2] += (bz + pvz - tmp[2]) * hw;
                  if (this.air > 0.01F && hw > 0.0F) {
                     float rx = this.restN[o];
                     float rz = this.restN[o + 2];
                     float rl = (float)Math.sqrt(rx * rx + rz * rz) + 1.0E-6F;
                     float ph = swayT + 2.0F * (float)Math.atan2(rx, rz) + this.restN[o + 1] * 0.08F;
                     float amp = SWAY_PX * this.air * hw / 0.9F;
                     float sw = (float)Math.sin(ph) * amp;
                     tmp[0] += rx / rl * sw;
                     tmp[2] += rz / rl * sw;
                     tmp[1] -= (float)Math.cos(ph) * amp * 0.5F;
                  }

                  this.pos[o] = this.pos[o] + (tmp[0] - this.pos[o]) * k;
                  this.pos[o + 1] = this.pos[o + 1] + (tmp[1] - this.pos[o + 1]) * k;
                  this.pos[o + 2] = this.pos[o + 2] + (tmp[2] - this.pos[o + 2]) * k;
               }
            }

            for (int it = 0; it < 5; it++) {
               for (int nxx = 0; nxx < this.nodes; nxx++) {
                  int p = this.parent[nxx];
                  if (!this.fixed[nxx]) {
                     int o = nxx * 3;
                     int op = p * 3;
                     float dx = this.pos[o] - this.pos[op];
                     float dy = this.pos[o + 1] - this.pos[op + 1];
                     float dz = this.pos[o + 2] - this.pos[op + 2];
                     float d = (float)Math.sqrt(dx * dx + dy * dy + dz * dz) + 1.0E-6F;
                     float target = d > this.len[nxx] ? this.len[nxx] : Math.max(d, this.len[nxx] * SQUASH);
                     float diff = (d - target) / d;
                     if (diff != 0.0F) {
                        if (this.fixed[p]) {
                           this.pos[o] = this.pos[o] - dx * diff;
                           this.pos[o + 1] = this.pos[o + 1] - dy * diff;
                           this.pos[o + 2] = this.pos[o + 2] - dz * diff;
                        } else {
                           this.pos[o] = this.pos[o] - dx * diff * 0.75F;
                           this.pos[o + 1] = this.pos[o + 1] - dy * diff * 0.75F;
                           this.pos[o + 2] = this.pos[o + 2] - dz * diff * 0.75F;
                           this.pos[op] = this.pos[op] + dx * diff * 0.25F;
                           this.pos[op + 1] = this.pos[op + 1] + dy * diff * 0.25F;
                           this.pos[op + 2] = this.pos[op + 2] + dz * diff * 0.25F;
                        }
                     }
                  }
               }

               for (int i = 0; i < this.linkMax.length; i++) {
                  int na = this.link[i * 2];
                  int nb = this.link[i * 2 + 1];
                  int oa = na * 3;
                  int ob = nb * 3;
                  float dx = this.pos[ob] - this.pos[oa];
                  float dy = this.pos[ob + 1] - this.pos[oa + 1];
                  float dz = this.pos[ob + 2] - this.pos[oa + 2];
                  float d = (float)Math.sqrt(dx * dx + dy * dy + dz * dz) + 1.0E-6F;
                  if (!(d <= this.linkMax[i])) {
                     float diff = (d - this.linkMax[i]) / d * 0.5F;
                     float wa = this.fixed[na] ? 0.0F : 1.0F;
                     float wb = this.fixed[nb] ? 0.0F : 1.0F;
                     float sum = wa + wb;
                     this.pos[oa] = this.pos[oa] + dx * diff * 2.0F * wa / sum;
                     this.pos[oa + 1] = this.pos[oa + 1] + dy * diff * 2.0F * wa / sum;
                     this.pos[oa + 2] = this.pos[oa + 2] + dz * diff * 2.0F * wa / sum;
                     this.pos[ob] = this.pos[ob] - dx * diff * 2.0F * wb / sum;
                     this.pos[ob + 1] = this.pos[ob + 1] - dy * diff * 2.0F * wb / sum;
                     this.pos[ob + 2] = this.pos[ob + 2] - dz * diff * 2.0F * wb / sum;
                  }
               }

               for (int nxxx = 0; nxxx < this.nodes; nxxx++) {
                  if (!this.fixed[nxxx]) {
                     this.collide(a, c, nxxx, ground, tmp);
                  }
               }
            }

            for (int nxxxx = 0; nxxxx < this.nodes; nxxxx++) {
               int o = nxxxx * 3;
               if (!this.fixed[nxxxx] && this.pos[o + 1] >= ground.px(this.pos[o], this.pos[o + 2]) - 0.05F) {
                  this.prev[o] = this.pos[o] + (this.prev[o] - this.pos[o]) * fr;
                  this.prev[o + 2] = this.pos[o + 2] + (this.prev[o + 2] - this.pos[o + 2]) * fr;
                  this.prev[o + 1] = this.pos[o + 1];
               }
            }
         }

         if (steps > 0) {
            this.lastH = h;
         }

         System.arraycopy(aIn, 0, this.lastA, 0, 9);
         System.arraycopy(cIn, 0, this.lastC, 0, 3);
         this.lex = ex;
         this.ley = ey;
         this.lez = ez;
         this.skin(aIn, cIn, ground, out);
      }
   }

   private static double sq(double x) {
      return x * x;
   }

   private void reset(float[] a, float[] c, float[] m, double ex, double ey, double ez, double t) {
      this.started = true;
      this.lastT = t;
      this.air = 0.0F;
      System.arraycopy(a, 0, this.lastA, 0, 9);
      System.arraycopy(c, 0, this.lastC, 0, 3);
      this.lastH = 0.0F;

      for (int n = 0; n < this.nodes; n++) {
         bodyToU(a, c, this.floorN, n, this.pos, n * 3);
         this.prev[n * 3] = this.pos[n * 3];
         this.prev[n * 3 + 1] = this.pos[n * 3 + 1];
         this.prev[n * 3 + 2] = this.pos[n * 3 + 2];
      }

      System.arraycopy(m, 0, this.lastM, 0, 9);
      this.lex = ex;
      this.ley = ey;
      this.lez = ez;
   }

   private void toNewU(float[] arr, int n, float[] m, double ex, double ey, double ez, float[] tmp) {
      int o = n * 3;
      float ux = arr[o] / 16.0F;
      float uy = arr[o + 1] / 16.0F;
      float uz = arr[o + 2] / 16.0F;
      double wx = this.lastM[0] * ux + this.lastM[3] * uy + this.lastM[6] * uz + this.lex;
      double wy = this.lastM[1] * ux + this.lastM[4] * uy + this.lastM[7] * uz + this.ley;
      double wz = this.lastM[2] * ux + this.lastM[5] * uy + this.lastM[8] * uz + this.lez;
      double rx = wx - ex;
      double ry = wy - ey;
      double rz = wz - ez;
      arr[o] = (float)(16.0 * (m[0] * rx + m[1] * ry + m[2] * rz));
      arr[o + 1] = (float)(16.0 * (m[3] * rx + m[4] * ry + m[5] * rz));
      arr[o + 2] = (float)(16.0 * (m[6] * rx + m[7] * ry + m[8] * rz));
   }

   private void rootToU(float[] a, float[] c, float[] src, int n, float[] dst, int o) {
      if (this.rootRel == null) {
         bodyToU(a, c, src, n, dst, o);
      } else {
         float[] r = this.rootRel;
         float x = src[n * 3];
         float y = src[n * 3 + 1];
         float z = src[n * 3 + 2];
         float bx = r[0] * x + r[1] * y + r[2] * z + this.rootOff[0];
         float by = r[3] * x + r[4] * y + r[5] * z + this.rootOff[1];
         float bz = r[6] * x + r[7] * y + r[8] * z + this.rootOff[2];
         dst[o] = a[0] * bx + a[1] * by + a[2] * bz + c[0];
         dst[o + 1] = a[3] * bx + a[4] * by + a[5] * bz + c[1];
         dst[o + 2] = a[6] * bx + a[7] * by + a[8] * bz + c[2];
      }
   }

   private static void bodyToU(float[] a, float[] c, float[] src, int n, float[] dst, int o) {
      float x = src[n * 3];
      float y = src[n * 3 + 1];
      float z = src[n * 3 + 2];
      dst[o] = a[0] * x + a[1] * y + a[2] * z + c[0];
      dst[o + 1] = a[3] * x + a[4] * y + a[5] * z + c[1];
      dst[o + 2] = a[6] * x + a[7] * y + a[8] * z + c[2];
   }

   private void collide(float[] a, float[] c, int n, DressSim.Ground ground, float[] tmp) {
      int o = n * 3;
      float ux = this.pos[o] - c[0];
      float uy = this.pos[o + 1] - c[1];
      float uz = this.pos[o + 2] - c[2];
      float bx = a[0] * ux + a[3] * uy + a[6] * uz;
      float by = a[1] * ux + a[4] * uy + a[7] * uz;
      float bz = a[2] * ux + a[5] * uy + a[8] * uz;
      if (by > this.cfg.bandY0 && by < this.cfg.bandY1) {
         float e = this.ellipse(bx, bz);
         float lim = Math.min(1.0F, this.rest01[n] * 0.97F);
         if (e < lim && e > 1.0E-5F) {
            float k = (float)Math.sqrt(lim / e);
            bx *= k;
            bz *= k;
            this.pos[o] = a[0] * bx + a[1] * by + a[2] * bz + c[0];
            this.pos[o + 1] = a[3] * bx + a[4] * by + a[5] * bz + c[1];
            this.pos[o + 2] = a[6] * bx + a[7] * by + a[8] * bz + c[2];
         }
      }

      float g = ground.px(this.pos[o], this.pos[o + 2]);
      if (this.pos[o + 1] > g) {
         this.pos[o + 1] = g;
      }
   }

   private void skin(float[] a, float[] c, DressSim.Ground ground, float[] out) {
      float[] rot = this.skinRot;
      float[] headU = this.skinHeadU;
      float[] headR = this.skinHeadR;
      float[] tmp = new float[3];

      for (int b = 0; b < this.bones; b++) {
         int hn = this.boneHead[b];
         int nn = this.boneNext[b];
         bodyToU(a, c, this.restN, hn, tmp, 0);
         float r0x = tmp[0];
         float r0y = tmp[1];
         float r0z = tmp[2];
         bodyToU(a, c, this.restN, nn, tmp, 0);
         float ax = tmp[0] - r0x;
         float ay = tmp[1] - r0y;
         float az = tmp[2] - r0z;
         float bx = this.pos[nn * 3] - this.pos[hn * 3];
         float by = this.pos[nn * 3 + 1] - this.pos[hn * 3 + 1];
         float bz = this.pos[nn * 3 + 2] - this.pos[hn * 3 + 2];
         rotationBetween(ax, ay, az, bx, by, bz, rot, b * 9);
         headU[b * 3] = this.pos[hn * 3];
         headU[b * 3 + 1] = this.pos[hn * 3 + 1];
         headU[b * 3 + 2] = this.pos[hn * 3 + 2];
         headR[b * 3] = r0x;
         headR[b * 3 + 1] = r0y;
         headR[b * 3 + 2] = r0z;
      }

      int nv = this.restV.length / 3;

      for (int i = 0; i < nv; i++) {
         bodyToU(a, c, this.restV, i, tmp, 0);
         float vx = tmp[0];
         float vy = tmp[1];
         float vz = tmp[2];
         this.rootToU(a, c, this.restV, i, tmp, 0);
         float qx = tmp[0];
         float qy = tmp[1];
         float qz = tmp[2];
         float sx = 0.0F;
         float sy = 0.0F;
         float sz = 0.0F;
         float sw = 0.0F;

         for (int k = 0; k < 4; k++) {
            int b = this.wBone[i * 4 + k];
            if (b < 0) {
               break;
            }

            float w = this.wVal[i * 4 + k];
            float dx = vx - headR[b * 3];
            float dy = vy - headR[b * 3 + 1];
            float dz = vz - headR[b * 3 + 2];
            int r = b * 9;
            sx += w * (headU[b * 3] + rot[r] * dx + rot[r + 1] * dy + rot[r + 2] * dz);
            sy += w * (headU[b * 3 + 1] + rot[r + 3] * dx + rot[r + 4] * dy + rot[r + 5] * dz);
            sz += w * (headU[b * 3 + 2] + rot[r + 6] * dx + rot[r + 7] * dy + rot[r + 8] * dz);
            sw += w;
         }

         float rest = Math.max(0.0F, 1.0F - sw);
         float ux = sx + qx * rest;
         float uy = sy + qy * rest;
         float uz = sz + qz * rest;
         if (sw > 1.0F) {
            ux = sx / sw;
            uy = sy / sw;
            uz = sz / sw;
         }

         if (this.restNrm != null) {
            float nx0 = this.restNrm[i * 3];
            float ny0 = this.restNrm[i * 3 + 1];
            float nz0 = this.restNrm[i * 3 + 2];
            float nx = a[0] * nx0 + a[1] * ny0 + a[2] * nz0;
            float ny = a[3] * nx0 + a[4] * ny0 + a[5] * nz0;
            float nz = a[6] * nx0 + a[7] * ny0 + a[8] * nz0;
            float mx = nx * rest;
            float my = ny * rest;
            float mz = nz * rest;

            for (int k = 0; k < 4; k++) {
               int b = this.wBone[i * 4 + k];
               if (b < 0) {
                  break;
               }

               float w = this.wVal[i * 4 + k];
               int r = b * 9;
               mx += w * (rot[r] * nx + rot[r + 1] * ny + rot[r + 2] * nz);
               my += w * (rot[r + 3] * nx + rot[r + 4] * ny + rot[r + 5] * nz);
               mz += w * (rot[r + 6] * nx + rot[r + 7] * ny + rot[r + 8] * nz);
            }

            float ml = (float)Math.sqrt(mx * mx + my * my + mz * mz) + 1.0E-9F;
            mx /= ml;
            my /= ml;
            mz /= ml;
            this.outNrm[i * 3] = a[0] * mx + a[3] * my + a[6] * mz;
            this.outNrm[i * 3 + 1] = a[1] * mx + a[4] * my + a[7] * mz;
            this.outNrm[i * 3 + 2] = a[2] * mx + a[5] * my + a[8] * mz;
         }

         float g = ground.px(ux, uz) - 0.15F - LAYER_LIFT * Math.max(0.0F, this.restR[i] - 4.0F);
         if (uy > g) {
            uy = g;
         }

         float px = ux - c[0];
         float py = uy - c[1];
         float pz = uz - c[2];
         out[i * 3] = a[0] * px + a[3] * py + a[6] * pz;
         out[i * 3 + 1] = a[1] * px + a[4] * py + a[7] * pz;
         out[i * 3 + 2] = a[2] * px + a[5] * py + a[8] * pz;
      }
   }

   private static void rotationBetween(float ax, float ay, float az, float bx, float by, float bz, float[] r, int o) {
      float la = (float)Math.sqrt(ax * ax + ay * ay + az * az) + 1.0E-9F;
      float lb = (float)Math.sqrt(bx * bx + by * by + bz * bz) + 1.0E-9F;
      ax /= la;
      ay /= la;
      az /= la;
      bx /= lb;
      by /= lb;
      bz /= lb;
      float vx = ay * bz - az * by;
      float vy = az * bx - ax * bz;
      float vz = ax * by - ay * bx;
      float cos = ax * bx + ay * by + az * bz;
      if (cos < -0.9999F) {
         cos = -0.9999F;
      }

      float k = 1.0F / (1.0F + cos);
      r[o] = vx * vx * k + cos;
      r[o + 1] = vx * vy * k - vz;
      r[o + 2] = vx * vz * k + vy;
      r[o + 3] = vy * vx * k + vz;
      r[o + 4] = vy * vy * k + cos;
      r[o + 5] = vy * vz * k - vx;
      r[o + 6] = vz * vx * k - vy;
      r[o + 7] = vz * vy * k + vx;
      r[o + 8] = vz * vz * k + cos;
   }

   public static final class Config {
      public float pivotY = 10.0F;
      public float hangLen = 16.0F;
      public float hangFromY = 12.0F;
      public float hangFromLen = 10.0F;
      public float hangDown = DressSim.HANG_DOWN;
      public float floatSpread = DressSim.FLOAT_SPREAD;
      public float groundHang = 0.0F;
      public float bandY0 = 6.0F;
      public float bandY1 = 25.0F;
      public float ellX = 4.4F;
      public float ellZ = 2.6F;
      public float hangMax = 0.9F;
      public float hugBack = Float.NaN;
      public float hugKeep = 0.25F;
      public float pinAboveY = Float.NEGATIVE_INFINITY;

      public static DressSim.Config veil() {
         DressSim.Config c = new DressSim.Config();
         c.pivotY = 0.0F;
         c.hangLen = 6.0F;
         c.hangFromY = 0.0F;
         c.hangFromLen = 8.0F;
         c.floatSpread = 0.08F;
         c.groundHang = 1.0F;
         c.bandY0 = -2.0F;
         c.bandY1 = 25.0F;
         c.ellX = 5.2F;
         c.ellZ = 3.0F;
         c.hangMax = 0.3F;
         c.hangDown = 0.35F;
         c.pinAboveY = 0.5F;
         return c;
      }
   }

   public interface Ground {
      float px(float var1, float var2);
   }
}
