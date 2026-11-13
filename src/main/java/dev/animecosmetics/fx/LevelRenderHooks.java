package dev.animecosmetics.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public final class LevelRenderHooks {
   private static final List<Consumer<LevelRenderHooks.Context>> LISTENERS = new ArrayList<>();

   private LevelRenderHooks() {
   }

   public static void onCollectSubmits(Consumer<LevelRenderHooks.Context> listener) {
      LISTENERS.add(listener);
   }

   public static void fire(LevelRenderState levelState, SubmitNodeCollector collector) {
      if (!LISTENERS.isEmpty()) {
         LevelRenderHooks.Context ctx = new LevelRenderHooks.Context(levelState, collector);

         for (Consumer<LevelRenderHooks.Context> l : LISTENERS) {
            l.accept(ctx);
         }
      }
   }

   public record Context(LevelRenderState levelState, SubmitNodeCollector submitNodeCollector) {
   }
}
