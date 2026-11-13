package dev.animecosmetics.mixin;

import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderSetup.class})
public interface RenderSetupAccessor {
   @Accessor("oitPipelineSet")
   OitPipelineSet animecosmetics$oit();
}
