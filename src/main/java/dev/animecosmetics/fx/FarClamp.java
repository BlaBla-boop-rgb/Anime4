package dev.animecosmetics.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FarClamp {
   private static float limit = Float.MAX_VALUE;
   private static long frame = -1L;

   private FarClamp() {
   }

   public static float limit() {
      k(0.0, 0.0, 0.0);
      return limit;
   }

   public static float k(double dx, double dy, double dz) {
      long f = Minecraft.getInstance().getFrameTimeNs();
      if (f != frame) {
         frame = f;
         Minecraft mc = Minecraft.getInstance();
         float rd = mc.options.getEffectiveRenderDistance() * 16;
         if (FxTypes.shadersOn()) {
            limit = (rd - Mth.clamp(rd / 10.0F, 4.0F, 64.0F)) * 0.95F;
         } else {
            limit = Math.max(rd * 4.0F, (float)((Integer)mc.options.cloudRange().get() * 16)) * 0.95F;
         }
      }

      double d2 = dx * dx + dy * dy + dz * dz;
      return d2 <= (double)limit * limit ? 1.0F : (float)(limit / Math.sqrt(d2));
   }
}
