package dev.animecosmetics.mixin;

import dev.animecosmetics.fx.RiftVortex;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelExtractor.class})
public abstract class CloudFadeMixin {
   @Shadow
   @Final
   private LevelRenderState levelRenderState;

   @Inject(
      method = {"extract"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;cloudColor:I",
         opcode = 181,
         shift = Shift.AFTER
      )},
      require = 0
   )
   private void animecosmetics$fadeClouds(CallbackInfo ci) {
      float hide = RiftVortex.cloudHide();
      int c = this.levelRenderState.cloudColor;
      if (hide > 0.001F) {
         c = ARGB.color(Math.round(ARGB.alpha(c) * (1.0F - hide)), ARGB.red(c), ARGB.green(c), ARGB.blue(c));
         this.levelRenderState.cloudColor = c;
      }

      RiftVortex.debugCloudColor = c;
   }
}
