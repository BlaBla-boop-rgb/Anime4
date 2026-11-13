package dev.animecosmetics.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({RenderType.class})
public interface RenderTypeAccessor {
   @Accessor("state")
   RenderSetup animecosmetics$state();

   @Invoker("create")
   static RenderType animecosmetics$create(String name, RenderSetup state) {
      throw new AssertionError();
   }
}
