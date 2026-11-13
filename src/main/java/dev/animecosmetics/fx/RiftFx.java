package dev.animecosmetics.fx;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import dev.animecosmetics.AnimeCosmetics;
import dev.animecosmetics.Toggles;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

/**
 * "Riss-Auge": the rift of Ronova's "Authority of Death" (ported from Ronova's DeathAuthorityFx),
 * as a client-side cosmetic. Only you see it.
 *
 * It hangs fixed in the northern sky, 40 blocks away and 24 blocks above your eyes. Like the sun it keeps
 * that spot when you walk or turn, so it is always near. Its eye looks at you. It opens when the feature is
 * switched on and stays open until it is switched off.
 */
public final class RiftFx {
   static final Identifier TEX = AnimeCosmetics.id("textures/entity/fx/death_all.png");
   private static final int FULL = 15728880;
   static final float[] GLOW = fx(0.252F, 0.252F, 0.498F, 0.498F);
   static final float[] STAR = fx(0.502F, 0.252F, 0.748F, 0.498F);
   static final float[] VEIL = fx(0.752F, 0.252F, 0.998F, 0.498F);
   static final float[] PUFF = fx(0.002F, 0.502F, 0.248F, 0.748F);
   static final float[] CRACK = fx(0.252F, 0.502F, 0.498F, 0.748F);
   static final float[] GLOW_ADD = fx(0.752F, 0.002F, 0.998F, 0.248F);
   static final float[] RIFT_C = new float[]{5.0E-4F, 2.0005F, 0.9995F, 2.4995F};
   static final float[] RIFT_LIGHT_C = new float[]{0.5005F, 0.5005F, 0.9995F, 0.7495F};
   static final float[] HAZE_C = new float[]{5.0E-4F, 2.5005F, 0.4995F, 2.7495F};
   static final float[] HAZE = HAZE_C;
   static final float V_SCALE = 0.36363637F;
   static final int FIRE_NX = 256;
   static final int FIRE_NY = 144;
   static final int FIRE_TW = 512;
   static final int FIRE_TH = 288;
   static final float FIRE_W = 29.3F;
   private static final int FIRE_AX = 1024;
   private static final int FIRE_AY = 5120;
   private static final float[] FIRE_C = new float[]{0.5005F, 2.5005F, 0.7495F, 2.640125F};
   private static final NativeImage FIRE_IMG = new NativeImage(512, 288, true);
   private static final float SIM_DT = 0.033333335F;
   static final int BACK_NX = 192;
   static final int BACK_NY = 96;
   static final int BACK_TW = 512;
   static final int BACK_TH = 256;
   private static final int BACK_AX = 1536;
   private static final int BACK_AY = 5120;
   private static final float[] BACK_C = new float[]{0.7505F, 2.5005F, 0.9995F, 2.6245F};
   private static final NativeImage BACK_IMG = new NativeImage(512, 256, true);
   private static final float LIFE = 6.6F;
   private static final float FLASH_S = 0.45F;
   private static final float CLOSE_AT = 5.6F;
   static final float HOLD_AT = 4.6F;
   static final float HOLD_R = 5.0F;
   private static final float MISSING_MAX_S = 15.0F;
   static final float W = 5.5F;
   static final float H = 2.2F;
   static final float SCALE = 2.0F;
   private static final int SEG = 10;
   private static final List<RiftFx.Rift> RIFTS = new ArrayList<>();
   private static final List<double[]> GLOWS = new ArrayList<>();
   private static final List<double[]> FIRES = new ArrayList<>();
   private static final Random RND = new Random();
   private static Vec3 cam = Vec3.ZERO;
   private static Vector3f camRight = new Vector3f(1.0F, 0.0F, 0.0F);
   private static Vector3f camUp = new Vector3f(0.0F, 1.0F, 0.0F);
   static float detail = 1.0F;
   static boolean doubleSided = true;
   private static final float OPEN_S = 1.9F;
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
   private static final float GAZE_U = 1.3F;
   private static final float GAZE_V = 0.5F;
   private static final float GAZE_SMOOTH_S = 0.12F;
   static float[] skyBlend;


   private static float[] fx(float u0, float v0, float u1, float v1) {
      return new float[]{u0 * 0.5F, 0.5F + v0 * 0.5F, u1 * 0.5F, 0.5F + v1 * 0.5F};
   }


   private static void fireQuad(Pose pose, VertexConsumer buf, RiftFx.Rift r, float z, float hw, float hh, float a) {
      if (!(a <= 0.004F)) {
         if (!FxTypes.shadersOn()) {
            gridQuad(pose, buf, r, FIRE_C, 0.0F, 0.0F, z, hw, hh, 16, 9, 1.0F, 1.0F, 1.0F, a);
         } else {
            Vec3 p0 = r.at(-hw, hh, z);
            Vec3 p1 = r.at(hw, hh, z);
            Vec3 p2 = r.at(hw, -hh, z);
            Vec3 p3 = r.at(-hw, -hh, z);
            FIRES.add(new double[]{p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, a});
         }
      }
   }

   static void glowFlat(
      Pose pose, VertexConsumer buf, RiftFx.Rift r, float cx, float cy, float z, float hw, float hh, float red, float g, float b, float a
   ) {
      if (!(a <= 0.004F)) {
         if (!FxTypes.shadersOn()) {
            flat(pose, buf, r, GLOW, cx, cy, z, hw, hh, red, g, b, a);
         } else {
            Vec3 p0 = r.at(cx - hw, cy + hh, z);
            Vec3 p1 = r.at(cx + hw, cy + hh, z);
            Vec3 p2 = r.at(cx + hw, cy - hh, z);
            Vec3 p3 = r.at(cx - hw, cy - hh, z);
            GLOWS.add(new double[]{p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, red, g, b, a});
         }
      }
   }

   static void glowStrip(Pose pose, VertexConsumer buf, Vec3 a, Vec3 b, float w, float red, float g, float bl, float al) {
      if (!(al <= 0.004F)) {
         Vec3 d = b.subtract(a);
         Vec3 view = a.add(b).scale(0.5).subtract(cam);
         Vec3 side = d.cross(view);
         if (side.lengthSqr() < 1.0E-6) {
            side = d.cross(new Vec3(0.0, 1.0, 0.0));
         }

         if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1.0, 0.0, 0.0);
         }

         side = side.normalize().scale(w / 2.0F);
         Vec3 p0 = a.add(side);
         Vec3 p1 = b.add(side);
         Vec3 p2 = b.subtract(side);
         Vec3 p3 = a.subtract(side);
         if (!FxTypes.shadersOn()) {
            float uc = (GLOW[0] + GLOW[2]) / 2.0F;
            float vc = (GLOW[1] + GLOW[3]) / 2.0F;
            float u0 = GLOW[0] + (uc - GLOW[0]) * 0.1F;
            float u1 = GLOW[2] - (GLOW[2] - uc) * 0.1F;
            quadUv(pose, buf, p0, p1, p2, p3, u0, vc, u0, vc, u1, vc, u1, vc, red, g, bl, al);
         } else {
            GLOWS.add(new double[]{p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, red, g, bl, al, 1.0});
         }
      }
   }


   public static void moveTo(RiftFx.Rift r, Vec3 c, Vec3 f) {
      r.c = c;
      f = f.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : f.normalize();
      r.f = f;
      Vec3 side = new Vec3(0.0, 1.0, 0.0).cross(f);
      r.x = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
      r.y = f.cross(r.x).normalize();
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
      return detail >= 0.999F ? full : Math.max(min, Math.round(full * detail));
   }


   private static float openness(float t) {
      if (t < 0.45F) {
         return 0.0F;
      } else {
         float o = t < 0.7F ? 0.12F + 0.18F * smooth((t - 0.45F) / 0.25F) : 0.3F + 0.7F * smooth((t - 0.7F) / 1.1F);
         return o * (1.0F - smooth((t - 5.6F) / 0.8F));
      }
   }


   private static void drawCasterOpening(Pose pose, VertexConsumer buf, RiftFx.Rift r, float t) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && r.casterId >= 0 && !(t > 2.5F)) {
         Entity e = mc.level.getEntity(r.casterId);
         if (e != null) {
            Vec3 look = e.getViewVector(partial());
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
            Vec3 feet = e.getPosition(partial());
            Vec3 hand = e.getEyePosition(partial()).add(0.0, 0.78, 0.0).add(flat.scale(0.12));
            if (!r.openFx) {
               r.openFx = true;
            }

            float in = smooth(t / 0.3F);
            float out = 1.0F - smooth((t - 1.9F) / 0.6F);
            float k = in * out;
            float open = smooth((t - 0.7F) / 1.1F);
            r.handBb = r.handBb == null ? new RiftFx.Rift(hand, cam.subtract(hand), r.seed, true) : r.handBb;
            moveTo(r.handBb, hand, cam.subtract(hand));
            float pulse = 0.8F + 0.2F * (float)Math.sin(t * 11.0F);
            glowFlat(pose, buf, r.handBb, 0.0F, 0.0F, 0.0F, 0.45F + 0.2F * open, 0.45F + 0.2F * open, 1.0F, 0.3F, 0.25F, 0.8F * k * pulse);
            glowFlat(pose, buf, r.handBb, 0.0F, 0.0F, 0.02F, 0.16F, 0.16F, 1.0F, 0.85F, 0.8F, 0.9F * k);
            Vec3 eye = r.c;
            int seg = 18;

            for (int n = 0; n < 4; n++) {
               float ph = n * 1.57F + r.seed % 7;
               Vec3 prev = null;
               Vec3 axis = eye.subtract(hand);
               Vec3 s1 = axis.cross(new Vec3(0.0, 1.0, 0.0));
               s1 = s1.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : s1.normalize();
               Vec3 s2 = axis.normalize().cross(s1);

               for (int i = 0; i <= seg; i++) {
                  float sN = (float)i / seg;
                  double wob = Math.sin(sN * 6.283 + t * 7.0F + ph) * 0.35 * Math.sin(sN * Math.PI);
                  double wob2 = Math.cos(sN * 5.1 - t * 5.0F + ph) * 0.25 * Math.sin(sN * Math.PI);
                  Vec3 q = hand.add(axis.scale(sN)).add(s1.scale(wob)).add(s2.scale(wob2));
                  if (prev != null) {
                     float run = (float)Math.pow(0.5 + 0.5 * Math.sin(sN * 12.0F - t * 9.0F + ph), 2.0);
                     glowStrip(pose, buf, prev, q, 0.07F + 0.04F * open, 1.0F, 0.28F, 0.22F, (0.3F + 0.6F * run) * k * (0.5F + 0.5F * open));
                  }

                  prev = q;
               }
            }

            float sp = (1.0F - smooth((t - 0.9F) / 0.4F)) * in;
            if (sp > 0.004F) {
               for (int n = 0; n < 3; n++) {
                  float ph = n * 2.094F;
                  Vec3 prev = null;

                  for (int i = 0; i <= 16; i++) {
                     float sN = i / 16.0F;
                     float h = sN * 2.2F;
                     double a = ph + t * 5.0F + h * 3.2;
                     float rad = 0.8F - 0.45F * sN;
                     Vec3 q = feet.add(Math.cos(a) * rad, 0.05 + h, Math.sin(a) * rad);
                     if (prev != null) {
                        float head = 1.0F - smooth((sN - smooth(t / 0.8F) * 1.1F) / 0.1F);
                        glowStrip(pose, buf, prev, q, 0.1F, 1.0F, 0.25F, 0.2F, 0.7F * sp * head);
                     }

                     prev = q;
                  }
               }
            }
         }
      }
   }

   private static void draw(Pose pose, VertexConsumer buf, RiftFx.Rift r, float t) {
      float ta = r.real();
      float fade = 1.0F - smooth((t - 5.8F) / 0.8F);
      float o = openness(t);
      float world = smooth((t - 0.7F) / 0.3F);
      glowFlat(pose, buf, r, 0.4807F, -0.8464F, -1.9F, 24.954601F, 15.80458F, 0.6F, 0.02F, 0.05F, 0.5F * world * fade);
      glowFlat(pose, buf, r, 0.0F, 0.0F, -1.7F, 14.299999F, 9.24F, 0.95F, 0.12F, 0.12F, 0.32F * world * fade);
      drawAura(pose, buf, r, ta, world * fade);
      if (t < 0.55F) {
         drawSlit(pose, buf, r, smooth(t / 0.45F));
      }

      if (t < 0.7F) {
         drawCasterOpening(pose, buf, r, t);
      } else {
         float riftA = smooth((t - 0.7F) / 0.1F) * fade;

         drawRift(pose, buf, r, ta, riftA);

         for (int i = 0; i < DeathRiftEyes.EYES.length; i++) {
            float so = smooth((t - (1.05F + 0.15F * i)) / 0.9F) * (1.0F - smooth((t - 5.4F) / 0.7F));
            if (so > 0.01F) {
               drawOrbitEye(pose, buf, r, i, so, ta + i * 1.7F, 0.95F * fade, -0.45F, r.seed + i + 1);
            }
         }

         if (o > 0.005F) {
            updateGaze(r);
            drawFlow(pose, buf, r, t, ta, o, fade);
            drawTendrils(pose, buf, r, ta, smooth((t - 0.7F - 0.3F) / 0.6F) * fade * smooth(o * 1.5F), o);
            drawEye(pose, buf, r, 0.0F, 0.0F, 1.0F, o, ta, fade, 0.0F, r.seed);
         }

         if (r.fireStarted) {
            fireQuad(pose, buf, r, 0.9F, 14.65F, 8.240624F, fade);
         }

         drawCasterOpening(pose, buf, r, t);
      }
   }


   private static float[][] makeWave(Random rs, float t0, boolean darkWave) {
      float[][] w = new float[13][];
      w[0] = new float[]{t0};

      for (int i = 0; i < 12; i++) {
         double th = i * Math.PI * 2.0 / 12.0 + (rs.nextFloat() - 0.5) * 0.5;
         float k = darkWave ? 0.85F : 0.7F;
         float ox = (float)(4.6 * k * Math.cos(th));
         float oy = (float)(2.0 * k * Math.sin(th));
         float[] col = FireSim.SCARLETS[rs.nextInt(FireSim.SCARLETS.length)];
         float ang = (float)(Math.atan2(oy / 2.0, ox / 4.6) + rs.nextGaussian() * 0.2);
         w[i + 1] = darkWave
            ? new float[]{
               ox,
               oy,
               ang,
               4.0F + rs.nextFloat() * 4.0F,
               1.3F + rs.nextFloat() * 0.9F,
               15.0F,
               col[0],
               col[1],
               col[2],
               i % 2 == 0 ? 1.0F : 0.0F,
               0.2F + rs.nextFloat() * 0.25F
            }
            : new float[]{
               ox, oy, ang, 4.0F + rs.nextFloat() * 3.0F, 1.3F + rs.nextFloat() * 0.7F, 16.0F, col[0], col[1], col[2], 0.0F, 0.9F + rs.nextFloat() * 0.1F
            };
      }

      return w;
   }

   private static void simStep(RiftFx.Rift r, float st, float se) {
      for (float[] b : r.bursts) {
         if (b[11] == 0.0F && b[0] <= se) {
            b[11] = 1.0F;
            r.sim.ribbon(b[1], b[2], b[3], b[4], b[5], b[6], new float[]{b[7], b[8], b[9]}, b[10] > 0.5F, b[12]);
         }
      }

      float em = 1.25F * smooth((se - 1.3499999F) / 0.5F) * (1.0F - smooth((se - 4.9F) / 0.6F));
      r.sim.emit(st, em, 6.5F);
      r.sim.step(0.033333335F);
      if (se >= 0.7F && se < 5.7F && st >= r.nextBack) {
         r.nextBack = st + 0.9F + r.backRnd.nextFloat() * 0.2F;
         r.backWaves.add(makeWave(r.backRnd, st, false));
         r.backWaves.add(makeWave(r.backRnd, st + 0.4F, true));
      }

      for (int wi = r.backWaves.size() - 1; wi >= 0; wi--) {
         float[][] wv = r.backWaves.get(wi);
         float el = st - wv[0][0];
         if (!(el < 0.0F)) {
            if (el > 0.36F) {
               r.backWaves.remove(wi);
            } else {
               float amt = smooth(el / 0.35F);

               for (int q = 1; q < wv.length; q++) {
                  float[] bx = wv[q];
                  r.back.ribbon(bx[0], bx[1], bx[2], bx[3], bx[4], bx[5], new float[]{bx[6], bx[7], bx[8]}, bx[9] > 0.5F, bx[10], amt);
               }
            }
         }
      }

      r.back.step(0.033333335F);
   }

   private static void uploadFire(RiftFx.Rift r) {
      if (r.worker == null) {
         r.worker = new RiftFx.FireWorker(r);
         r.worker.start();
      }

      int[][] fr = r.ready.getAndSet(null);
      if (fr != null) {
         try {
            MemoryUtil.memIntBuffer(FIRE_IMG.getPointer(), 147456).put(fr[0]);
            MemoryUtil.memIntBuffer(BACK_IMG.getPointer(), 131072).put(fr[1]);
            AbstractTexture atlas = Minecraft.getInstance().getTextureManager().getTexture(TEX);
            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
            enc.writeToTexture(atlas.getTexture(), FIRE_IMG, 0, 0, 1024, 5120);
            enc.writeToTexture(atlas.getTexture(), BACK_IMG, 0, 0, 1536, 5120);
         } catch (Exception var4) {
            AnimeCosmetics.LOGGER.warn("death fire upload: {}", var4.toString());
         }
      }
   }

   private static void drawFlow(Pose pose, VertexConsumer buf, RiftFx.Rift r, float tSched, float t, float open, float fade) {
      float show = smooth((open - 0.3F) / 0.5F) * (1.0F - smooth((tSched - 4.2F) / 1.2F)) * fade;
      if (!(show <= 0.01F)) {
         float cx = -0.209F;
         float top = 0.29678002F;
         float w0 = 2.996224F;
         float len = 6.82176F;
         float uc = (GLOW[0] + GLOW[2]) / 2.0F;
         float vc = (GLOW[1] + GLOW[3]) / 2.0F;
         float[] uv = new float[]{uc, vc, uc, vc, uc, vc, uc, vc};
         int seg = 20;

         for (int layer = 0; layer < 2; layer++) {
            int n = 7 - layer;

            for (int k = 0; k < n; k++) {
               float x0 = (-1.0F + 2.0F * (k + 0.5F) / n) * w0 * (layer == 0 ? 1.0F : 0.8F);
               float ph = hash(r.seed, k + 10 * layer, 41) * 6.283F;
               float speed = 5.0F + 3.0F * hash(r.seed, k + 10 * layer, 42);
               Vec3 pl = null;
               Vec3 pr = null;
               float[] cPrev = null;

               for (int i = 0; i <= seg; i++) {
                  float sIn = (float)i / seg;
                  float conv = (float)Math.pow(1.0F - sIn, 2.2);
                  float wv = (float)Math.sin(sIn * 7.0F - t * speed * 0.55 + ph);
                  float bump = (float)Math.pow(Math.max(0.0F, wv), 3.0);
                  float x = cx + x0 * conv + (0.04F * (float)Math.sin(t * 1.8 + sIn * 4.0F + ph) + 0.09F * wv * (0.3F + sIn)) * sIn;
                  float y = top - sIn * len;
                  float half = 0.13F * (1.0F - 0.5F * sIn) * (1.0F + 1.1F * bump) * (layer == 0 ? 1.0F : 0.7F);
                  float pulse = 0.65F + 0.35F * (float)Math.sin(sIn * 11.0F - t * speed + ph);
                  float a = show * (1.0F - smooth((sIn - 0.3F) / 0.22F)) * pulse * (layer == 0 ? 1.0F : 0.75F);
                  float[] col = new float[]{1.0F, 0.34F - 0.22F * sIn, 0.2F - 0.12F * sIn, a};
                  float dxs = -x0 * 2.2F * (float)Math.pow(1.0F - sIn, 1.2) / seg;
                  float dys = -len / seg;
                  float ln = (float)Math.hypot(dxs, dys) + 1.0E-6F;
                  float nx = -dys / ln;
                  float ny = dxs / ln;
                  Vec3 l = r.at(x + nx * half, y + ny * half, -0.2 + 0.02 * layer);
                  Vec3 rr = r.at(x - nx * half, y - ny * half, -0.2 + 0.02 * layer);
                  if (pl != null) {
                     quadV(pose, buf, pl, pr, rr, l, uv, cPrev, cPrev, col, col);
                  }

                  pl = l;
                  pr = rr;
                  cPrev = col;
               }
            }
         }
      }
   }

   private static void drawTendrils(Pose pose, VertexConsumer buf, RiftFx.Rift r, float t, float a, float open) {
      if (!(a <= 0.004F)) {
         RiftFx.Frame eye = new RiftFx.Frame(r, -0.209F, -0.1045F, 0.0F, 5.3504F, 2.6752F * open, DeathEyeShape.TOP, DeathEyeShape.BOT);
         float uc = (GLOW[0] + GLOW[2]) / 2.0F;
         float vc = (GLOW[1] + GLOW[3]) / 2.0F;
         float[] uv = new float[]{uc, vc, uc, vc, uc, vc, uc, vc};
         int seg = 26;

         for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 5; k++) {
               int id = (side + 1) * 5 + k;
               float rootX = side * 5.3504F * (0.28F + 0.17F * hash(r.seed, id, 31)) + -0.209F;
               float rootY = -0.1045F + (hash(r.seed, id, 32) - 0.5F) * 0.8F;
               float len = 8.0F + 5.0F * hash(r.seed, id, 33);
               float dir = side > 0 ? (hash(r.seed, id, 34) - 0.5F) * 0.9F : (float) Math.PI + (hash(r.seed, id, 34) - 0.5F) * 0.9F;
               float w1 = 1.3F + 1.4F * hash(r.seed, id, 35);
               float w2 = 0.7F + 0.9F * hash(r.seed, id, 36);
               float ph = hash(r.seed, id, 37) * 6.283F;
               float dx = (float)Math.cos(dir);
               float dy = (float)Math.sin(dir);
               float nx = -dy;
               float ny = dx;
               Vec3 pl = null;
               Vec3 pr = null;
               float aPrev = 0.0F;
               float[] cPrev = null;

               for (int i = 0; i <= seg; i++) {
                  float sIn = (float)i / seg;
                  float wig = (0.25F + 0.75F * sIn)
                     * (0.55F * (float)Math.sin(sIn * 7.5F - t * w1 + ph) + 0.3F * (float)Math.sin(sIn * 13.0F + t * w2 + ph * 2.0F));
                  float x = rootX + dx * len * sIn + nx * wig;
                  float y = rootY + dy * len * sIn + ny * wig - 0.35F * sIn * sIn;
                  float half = 0.22F;
                  Vec3 l = r.at(x + nx * half, y + ny * half, -0.3);
                  Vec3 rr = r.at(x - nx * half, y - ny * half, -0.3);
                  float fade = (1.0F - smooth((sIn - 0.45F) / 0.55F)) * 0.55F * a * (1.0F - eye.clipA(x - -0.209F, y - -0.1045F));
                  float[] col = new float[]{0.85F - 0.45F * sIn, 0.06F * (1.0F - sIn), 0.04F, fade};
                  if (pl != null) {
                     quadV(pose, buf, pl, pr, rr, l, uv, cPrev, cPrev, col, col);
                  }

                  pl = l;
                  pr = rr;
                  cPrev = col;
               }
            }
         }
      }
   }

   private static void drawAura(Pose pose, VertexConsumer buf, RiftFx.Rift r, float t, float a) {
      if (!(a <= 0.004F)) {
         float cx = 0.4807F;
         float cy = -0.8464F;
         float ax = 11.978209F;
         float ay = 7.90229F;
         float breath = 1.0F + 0.05F * (float)Math.sin(t * 1.7) + 0.02F * (float)Math.sin(t * 4.3 + 1.0);
         float uc = (GLOW[0] + GLOW[2]) / 2.0F;
         float vc = (GLOW[1] + GLOW[3]) / 2.0F;
         int n = 64;
         float[] uv = new float[]{uc, vc, uc, vc, uc, vc, uc, vc};

         for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2.0 / n;
            double a1 = (i + 1) * Math.PI * 2.0 / n;
            float w0 = 1.0F + 0.08F * (float)Math.sin(a0 * 5.0 - t * 0.9);
            float w1 = 1.0F + 0.08F * (float)Math.sin(a1 * 5.0 - t * 0.9);
            float k0 = breath * w0;
            float k1 = breath * w1;
            float[] rings = new float[]{0.82F, 1.0F, 1.25F};
            float[] alph = new float[]{0.0F, 0.32F * a * (0.85F + 0.15F * (float)Math.sin(t * 2.3)), 0.0F};

            for (int b = 0; b < 2; b++) {
               Vec3 p0 = r.at(cx + (float)Math.cos(a0) * ax * k0 * rings[b], cy + (float)Math.sin(a0) * ay * k0 * rings[b], -1.4);
               Vec3 p1 = r.at(cx + (float)Math.cos(a1) * ax * k1 * rings[b], cy + (float)Math.sin(a1) * ay * k1 * rings[b], -1.4);
               Vec3 q1 = r.at(cx + (float)Math.cos(a1) * ax * k1 * rings[b + 1], cy + (float)Math.sin(a1) * ay * k1 * rings[b + 1], -1.4);
               Vec3 q0 = r.at(cx + (float)Math.cos(a0) * ax * k0 * rings[b + 1], cy + (float)Math.sin(a0) * ay * k0 * rings[b + 1], -1.4);
               quadV(
                  pose,
                  buf,
                  p0,
                  p1,
                  q1,
                  q0,
                  uv,
                  new float[]{0.72F, 0.05F, 0.07F, alph[b]},
                  new float[]{0.72F, 0.05F, 0.07F, alph[b]},
                  new float[]{0.72F, 0.05F, 0.07F, alph[b + 1]},
                  new float[]{0.72F, 0.05F, 0.07F, alph[b + 1]}
               );
            }
         }
      }
   }

   private static void drawSlit(Pose pose, VertexConsumer buf, RiftFx.Rift r, float grow) {
      if (!(grow <= 0.01F)) {
         float bx = -0.209F;
         float by = -0.1045F;
         float hw = 5.3504F;
         float hh = 0.187264F;
         int n = 24;

         for (int pass = 0; pass < 2; pass++) {
            float[] cell = pass == 0 ? BASE_C : LID_C;
            float z = 0.05F + 0.02F * pass;

            for (int i = 0; i < n; i++) {
               float x0 = -grow + 2.0F * grow * i / n;
               float x1 = -grow + 2.0F * grow * (i + 1) / n;
               float y0 = by - 0.484F * (0.35F - x0 * x0) * 2.0F;
               float y1 = by - 0.484F * (0.35F - x1 * x1) * 2.0F;
               float u0 = cell[0] + (cell[2] - cell[0]) * (x0 + 1.0F) / 2.0F;
               float u1 = cell[0] + (cell[2] - cell[0]) * (x1 + 1.0F) / 2.0F;
               float tip = Math.min(1.0F, (grow - Math.max(Math.abs(x0), Math.abs(x1))) / 0.04F + 0.2F);
               quadV(
                  pose,
                  buf,
                  r.at(bx + x0 * hw, y0 + hh, z),
                  r.at(bx + x1 * hw, y1 + hh, z),
                  r.at(bx + x1 * hw, y1 - hh, z),
                  r.at(bx + x0 * hw, y0 - hh, z),
                  new float[]{u0, cell[1], u1, cell[1], u1, cell[3], u0, cell[3]},
                  new float[]{1.0F, 1.0F, 1.0F, tip},
                  new float[]{1.0F, 1.0F, 1.0F, tip},
                  new float[]{1.0F, 1.0F, 1.0F, tip},
                  new float[]{1.0F, 1.0F, 1.0F, tip}
               );
            }
         }

         glowFlat(pose, buf, r, bx, by - 0.11000001F, 0.04F, hw * grow * 1.1F, 0.9F, 1.0F, 0.16F, 0.1F, 0.6F * grow);
      }
   }

   private static void gridQuad(
      Pose pose,
      VertexConsumer buf,
      RiftFx.Rift r,
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

   private static void drawRift(Pose pose, VertexConsumer buf, RiftFx.Rift r, float t, float a) {
      if (!(a <= 0.004F)) {
         float cx = 0.4807F;
         float cy = -0.8464F;
         float hw = 16.6364F;
         float hh = 8.3182F;
         int nx = 24;
         int ny = 12;
         gridQuad(pose, buf, r, HAZE_C, cx, cy, -1.1F, hw, hh, 12, 6, 1.0F, 1.0F, 1.0F, 0.7F * a);
         if (r.fireStarted) {
            gridQuad(pose, buf, r, BACK_C, cx, cy, -0.95F, hw, hh, 12, 6, 1.0F, 1.0F, 1.0F, a);
         }

         gridQuad(pose, buf, r, RIFT_C, cx, cy, -0.7F, hw, hh, nx, ny, 1.0F, 1.0F, 1.0F, a);
         float uc0 = RIFT_LIGHT_C[0];
         float vc0 = RIFT_LIGHT_C[1];
         float du = RIFT_LIGHT_C[2] - RIFT_LIGHT_C[0];
         float dv = RIFT_LIGHT_C[3] - RIFT_LIGHT_C[1];
         Vec3[] pts = new Vec3[(nx + 1) * (ny + 1)];
         float[] al = new float[pts.length];

         for (int j = 0; j <= ny; j++) {
            for (int i = 0; i <= nx; i++) {
               float gx = -1.0F + 2.0F * i / nx;
               float gy = -1.0F + 2.0F * j / ny;
               float x = cx + gx * hw;
               float y = cy + gy * hh;
               double sh = Math.sin(x * 0.55 - t * 3.4 + y * 0.9)
                  + 0.7 * Math.sin(x * 1.3 + t * 2.5 - y * 1.7 + 1.1)
                  + 0.5 * Math.sin(y * 2.1 - t * 4.1 + x * 0.35 + 2.4);
               float shimmer = smooth(((float)sh / 2.2F - 0.1F) / 0.6F);
               int k = j * (nx + 1) + i;
               pts[k] = r.at(x, y, -0.62F);
               al[k] = 0.9F * shimmer * a;
            }
         }

         for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
               int k00 = j * (nx + 1) + i;
               int k10 = k00 + 1;
               int k01 = k00 + nx + 1;
               int k11 = k01 + 1;
               float u0 = uc0 + du * i / nx;
               float u1 = uc0 + du * (i + 1) / nx;
               float v0 = vc0 + dv * (1.0F - (float)j / ny);
               float v1 = vc0 + dv * (1.0F - (float)(j + 1) / ny);
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
   }

   private static void updateGaze(RiftFx.Rift r) {
      Minecraft mc = Minecraft.getInstance();
      long now = System.nanoTime();
      float dt = r.gazeNs < 0L ? 0.1F : Math.min(0.1F, (float)(now - r.gazeNs) / 1.0E9F);
      r.gazeNs = now;
      float tu = 0.0F;
      float tv = 0.0F;
      Entity caster = r.casterId >= 0 && mc.level != null ? mc.level.getEntity(r.casterId) : null;
      if (caster == null && r.gazeTarget != null && (float)(now - r.gazeTargetNs) / 1.0E9F < 0.5F) {
         Vec3 d = r.gazeTarget.subtract(r.at(-0.209F, -0.1045F, 0.0));
         if (d.lengthSqr() > 1.0E-4) {
            d = d.normalize();
            tu = Math.max(-1.0F, Math.min(1.0F, (float)d.dot(r.x) * 1.4F));
            tv = Math.max(-1.0F, Math.min(1.0F, (float)d.dot(r.y) * 1.4F));
         }
      } else if (caster != null) {
         float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 eye = caster.getEyePosition(partial);
         Vec3 look = caster.getViewVector(partial);
         HitResult hit = caster.pick(48.0, partial, false);
         Vec3 target = hit.getType() == Type.BLOCK ? hit.getLocation() : eye.add(look.scale(48.0));
         Vec3 d = target.subtract(r.at(-0.209F, -0.1045F, 0.0));
         if (d.lengthSqr() > 1.0E-4) {
            d = d.normalize();
            tu = Math.max(-1.0F, Math.min(1.0F, (float)d.dot(r.x) * 1.4F));
            tv = Math.max(-1.0F, Math.min(1.0F, (float)d.dot(r.y) * 1.4F));
         }
      }

      float k = 1.0F - (float)Math.exp(-dt / 0.12F);
      r.gazeU = r.gazeU + (tu - r.gazeU) * k;
      r.gazeV = r.gazeV + (tv - r.gazeV) * k;
   }


   static void drawEye(Pose pose, VertexConsumer buf, RiftFx.Rift r, float cx, float cy, float s, float open, float t, float alpha, float z, int seed) {
      if (!(open <= 0.002F) && !(alpha <= 0.004F)) {
         float bx = cx + -0.209F * s;
         float by = cy + -0.1045F * s;
         RiftFx.Frame f = new RiftFx.Frame(r, bx, by, 0.0F, 5.3504F * s, 2.6752F * s * open, DeathEyeShape.TOP, DeathEyeShape.BOT);
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

   private static void drawOrbitEye(Pose pose, VertexConsumer buf, RiftFx.Rift r, int i, float open, float t, float a, float z, int seed) {
      if (!(open <= 0.002F) && !(a <= 0.004F)) {
         float[] e = DeathRiftEyes.EYES[i];
         float hl = e[3];
         float disc = e[4];
         RiftFx.Frame f = new RiftFx.Frame(r, e[0], e[1], e[2], hl, hl, DeathRiftEyes.EYE_TOP[i], DeathRiftEyes.EYE_BOT[i]);
         float[] cell = new float[]{e[5], e[6], e[7], e[8]};
         float seg = t / 1.3F + hash(seed, 0, 21) * 3.0F;
         int k0 = (int)Math.floor(seg);
         float q = smooth((seg - k0 - 0.55F) / 0.35F);
         float gu0 = (hash(seed, k0, 22) - 0.5F) * 0.5F * hl;
         float gu1 = (hash(seed, k0 + 1, 22) - 0.5F) * 0.5F * hl;
         float gv0 = (hash(seed, k0, 23) - 0.5F) * 0.5F * disc;
         float gv1 = (hash(seed, k0 + 1, 23) - 0.5F) * 0.5F * disc;
         float gu = gu0 + (gu1 - gu0) * q;
         float gv = gv0 + (gv1 - gv0) * q;
         float aa = a * smooth(open * 1.5F);
         drawIris(pose, buf, f, gu, gv, disc, disc * 1.08F, cell, 32, t, aa, z);
         float pr = e[13];
         float gmax = Math.max(0.0F, disc - pr * 1.05F);
         float nu = gu / (0.25F * hl);
         float nv = gv / (0.25F * disc);
         float pu = gu + nu * gmax * 0.95F;
         float pv = gv + nv * gmax * 0.95F;
         float[] pc = new float[]{e[9], e[10], e[11], e[12]};
         float[] col = new float[]{1.0F, 1.0F, 1.0F, aa * f.clipA(pu, pv)};
         quadV(
            pose,
            buf,
            f.at(pu - pr, f.clip(pu - pr, pv + pr), z + 0.06),
            f.at(pu + pr, f.clip(pu + pr, pv + pr), z + 0.06),
            f.at(pu + pr, f.clip(pu + pr, pv - pr), z + 0.06),
            f.at(pu - pr, f.clip(pu - pr, pv - pr), z + 0.06),
            new float[]{pc[0], pc[1], pc[2], pc[1], pc[2], pc[3], pc[0], pc[3]},
            col,
            col,
            col,
            col
         );
      }
   }

   private static float wave(double a, float t) {
      return (float)(Math.sin(7.0 * a - 2.1 * t) + 0.6 * Math.sin(11.0 * a + 3.1 * t) + 0.35 * Math.sin(4.0 * a - 1.05 * t + 1.3));
   }

   static void drawIris(
      Pose pose, VertexConsumer buf, RiftFx.Frame f, float ux, float uy, float R, float cellH, float[] cell, int na, float t, float a, float z
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

   private static void drawPetals(Pose pose, VertexConsumer buf, RiftFx.Frame f, float s, float t, float a, float z, float ox, float oy) {
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
      Pose pose, VertexConsumer buf, RiftFx.Frame f, float ux, float uy, float h, float tipR, int na, float t, float a, float z, int seed
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

      float[] var10000 = new float[]{1.0F, 1.0F, 1.0F, a};

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


   static void flat(
      Pose pose,
      VertexConsumer buf,
      RiftFx.Rift r,
      float[] cell,
      float cx,
      float cy,
      float z,
      float hw,
      float hh,
      float red,
      float g,
      float b,
      float a
   ) {
      quadUv(
         pose,
         buf,
         r.at(cx - hw, cy + hh, z),
         r.at(cx + hw, cy + hh, z),
         r.at(cx + hw, cy - hh, z),
         r.at(cx - hw, cy - hh, z),
         cell[0],
         cell[1],
         cell[2],
         cell[1],
         cell[2],
         cell[3],
         cell[0],
         cell[3],
         red,
         g,
         b,
         a
      );
   }

   private static final class FireWorker extends Thread {
      final RiftFx.Rift r;
      volatile boolean stop;
      private final int[] fire = new int[147456];
      private final int[] back = new int[131072];
      private float lastRender = -1.0F;
      private final boolean FIRE15 = false;

      FireWorker(RiftFx.Rift r) {
         super("animecosmetics-rift-fire");
         this.r = r;
         this.setDaemon(true);
      }

      @Override
      public void run() {
         try {
            while (!this.stop) {
               float t = this.r.age();
               float tr = this.r.real();
               if (t > 6.6F) {
                  return;
               }

               if (t < 0.68F) {
                  Thread.sleep(5L);
               } else {
                  if (this.r.simTime < 0.0F) {
                     this.r.simTime = tr;
                  }

                  if (tr - this.r.simTime > 0.5F) {
                     this.r.simTime = tr - 0.033333335F;
                  }

                  int steps;
                  for (steps = 0; this.r.simTime + 0.033333335F <= tr && steps < 2; steps++) {
                     RiftFx.simStep(this.r, this.r.simTime, this.r.age());
                     this.r.simTime += 0.033333335F;
                  }

                  if (steps > 0 && tr - this.lastRender >= (this.FIRE15 ? 0.06666667F : 0.030000001F)) {
                     this.lastRender = tr;
                     this.renderFrame(this.r, t, tr);
                     this.r.fireStarted = true;
                  }

                  long wait = (long)((this.r.simTime + 0.033333335F - this.r.real()) * 1000.0F);
                  Thread.sleep(Math.max(1L, Math.min(wait, 33L)));
               }
            }
         } catch (InterruptedException var6) {
         } catch (Throwable var7) {
            AnimeCosmetics.LOGGER.warn("death fire worker died, fire frozen until the rift closes", var7);
         }
      }

      private void renderFrame(RiftFx.Rift r, float t, float tr) {
         float dissolve = 0.12F * RiftFx.smooth((t - 1.6F) / 1.0F);
         float opacity = 1.0F - 0.4F * RiftFx.smooth((t - 1.7F) / 0.9F);
         float soft = RiftFx.smooth((t - 1.3F) / 0.7F);
         float world = RiftFx.smooth((t - 0.7F) / 0.3F);
         float cor = RiftFx.smooth((t - 1.2F) / 0.6F) * (1.0F - RiftFx.smooth((t - 5.0F) / 0.8F));
         r.sim.render(this.fire, 512, 288, dissolve, opacity, soft);
         r.sim.overlayCoronaRings(this.fire, 512, 288, tr, 0.7F, cor, world * (1.0F - RiftFx.smooth((t - 5.2F) / 0.5F)));
         r.back.render(this.back, 512, 256, 0.05F, 0.9F, 0.0F);
         int[] f2 = new int[this.fire.length];
         int[] b2 = new int[this.back.length];

         for (int k = 0; k < this.fire.length; k++) {
            f2[k] = abgr(this.fire[k]);
         }

         for (int k = 0; k < this.back.length; k++) {
            b2[k] = abgr(this.back[k]);
         }

         r.ready.set(new int[][]{f2, b2});
      }

      private static int abgr(int argb) {
         return argb & -16711936 | (argb & 0xFF) << 16 | argb >> 16 & 0xFF;
      }
   }

   static final class Frame {
      final RiftFx.Rift r;
      final float cx;
      final float cy;
      final float cs;
      final float sn;
      final float hl;
      final float hh;
      final float[] top;
      final float[] bot;

      Frame(RiftFx.Rift r, float cx, float cy, float rot, float hl, float hh, float[] top, float[] bot) {
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
      Vec3 c;
      Vec3 f;
      Vec3 x;
      Vec3 y;
      float openMul = 1.0F;
      boolean hideFirstPerson;
      boolean openFx;
      RiftFx.Rift handBb;
      long t0 = System.nanoTime();
      final int seed;
      int casterId = -1;
      Vec3 anchor = Vec3.ZERO;
      long holdNs;
      long lastNs = System.nanoTime();
      Vec3 gazeTarget;
      long gazeTargetNs;
      long missingNs;
      float gazeU;
      float gazeV;
      long gazeNs = -1L;
      boolean closing;
      long vortexT0 = -1L;
      float vortexDur = 1.0F;
      long shatterNs = -1L;
      boolean shatterFx;
      boolean burstFx;
      float zSign = 1.0F;
      /** Anime Cosmetics: rift hangs in the sky, follows the camera and stays open until switched off. */
      boolean sky;
      /** Anime Cosmetics: size multiplier, so the sky rift keeps its size at any render distance. */
      float sc = 1.0F;
      final FireSim sim;
      final FireSim back;
      float simTime = -1.0F;
      float nextBack = 0.0F;
      float backDarkAt = -1.0F;
      volatile boolean fireStarted;
      RiftFx.FireWorker worker;
      final AtomicReference<int[][]> ready = new AtomicReference<>();
      final List<float[][]> backWaves = new ArrayList<>();
      final Random backRnd;
      final List<float[]> bursts = new ArrayList<>();

      Rift(Vec3 c, Vec3 f, int seed) {
         this(c, f, seed, false);
      }

      Rift(Vec3 c, Vec3 f, int seed, boolean light) {
         this.sim = light ? null : new FireSim(256, 144, 29.3F, 0L);
         this.back = light ? null : new FireSim(192, 96, 33.2728F, 7L);
         this.c = c;
         this.f = f.normalize();
         Vec3 up = new Vec3(0.0, 1.0, 0.0);
         Vec3 side = up.cross(this.f);
         this.x = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         this.y = this.f.cross(this.x).normalize();
         this.seed = seed;
         this.backRnd = new Random(seed * 31L + 7L);
         if (!light) {
            Random rs = new Random(seed);

            for (int i = 0; i < 12; i++) {
               double th = i * Math.PI * 2.0 / 12.0 + (rs.nextFloat() - 0.5) * 0.4;
               float ox = (float)(3.2199999999999998 * Math.cos(th));
               float oy = (float)(1.4 * Math.sin(th));
               float[] col = FireSim.SCARLETS[rs.nextInt(FireSim.SCARLETS.length)];
               float bright = 0.9F + rs.nextFloat() * 0.1F;
               this.bursts
                  .add(
                     new float[]{
                        0.7F + rs.nextFloat() * 0.02F,
                        ox,
                        oy,
                        (float)(Math.atan2(oy / 2.0, ox / 4.6) + rs.nextGaussian() * 0.15),
                        4.0F + rs.nextFloat() * 3.0F,
                        1.3F + rs.nextFloat() * 0.7F,
                        16.0F,
                        col[0],
                        col[1],
                        col[2],
                        0.0F,
                        0.0F,
                        bright
                     }
                  );
            }

            for (int i = 0; i < 12; i++) {
               double th = i * Math.PI * 2.0 / 12.0 + (rs.nextFloat() - 0.5) * 0.5;
               float ox = (float)(3.9099999999999997 * Math.cos(th));
               float oy = (float)(1.7 * Math.sin(th));
               float[] col = FireSim.SCARLETS[rs.nextInt(FireSim.SCARLETS.length)];
               boolean inkq = i % 2 == 0;
               this.bursts
                  .add(
                     new float[]{
                        0.82F + rs.nextFloat() * 0.06F,
                        ox,
                        oy,
                        (float)(Math.atan2(oy / 2.0, ox / 4.6) + rs.nextGaussian() * 0.25),
                        4.0F + rs.nextFloat() * 4.0F,
                        1.2F + rs.nextFloat() * 0.8F,
                        14.0F,
                        col[0],
                        col[1],
                        col[2],
                        inkq ? 1.0F : 0.0F,
                        0.0F,
                        0.2F + rs.nextFloat() * 0.25F
                     }
                  );
            }

            for (int i = 0; i < 10; i++) {
               double th = i * Math.PI * 2.0 / 10.0 + (rs.nextFloat() - 0.5) * 0.5;
               float ox = (float)(3.6799999999999997 * Math.cos(th));
               float oy = (float)(1.6 * Math.sin(th));
               this.bursts
                  .add(
                     new float[]{
                        0.75F + rs.nextFloat() * 0.03F,
                        ox,
                        oy,
                        (float)(Math.atan2(oy / 2.0, ox / 4.6) + rs.nextGaussian() * 0.2),
                        5.0F + rs.nextFloat() * 4.0F,
                        1.5F + rs.nextFloat() * 0.9F,
                        18.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        0.0F,
                        0.0F
                     }
                  );
            }
         }
      }

      float age() {
         return (float)(System.nanoTime() - this.t0 - this.holdNs) / 1.0E9F;
      }

      float real() {
         return (float)(System.nanoTime() - this.t0) / 1.0E9F;
      }

      void hold(Minecraft mc) {
         long now = System.nanoTime();
         long dt = now - this.lastNs;
         this.lastNs = now;
         if (this.sky && !this.closing && !(this.age() < 4.6F)) {
            this.holdNs += dt;
         } else if (!this.closing && !(this.age() < 4.6F) && mc.level != null) {
            Entity caster = mc.level.getEntity(this.casterId);
            if (caster == null) {
               this.missingNs += dt;
               if ((float)this.missingNs / 1.0E9F < 15.0F) {
                  this.holdNs += dt;
               }
            } else {
               this.missingNs = 0L;
               if (caster.position().distanceTo(this.anchor) <= 5.0) {
                  this.holdNs += dt;
               }
            }
         }
      }

      Vec3 at(double px, double py, double pz) {
         double k = 2.0 * this.sc;
         return this.c.add(this.x.scale(px * k)).add(this.y.scale(py * k)).add(this.f.scale(pz * k * this.zSign));
      }
   }

   // ------------------------------------------------------------------------------------------------
   // Anime Cosmetics: control
   // The rift hangs in the sky (like the sun, it moves with you) and stays open while the
   // feature is on. The vortex sky (RiftVortex + RiftAtmosphere) runs independently of it.
   // ------------------------------------------------------------------------------------------------

   private static final SoundEvent SOUND_OPEN = SoundEvent.createFixedRangeEvent(AnimeCosmetics.id("authority_open"), 96.0F);
   private static final SoundEvent SOUND_CLOSE = SoundEvent.createFixedRangeEvent(AnimeCosmetics.id("authority_close"), 48.0F);
   private static final RandomSource SOUND_RND = RandomSource.create();
   /** Fixed sky direction of the rift: north (-Z). It never turns with you. */
   private static final Vec3 SKY_DIR = new Vec3(0.0, 0.0, -1.0);
   /** How far north of you the rift hangs (blocks) ... */
   private static final double SKY_DIST = 40.0;
   /** ... and how high above your eyes. */
   private static final double SKY_UP = 24.0;
   private static final DustParticleOptions SPARK = new DustParticleOptions(0xFF5A3C, 0.7F);
   private static final DustParticleOptions EMBER = new DustParticleOptions(0xD81E14, 0.9F);
   private static ClientLevel lastLevel;
   private static boolean wasOn;

   private RiftFx() {
   }

   public static void register() {
      LevelRenderHooks.onCollectSubmits(RiftFx::render);
   }

   static float partial() {
      return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
   }

   private static void sound(SoundEvent e, float volume) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         Vec3 p = mc.player.position();
         mc.getSoundManager().play(new SimpleSoundInstance(e, SoundSource.PLAYERS, volume, 0.97F + SOUND_RND.nextFloat() * 0.06F, SOUND_RND, p.x, p.y, p.z));
      }
   }

   static boolean overworld(Minecraft mc) {
      return mc.level != null && mc.level.dimension().equals(Level.OVERWORLD);
   }

   public static void clear() {
      for (RiftFx.Rift r : RIFTS) {
         if (r.worker != null) {
            r.worker.stop = true;
         }
      }

      RIFTS.clear();
      GLOWS.clear();
      FIRES.clear();
      RiftAtmosphere.clear();
      RiftVortex.clear();
      wasOn = false;
   }

   private static boolean hasOpenRift() {
      for (RiftFx.Rift r : RIFTS) {
         if (!r.closing) {
            return true;
         }
      }

      return false;
   }

   /** Where the rift hangs for a given eye position: fixed in the northern sky, near you. */
   private static Vec3 skyPos(Vec3 eye) {
      return eye.add(SKY_DIR.scale(SKY_DIST)).add(0.0, SKY_UP, 0.0);
   }

   /** Opens the rift in the sky; render() keeps it at the same spot relative to you every frame. */
   private static void open(Minecraft mc) {
      RiftFx.Rift rift = new RiftFx.Rift(skyPos(mc.player.getEyePosition()), SKY_DIR.scale(-1.0), RND.nextInt());
      rift.sky = true;
      RIFTS.add(rift);
      sound(SOUND_OPEN, 0.7F);
   }

   /** Same as Ronova's close: jumps the rift to its closing phase. */
   private static void closeAll() {
      boolean any = false;

      for (RiftFx.Rift r : RIFTS) {
         if (!r.closing) {
            r.closing = true;
            any = true;
            if (r.age() < 5.6F) {
               r.holdNs = r.holdNs - (long)((5.6F - r.age()) * 1.0E9);
            }
         }
      }

      if (any) {
         sound(SOUND_CLOSE, 0.7F);
      }
   }

   public static void tick(Minecraft mc) {
      if (mc.level != lastLevel) {
         lastLevel = mc.level;
         clear();
      }

      if (mc.level != null && mc.player != null && !mc.isPaused()) {
         boolean on = Toggles.get(Toggles.Feature.RIFT) && overworld(mc);
         if (!on && wasOn) {
            closeAll();
         }

         if (on && !hasOpenRift()) {
            open(mc);
         }

         wasOn = on;
         RIFTS.removeIf(rx -> {
            if (rx.age() > 6.6F) {
               if (rx.worker != null) {
                  rx.worker.stop = true;
               }

               return true;
            } else {
               return false;
            }
         });

         // sparks and embers around the rift, as in Ronova
         for (RiftFx.Rift rx : RIFTS) {
            float t = rx.age();
            if (!(t < 0.7F) && !(t > 5.8F)) {
               int n = t < 1.2F ? 90 : (t < 2.2F ? 30 : 10);

               for (int i = 0; i < n; i++) {
                  double a = RND.nextDouble() * Math.PI * 2.0;
                  double rr = Math.sqrt(RND.nextDouble());
                  Vec3 pp = rx.at(Math.cos(a) * rr * 5.5 * 2.4, Math.sin(a) * rr * 2.2 * 3.2, 0.3 + RND.nextDouble() * 1.2);
                  double sp = t < 0.95F && RND.nextBoolean() ? 0.6 + 0.6 * RND.nextDouble() : 0.05 + 0.15 * RND.nextDouble();
                  Vec3 v = pp.subtract(rx.c).normalize().scale(sp).add(0.0, 0.03, 0.0);
                  mc.level.addParticle(i % 3 == 0 ? EMBER : SPARK, pp.x, pp.y, pp.z, v.x, v.y, v.z);
               }
            }
         }
      }
   }

   private static void render(LevelRenderHooks.Context ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         RIFTS.clear();
         RiftAtmosphere.clear();
         RiftVortex.clear();
      } else {
         cam = ctx.levelState().cameraRenderState.pos;
         boolean skyOn = Toggles.get(Toggles.Feature.RIFT_SKY) && overworld(mc);
         RiftAtmosphere.update(skyOn, cam);
         RiftVortex.update(skyOn, cam);
         if (RiftAtmosphere.active()) {
            RiftAtmosphere.submit(ctx, cam);
         }

         // always called: it also resets the cloud hiding once the vortex is gone
         RiftVortex.submit(ctx, cam);
         if (!RIFTS.isEmpty()) {
            boolean iris = FxTypes.shadersOn();
            FireSim.VIG_W = iris ? 0.55F : 0.45F;
            FireSim.VIG_P = iris ? 2.0F : 1.4F;
            Quaternionf rot = ctx.levelState().cameraRenderState.orientation;
            camRight = rot.transform(new Vector3f(1.0F, 0.0F, 0.0F));
            camUp = rot.transform(new Vector3f(0.0F, 1.0F, 0.0F));
            if (mc.player != null) {
               // fixed in the northern sky like the sun, but near: it keeps its distance to you and never turns
               long now = System.nanoTime();
               Vec3 eye = mc.player.getEyePosition(partial());
               Vec3 c = skyPos(eye);

               for (RiftFx.Rift r : RIFTS) {
                  if (r.sky) {
                     moveTo(r, c, SKY_DIR.scale(-1.0));
                     // the big eye looks at you
                     r.gazeTarget = eye;
                     r.gazeTargetNs = now;
                  }
               }
            }

            if (FxTypes.shadersOn() && !GLOWS.isEmpty()) {
               List<double[]> glows = new ArrayList<>(GLOWS);
               GLOWS.clear();
               ctx.submitNodeCollector()
                  .order(-1)
                  .submitCustomGeometry(
                     new PoseStack(),
                     RiftTypes.glowIris(TEX),
                     (pose, buf) -> {
                        float uc = (GLOW_ADD[0] + GLOW_ADD[2]) / 2.0F;
                        float vc = (GLOW_ADD[1] + GLOW_ADD[3]) / 2.0F;
                        float su0 = GLOW_ADD[0] + (uc - GLOW_ADD[0]) * 0.1F;
                        float su1 = GLOW_ADD[2] - (GLOW_ADD[2] - uc) * 0.1F;

                        for (double[] q : glows) {
                           Vec3 p0 = new Vec3(q[0], q[1], q[2]);
                           Vec3 p1 = new Vec3(q[3], q[4], q[5]);
                           Vec3 p2 = new Vec3(q[6], q[7], q[8]);
                           Vec3 p3 = new Vec3(q[9], q[10], q[11]);
                           float r = (float)(q[12] * q[15]);
                           float g = (float)(q[13] * q[15]);
                           float b = (float)(q[14] * q[15]);
                           if (q.length > 16) {
                              quadUv(pose, buf, p0, p1, p2, p3, su0, vc, su0, vc, su1, vc, su1, vc, r, g, b, 1.0F);
                           } else {
                              quadUv(
                                 pose, buf, p0, p1, p2, p3,
                                 GLOW_ADD[0], GLOW_ADD[1], GLOW_ADD[2], GLOW_ADD[1], GLOW_ADD[2], GLOW_ADD[3], GLOW_ADD[0], GLOW_ADD[3],
                                 r, g, b, 1.0F
                              );
                           }
                        }
                     }
                  );
            } else if (!FxTypes.shadersOn()) {
               GLOWS.clear();
            }

            if (FxTypes.shadersOn() && !FIRES.isEmpty()) {
               List<double[]> fires = new ArrayList<>(FIRES);
               FIRES.clear();
               ctx.submitNodeCollector()
                  .order(1)
                  .submitCustomGeometry(
                     new PoseStack(),
                     RiftTypes.fireIris(TEX),
                     (pose, buf) -> {
                        for (double[] q : fires) {
                           quadUv(
                              pose, buf,
                              new Vec3(q[0], q[1], q[2]), new Vec3(q[3], q[4], q[5]), new Vec3(q[6], q[7], q[8]), new Vec3(q[9], q[10], q[11]),
                              FIRE_C[0], FIRE_C[1], FIRE_C[2], FIRE_C[1], FIRE_C[2], FIRE_C[3], FIRE_C[0], FIRE_C[3],
                              1.0F, 1.0F, 1.0F, (float)q[12]
                           );
                        }
                     }
                  );
            } else if (!FxTypes.shadersOn()) {
               FIRES.clear();
            }

            List<RiftFx.Rift> snap = new ArrayList<>(RIFTS);
            RenderType single = RiftTypes.softNoSortNoCull(TEX);
            ctx.submitNodeCollector().submitCustomGeometry(new PoseStack(), single != null ? single : RiftTypes.softNoSort(TEX), (pose, buf) -> {
               doubleSided = single == null;

               try {
                  for (RiftFx.Rift r : snap) {
                     r.hold(mc);
                     r.zSign = cam.subtract(r.c).dot(r.f) >= 0.0 ? 1.0F : -1.0F;
                     uploadFire(r);
                     draw(pose, buf, r, r.age());
                  }
               } finally {
                  doubleSided = true;
               }
            });
         }
      }
   }
}
