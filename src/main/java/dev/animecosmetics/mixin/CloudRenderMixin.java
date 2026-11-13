package dev.animecosmetics.mixin;

import dev.animecosmetics.AnimeCosmetics;
import dev.animecosmetics.fx.RiftVortex;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CloudRenderer.class})
public abstract class CloudRenderMixin {
   private static boolean animecosmetics$logged;

   @Inject(
      method = {"render(Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void animecosmetics$skipClouds(CallbackInfo ci) {
      float hide = RiftVortex.cloudHide();
      RiftVortex.cloudRenderCalls++;
      if (!animecosmetics$logged && hide > 0.5F) {
         animecosmetics$logged = true;
         AnimeCosmetics.LOGGER.info("rift vortex: vanilla clouds hidden under the dome");
      }

      if (hide > 0.5F) {
         ci.cancel();
      }
   }
}
