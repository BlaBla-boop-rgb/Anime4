package dev.animecosmetics.look;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import dev.animecosmetics.AnimeCosmetics;
import dev.animecosmetics.fx.FxTypes;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class EyeFeathers {
   static final Identifier TEXTURE = AnimeCosmetics.id("textures/entity/eye_feathers_fire.png");
   private static final int FULL_BRIGHT = 15728880;
   static final float PX = 160.0F;
   static final float ROOT_X = 0.06F;
   static final float ROOT_Y = -0.08F;
   static final float IN_S = 1.3F;
   static final float OUT_S = 0.8F;
   static final float BAND = 0.22F;
   static final float SOFT = 0.1F;
   static final float RAG = 0.24F;
   static final float HAZE = 0.07F;
   static final int GRID = 24;
   static final float GAZE_YAW = 50.0F;
   static final float GAZE_PITCH = 40.0F;
   static final float GAZE_T = 0.1F;
   static final float DZ_ORN = 0.06F;
   static final float DZ_IRIS = 0.1F;
   static final float DZ_PUPIL = 0.12F;
   private static final Map<String, EyeFeathers.Spec> SPECS = Map.of(
      "top", new EyeFeathers.Spec(0.96F, 0.0F, 10.0F), "middle", new EyeFeathers.Spec(1.1F, 0.1F, 8.0F), "lower", new EyeFeathers.Spec(0.975F, 0.2F, 6.0F)
   );
   private static EyeFeathers.Feather[] feathers;
   private static EyeFeathers.Layer[] liveLayers = new EyeFeathers.Layer[0];
   private static float fps = 16.0F;
   private static final EyeFeathersLive LIVE = new EyeFeathersLive(
      "/assets/animecosmetics/textures/entity/eye_feathers_fire.png", TEXTURE, "animecosmetics-feather-fire"
   );
   private static final EyeFeathersLive.EyeFeathersAtlas LIVE_ATLAS = new EyeFeathersLive.EyeFeathersAtlas() {
      @Override
      public int count() {
         EyeFeathers.feathers();
         return EyeFeathers.liveLayers.length;
      }

      @Override
      public int[][] frameCells(int i) {
         return EyeFeathers.liveLayers[i].framePx;
      }

      @Override
      public int[][] liveCells(int i) {
         return EyeFeathers.liveLayers[i].livePx;
      }

      @Override
      public float fps() {
         return EyeFeathers.fps;
      }
   };
   private static final Map<Integer, EyeFeathers.State> STATES = new HashMap<>();
   private static final float MISSING_S = 6.0F;
   private static final Map<Integer, Vec3[]> TOP_EYES = new HashMap<>();
   private static boolean twoSided = true;

   private static EyeFeathers.Feather[] feathers() {
      if (feathers != null) {
         return feathers;
      } else {
         feathers = new EyeFeathers.Feather[0];

         try {
            EyeFeathers.Feather[] var29x;
            try (InputStream in = EyeFeathers.class.getResourceAsStream("/assets/animecosmetics/meshes/eye_feathers.json")) {
               if (in != null) {
                  JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                  JsonArray atlas = o.getAsJsonArray("fire_atlas");
                  JsonArray root = o.getAsJsonArray("root");
                  float aw = atlas.get(0).getAsFloat();
                  float ah = atlas.get(1).getAsFloat();
                  float rx = root.get(0).getAsFloat();
                  float ry = root.get(1).getAsFloat();
                  fps = o.get("fps").getAsFloat();
                  JsonArray fs = o.getAsJsonArray("feathers");
                  EyeFeathers.Feather[] out = new EyeFeathers.Feather[fs.size()];
                  List<EyeFeathers.Layer> lives = new ArrayList<>();

                  for (int i = 0; i < out.length; i++) {
                     JsonObject f = fs.get(i).getAsJsonObject();
                     String name = f.get("name").getAsString();
                     JsonArray ls = f.getAsJsonArray("layers");
                     EyeFeathers.Layer base = layer(ls.get(0).getAsJsonObject(), 0.0F, rx, ry, aw, ah);
                     EyeFeathers.Layer orn = layer(ls.get(1).getAsJsonObject(), 0.06F, rx, ry, aw, ah);
                     lives.add(base);
                     lives.add(orn);
                     JsonObject parts = f.getAsJsonObject("parts");
                     EyeFeathers.Part iris = part(parts.getAsJsonObject("iris"), 0.1F, rx, ry, aw, ah);
                     EyeFeathers.Part pupil = part(parts.getAsJsonObject("pupil"), 0.12F, rx, ry, aw, ah);
                     JsonArray gz = f.getAsJsonArray("gaze");
                     float[] gaze = new float[4];

                     for (int k = 0; k < 4; k++) {
                        gaze[k] = gz.get(k).getAsFloat() / 160.0F;
                     }

                     float cx = Mth.clamp(0.0F, base.x0, base.x1);
                     float cy = Mth.clamp(0.0F, base.y0, base.y1);
                     float dMin = Mth.sqrt(cx * cx + cy * cy);
                     float dMax = Math.max(Math.max(len(base.x0, base.y0), len(base.x1, base.y0)), Math.max(len(base.x0, base.y1), len(base.x1, base.y1)));
                     out[i] = new EyeFeathers.Feather(
                        name,
                        SPECS.getOrDefault(name, new EyeFeathers.Spec(1.05F, 0.0F, 8.0F)),
                        base,
                        orn,
                        iris,
                        pupil,
                        uv(ls.get(0).getAsJsonObject().getAsJsonArray("fblur"), aw, ah),
                        gaze,
                        dMin,
                        dMax
                     );
                  }

                  liveLayers = lives.toArray(new EyeFeathers.Layer[0]);
                  feathers = out;
                  return feathers;
               }

               AnimeCosmetics.LOGGER.error("eye_feathers.json missing");
               var29x = feathers;
            }

            return var29x;
         } catch (Exception var29) {
            AnimeCosmetics.LOGGER.error("eye_feathers.json", var29);
            return feathers;
         }
      }
   }

   private static float len(float x, float y) {
      return Mth.sqrt(x * x + y * y);
   }

   private static EyeFeathers.Layer layer(JsonObject l, float dz, float rx, float ry, float aw, float ah) {
      JsonArray r = l.getAsJsonArray("frect");
      JsonArray fr = l.getAsJsonArray("frames");
      JsonArray lv = l.getAsJsonArray("live");
      float[][] frames = new float[fr.size()][];
      int[][] framePx = new int[fr.size()][];

      for (int k = 0; k < frames.length; k++) {
         frames[k] = uv(fr.get(k).getAsJsonArray(), aw, ah);
         framePx[k] = px(fr.get(k).getAsJsonArray());
      }

      return new EyeFeathers.Layer(
         (r.get(0).getAsFloat() - rx) / 160.0F,
         (r.get(1).getAsFloat() - ry) / 160.0F,
         (r.get(2).getAsFloat() - rx) / 160.0F,
         (r.get(3).getAsFloat() - ry) / 160.0F,
         dz,
         frames,
         new float[][]{uv(lv.get(0).getAsJsonArray(), aw, ah), uv(lv.get(1).getAsJsonArray(), aw, ah)},
         framePx,
         new int[][]{px(lv.get(0).getAsJsonArray()), px(lv.get(1).getAsJsonArray())}
      );
   }

   private static EyeFeathers.Part part(JsonObject p, float dz, float rx, float ry, float aw, float ah) {
      JsonArray r = p.getAsJsonArray("rect");
      return new EyeFeathers.Part(
         (r.get(0).getAsFloat() - rx) / 160.0F,
         (r.get(1).getAsFloat() - ry) / 160.0F,
         (r.get(2).getAsFloat() - rx) / 160.0F,
         (r.get(3).getAsFloat() - ry) / 160.0F,
         dz,
         uv(p.getAsJsonArray("fcell"), aw, ah)
      );
   }

   private static int[] px(JsonArray a) {
      return new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsInt()};
   }

   private static float[] uv(JsonArray a, float aw, float ah) {
      return new float[]{
         (a.get(0).getAsFloat() + 0.5F) / aw, (a.get(1).getAsFloat() + 0.5F) / ah, (a.get(2).getAsFloat() - 0.5F) / aw, (a.get(3).getAsFloat() - 0.5F) / ah
      };
   }

   public static void setActive(int entityId, boolean on) {
      long now = System.nanoTime();
      EyeFeathers.State s = STATES.get(entityId);
      if (s == null) {
         if (!on) {
            return;
         }

         s = new EyeFeathers.State();
         STATES.put(entityId, s);
         s.from = 0.0F;
         s.on = true;
         s.changedNs = now;
      } else if (s.on != on) {
         s.from = progress(s, now);
         s.on = on;
         s.changedNs = now;
      }

      if (on) {
         s.seenNs = now;
      }
   }

   private static float progress(EyeFeathers.State s, long now) {
      float dt = (float)(now - s.changedNs) / 1.0E9F;
      float total = 1.5600001F;
      return s.on ? Math.min(1.0F, s.from + dt / total) : Math.max(0.0F, s.from - dt / 0.8F);
   }

   public static void clear() {
      STATES.clear();
      TOP_EYES.clear();
   }

   public static Vec3 topEye(int entityId, int index) {
      Vec3[] e = TOP_EYES.get(entityId);
      return e == null ? null : e[index];
   }

   public static boolean has(int entityId) {
      return STATES.containsKey(entityId);
   }

   public static boolean isActive(int entityId) {
      EyeFeathers.State s = STATES.get(entityId);
      return s != null && s.on;
   }

   public static void submit(
      PoseStack poseStack,
      SubmitNodeCollector collector,
      AvatarRenderState state,
      ModelPart body,
      WingPhysics.Pose drift,
      int tint,
      float headYaw,
      float headPitch
   ) {
      EyeFeathers.State s = STATES.get(state.id);
      if (s != null) {
         long now = System.nanoTime();
         if (s.on && (float)(now - s.seenNs) / 1.0E9F > 6.0F) {
            s.from = progress(s, now);
            s.on = false;
            s.changedNs = now;
         }

         float p = progress(s, now);
         if (!s.on && p <= 0.0F) {
            STATES.remove(state.id);
         } else {
            EyeFeathers.Feather[] fs = feathers();
            if (fs.length != 0) {
               float dim = ARGB.red(tint) / 255.0F;
               poseStack.pushPose();
               poseStack.translate(drift.modelOffset().x(), drift.modelOffset().y(), drift.modelOffset().z());
               poseStack.rotateDegrees(Axis.YP, drift.yawDeg());
               poseStack.translate(body.x / 16.0F, body.y / 16.0F, body.z / 16.0F);
               float t = (float)(now % 1000000000000L) / 1.0E9F;
               float tx = Mth.clamp(-Mth.sin(headYaw * (float) (Math.PI / 180.0)) / Mth.sin(0.87266463F), -1.0F, 1.0F);
               float ty = Mth.clamp(headPitch / 40.0F, -1.0F, 1.0F);
               float gdt = s.gazeNs == 0L ? 1.0F : Math.min(0.2F, (float)(now - s.gazeNs) / 1.0E9F);
               float gk = 1.0F - (float)Math.exp(-gdt / 0.1F);
               s.gx = s.gx + (tx - s.gx) * gk;
               s.gy = s.gy + (ty - s.gy) * gk;
               s.gazeNs = now;
               boolean live = LIVE.want(LIVE_ATLAS);

               for (EyeFeathers.Feather f : fs) {
                  if (f.name.equals("top")) {
                     Vec3[] eyes = new Vec3[2];
                     float yaw = f.spec.yawDeg * (float) (Math.PI / 180.0);
                     float lx = (f.iris.x0 + f.iris.x1) * 0.5F;
                     float ly = (f.iris.y0 + f.iris.y1) * 0.5F;

                     for (int side = 0; side < 2; side++) {
                        float sx = side == 1 ? -1.0F : 1.0F;
                        Vector3f m = new Vector3f((0.06F + lx * Mth.cos(yaw)) * sx, -0.08F + ly, f.spec.z - lx * Mth.sin(yaw));
                        m.add(body.x / 16.0F, body.y / 16.0F, body.z / 16.0F);
                        m.rotateY(drift.yawDeg() * (float) (Math.PI / 180.0));
                        m.add(drift.modelOffset());
                        m.add(0.0F, -1.501F, 0.0F).mul(0.9375F).mul(-1.0F, -1.0F, 1.0F);
                        m.rotateY((180.0F - state.bodyRot) * (float) (Math.PI / 180.0));
                        m.mul(state.scale);
                        eyes[side] = new Vec3(state.x + m.x, state.y + m.y, state.z + m.z);
                     }

                     TOP_EYES.put(state.id, eyes);
                  }
               }

               List<EyeFeathers.Draw> draws = new ArrayList<>(fs.length * 2);

               for (int side = 0; side < 2; side++) {
                  for (EyeFeathers.Feather fx : fs) {
                     float lp = local(p, fx, s.on);
                     if (!(lp <= 0.0F)) {
                        if (live) {
                           draws.add(new EyeFeathers.Draw(fx, side, lp, fx.base.live[side], null, fx.orn.live[side], null, 0.0F, t, dim, s.gx, s.gy));
                        } else {
                           int n = fx.base.frames.length;
                           float ft = t * fps + (side == 1 ? n * 0.5F : 0.0F);
                           int fa = Mth.floor(ft);
                           int a = Math.floorMod(fa, n);
                           int b = Math.floorMod(fa + 1, n);
                           draws.add(
                              new EyeFeathers.Draw(
                                 fx, side, lp, fx.base.frames[a], fx.base.frames[b], fx.orn.frames[a], fx.orn.frames[b], ft - fa, t, dim, s.gx, s.gy
                              )
                           );
                        }
                     }
                  }
               }

               Minecraft mc = Minecraft.getInstance();
               Vec3 camW = mc.gameRenderer.mainCamera().position();
               double yawR = Math.toRadians(state.bodyRot);
               double bx = -Math.sin(yawR);
               double bz = Math.cos(yawR);
               boolean behind = (camW.x - state.x) * bx + (camW.z - state.z) * bz < 0.0;
               boolean split = mc.level != null && water(mc, camW.x, camW.y, camW.z) != water(mc, state.x, state.y + 1.6, state.z);
               RenderType type = split ? RenderTypes.entityTranslucent(TEXTURE) : FxTypes.layered(TEXTURE);
               boolean oneSided = FxTypes.noCull(type);
               collector.order(behind ? 7 : 2).submitCustomGeometry(poseStack, type, (pose, buf) -> {
                  twoSided = !oneSided;
                  Vector3f cam = new Matrix4f(pose.pose()).invert().transformPosition(new Vector3f());
                  List<EyeFeathers.Draw> order = new ArrayList<>(draws);
                  order.sort((a, b) -> Float.compare(dist2(b, cam), dist2(a, cam)));

                  for (EyeFeathers.Draw d : order) {
                     drawFeather(pose, buf, d, cam);
                  }

                  twoSided = true;
               });
               RenderType depth = split ? null : FxTypes.depthOnly(TEXTURE);
               if (depth != null) {
                  collector.order(20).submitCustomGeometry(poseStack, depth, (pose, buf) -> {
                     Vector3f cam = new Matrix4f(pose.pose()).invert().transformPosition(new Vector3f());

                     for (EyeFeathers.Draw d : draws) {
                        drawFeather(pose, buf, d, cam);
                     }
                  });
               }

               poseStack.popPose();
            }
         }
      }
   }

   private static boolean water(Minecraft mc, double x, double y, double z) {
      return mc.level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);
   }

   private static float local(float p, EyeFeathers.Feather f, boolean on) {
      return !on ? p : Mth.clamp(p * 1.2F - f.spec.delay, 0.0F, 1.0F);
   }

   private static float dist2(EyeFeathers.Draw d, Vector3f cam) {
      EyeFeathers.Feather f = d.f;
      float sx = d.side == 1 ? -1.0F : 1.0F;
      float yaw = f.spec.yawDeg * (float) (Math.PI / 180.0);
      float lx = (f.base.x0 + f.base.x1) * 0.5F;
      float ly = (f.base.y0 + f.base.y1) * 0.5F;
      float x = (0.06F + lx * Mth.cos(yaw)) * sx - cam.x;
      float y = -0.08F + ly - cam.y;
      float z = f.spec.z - lx * Mth.sin(yaw) - cam.z;
      return x * x + y * y + z * z;
   }

   static float noise(float x, float y) {
      int ix = Mth.floor(x);
      int iy = Mth.floor(y);
      float fx = x - ix;
      float fy = y - iy;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      float a = hash(ix, iy);
      float b = hash(ix + 1, iy);
      float c = hash(ix, iy + 1);
      float d = hash(ix + 1, iy + 1);
      return Mth.lerp(fy, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
   }

   private static float hash(int x, int y) {
      int h = x * 374761393 + y * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      return ((h ^ h >>> 16) & 16777215) / 1.6777215E7F;
   }

   private static float frontAlpha(EyeFeathers.Feather f, float p, float lx, float ly) {
      if (p >= 1.0F) {
         return 1.0F;
      } else {
         float s = p * p * (3.0F - 2.0F * p);
         float front = -0.22F + s * 1.6800001F;
         float d = (len(lx, ly) - f.dMin) / Math.max(0.001F, f.dMax - f.dMin);
         return Mth.clamp((front - 0.11F - d) / 0.1F, 0.0F, 1.0F);
      }
   }

   private static void drawFeather(Pose pose, VertexConsumer buf, EyeFeathers.Draw d, Vector3f cam) {
      EyeFeathers.Feather f = d.f;
      float sx = d.side == 1 ? -1.0F : 1.0F;
      float yaw = f.spec.yawDeg * (float) (Math.PI / 180.0);
      float cyw = Mth.cos(yaw);
      float syw = Mth.sin(yaw);
      float toCam = (cam.z - f.spec.z) * cyw + (cam.x - 0.06F * sx) * syw * sx < 0.0F ? -1.0F : 1.0F;
      float mx = d.gx * sx;
      float ox = mx < 0.0F ? mx * f.gaze[0] : mx * f.gaze[1];
      float oy = d.gy < 0.0F ? d.gy * f.gaze[2] : d.gy * f.gaze[3];
      drawBase(pose, buf, d, sx, cyw, syw);
      EyeFeathers.Layer o = f.orn;
      float a = frontAlpha(f, d.p, (o.x0 + o.x1) * 0.5F, (o.y0 + o.y1) * 0.5F);
      rect(pose, buf, o.x0, o.y0, o.x1, o.y1, d.ornA, a * d.dim, sx, cyw, syw, f.spec.z + o.dz * toCam);
      if (d.ornB != null && d.mix > 0.01F) {
         rect(pose, buf, o.x0, o.y0, o.x1, o.y1, d.ornB, a * d.mix * d.dim, sx, cyw, syw, f.spec.z + o.dz * toCam);
      }

      part(pose, buf, f, f.iris, d, ox, oy, sx, cyw, syw, toCam);
      part(pose, buf, f, f.pupil, d, ox, oy, sx, cyw, syw, toCam);
   }

   private static void part(
      Pose pose,
      VertexConsumer buf,
      EyeFeathers.Feather f,
      EyeFeathers.Part pt,
      EyeFeathers.Draw d,
      float ox,
      float oy,
      float sx,
      float cyw,
      float syw,
      float toCam
   ) {
      float a = frontAlpha(f, d.p, (pt.x0 + pt.x1) * 0.5F, (pt.y0 + pt.y1) * 0.5F) * d.dim;
      rect(pose, buf, pt.x0 + ox, pt.y0 + oy, pt.x1 + ox, pt.y1 + oy, pt.uv, a, sx, cyw, syw, f.spec.z + pt.dz * toCam);
   }

   private static void drawBase(Pose pose, VertexConsumer buf, EyeFeathers.Draw d, float sx, float cyw, float syw) {
      EyeFeathers.Feather f = d.f;
      EyeFeathers.Layer L = f.base;
      if (d.p >= 1.0F) {
         rect(pose, buf, L.x0, L.y0, L.x1, L.y1, d.baseA, d.dim, sx, cyw, syw, f.spec.z);
         if (d.baseB != null && d.mix > 0.01F) {
            rect(pose, buf, L.x0, L.y0, L.x1, L.y1, d.baseB, d.mix * d.dim, sx, cyw, syw, f.spec.z);
         }
      } else {
         float p = d.p;
         float t = d.t;
         float s = p * p * (3.0F - 2.0F * p);
         float front = -0.22F + s * 1.6800001F;
         int n = 25;
         float[] gx = new float[n * n];
         float[] gy = new float[n * n];
         int[] cS = new int[n * n];
         int[] cB = new int[n * n];
         float span = Math.max(0.001F, f.dMax - f.dMin);

         for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
               int k = j * n + i;
               float lx = Mth.lerp(i / 24.0F, L.x0, L.x1);
               float ly = Mth.lerp(j / 24.0F, L.y0, L.y1);
               float dd = (len(lx, ly) - f.dMin) / span;
               float r = dd + (noise(lx * 2.2F + t * 0.6F, ly * 2.2F - t * 0.9F) - 0.5F) * 2.0F * 0.24F;
               float sharp = Mth.clamp((front - 0.11F - r) / 0.1F, 0.0F, 1.0F);
               float blur = Math.max(0.0F, 1.0F - Math.abs(r - front) / 0.22F);
               cS[k] = ARGB.colorFromFloat(sharp * d.dim, 1.0F, 1.0F, 1.0F);
               cB[k] = ARGB.colorFromFloat(blur * blur * 0.9F * (1.0F - sharp * 0.6F) * d.dim, 1.0F, 1.0F, 1.0F);
               float h = 0.07F * blur;
               gx[k] = lx + h * (noise(lx * 5.0F + t * 3.1F, ly * 5.0F) - 0.5F) * 2.0F;
               gy[k] = ly + h * (noise(lx * 5.0F + 17.0F, ly * 5.0F - t * 2.7F) - 0.5F) * 2.0F;
            }
         }

         grid(pose, buf, f.blur, gx, gy, cB, sx, cyw, syw, f.spec.z);
         grid(pose, buf, d.baseA, gx, gy, cS, sx, cyw, syw, f.spec.z);
      }
   }

   private static void rect(Pose pose, VertexConsumer buf, float x0, float y0, float x1, float y1, float[] u, float a, float sx, float cyw, float syw, float z) {
      if (!(a <= 0.003F)) {
         int col = ARGB.colorFromFloat(Math.min(1.0F, a), 1.0F, 1.0F, 1.0F);
         float[][] c = new float[][]{{x0, y0, u[0], u[1]}, {x1, y0, u[2], u[1]}, {x1, y1, u[2], u[3]}, {x0, y1, u[0], u[3]}};

         for (int w = 0; w < (twoSided ? 2 : 1); w++) {
            for (int i = 0; i < 4; i++) {
               float[] q = c[w == 0 ? i : 3 - i];
               vertex(pose, buf, q[0], q[1], q[2], q[3], col, sx, cyw, syw, z);
            }
         }
      }
   }

   private static void grid(Pose pose, VertexConsumer buf, float[] uv, float[] gx, float[] gy, int[] col, float sx, float cyw, float syw, float z) {
      int n = 25;

      for (int j = 0; j < 24; j++) {
         for (int i = 0; i < 24; i++) {
            int a = j * n + i;
            int b = a + 1;
            int c = a + n + 1;
            int dd = a + n;
            if (ARGB.alpha(col[a]) >= 1 || ARGB.alpha(col[b]) >= 1 || ARGB.alpha(col[c]) >= 1 || ARGB.alpha(col[dd]) >= 1) {
               int[] order = twoSided ? new int[]{a, b, c, dd, dd, c, b, a} : new int[]{a, b, c, dd};

               for (int v : order) {
                  int ii = v % n;
                  int jj = v / n;
                  vertex(pose, buf, gx[v], gy[v], Mth.lerp(ii / 24.0F, uv[0], uv[2]), Mth.lerp(jj / 24.0F, uv[1], uv[3]), col[v], sx, cyw, syw, z);
               }
            }
         }
      }
   }

   private static void vertex(Pose pose, VertexConsumer buf, float lx, float ly, float u, float v, int color, float sx, float cyw, float syw, float z) {
      buf.addVertex(pose, (0.06F + lx * cyw) * sx, -0.08F + ly, z - lx * syw)
         .setColor(color)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 0.0F, 1.0F);
   }

   private EyeFeathers() {
   }

   private record Draw(
      EyeFeathers.Feather f, int side, float p, float[] baseA, float[] baseB, float[] ornA, float[] ornB, float mix, float t, float dim, float gx, float gy
   ) {
   }

   private record Feather(
      String name,
      EyeFeathers.Spec spec,
      EyeFeathers.Layer base,
      EyeFeathers.Layer orn,
      EyeFeathers.Part iris,
      EyeFeathers.Part pupil,
      float[] blur,
      float[] gaze,
      float dMin,
      float dMax
   ) {
   }

   private record Layer(float x0, float y0, float x1, float y1, float dz, float[][] frames, float[][] live, int[][] framePx, int[][] livePx) {
   }

   private record Part(float x0, float y0, float x1, float y1, float dz, float[] uv) {
   }

   private record Spec(float z, float delay, float yawDeg) {
   }

   private static final class State {
      float gx;
      float gy;
      long gazeNs;
      boolean on;
      float from;
      long changedNs;
      long seenNs;
   }
}
