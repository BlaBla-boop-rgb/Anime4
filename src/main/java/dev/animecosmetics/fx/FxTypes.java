package dev.animecosmetics.fx;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import dev.animecosmetics.AnimeCosmetics;
import dev.animecosmetics.mixin.RenderPipelinesAccessor;
import dev.animecosmetics.mixin.RenderTypeAccessor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class FxTypes {
   private static final RenderPipeline SOFT_NOCULL = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/fx_soft_nocull"))
      .withFragmentShader(AnimeCosmetics.id("core/fx"))
      .withShaderDefine("ALPHA_CUTOUT", 0.06F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
      .withCull(false)
      .build();
   private static final RenderPipeline AURA_ADD = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/fx_aura_add"))
      .withFragmentShader(AnimeCosmetics.id("core/fx_add"))
      .withShaderDefine("ALPHA_CUTOUT", 0.004F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withCull(false)
      .build();
   private static final RenderPipeline GUI_SOFT = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/fx_gui_soft"))
      .withFragmentShader(AnimeCosmetics.id("core/fx_add"))
      .withShaderDefine("ALPHA_CUTOUT", 0.002F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withCull(false)
      .build();
   private static final RenderPipeline FIRE_IRIS = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/fx_fire_iris"))
      .withShaderDefine("ALPHA_CUTOUT", 0.004F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .build();
   private static final RenderPipeline DEPTH_ONLY = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/fx_depth_only"))
      .withShaderDefine("ALPHA_CUTOUT", 0.5F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.DEFAULT.format(), 0))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
      .withCull(false)
      .build();
   private static final Map<Identifier, RenderType> AURA_ADD_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> VANILLA_EYES = new HashMap<>();
   private static final Map<Identifier, RenderType> GUI_SOFT_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> LAYERED_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> DEPTH_ONLY_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> NOSORT_NOCULL_TYPES = new HashMap<>();
   private static Method irisInstance;
   private static Method irisInUse;
   private static boolean irisChecked;
   private static long shadersCheckedNs;
   private static boolean shadersCached;
   private static boolean shadersEverChecked;

   private FxTypes() {
   }

   public static RenderType softNoSortNoCull(Identifier texture) {
      return shadersOn()
         ? null
         : NOSORT_NOCULL_TYPES.computeIfAbsent(
            texture,
            t -> LateGlow.late(
               RenderTypeAccessor.animecosmetics$create(
                  "animecosmetics_fx_soft_nosort_nocull",
                  RenderSetup.builder(SOFT_NOCULL).withTexture("Sampler0", t).useLightmap().useOverlay().createRenderSetup()
               )
            )
         );
   }

   private static RenderType custom(String name, RenderPipeline pipeline, Identifier texture) {
      return LateGlow.late(
         RenderTypeAccessor.animecosmetics$create(
            name, RenderSetup.builder(pipeline).withTexture("Sampler0", texture).useLightmap().useOverlay().createRenderSetup()
         )
      );
   }

   public static RenderType glowAdd(Identifier texture) {
      return shadersOn()
         ? VANILLA_EYES.computeIfAbsent(texture, t -> LateGlow.late(RenderTypes.eyes(t)))
         : AURA_ADD_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_fx_aura_add", AURA_ADD, t));
   }

   public static RenderType guiSoft(Identifier texture) {
      return GUI_SOFT_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_fx_gui_soft", GUI_SOFT, t));
   }

   public static RenderType layered(Identifier texture) {
      return shadersOn()
         ? RenderTypes.entityTranslucent(texture)
         : LAYERED_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_fx_layered", FIRE_IRIS, t));
   }

   public static RenderType depthOnly(Identifier texture) {
      return shadersOn() ? null : DEPTH_ONLY_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_fx_depth_only", DEPTH_ONLY, t));
   }

   public static boolean noCull(RenderType t) {
      return t != null && AURA_ADD_TYPES.containsValue(t);
   }

   public static RenderType skyType(Identifier texture) {
      RenderType custom = softNoSortNoCull(texture);
      return custom != null ? custom : RenderTypes.entityTranslucent(texture);
   }

   public static boolean shadersOn() {
      long now = System.nanoTime();
      if (shadersEverChecked && now - shadersCheckedNs < 100000000L) {
         return shadersCached;
      } else {
         shadersEverChecked = true;
         shadersCheckedNs = now;
         shadersCached = shadersOnUncached();
         return shadersCached;
      }
   }

   private static boolean shadersOnUncached() {
      try {
         if (!irisChecked) {
            irisChecked = true;
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisInstance = api.getMethod("getInstance");
            irisInUse = api.getMethod("isShaderPackInUse");
         }

         return irisInstance != null && (Boolean)irisInUse.invoke(irisInstance.invoke(null));
      } catch (Throwable var11) {
         irisInstance = null;
         return false;
      }
   }
}
