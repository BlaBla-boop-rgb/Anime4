package dev.animecosmetics.look;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.animecosmetics.AnimeCosmetics;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

public final class WingMesh {
   private final Map<String, WingMesh.Group> groups = new LinkedHashMap<>();
   private static WingMesh pins;
   private static WingMesh customWings;
   private static WingMesh customVeil;
   private static WingMesh customDress;
   private static final float SOFT_GAIN = 0.8F;
   private static final float SHADER_ALPHA = 0.55F;
   private int[] guiColors;
   private int[] shaderColors;
   private static final Vector3f AURA_NORMAL = new Vector3f();
   private int[] northColors;
   private float[] northT;
   private float northSec = Float.NaN;
   private float northDim;
   private static final double RAY_CX = 0.5625;
   private static final double RAY_CY = -0.5;

   private WingMesh(JsonObject json) {
      for (String name : json.keySet()) {
         if (json.get(name).isJsonObject()) {
            JsonObject g = json.getAsJsonObject(name);
            if (g.has("v")) {
               this.groups.put(name, new WingMesh.Group(floats(g.getAsJsonArray("v")), ints(g.getAsJsonArray("q"))));
            }
         }
      }
   }

   public static WingMesh pins() {
      if (pins == null) {
         pins = load("/assets/animecosmetics/meshes/custom_pin_mesh.json");
      }

      return pins;
   }

   public static WingMesh customWings() {
      if (customWings == null) {
         customWings = load("/assets/animecosmetics/meshes/custom_wing_mesh.json");
      }

      return customWings;
   }

   public static WingMesh customVeil() {
      if (customVeil == null) {
         customVeil = load("/assets/animecosmetics/meshes/custom_veil.json");
      }

      return customVeil;
   }

   public static WingMesh customDress() {
      if (customDress == null) {
         customDress = load("/assets/animecosmetics/meshes/custom_dress.json");
      }

      return customDress;
   }

   private static WingMesh load(String path) {
      try {
         WingMesh var3;
         try (InputStream in = WingMesh.class.getResourceAsStream(path)) {
            if (in == null) {
               throw new IllegalStateException("missing " + path);
            }

            WingMesh m = new WingMesh(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            AnimeCosmetics.LOGGER.info("mesh loaded {}: {}", path, m.describe());
            var3 = m;
         }

         return var3;
      } catch (Exception var7) {
         throw new IllegalStateException("cannot load mesh " + path, var7);
      }
   }

   private String describe() {
      StringBuilder sb = new StringBuilder();
      this.groups.forEach((k, g) -> sb.append(k).append('=').append(g.vertexCount()).append("v/").append(g.q.length / 4).append("q "));
      return sb.toString().trim();
   }

   public WingMesh.Group group(String name) {
      return this.groups.get(name);
   }

   public void render(String name, Pose pose, VertexConsumer buf, int light, int overlay, int color, boolean mirror) {
      WingMesh.Group g = this.groups.get(name);
      if (g != null) {
         float[] v = g.v;
         int[] q = g.q;
         float sx = mirror ? -1.0F : 1.0F;

         for (int i = 0; i < q.length; i += 4) {
            if (mirror) {
               vertex(v, q[i + 3], pose, buf, light, overlay, color, sx);
               vertex(v, q[i + 2], pose, buf, light, overlay, color, sx);
               vertex(v, q[i + 1], pose, buf, light, overlay, color, sx);
               vertex(v, q[i], pose, buf, light, overlay, color, sx);
            } else {
               vertex(v, q[i], pose, buf, light, overlay, color, sx);
               vertex(v, q[i + 1], pose, buf, light, overlay, color, sx);
               vertex(v, q[i + 2], pose, buf, light, overlay, color, sx);
               vertex(v, q[i + 3], pose, buf, light, overlay, color, sx);
            }
         }
      }
   }

   public void renderAuraSoft(Pose pose, VertexConsumer buf, float sec, boolean mirror, float dim, boolean bothSides) {
      this.renderAuraSoft(pose, buf, sec, mirror, dim, bothSides, false);
   }

   public void renderAuraSoft(Pose pose, VertexConsumer buf, float sec, boolean mirror, float dim, boolean bothSides, boolean gui) {
      WingMesh.Group g = this.groups.get("aura");
      if (g != null) {
         float[] v = g.v;
         int[] q = g.q;
         float sx = mirror ? -1.0F : 1.0F;
         int n = v.length / 8;
         if (this.northColors == null || this.northColors.length != n) {
            this.northColors = new int[n];
            this.northT = new float[n];

            for (int idx = 0; idx < n; idx++) {
               int o = idx * 8;
               this.northT[idx] = (float)((Math.atan2(v[o + 1] - -0.5, -v[o] - 0.5625) + Math.PI) / (Math.PI * 2));
            }

            this.northSec = Float.NaN;
         }

         int[] col = this.northColors;
         if (this.guiColors == null || this.guiColors.length != n) {
            this.guiColors = new int[n];
            this.northSec = Float.NaN;
         }

         if (sec != this.northSec || dim != this.northDim) {
            this.northSec = sec;
            this.northDim = dim;

            for (int idx = 0; idx < n; idx++) {
               float f = v[idx * 8 + 5];
               float t = this.northT[idx];
               float k = 1.0F - f;
               float ray = 0.75F
                  + 0.25F
                     * (0.5F + 0.5F * (float)Math.sin(t * 6.283F * 7.0F + sec * 2.2F))
                     * (0.5F + 0.5F * (float)Math.sin(t * 6.283F * 3.0F - sec * 1.1F + f * 1.5F));
               float a = 0.8F * ray * dim;
               float ws = 0.5F + 0.5F * (float)Math.sin(t * 6.283F * 2.0F + sec * 0.8F + f * 2.0F);
               float w = ws * ws * (3.0F - 2.0F * ws);
               float ra = 1.0F;
               float ga = 0.06F + 0.24000001F * f;
               float ba = 0.16F + -0.12F * f;
               float rb = 1.0F;
               float gb = 0.8F + 0.14999998F * f;
               float bb2 = 0.2F + 0.24999999F * f;
               float r = ra + (rb - ra) * w;
               float gg = ga + (gb - ga) * w;
               float bb = ba + (bb2 - ba) * w;
               int ri = (int)(255.0F * Math.min(1.0F, r * a));
               int gi = (int)(255.0F * Math.min(1.0F, gg * a));
               int bi = (int)(255.0F * Math.min(1.0F, bb * a));
               col[idx] = 0xFF000000 | ri << 16 | gi << 8 | bi;
               int ai = (int)(255.0F * Math.min(1.0F, a / 0.8F));
               this.guiColors[idx] = ai << 24 | (int)(255.0F * r) << 16 | (int)(255.0F * gg) << 8 | (int)(255.0F * bb);
            }
         }

         if (gui) {
            col = this.guiColors;
            if (!LookKey.inGui) {
               if (this.shaderColors == null || this.shaderColors.length != n) {
                  this.shaderColors = new int[n];
               }

               for (int idx = 0; idx < n; idx++) {
                  int c = this.guiColors[idx];
                  this.shaderColors[idx] = (int)((c >>> 24) * 0.55F) << 24 | c & 16777215;
               }

               col = this.shaderColors;
            }
         }

         int[] order = bothSides ? new int[]{0, 1, 2, 3, 3, 2, 1, 0} : new int[]{0, 1, 2, 3};
         Vector3f nrm = pose.transformNormal(0.0F, -1.0F, 0.0F, AURA_NORMAL);
         float nx = nrm.x;
         float ny = nrm.y;
         float nz = nrm.z;
         int overlay = OverlayTexture.NO_OVERLAY;

         for (int i = 0; i < q.length; i += 4) {
            for (int k = 0; k < order.length; k++) {
               int idx = q[i + order[k]];
               int o = idx * 8;
               buf.addVertex(pose, v[o] * sx, v[o + 1], v[o + 2])
                  .setColor(col[idx])
                  .setUv(v[o + 3], v[o + 4])
                  .setOverlay(overlay)
                  .setLight(15728880)
                  .setNormal(nx, ny, nz);
            }
         }
      }
   }

   private static void vertex(float[] v, int idx, Pose pose, VertexConsumer buf, int light, int overlay, int color, float sx) {
      int o = idx * 8;
      buf.addVertex(pose, v[o] * sx, v[o + 1], v[o + 2])
         .setColor(color)
         .setUv(v[o + 3], v[o + 4])
         .setOverlay(overlay)
         .setLight(light)
         .setNormal(pose, v[o + 5] * sx, v[o + 6], v[o + 7]);
   }

   private static float[] floats(JsonArray a) {
      float[] out = new float[a.size()];

      for (int i = 0; i < out.length; i++) {
         out[i] = a.get(i).getAsFloat();
      }

      return out;
   }

   private static int[] ints(JsonArray a) {
      int[] out = new int[a.size()];

      for (int i = 0; i < out.length; i++) {
         out[i] = a.get(i).getAsInt();
      }

      return out;
   }

   public record Group(float[] v, int[] q) {
      int vertexCount() {
         return this.v.length / 8;
      }
   }
}
