package dev.animecosmetics.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.animecosmetics.AnimeCosmetics;
import dev.animecosmetics.Toggles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SkyFx {
   private static final Identifier TEX = AnimeCosmetics.id("textures/entity/fx/death_all.png");
   private static final Identifier EYE_TEX = AnimeCosmetics.id("textures/entity/fx/death_all_sky.png");
   private static final float[] GLOW = new float[]{0.126F, 0.626F, 0.249F, 0.749F};
   private static float[] V = new float[0];
   private static float domeR = -1.0F;
   private static float domeWaves = 1.0F;
   private static int domeFrame;

   private SkyFx() {
   }

   public static void register() {
      LevelRenderHooks.onCollectSubmits(SkyFx::render);
   }

   private static void render(LevelRenderHooks.Context ctx) {
      Minecraft mc = Minecraft.getInstance();
      boolean dome = Toggles.get(Toggles.Feature.SKY);
      boolean eye = Toggles.get(Toggles.Feature.SUN_EYE);
      if ((dome || eye) && mc.level != null && mc.level.dimension().equals(Level.OVERWORLD)) {
         float t = (float)(System.nanoTime() % 1000000000000L) / 1.0E9F;
         float r = Math.min(FarClamp.limit() * 0.97F, mc.options.getEffectiveRenderDistance() * 16 * 1.08F + 48.0F);
         RenderType type = FxTypes.skyType(TEX);
         if (dome) {
            renderDome(ctx, type, r, t);
         }

         if (eye) {
            renderSunEye(ctx, mc, r, t);
         }
      }
   }

   private static void renderDome(LevelRenderHooks.Context ctx, RenderType type, float r, float t) {
      domeWaves = FxTypes.shadersOn() ? 1.6F : 2.2F;
      int w = 85;
      boolean rebuild = V.length < 43 * w * 9 || domeR != r;
      if (rebuild) {
         V = new float[43 * w * 9];
         domeR = r;
      }

      if (rebuild || domeFrame++ % 3 == 0) {
         for (int i = 0; i <= 42; i++) {
            double u = i / 42.0 * 2.0 - 1.0;
            double la = (Math.PI / 2) * Math.signum(u) * Math.pow(Math.abs(u), 1.5);

            for (int j = 0; j <= 84; j++) {
               vtx(V, (i * w + j) * 9, la, (Math.PI * 2) * j / 84.0, t, r);
            }
         }
      }

      ctx.submitNodeCollector().order(-3).submitCustomGeometry(new PoseStack(), type, (pose, buf) -> {
         for (int ix = 0; ix < 42; ix++) {
            for (int jx = 0; jx < 84; jx++) {
               int a = ix * w + jx + 1;
               int b = ix * w + jx;
               int c = (ix + 1) * w + jx;
               int d = (ix + 1) * w + jx + 1;
               emit(pose, buf, a);
               emit(pose, buf, b);
               emit(pose, buf, c);
               emit(pose, buf, d);
            }
         }
      });
   }

   private static void renderSunEye(LevelRenderHooks.Context ctx, Minecraft mc, float r, float t) {
      Float angle = (Float)mc.level.environmentAttributes().getDimensionValue(EnvironmentAttributes.SUN_ANGLE);
      if (angle != null) {
         double a = Math.toRadians(angle.floatValue());
         Vec3 dir = new Vec3(-Math.sin(a), Math.cos(a), 0.0);
         float above = (float)dir.y;
         float open = SkyEye.smooth((above + 0.12F) / 0.2F);
         if (!(open <= 0.002F)) {
            RenderType eyeType = FxTypes.skyType(EYE_TEX);
            Vec3 camPos = ctx.levelState().cameraRenderState.pos;
            float eyeDist = r * 0.8F;
            float[] tint = null;
            ctx.submitNodeCollector()
               .order(-2)
               .submitCustomGeometry(
                  new PoseStack(), eyeType, (pose, buf) -> SkyEye.drawSkyEye(pose, buf, camPos, dir, eyeDist, 0.38F * eyeDist, open, t, 7, false, tint)
               );
         }
      }
   }

   private static void vtx(float[] out, int o, double lat, double lon, float t, float rad) {
      double cl = Math.cos(lat);
      float dx = (float)(cl * Math.cos(lon));
      float dy = (float)Math.sin(lat);
      float dz = (float)(cl * Math.sin(lon));
      float[] c = new float[3];
      skyColor(dx, dy, dz, t, c);
      out[o] = dx * rad;
      out[o + 1] = dy * rad;
      out[o + 2] = dz * rad;
      out[o + 3] = c[0];
      out[o + 4] = c[1];
      out[o + 5] = c[2];
      out[o + 6] = -dx;
      out[o + 7] = -dy;
      out[o + 8] = -dz;
   }

   private static void skyColor(float dx, float dy, float dz, float t, float[] out) {
      float cr;
      float cg;
      float cb;
      if (dy >= 0.0F) {
         float k = (float)Math.pow(dy, 0.7);
         cr = Mth.lerp(k, 0.6F, 0.24F);
         cg = Mth.lerp(k, 0.16F, 0.04F);
         cb = Mth.lerp(k, 0.28F, 0.11F);
      } else {
         float k = Mth.clamp(-dy * 2.2F, 0.0F, 1.0F);
         float haze = (float)Math.exp(-(-dy) * 9.0F) * 0.1F;
         cr = Mth.lerp(k, 0.56F, 0.16F) + haze;
         cg = Mth.lerp(k, 0.16F, 0.03F) + haze * 0.3F;
         cb = Mth.lerp(k, 0.27F, 0.07F) + haze * 0.5F;
      }

      float warp1 = (noise3(dx * 2.2F + t * 0.08F, dy * 2.2F + t * 0.03F, dz * 2.2F - t * 0.06F) - 0.5F) * 5.5F;
      float warp2 = (noise3(dx * 3.5F - t * 0.11F, dy * 3.5F + t * 0.07F, dz * 3.5F) - 0.5F) * 2.2F;
      float warp = warp1 + warp2;
      float p1 = (0.12F * dx + 1.0F * dy + 0.08F * dz) * 7.0F + warp + t * 0.25F;
      float p2 = (-0.18F * dx + 1.0F * dy + 0.22F * dz) * 11.0F + warp * 1.3F - t * 0.32F;
      float p3 = (0.08F * dx + 1.0F * dy - 0.14F * dz) * 13.0F + warp2 * 2.5F + t * 0.4F;
      float s1 = 0.5F + 0.5F * Mth.sin(p1);
      float s2 = 0.5F + 0.5F * Mth.sin(p2);
      float s3 = 0.5F + 0.5F * Mth.sin(p3);
      float boost = domeWaves;
      float ribbon = (s1 * s1 * s1 * s1 * 0.35F + s2 * s2 * s2 * s2 * 0.25F + s3 * s3 * s3 * s3 * 0.12F) * boost;
      float halo = (s1 * s1 * 0.16F + s2 * s2 * 0.08F) * boost;
      float vis = 0.3F + 0.7F * Mth.clamp((dy + 0.5F) / 1.2F, 0.0F, 1.0F);
      float grain = 0.85F + 0.3F * noise3(dx * 4.0F + t * 0.15F, dy * 4.0F, dz * 4.0F);
      float wave = (ribbon * grain + halo) * vis;
      cr += wave * 0.5F;
      cg += wave * 0.14F;
      cb += wave * 0.24F;
      out[0] = Mth.clamp(cr, 0.0F, 1.0F);
      out[1] = Mth.clamp(cg, 0.0F, 1.0F);
      out[2] = Mth.clamp(cb, 0.0F, 1.0F);
   }

   private static void emit(Pose pose, VertexConsumer buf, int q) {
      float[] v = V;
      int o = q * 9;
      float u = (GLOW[0] + GLOW[2]) / 2.0F;
      float vv = (GLOW[1] + GLOW[3]) / 2.0F;
      buf.addVertex(pose, v[o], v[o + 1], v[o + 2])
         .setColor(v[o + 3], v[o + 4], v[o + 5], 1.0F)
         .setUv(u, vv * 0.36363637F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, v[o + 6], v[o + 7], v[o + 8]);
   }

   private static float hash(int x, int y, int z) {
      int h = x * 374761393 + y * 668265263 + z * 1442695041;
      h = (h ^ h >>> 13) * 1274126177;
      return ((h ^ h >>> 16) & 16777215) / 1.6777215E7F;
   }

   private static float noise3(float x, float y, float z) {
      int ix = Mth.floor(x);
      int iy = Mth.floor(y);
      int iz = Mth.floor(z);
      float fx = x - ix;
      float fy = y - iy;
      float fz = z - iz;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      fz = fz * fz * (3.0F - 2.0F * fz);
      float c00 = Mth.lerp(fx, hash(ix, iy, iz), hash(ix + 1, iy, iz));
      float c10 = Mth.lerp(fx, hash(ix, iy + 1, iz), hash(ix + 1, iy + 1, iz));
      float c01 = Mth.lerp(fx, hash(ix, iy, iz + 1), hash(ix + 1, iy, iz + 1));
      float c11 = Mth.lerp(fx, hash(ix, iy + 1, iz + 1), hash(ix + 1, iy + 1, iz + 1));
      return Mth.lerp(fz, Mth.lerp(fy, c00, c10), Mth.lerp(fy, c01, c11));
   }
}
