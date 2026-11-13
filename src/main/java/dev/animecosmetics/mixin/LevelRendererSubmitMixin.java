package dev.animecosmetics.mixin;

import dev.animecosmetics.fx.LevelRenderHooks;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public abstract class LevelRendererSubmitMixin {
   @Inject(
      method = {"submitFeatures"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/state/level/ParticlesRenderState;submit(Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
         shift = Shift.AFTER
      )}
   )
   private void animecosmetics$collectSubmits(LevelRenderState levelState, SubmitNodeCollector collector, boolean withOutline, CallbackInfo ci) {
      LevelRenderHooks.fire(levelState, collector);
   }
}
