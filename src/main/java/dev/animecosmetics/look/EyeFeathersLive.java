package dev.animecosmetics.look;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import dev.animecosmetics.AnimeCosmetics;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public final class EyeFeathersLive {
   private final String atlasPath;
   private final Identifier texture;
   private final String name;
   private static final long PERIOD_NS = 25000000L;
   private static final long IDLE_NS = 2000000000L;
   private volatile EyeFeathersLive.Cells[] cells;
   private float fps = 16.0F;
   private volatile long wantNs;
   private volatile boolean pending;
   private volatile int back = 1;
   private Thread worker;
   private volatile boolean failed;
   private boolean uploadedOnce;
   private boolean starting;

   public EyeFeathersLive(String atlasPath, Identifier texture, String name) {
      this.atlasPath = atlasPath;
      this.texture = texture;
      this.name = name;
   }

   public boolean want(EyeFeathersLive.EyeFeathersAtlas atlas) {
      this.wantNs = System.nanoTime();
      if (this.failed) {
         return false;
      } else if (this.cells == null) {
         if (!this.starting) {
            this.starting = true;
            this.worker = new Thread(() -> {
               if (this.init(atlas)) {
                  this.run();
               } else {
                  this.failed = true;
               }
            }, this.name);
            this.worker.setDaemon(true);
            this.worker.start();
         }

         return false;
      } else {
         if (this.pending) {
            this.upload();
         }

         return this.uploadedOnce;
      }
   }

   private boolean init(EyeFeathersLive.EyeFeathersAtlas atlas) {
      try {
         boolean var17;
         try (InputStream in = EyeFeathersLive.class.getResourceAsStream(this.atlasPath)) {
            if (in == null) {
               AnimeCosmetics.LOGGER.error("eye feathers live: atlas missing");
               return false;
            }

            NativeImage img = NativeImage.read(in);
            int aw = img.getWidth();
            int[] px = img.getPixelsABGR();
            img.close();
            EyeFeathersLive.Cells[] out = new EyeFeathersLive.Cells[atlas.count()];

            for (int f = 0; f < out.length; f++) {
               int[][] fc = atlas.frameCells(f);
               int[][] lc = atlas.liveCells(f);
               EyeFeathersLive.Cells c = new EyeFeathersLive.Cells();
               c.w = fc[0][2] - fc[0][0];
               c.h = fc[0][3] - fc[0][1];
               c.frames = new int[fc.length][c.w * c.h];

               for (int k = 0; k < fc.length; k++) {
                  for (int j = 0; j < c.h; j++) {
                     System.arraycopy(px, (fc[k][1] + j) * aw + fc[k][0], c.frames[k], j * c.w, c.w);
                  }
               }

               c.live = new int[][]{{lc[0][0], lc[0][1]}, {lc[1][0], lc[1][1]}};
               c.buf = new NativeImage[2][2];

               for (int side = 0; side < 2; side++) {
                  for (int b = 0; b < 2; b++) {
                     c.buf[side][b] = new NativeImage(c.w, c.h, false);
                  }
               }

               out[f] = c;
            }

            this.fps = atlas.fps();
            this.cells = out;
            var17 = true;
         }

         return var17;
      } catch (Exception var16) {
         AnimeCosmetics.LOGGER.error("eye feathers live", var16);
         return false;
      }
   }

   private void upload() {
      try {
         AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(this.texture);
         CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
         int b = this.back;

         for (EyeFeathersLive.Cells c : this.cells) {
            for (int side = 0; side < 2; side++) {
               enc.writeToTexture(tex.getTexture(), c.buf[side][b], 0, 0, c.live[side][0], c.live[side][1]);
            }
         }

         this.back = 1 - b;
         this.uploadedOnce = true;
      } catch (Exception var121) {
         AnimeCosmetics.LOGGER.warn("eye feathers live upload: {}", var121.toString());
      } finally {
         this.pending = false;
      }
   }

   private void run() {
      try {
         while (true) {
            long now = System.nanoTime();
            if (now - this.wantNs <= 2000000000L && !this.pending) {
               float t = (float)(now % 1000000000000L) / 1.0E9F;
               int b = this.back;

               for (EyeFeathersLive.Cells c : this.cells) {
                  int n = c.frames.length;

                  for (int side = 0; side < 2; side++) {
                     double ft = t * this.fps + (side == 1 ? n * 0.5 : 0.0);
                     int fa = (int)Math.floor(ft);
                     int m8 = (int)((ft - fa) * 256.0);
                     mix(c.frames[Math.floorMod(fa, n)], c.frames[Math.floorMod(fa + 1, n)], m8, c.buf[side][b], c.w, c.h);
                  }
               }

               this.pending = true;
               long spent = System.nanoTime() - now;
               if (spent < 25000000L) {
                  Thread.sleep((25000000L - spent) / 1000000L, (int)((25000000L - spent) % 1000000L));
               }
            } else {
               Thread.sleep(this.pending ? 2L : 100L);
            }
         }
      } catch (InterruptedException var15) {
      } catch (Throwable var161) {
         AnimeCosmetics.LOGGER.error("eye feathers live worker", var161);
         this.failed = true;
      }
   }

   private static void mix(int[] a, int[] b, int m8, NativeImage out, int w, int h) {
      int im8 = 256 - m8;
      int j = 0;

      for (int k = 0; j < h; j++) {
         for (int i = 0; i < w; k++) {
            int pa = a[k];
            int pb = b[k];
            int wa = (pa >>> 24) * im8;
            int wb = (pb >>> 24) * m8;
            int ws = wa + wb;
            int v;
            if (ws == 0) {
               v = 0;
            } else {
               int r = ((pa & 0xFF) * wa + (pb & 0xFF) * wb) / ws;
               int g = ((pa >>> 8 & 0xFF) * wa + (pb >>> 8 & 0xFF) * wb) / ws;
               int bl = ((pa >>> 16 & 0xFF) * wa + (pb >>> 16 & 0xFF) * wb) / ws;
               v = ws >>> 8 << 24 | bl << 16 | g << 8 | r;
            }

            out.setPixelABGR(i, j, v);
            i++;
         }
      }
   }

   private static final class Cells {
      int w;
      int h;
      int[][] frames;
      int[][] live;
      NativeImage[][] buf;
   }

   public interface EyeFeathersAtlas {
      int count();

      int[][] frameCells(int var1);

      int[][] liveCells(int var1);

      float fps();
   }
}
