package dev.animecosmetics.look;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.animecosmetics.AnimeCosmetics;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class WingGlimmer {
   public static final Identifier TEXTURE = AnimeCosmetics.id("textures/entity/fx/wing_glint.png");
   private static final int SLOTS = 22;
   private static float[][] pts;

   private WingGlimmer() {
   }

   private static float[][] points() {
      if (pts == null) {
         pts = load("/assets/animecosmetics/meshes/custom_wing_glints.json");
      }

      return pts;
   }

   private static float[][] load(String path) {
      try {
         float[][] var10x;
         try (InputStream in = WingGlimmer.class.getResourceAsStream(path)) {
            JsonArray a = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            float[][] out = new float[a.size()][6];

            for (int i = 0; i < a.size(); i++) {
               JsonArray p = a.get(i).getAsJsonArray();

               for (int k = 0; k < 6; k++) {
                  out[i][k] = p.get(k).getAsFloat();
               }
            }

            var10x = out;
         }

         return var10x;
      } catch (Exception var10) {
         AnimeCosmetics.LOGGER.warn("{}: {}", path, var10.toString());
         return new float[0][6];
      }
   }

   private static float hash(int a, int b) {
      int h = a * 668265261 ^ b * 374761393;
      h ^= h >>> 15;
      h *= -2048144789;
      h ^= h >>> 13;
      return (h & 16777215) / 1.6777216E7F;
   }

   public static void render(Pose pose, VertexConsumer buf, float timeSec, int seed, boolean mirror) {
      float[][] p = points();
      if (p.length != 0) {
         float sx = mirror ? -1.0F : 1.0F;
         int side = mirror ? 7919 : 0;

         for (int i = 0; i < 22; i++) {
            int id = i + side + seed * 131;
            float period = 0.9F + 1.1F * hash(id, 1);
            float tt = timeSec / period + hash(id, 2);
            int cycle = (int)Math.floor(tt);
            float k = tt - cycle;
            if (!(k > 0.5F)) {
               float env = (float)Math.sin(Math.PI * k / 0.5);
               float[] q = p[(int)(hash(id, cycle * 7 + 3) * p.length) % p.length];
               float size = (0.025F + 0.04F * hash(id, cycle * 5 + 11)) * env;
               float ang = hash(id, cycle * 3 + 17) * 1.57F;
               boolean crimson = hash(id, cycle + 23) < 0.35F;
               int r = 255;
               int g = crimson ? 90 : 225;
               int b = crimson ? 90 : 220;
               int al = (int)(255.0F * Math.min(1.0F, env * 1.3F));
               float nx = q[3] * sx;
               float ny = q[4];
               float nz = q[5];
               float ax;
               float ay;
               float az;
               if (Math.abs(ny) < 0.9F) {
                  ax = -nz;
                  ay = 0.0F;
                  az = nx;
               } else {
                  ax = 0.0F;
                  ay = nz;
                  az = -ny;
               }

               float l = (float)Math.sqrt(ax * ax + ay * ay + az * az) + 1.0E-6F;
               ax /= l;
               ay /= l;
               az /= l;
               float bx = ny * az - nz * ay;
               float by = nz * ax - nx * az;
               float bz = nx * ay - ny * ax;
               float c = (float)Math.cos(ang);
               float s = (float)Math.sin(ang);
               float ux = (ax * c + bx * s) * size;
               float uy = (ay * c + by * s) * size;
               float uz = (az * c + bz * s) * size;
               float vx = (bx * c - ax * s) * size;
               float vy = (by * c - ay * s) * size;
               float vz = (bz * c - az * s) * size;

               for (int face = -1; face <= 1; face += 2) {
                  float ox = q[0] * sx + nx * 0.012F * face;
                  float oy = q[1] + ny * 0.012F * face;
                  float oz = q[2] + nz * 0.012F * face;
                  float[][] corner = new float[][]{{-1.0F, -1.0F, 0.0F, 1.0F}, {1.0F, -1.0F, 1.0F, 1.0F}, {1.0F, 1.0F, 1.0F, 0.0F}, {-1.0F, 1.0F, 0.0F, 0.0F}};
                  int[] order = face > 0 ? new int[]{0, 1, 2, 3} : new int[]{3, 2, 1, 0};

                  for (int o : order) {
                     float[] cc = corner[o];
                     buf.addVertex(pose, ox + ux * cc[0] + vx * cc[1], oy + uy * cc[0] + vy * cc[1], oz + uz * cc[0] + vz * cc[1])
                        .setColor(r, g, b, al)
                        .setUv(cc[2], cc[3])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(15728880)
                        .setNormal(pose, nx * face, ny * face, nz * face);
                  }
               }
            }
         }
      }
   }
}
