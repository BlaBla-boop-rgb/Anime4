package dev.animecosmetics.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.animecosmetics.fx.LateGlow;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer.Submit;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SubmitNodeCollection.class})
public abstract class SubmitNodeCollectionMixin {
   @Unique
   private static boolean animecosmetics$oitMode(SubmitNodeCollection self) {
      return self.translucentCustomGeometry == self.oitTranslucent;
   }

   @Unique
   private static boolean animecosmetics$noOit(SubmitNodeCollection self, RenderType type) {
      return animecosmetics$oitMode(self)
         && !type.isOutline()
         && ((RenderSetupAccessor)(Object)((RenderTypeAccessor)type).animecosmetics$state()).animecosmetics$oit() == null;
   }

   @Inject(
      method = {"submitCustomGeometry"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void animecosmetics$lateGlow(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer renderer, CallbackInfo ci) {
      SubmitNodeCollection self = (SubmitNodeCollection)(Object)this;
      if (animecosmetics$noOit(self, renderType)) {
         self.solid.submit(new Submit(poseStack.last().copy(), renderType, renderer));
         ci.cancel();
      } else if (!animecosmetics$oitMode(self) && LateGlow.isLate(renderType)) {
         Pose pose = poseStack.last().copy();
         self.afterTerrain.submit(new Submit(pose, renderType, renderer));
         ci.cancel();
      }
   }
}
