package dev.animecosmetics;

import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.animecosmetics.fx.RiftFx;
import dev.animecosmetics.fx.SkyFx;
import dev.animecosmetics.gui.CosmeticsScreen;
import dev.animecosmetics.look.AnimeLookLayer;
import dev.animecosmetics.look.CustomDress;
import dev.animecosmetics.look.CustomVeil;
import dev.animecosmetics.look.EyeFeathers;
import dev.animecosmetics.look.SkirtPhysics;
import dev.animecosmetics.look.WingPhysics;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public final class AnimeCosmeticsClient implements ClientModInitializer {
   private static final Category CATEGORY = Category.register(AnimeCosmetics.id("main"));
   /** GLFW key code of R. */
   private static final int KEY_R = 82;
   /** Opens the menu where every cosmetic is switched on or off. */
   private static final KeyMapping KEY_MENU = new KeyMapping("key.animecosmetics.menu", Type.KEYBOARD, KEY_R, CATEGORY);

   public void onInitializeClient() {
      KeyMappingHelper.registerKeyMapping(KEY_MENU);
      SkyFx.register();
      RiftFx.register();
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         while (KEY_MENU.consumeClick()) {
            openMenu(client);
         }

         RiftFx.tick(client);
      });
      LivingEntityRenderLayerRegistrationCallback.EVENT.register((LivingEntityRenderLayerRegistrationCallback)(entityType, renderer, helper, context) -> {
         if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
            helper.register(cast(new AnimeLookLayer(avatarRenderer)));
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         WingPhysics.clear();
         SkirtPhysics.clear();
         CustomDress.clear();
         CustomVeil.clear();
         EyeFeathers.clear();
         RiftFx.clear();
      });
   }

   private static void openMenu(Minecraft client) {
      if (client.player != null && client.gui.screen() == null) {
         client.gui.setScreen(new CosmeticsScreen(KEY_MENU));
      }
   }

   private static <L> L cast(Object layer) {
      return (L)layer;
   }
}
