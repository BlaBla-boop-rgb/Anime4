package dev.animecosmetics.fx;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class LateGlow {
   private static final Set<RenderType> LATE = Collections.newSetFromMap(new IdentityHashMap<>());
   private static final Set<RenderType> SOLID = Collections.newSetFromMap(new IdentityHashMap<>());

   private LateGlow() {
   }

   public static RenderType late(RenderType type) {
      LATE.add(type);
      return type;
   }

   public static boolean isLate(RenderType type) {
      return LATE.contains(type);
   }

   public static RenderType solid(RenderType type) {
      SOLID.add(type);
      return type;
   }

   public static boolean isSolid(RenderType type) {
      return SOLID.contains(type);
   }
}
