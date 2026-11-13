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
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class CustomDress {
   public static final Identifier TEXTURE = AnimeCosmetics.id("textures/entity/custom_dress.png");
   public static final Identifier CLASP_TEXTURE = AnimeCosmetics.id("textures/entity/custom_clasp.png");
   private static final float PX = 0.0625F;
   private static final Map<Integer, CustomDress.Sim> SIMS = new HashMap<>();
   private static float[][] weights;
   private static float[][] heads;
   private static int[] parents;

   private CustomDress() {
   }

   public static void render(float[][] sim, Pose pose, VertexConsumer buf, int light, int overlay, int color) {
      draw(sim, pose, buf, light, overlay, color);
   }

   public static void clear() {
      SIMS.clear();
   }

   private static void loadRig() {
      try {
         try (InputStream in = CustomDress.class.getResourceAsStream("/assets/animecosmetics/meshes/custom_dress.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray w = root.getAsJsonObject("dress").getAsJsonArray("w");
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

   public static float[][] simulate(AvatarRenderState state, SkirtPhysics.Pose phys, BodyFrame frame, WingPhysics.Pose drift) {
      WingMesh.Group g = WingMesh.customDress().group("dress");
      if (g == null) {
         return null;
      } else {
         double t = state.ageInTicks / 20.0;
         CustomDress.Sim s = SIMS.get(LookKey.of(state.id));
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

            s = new CustomDress.Sim(new DressSim(p, weights, heads, parents));
            s.sim.setNormals(nr);
            SIMS.put(LookKey.of(state.id), s);
         }

         s.lastSeen = t;
         if (SIMS.size() > 8) {
            SIMS.values().removeIf(o -> Math.abs(t - o.lastSeen) > 10.0);
         }

         float[] a = rows(frame.a());
         Vector3f c = frame.c();
         Matrix3f m3 = new Matrix3f().scaling(-1.0F, -1.0F, 1.0F).mul(new Matrix3f().rotationY((float)Math.toRadians(-(180.0 - state.bodyRot))));
         float[] out = new float[g.v().length / 8 * 3];
         SkirtPhysics.Ground ground = phys.ground();
         s.sim.step(a, new float[]{c.x, c.y, c.z}, rows(m3), state.x, state.y, state.z, t, ground::rawPx, out);
         return new float[][]{out, (float[])s.sim.normals().clone()};
      }
   }

   private static float[] rows(Matrix3f m) {
      return new float[]{m.m00, m.m10, m.m20, m.m01, m.m11, m.m21, m.m02, m.m12, m.m22};
   }

   public static void drawClasps(Pose pose, VertexConsumer buf, int light, int overlay, int color) {
      WingMesh.Group g = WingMesh.customDress().group("clasp");
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

   public static void draw(float[][] sim, Pose pose, VertexConsumer buf, int light, int overlay, int color) {
      WingMesh.Group g = WingMesh.customDress().group("dress");
      if (g != null && sim != null) {
         float[] v = g.v();
         int[] q = g.q();
         float[] p = sim[0];
         float[] nr = sim[1];

         for (int i = 0; i < q.length; i++) {
            int idx = q[i];
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

   private static final class Sim {
      final DressSim sim;
      double lastSeen;

      Sim(DressSim sim) {
         this.sim = sim;
      }
   }
}
