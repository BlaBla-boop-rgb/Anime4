package dev.animecosmetics.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import dev.animecosmetics.AnimeCosmetics;
import dev.animecosmetics.mixin.RenderPipelinesAccessor;
import dev.animecosmetics.mixin.RenderTypeAccessor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Render types for the rift of the "Riss-Auge" (ported from Ronova's FxTypes).
 * Kept apart from {@link FxTypes} so the existing cosmetics render exactly as before.
 */
public final class RiftTypes {
   private static final RenderPipeline SOFT = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/rift_soft"))
      .withFragmentShader(AnimeCosmetics.id("core/fx"))
      .withShaderDefine("ALPHA_CUTOUT", 0.06F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
      .build();
   private static final RenderPipeline GLOW_ADD = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/rift_glow_add"))
      .withShaderDefine("ALPHA_CUTOUT", 0.004F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .build();
   private static final RenderPipeline FIRE_IRIS = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/rift_fire_iris"))
      .withShaderDefine("ALPHA_CUTOUT", 0.004F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .build();
   private static final RenderPipeline VOL_MUL = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/rift_vol_mul"))
      .withFragmentShader(AnimeCosmetics.id("core/fx_add"))
      .withShaderDefine("ALPHA_CUTOUT", 0.004F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.SRC_COLOR)))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withCull(false)
      .build();
   private static final RenderPipeline VOL_IRIS = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/rift_vol_iris"))
      .withShaderDefine("ALPHA_CUTOUT", 0.004F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .withCull(false)
      .build();
   private static final RenderPipeline SKY_DEPTH = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$entitySnippet()})
      .withLocation(AnimeCosmetics.id("pipeline/rift_sky_depth"))
      .withFragmentShader(AnimeCosmetics.id("core/fx_add"))
      .withShaderDefine("ALPHA_CUTOUT", 0.02F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
      .withCull(false)
      .build();
   private static final RenderPipeline SKY_IRIS = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.animecosmetics$globalsSnippet()})
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withLocation(AnimeCosmetics.id("pipeline/rift_sky_iris"))
      .withVertexShader("core/position_tex_color")
      .withFragmentShader("core/position_tex_color")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
      .withShaderDefine("ALPHA_CUTOUT", 0.05F)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withCull(false)
      .build();
   private static final Map<Identifier, RenderType> SOFT_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> VANILLA = new HashMap<>();
   private static final Map<Identifier, RenderType> NOSORT_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> IRIS_NOSORT_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> IRIS_GLOW_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> IRIS_FIRE_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> VOL_MUL_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> VOL_IRIS_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> SKY_DEPTH_TYPES = new HashMap<>();
   private static final Map<Identifier, RenderType> SKY_IRIS_TYPES = new HashMap<>();
   private static boolean irisTried;
   private static boolean irisOk;
   private static boolean skyIrisOk;

   private RiftTypes() {
   }

   private static RenderType custom(String name, RenderPipeline pipeline, Identifier texture) {
      return LateGlow.late(
         RenderTypeAccessor.animecosmetics$create(
            name, RenderSetup.builder(pipeline).withTexture("Sampler0", texture).useLightmap().useOverlay().createRenderSetup()
         )
      );
   }

   public static RenderType soft(Identifier texture) {
      return FxTypes.shadersOn()
         ? VANILLA.computeIfAbsent(texture, t -> LateGlow.late(RenderTypes.entityTranslucentCull(t)))
         : SOFT_TYPES.computeIfAbsent(
            texture,
            t -> LateGlow.late(
               RenderTypeAccessor.animecosmetics$create(
                  "animecosmetics_rift_soft",
                  RenderSetup.builder(SOFT)
                     .setOitPipelines(RenderPipelines.OIT_ENTITY_CULL)
                     .withTexture("Sampler0", t)
                     .useLightmap()
                     .useOverlay()
                     .sortOnUpload()
                     .createRenderSetup()
               )
            )
         );
   }

   public static RenderType softNoSort(Identifier texture) {
      return FxTypes.shadersOn()
         ? IRIS_NOSORT_TYPES.computeIfAbsent(
            texture,
            t -> LateGlow.late(
               RenderTypeAccessor.animecosmetics$create(
                  "animecosmetics_rift_iris_nosort",
                  RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_CULL)
                     .setOitPipelines(RenderPipelines.OIT_ENTITY_CULL)
                     .withTexture("Sampler0", t)
                     .useLightmap()
                     .useOverlay()
                     .affectsCrumbling()
                     .createRenderSetup()
               )
            )
         )
         : NOSORT_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_rift_soft_nosort", SOFT, t));
   }

   /** Without shaders: the no-cull type from {@link FxTypes}; with shaders: null (caller falls back to {@link #softNoSort}). */
   public static RenderType softNoSortNoCull(Identifier texture) {
      return FxTypes.softNoSortNoCull(texture);
   }

   public static RenderType glowIris(Identifier texture) {
      return !bindIris() ? soft(texture) : IRIS_GLOW_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_rift_glow_add", GLOW_ADD, t));
   }

   public static RenderType fireIris(Identifier texture) {
      return !bindIris() ? soft(texture) : IRIS_FIRE_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_rift_fire_iris", FIRE_IRIS, t));
   }

   public static RenderType volumeMul(Identifier texture) {
      return VOL_MUL_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_rift_vol_mul", VOL_MUL, t));
   }

   public static RenderType volumeIris(Identifier texture) {
      return !bindIris() ? soft(texture) : VOL_IRIS_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_rift_vol_iris", VOL_IRIS, t));
   }

   public static RenderType skyDepthNoSort(Identifier texture) {
      return SKY_DEPTH_TYPES.computeIfAbsent(texture, t -> custom("animecosmetics_rift_sky_depth", SKY_DEPTH, t));
   }

   public static RenderType skyIris(Identifier texture) {
      return bindIris() && skyIrisOk
         ? SKY_IRIS_TYPES.computeIfAbsent(
            texture,
            t -> LateGlow.late(
               RenderTypeAccessor.animecosmetics$create(
                  "animecosmetics_rift_sky_iris", RenderSetup.builder(SKY_IRIS).withTexture("Sampler0", t).createRenderSetup()
               )
            )
         )
         : null;
   }

   private static boolean bindIris() {
      if (irisTried) {
         return irisOk;
      } else {
         irisTried = true;

         try {
            Class<?> ip = Class.forName("net.irisshaders.iris.pipeline.IrisPipelines");
            Class<?> keys = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderKey");
            Object eyes = Enum.valueOf(keys.asSubclass(Enum.class), "ENTITIES_EYES");
            Method assign = ip.getMethod("assignPipeline", RenderPipeline.class, keys);
            assign.invoke(null, GLOW_ADD, eyes);
            assign.invoke(null, FIRE_IRIS, eyes);
            assign.invoke(null, VOL_IRIS, eyes);
            irisOk = true;

            try {
               assign.invoke(null, SKY_IRIS, Enum.valueOf(keys.asSubclass(Enum.class), "SKY_TEXTURED_COLOR"));
               skyIrisOk = true;
            } catch (Throwable e) {
               AnimeCosmetics.LOGGER.warn("iris sky pipeline bind failed: {}", e.toString());
            }
         } catch (Throwable e) {
            AnimeCosmetics.LOGGER.warn("iris pipeline bind failed, falling back to vanilla types under shaders: {}", e.toString());
         }

         return irisOk;
      }
   }
}
