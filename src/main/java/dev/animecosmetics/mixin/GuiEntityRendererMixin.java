package dev.animecosmetics.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.animecosmetics.look.LookKey;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiEntityRenderer.class})
public abstract class GuiEntityRendererMixin {
   @Inject(
      method = {"renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"},
      at = {@At("HEAD")}
   )
   private void animecosmetics$guiStart(GuiEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CallbackInfo ci) {
      LookKey.inGui = true;
   }

   @Inject(
      method = {"renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"},
      at = {@At("RETURN")}
   )
   private void animecosmetics$guiEnd(GuiEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CallbackInfo ci) {
      LookKey.inGui = false;
   }
}
