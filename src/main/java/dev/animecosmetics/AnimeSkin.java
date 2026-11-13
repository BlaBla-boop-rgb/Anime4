package dev.animecosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public final class AnimeSkin {
   private static final Texture BODY = new ResourceTexture(AnimeCosmetics.id("entity/anime_skin"));

   private AnimeSkin() {
   }

   public static PlayerSkin replace(Object player, PlayerSkin original) {
      Minecraft mc = Minecraft.getInstance();
      return original != null && player == mc.player && Toggles.get(Toggles.Feature.SKIN)
         ? new PlayerSkin(BODY, original.cape(), original.elytra(), PlayerModelType.SLIM, true)
         : null;
   }
}
