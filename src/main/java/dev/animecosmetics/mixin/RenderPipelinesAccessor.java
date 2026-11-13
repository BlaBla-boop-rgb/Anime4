package dev.animecosmetics.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderPipelines.class})
public interface RenderPipelinesAccessor {
   @Accessor("ENTITY_SNIPPET")
   static Snippet animecosmetics$entitySnippet() {
      throw new AssertionError();
   }

   @Accessor("GLOBALS_SNIPPET")
   static Snippet animecosmetics$globalsSnippet() {
      throw new AssertionError();
   }
}
