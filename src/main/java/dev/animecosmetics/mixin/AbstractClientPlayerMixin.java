package dev.animecosmetics.mixin;

import dev.animecosmetics.AnimeSkin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class AbstractClientPlayerMixin {
   @Inject(
      method = {"getSkin"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void animecosmetics$skin(CallbackInfoReturnable<PlayerSkin> cir) {
      PlayerSkin skin = AnimeSkin.replace(this, (PlayerSkin)cir.getReturnValue());
      if (skin != null) {
         cir.setReturnValue(skin);
      }
   }
}
