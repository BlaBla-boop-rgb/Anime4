package dev.animecosmetics.look;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.animecosmetics.AnimeCosmetics;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class CustomVeil {
   public static final Identifier TEXTURE = AnimeCosmetics.id("textures/entity/custom_veil.png");
   public static final Identifier CLIP_TEXTURE = AnimeCosmetics.id("textures/entity/custom_veil_clip.png");
   private static final float PX = 0.0625F;
   private static final Map<Integer, CustomVeil.Sim> SIMS = new HashMap<>();
   private static float[][] weights;
   private static float[][] heads;
   private static int[] parents;

   private CustomVeil() {
   }

   public static void clear() {
      SIMS.clear();
   }

   private static void loadRig() {
      try {
         try (InputStream in = CustomVeil.class.getResourceAsStream("/assets/animecosmetics/meshes/custom_veil.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray w = root.getAsJsonObject("veil").getAsJsonArray("w");
            weights = new float[w.size()][];

            for (int i = 0; i < w.size(); i++) {
               JsonArray a = w.get(i).getAsJsonArray();
               weights[i] = new float[a.size()];

               for (int k = 0; k < a.size(); k++) {
                  weights[i][k] = a.get(k).getAsFloat();
               }
            }

            JsonArray b = root.getAsJsonArray("bones");
            heads = new float[b.size()][3];
            parents = new int[b.size()];

            for (int i = 0; i < b.size(); i++) {
               JsonObject o = b.get(i).getAsJsonObject();
               parents[i] = o.get("parent").getAsInt();
               JsonArray h = o.getAsJsonArray("head");

               for (int k = 0; k < 3; k++) {
                  heads[i][k] = h.get(k).getAsFloat();
               }
            }
         }
      } catch (IOException var101) {
         throw new UncheckedIOException(var101);
      }
   }

   public static float[][] simulate(AvatarRenderState state, SkirtPhysics.Pose phys, BodyFrame frame, ModelPart body, ModelPart head) {
      WingMesh.Group g = WingMesh.customVeil().group("veil");
      if (g == null) {
         return null;
      } else {
         double t = state.ageInTicks / 20.0;
         CustomVeil.Sim s = SIMS.get(LookKey.of(state.id));
         if (s == null) {
            if (weights == null) {
               loadRig();
            }

            float[] v = g.v();
            int n = v.length / 8;
            float[] p = new float[n * 3];
            float[] nr = new float[n * 3];

            for (int i = 0; i < n; i++) {
               for (int k = 0; k < 3; k++) {
                  p[i * 3 + k] = v[i * 8 + k] / 0.0625F;
                  nr[i * 3 + k] = v[i * 8 + 5 + k];
               }
            }

            s = new CustomVeil.Sim(new DressSim(p, weights, heads, parents, DressSim.Config.veil()));
            s.sim.setNormals(nr);
            SIMS.put(LookKey.of(state.id), s);
         }

         s.lastSeen = t;
         if (SIMS.size() > 8) {
            SIMS.values().removeIf(o -> Math.abs(t - o.lastSeen) > 10.0);
         }

         Matrix3f rb = new Matrix3f().rotationZYX(body.zRot, body.yRot, body.xRot);
         Matrix3f rbt = new Matrix3f(rb).transpose();
         Matrix3f rel = new Matrix3f(rbt).mul(new Matrix3f().rotationZYX(head.zRot, head.yRot, head.xRot));
         Vector3f off = new Vector3f(head.x - body.x, head.y - body.y, head.z - body.z);
         rbt.transform(off);
         s.sim.setRootFrame(rows(rel), new float[]{off.x, off.y, off.z});
         Matrix3f m3 = new Matrix3f().scaling(-1.0F, -1.0F, 1.0F).mul(new Matrix3f().rotationY((float)Math.toRadians(-(180.0 - state.bodyRot))));
         float[] out = new float[g.v().length / 8 * 3];
         SkirtPhysics.Ground ground = phys.ground();
         Vector3f c = frame.c();
         s.sim.step(rows(frame.a()), new float[]{c.x, c.y, c.z}, rows(m3), state.x, state.y, state.z, t, ground::rawPx, out);
         return new float[][]{out, (float[])s.sim.normals().clone()};
      }
   }

   private static float[] rows(Matrix3f m) {
      return new float[]{m.m00, m.m10, m.m20, m.m01, m.m11, m.m21, m.m02, m.m12, m.m22};
   }

   public static void drawVeil(float[][] sim, Pose pose, VertexConsumer buf, int light, int overlay, int color) {
      WingMesh.Group g = WingMesh.customVeil().group("veil");
      if (g != null && sim != null) {
         float[] v = g.v();
         float[] p = sim[0];
         float[] nr = sim[1];

         for (int idx : g.q()) {
            int o = idx * 8;
            buf.addVertex(pose, p[idx * 3] * 0.0625F, p[idx * 3 + 1] * 0.0625F, p[idx * 3 + 2] * 0.0625F)
               .setColor(color)
               .setUv(v[o + 3], v[o + 4])
               .setOverlay(overlay)
               .setLight(light)
               .setNormal(pose, nr[idx * 3], nr[idx * 3 + 1], nr[idx * 3 + 2]);
         }
      }
   }

   public static void drawClip(Pose pose, VertexConsumer buf, int light, int overlay, int color) {
      WingMesh.Group g = WingMesh.customVeil().group("clip");
      if (g != null) {
         float[] v = g.v();

         for (int idx : g.q()) {
            int o = idx * 8;
            buf.addVertex(pose, v[o], v[o + 1], v[o + 2])
               .setColor(color)
               .setUv(v[o + 3], v[o + 4])
               .setOverlay(overlay)
               .setLight(light)
               .setNormal(pose, v[o + 5], v[o + 6], v[o + 7]);
         }
      }
   }

   private static final class Sim {
      final DressSim sim;
      double lastSeen;

      Sim(DressSim sim) {
         this.sim = sim;
      }
   }
}
